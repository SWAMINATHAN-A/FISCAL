package com.fiscal.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Authoritative Database Configuration and Connection Factory for FISCAL.
 * Supports connection pooling parameters, environment overrides, fail-fast detection,
 * and silent in-memory fallback when Supabase is paused or unreachable.
 */
public final class DatabaseConfig {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());
    private static final Properties PROPERTIES = new Properties();
    private static boolean driverLoaded = false;
    private static volatile Boolean dbAvailable = null;

    static {
        loadProperties();
    }

    private DatabaseConfig() {}

    private static void loadProperties() {
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                PROPERTIES.load(input);
                LOGGER.info("Loaded db.properties successfully.");
            } else {
                LOGGER.warning("db.properties not found on classpath, using defaults.");
                setDefaultProperties();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load db.properties, applying defaults", e);
            setDefaultProperties();
        }

        java.util.Map<String, String> dotEnv = loadDotEnv();

        // Expand any ${VAR} placeholders in properties
        for (String key : PROPERTIES.stringPropertyNames()) {
            String val = PROPERTIES.getProperty(key);
            if (val != null && val.startsWith("${") && val.endsWith("}")) {
                String envVar = val.substring(2, val.length() - 1);
                String envVal = getEnvOrDotEnv(envVar, dotEnv);
                if (envVal != null) {
                    PROPERTIES.setProperty(key, envVal);
                }
            }
        }

        // Allow System environment variables or .env to override properties
        String envUrl = getEnvOrDotEnv("DATABASE_URL", dotEnv);
        if (envUrl == null) envUrl = getEnvOrDotEnv("DB_URL", dotEnv);
        if (envUrl == null) envUrl = getEnvOrDotEnv("ORACLE_URL", dotEnv);
        if (envUrl != null) {
            PROPERTIES.setProperty("db.url", envUrl);
        }

        String envUser = getEnvOrDotEnv("DB_USER", dotEnv);
        if (envUser == null) envUser = getEnvOrDotEnv("ORACLE_USER", dotEnv);
        if (envUser != null) {
            PROPERTIES.setProperty("db.user", envUser);
        }

        String envPass = getEnvOrDotEnv("DB_PASSWORD", dotEnv);
        if (envPass == null) envPass = getEnvOrDotEnv("ORACLE_PASSWORD", dotEnv);
        if (envPass != null) {
            PROPERTIES.setProperty("db.password", envPass);
        }

        String envFallback = getEnvOrDotEnv("ALLOW_FALLBACK", dotEnv);
        if (envFallback == null) envFallback = getEnvOrDotEnv("db.allowFallback", dotEnv);
        if (envFallback != null) {
            PROPERTIES.setProperty("db.allowFallback", envFallback);
        }

        String appMode = getEnvOrDotEnv("APP_MODE", dotEnv);
        if ("MOCK".equalsIgnoreCase(appMode) || "OFFLINE".equalsIgnoreCase(appMode)) {
            dbAvailable = false;
            LOGGER.info("APP_MODE set to " + appMode + ": Direct in-memory mode activated.");
        }

        ensureDriverLoaded();
    }

    private static String getEnvOrDotEnv(String key, java.util.Map<String, String> dotEnv) {
        String val = System.getenv(key);
        if (val == null || val.trim().isEmpty()) {
            val = dotEnv.get(key);
        }
        return (val != null && !val.trim().isEmpty()) ? val.trim() : null;
    }

    private static java.util.Map<String, String> loadDotEnv() {
        java.util.Map<String, String> envMap = new java.util.HashMap<>();
        java.io.File[] candidateFiles = new java.io.File[] {
            new java.io.File(".env"),
            new java.io.File(System.getProperty("user.dir"), ".env")
        };
        for (java.io.File f : candidateFiles) {
            if (f.exists() && f.isFile()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(f))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int eqIdx = line.indexOf('=');
                        if (eqIdx > 0) {
                            String k = line.substring(0, eqIdx).trim();
                            String v = line.substring(eqIdx + 1).trim();
                            if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                                v = v.substring(1, v.length() - 1);
                            }
                            envMap.put(k, v);
                        }
                    }
                    LOGGER.info("Loaded configuration properties from local .env");
                    break;
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Failed reading .env file: " + f.getAbsolutePath(), ex);
                }
            }
        }
        return envMap;
    }

    private static void setDefaultProperties() {
        PROPERTIES.setProperty("db.driver", "org.postgresql.Driver");
        PROPERTIES.setProperty("db.url", "jdbc:postgresql://localhost:5432/postgres");
        PROPERTIES.setProperty("db.user", "postgres");
        PROPERTIES.setProperty("db.password", "postgres");
        PROPERTIES.setProperty("db.allowFallback", "true");
    }

    private static synchronized void ensureDriverLoaded() {
        if (driverLoaded) return;
        String url = PROPERTIES.getProperty("db.url", "");
        String defaultDriver = url.startsWith("jdbc:postgresql:") ? "org.postgresql.Driver" : "oracle.jdbc.OracleDriver";
        String driver = PROPERTIES.getProperty("db.driver", defaultDriver);
        try {
            Class.forName(driver);
            driverLoaded = true;
            LOGGER.info("JDBC Driver loaded: " + driver);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "JDBC driver not found on classpath: " + driver, e);
        }
    }

    /**
     * Obtains a live JDBC Connection with fail-fast timeouts (3s connection timeout)
     * so paused Supabase instances are detected immediately without stalling.
     * @throws SQLException if connection cannot be established.
     */
    public static Connection getConnection() throws SQLException {
        ensureDriverLoaded();
        String url = PROPERTIES.getProperty("db.url");
        String user = PROPERTIES.getProperty("db.user");
        String pass = PROPERTIES.getProperty("db.password");

        Properties props = new Properties();
        if (user != null) props.setProperty("user", user);
        if (pass != null) props.setProperty("password", pass);

        // Fail-fast timeout for paused or slow cloud databases (3s connect, 5s socket)
        props.setProperty("connectTimeout", "3");
        props.setProperty("socketTimeout", "5");
        props.setProperty("loginTimeout", "3");

        DriverManager.setLoginTimeout(3);
        return DriverManager.getConnection(url, props);
    }

    /**
     * Probes whether the database instance is currently reachable.
     * If unreachable (e.g. Supabase paused), silently switches to resilient in-memory mode.
     */
    public static synchronized boolean isOracleReachable() {
        if (dbAvailable != null) {
            return dbAvailable;
        }
        try (Connection conn = getConnection()) {
            dbAvailable = (conn != null && !conn.isClosed());
            LOGGER.info("Database connection verified successfully (Live mode).");
            return dbAvailable;
        } catch (Exception e) {
            LOGGER.info("Supabase database is paused or unreachable (" + e.getMessage() + "). Auto mode silently activated in-memory fallback.");
            dbAvailable = false;
            return false;
        }
    }

    /**
     * Marks the database as unreachable mid-session so subsequent requests
     * switch instantly to the in-memory store with zero latency.
     */
    public static synchronized void markUnreachable() {
        if (dbAvailable == null || dbAvailable) {
            LOGGER.info("Database connection dropped or unavailable. Silently routing to resilient in-memory store.");
            dbAvailable = false;
        }
    }

    /**
     * Resets probed state to allow re-checking connectivity.
     */
    public static synchronized void resetConnectionState() {
        dbAvailable = null;
    }

    public static String getDbUrl() {
        return PROPERTIES.getProperty("db.url");
    }

    public static String getDbUser() {
        return PROPERTIES.getProperty("db.user");
    }

    public static boolean isFallbackAllowed() {
        String val = PROPERTIES.getProperty("db.allowFallback");
        if (val != null) {
            return !"false".equalsIgnoreCase(val.trim());
        }
        return true;
    }
}
