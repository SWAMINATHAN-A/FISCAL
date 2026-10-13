package com.fiscal.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Authoritative Database Configuration and Connection Factory for Oracle Database.
 * Supports connection pooling parameters, environment overrides, and offline resilience.
 */
public final class DatabaseConfig {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());
    private static final Properties PROPERTIES = new Properties();
    private static boolean driverLoaded = false;
    private static Boolean oracleAvailable = null;

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

        // Expand any ${VAR} placeholders in properties
        for (String key : PROPERTIES.stringPropertyNames()) {
            String val = PROPERTIES.getProperty(key);
            if (val != null && val.startsWith("${") && val.endsWith("}")) {
                String envVar = val.substring(2, val.length() - 1);
                String envVal = System.getenv(envVar);
                if (envVal != null) {
                    PROPERTIES.setProperty(key, envVal);
                }
            }
        }

        // Allow System environment variables to override properties (Postgres, Oracle, Cloud)
        String envUrl = System.getenv("DATABASE_URL");
        if (envUrl == null || envUrl.trim().isEmpty()) {
            envUrl = System.getenv("DB_URL");
        }
        if (envUrl == null || envUrl.trim().isEmpty()) {
            envUrl = System.getenv("ORACLE_URL");
        }
        if (envUrl != null && !envUrl.trim().isEmpty()) {
            PROPERTIES.setProperty("db.url", envUrl.trim());
        }

        String envUser = System.getenv("DB_USER");
        if (envUser == null || envUser.trim().isEmpty()) {
            envUser = System.getenv("ORACLE_USER");
        }
        if (envUser != null && !envUser.trim().isEmpty()) {
            PROPERTIES.setProperty("db.user", envUser.trim());
        }

        String envPass = System.getenv("DB_PASSWORD");
        if (envPass == null || envPass.trim().isEmpty()) {
            envPass = System.getenv("ORACLE_PASSWORD");
        }
        if (envPass != null && !envPass.trim().isEmpty()) {
            PROPERTIES.setProperty("db.password", envPass.trim());
        }

        ensureDriverLoaded();
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
     * Obtains a live JDBC Connection to Oracle Database.
     * @throws SQLException if connection cannot be established.
     */
    public static Connection getConnection() throws SQLException {
        ensureDriverLoaded();
        String url = PROPERTIES.getProperty("db.url");
        String user = PROPERTIES.getProperty("db.user");
        String pass = PROPERTIES.getProperty("db.password");
        
        DriverManager.setLoginTimeout(3); // 3-second timeout for responsive detection
        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * Probes whether the target Oracle instance is currently reachable.
     */
    public static synchronized boolean isOracleReachable() {
        if (oracleAvailable != null) {
            return oracleAvailable;
        }
        try (Connection conn = getConnection()) {
            oracleAvailable = (conn != null && !conn.isClosed());
            LOGGER.info("Oracle Database connection established successfully.");
            return oracleAvailable;
        } catch (Exception e) {
            LOGGER.warning("Oracle Database is not reachable at " + PROPERTIES.getProperty("db.url") 
                    + " (" + e.getMessage() + "). Offline resilience activated.");
            oracleAvailable = false;
            return false;
        }
    }

    /**
     * Resets probed state to allow re-checking connectivity.
     */
    public static synchronized void resetConnectionState() {
        oracleAvailable = null;
    }

    public static String getDbUrl() {
        return PROPERTIES.getProperty("db.url");
    }

    public static String getDbUser() {
        return PROPERTIES.getProperty("db.user");
    }

    public static boolean isFallbackAllowed() {
        return "true".equalsIgnoreCase(PROPERTIES.getProperty("db.allowFallback", "true"));
    }
}
