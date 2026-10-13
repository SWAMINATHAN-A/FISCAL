package com.fiscal.server;

import com.fiscal.config.DatabaseConfig;
import com.fiscal.dao.AppUserDAO;
import com.fiscal.dao.TaxRecordDAO;
import com.fiscal.model.*;
import com.fiscal.service.*;
import com.fiscal.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Built-in Embedded Development Server for FISCAL.
 * Enables zero-setup, instant execution of the complete Custom Tax Management System
 * on port 8080 (or PORT env var) without requiring external Tomcat installation.
 */
public class FiscalDevServer {

    private static final Logger LOGGER = Logger.getLogger(FiscalDevServer.class.getName());
    private static final int DEFAULT_PORT = 8080;

    private static final AuthService AUTH_SERVICE = new AuthService();
    private static final DashboardService DASHBOARD_SERVICE = new DashboardService();
    private static final TaxpayerService TAXPAYER_SERVICE = new TaxpayerService();
    private static final TaxTypeService TAX_TYPE_SERVICE = new TaxTypeService();
    private static final TaxPaymentService PAYMENT_SERVICE = new TaxPaymentService();
    private static final TaxCalculationService CALC_SERVICE = new TaxCalculationService();
    private static final AppUserDAO USER_DAO = new AppUserDAO();
    private static final TaxRecordDAO TAX_RECORD_DAO = new TaxRecordDAO();

    // In-memory session tracking for standalone HTTP server
    private static final Map<String, AppUser> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(16));

        LOGGER.info("===============================================================");
        LOGGER.info("  FISCAL — CUSTOM TAX MANAGEMENT SYSTEM");
        LOGGER.info("  Tagline: Clarity in every calculation.");
        LOGGER.info("===============================================================");
        LOGGER.info("Starting FISCAL Embedded Server on http://localhost:" + port);
        LOGGER.info("Checking Oracle Database connection: " + DatabaseConfig.getDbUrl());
        boolean dbOk = DatabaseConfig.isOracleReachable();
        LOGGER.info("Database Mode: " + (dbOk ? "LIVE ORACLE DATABASE" : "OFFLINE RESILIENT REPOSITORY (03_seed_data.sql)"));

        // Single root dispatcher handling APIs and static webapp assets
        server.createContext("/", new MasterHandler());

        server.start();
        LOGGER.info("FISCAL is ready at http://localhost:" + port);
        LOGGER.info("Demo accounts: admin / Admin@123  |  officer / Officer@123");
        LOGGER.info("===============================================================");
    }

    static class MasterHandler implements HttpHandler {
        private final Path webappRoot;

        public MasterHandler() {
            // Find webapp directory relative to working dir or target
            Path candidate = Paths.get("src/main/webapp").toAbsolutePath();
            if (Files.exists(candidate)) {
                this.webappRoot = candidate;
            } else {
                this.webappRoot = Paths.get("target/fiscal").toAbsolutePath();
            }
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod().toUpperCase();

            // Set standard security headers
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("X-Frame-Options", "SAMEORIGIN");

            // Handle API routes
            if (path.startsWith("/api/")) {
                handleApi(exchange, path, method);
                return;
            }

            // Handle static files
            handleStatic(exchange, path);
        }

        private void handleApi(HttpExchange exchange, String path, String method) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");

            // Extract session
            String sessionToken = extractSessionCookie(exchange);
            AppUser currentUser = (sessionToken != null) ? ACTIVE_SESSIONS.get(sessionToken) : null;

            // Authentication bypass for login and status
            if (path.equals("/api/auth/login") && "POST".equals(method)) {
                handleLogin(exchange);
                return;
            }
            if (path.equals("/api/auth/status") && "GET".equals(method)) {
                handleAuthStatus(exchange, currentUser);
                return;
            }
            if (path.equals("/api/auth/logout") && "POST".equals(method)) {
                handleLogout(exchange, sessionToken);
                return;
            }

            // Require session for all other /api/* endpoints
            if (currentUser == null) {
                sendJson(exchange, 401, ApiResponse.error("Authentication required. Please sign in."));
                return;
            }

            try {
                // 1. Auth Me
                if (path.equals("/api/auth/me") && "GET".equals(method)) {
                    sendJson(exchange, 200, ApiResponse.ok(sanitize(currentUser)));
                    return;
                }

                // 2. Dashboard
                if (path.equals("/api/dashboard/summary") && "GET".equals(method)) {
                    sendJson(exchange, 200, ApiResponse.ok(DASHBOARD_SERVICE.getDashboardSummary()));
                    return;
                }

                // 3. Taxpayers
                if (path.equals("/api/taxpayers/cities") && "GET".equals(method)) {
                    sendJson(exchange, 200, ApiResponse.ok(TAXPAYER_SERVICE.listCities()));
                    return;
                }
                if (path.equals("/api/taxpayers") && "GET".equals(method)) {
                    Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
                    List<Taxpayer> list = TAXPAYER_SERVICE.listTaxpayers(q.get("search"), q.get("city"), q.get("sort"));
                    sendJson(exchange, 200, ApiResponse.ok(list));
                    return;
                }
                if (path.startsWith("/api/taxpayers/") && "GET".equals(method)) {
                    Long id = parseId(path.substring("/api/taxpayers/".length()));
                    Taxpayer t = TAXPAYER_SERVICE.getTaxpayer(id);
                    if (t != null) sendJson(exchange, 200, ApiResponse.ok(t));
                    else sendJson(exchange, 404, ApiResponse.error("Taxpayer not found"));
                    return;
                }
                if (path.equals("/api/taxpayers") && "POST".equals(method)) {
                    Taxpayer payload = JsonUtil.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), Taxpayer.class);
                    Taxpayer created = TAXPAYER_SERVICE.createTaxpayer(payload);
                    sendJson(exchange, 201, ApiResponse.ok("Taxpayer registered successfully", created));
                    return;
                }
                if (path.startsWith("/api/taxpayers/") && "PUT".equals(method)) {
                    Long id = parseId(path.substring("/api/taxpayers/".length()));
                    Taxpayer payload = JsonUtil.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), Taxpayer.class);
                    payload.setTaxpayerId(id);
                    Taxpayer updated = TAXPAYER_SERVICE.updateTaxpayer(payload);
                    sendJson(exchange, 200, ApiResponse.ok("Taxpayer updated successfully", updated));
                    return;
                }
                if (path.startsWith("/api/taxpayers/") && "DELETE".equals(method)) {
                    Long id = parseId(path.substring("/api/taxpayers/".length()));
                    boolean deleted = TAXPAYER_SERVICE.deleteTaxpayer(id);
                    if (deleted) sendJson(exchange, 200, ApiResponse.ok("Taxpayer removed", null));
                    else sendJson(exchange, 404, ApiResponse.error("Taxpayer not found"));
                    return;
                }

                // 4. Tax Types
                if (path.equals("/api/tax-types") && "GET".equals(method)) {
                    Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
                    sendJson(exchange, 200, ApiResponse.ok(TAX_TYPE_SERVICE.listTaxTypes(q.get("search"))));
                    return;
                }
                if (path.startsWith("/api/tax-types/") && "GET".equals(method)) {
                    Long id = parseId(path.substring("/api/tax-types/".length()));
                    TaxType t = TAX_TYPE_SERVICE.getTaxType(id);
                    if (t != null) sendJson(exchange, 200, ApiResponse.ok(t));
                    else sendJson(exchange, 404, ApiResponse.error("Tax type not found"));
                    return;
                }
                if (path.equals("/api/tax-types") && "POST".equals(method)) {
                    TaxType payload = JsonUtil.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), TaxType.class);
                    TaxType created = TAX_TYPE_SERVICE.createTaxType(payload);
                    sendJson(exchange, 201, ApiResponse.ok("Tax category created", created));
                    return;
                }
                if (path.startsWith("/api/tax-types/") && "PUT".equals(method)) {
                    Long id = parseId(path.substring("/api/tax-types/".length()));
                    TaxType payload = JsonUtil.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), TaxType.class);
                    payload.setTaxId(id);
                    TaxType updated = TAX_TYPE_SERVICE.updateTaxType(payload);
                    sendJson(exchange, 200, ApiResponse.ok("Tax category updated", updated));
                    return;
                }
                if (path.startsWith("/api/tax-types/") && "DELETE".equals(method)) {
                    Long id = parseId(path.substring("/api/tax-types/".length()));
                    boolean deleted = TAX_TYPE_SERVICE.deleteTaxType(id);
                    if (deleted) sendJson(exchange, 200, ApiResponse.ok("Tax category removed", null));
                    else sendJson(exchange, 404, ApiResponse.error("Tax category not found"));
                    return;
                }

                // 5. Tax Calculation & Assessments
                if (path.equals("/api/tax/calculate") && "POST".equals(method)) {
                    TaxCalculationRequest req = JsonUtil.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), TaxCalculationRequest.class);
                    TaxCalculationResponse res = CALC_SERVICE.calculate(req);
                    sendJson(exchange, 200, ApiResponse.ok("Calculation complete", res));
                    return;
                }
                if (path.equals("/api/tax-records") && "GET".equals(method)) {
                    Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
                    Long tpId = (q.get("taxpayerId") != null && !q.get("taxpayerId").isEmpty()) ? Long.parseLong(q.get("taxpayerId")) : null;
                    Long ttId = (q.get("taxTypeId") != null && !q.get("taxTypeId").isEmpty()) ? Long.parseLong(q.get("taxTypeId")) : null;
                    List<TaxRecord> records = TAX_RECORD_DAO.findAll(q.get("search"), tpId, ttId, q.get("status"));
                    sendJson(exchange, 200, ApiResponse.ok(records));
                    return;
                }

                // 6. Payments
                if (path.equals("/api/payments") && "GET".equals(method)) {
                    Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
                    Long tpId = (q.get("taxpayerId") != null && !q.get("taxpayerId").isEmpty()) ? Long.parseLong(q.get("taxpayerId")) : null;
                    Long ttId = (q.get("taxId") != null && !q.get("taxId").isEmpty()) ? Long.parseLong(q.get("taxId")) : null;
                    List<TaxPayment> list = PAYMENT_SERVICE.listPayments(q.get("search"), tpId, ttId, q.get("status"), q.get("sort"));
                    sendJson(exchange, 200, ApiResponse.ok(list));
                    return;
                }
                if (path.startsWith("/api/payments/") && path.endsWith("/status") && "POST".equals(method)) {
                    String idStr = path.substring("/api/payments/".length(), path.lastIndexOf("/status"));
                    Long id = Long.parseLong(idStr);
                    @SuppressWarnings("unchecked")
                    Map<String, String> body = JsonUtil.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), Map.class);
                    TaxPayment updated = PAYMENT_SERVICE.updatePaymentStatus(id, body.get("status"), body.get("remarks"));
                    sendJson(exchange, 200, ApiResponse.ok("Status updated", updated));
                    return;
                }
                if (path.startsWith("/api/payments/") && "GET".equals(method)) {
                    Long id = parseId(path.substring("/api/payments/".length()));
                    TaxPayment p = PAYMENT_SERVICE.getPayment(id);
                    if (p != null) sendJson(exchange, 200, ApiResponse.ok(p));
                    else sendJson(exchange, 404, ApiResponse.error("Payment not found"));
                    return;
                }
                if (path.equals("/api/payments") && "POST".equals(method)) {
                    TaxPayment payload = JsonUtil.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), TaxPayment.class);
                    TaxPayment recorded = PAYMENT_SERVICE.recordPayment(payload);
                    sendJson(exchange, 201, ApiResponse.ok("Payment committed to ledger", recorded));
                    return;
                }
                if (path.startsWith("/api/payments/") && "DELETE".equals(method)) {
                    Long id = parseId(path.substring("/api/payments/".length()));
                    boolean deleted = PAYMENT_SERVICE.deletePayment(id);
                    if (deleted) sendJson(exchange, 200, ApiResponse.ok("Payment record removed", null));
                    else sendJson(exchange, 404, ApiResponse.error("Payment not found"));
                    return;
                }

                // 7. User Management (Admin Only)
                if (path.equals("/api/users") && "GET".equals(method)) {
                    if (!"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
                        sendJson(exchange, 403, ApiResponse.error("Administrative privilege required"));
                        return;
                    }
                    List<AppUser> users = USER_DAO.findAll();
                    users.forEach(u -> { u.setPasswordHash(null); u.setSalt(null); });
                    sendJson(exchange, 200, ApiResponse.ok(users));
                    return;
                }

                sendJson(exchange, 404, ApiResponse.error("Endpoint not found: " + path));

            } catch (IllegalArgumentException | IllegalStateException e) {
                sendJson(exchange, 400, ApiResponse.error(e.getMessage()));
            } catch (Exception e) {
                LOGGER.warning("API Error on " + path + ": " + e.getMessage());
                sendJson(exchange, 500, ApiResponse.error("Internal Server Error: " + e.getMessage()));
            }
        }

        private void handleLogin(HttpExchange exchange) throws IOException {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> payload = JsonUtil.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), Map.class);
                if (payload == null || payload.get("username") == null || payload.get("password") == null) {
                    sendJson(exchange, 400, ApiResponse.error("Username and password required"));
                    return;
                }
                AppUser user = AUTH_SERVICE.authenticate(payload.get("username"), payload.get("password"));
                if (user == null) {
                    sendJson(exchange, 401, ApiResponse.error("Invalid credentials or account inactive."));
                    return;
                }

                String token = UUID.randomUUID().toString();
                ACTIVE_SESSIONS.put(token, user);

                exchange.getResponseHeaders().set("Set-Cookie", "FISCALSESSIONID=" + token + "; Path=/; HttpOnly; SameSite=Lax");
                sendJson(exchange, 200, ApiResponse.ok("Login successful", sanitize(user)));
            } catch (Exception e) {
                sendJson(exchange, 500, ApiResponse.error("Login failure: " + e.getMessage()));
            }
        }

        private void handleAuthStatus(HttpExchange exchange, AppUser user) throws IOException {
            if (user != null) {
                sendJson(exchange, 200, ApiResponse.ok("Session active", sanitize(user)));
            } else {
                sendJson(exchange, 200, new ApiResponse<>(false, "Unauthenticated", null));
            }
        }

        private void handleLogout(HttpExchange exchange, String token) throws IOException {
            if (token != null) {
                ACTIVE_SESSIONS.remove(token);
            }
            exchange.getResponseHeaders().set("Set-Cookie", "FISCALSESSIONID=; Path=/; Max-Age=0; HttpOnly");
            sendJson(exchange, 200, ApiResponse.ok("Logged out", null));
        }

        private void handleStatic(HttpExchange exchange, String path) throws IOException {
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }
            // Strip leading slash
            String rel = path.startsWith("/") ? path.substring(1) : path;
            Path file = webappRoot.resolve(rel).normalize();

            if (!file.startsWith(webappRoot) || !Files.exists(file) || Files.isDirectory(file)) {
                // If requesting html route or fallback, serve index.html
                Path fallback = webappRoot.resolve("index.html");
                if (Files.exists(fallback)) {
                    file = fallback;
                } else {
                    String msg = "404 Not Found";
                    exchange.sendResponseHeaders(404, msg.length());
                    exchange.getResponseBody().write(msg.getBytes());
                    exchange.close();
                    return;
                }
            }

            String contentType = determineContentType(file.getFileName().toString());
            exchange.getResponseHeaders().set("Content-Type", contentType);

            byte[] bytes = Files.readAllBytes(file);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        private String determineContentType(String name) {
            String lower = name.toLowerCase();
            if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
            if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
            if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".woff2")) return "font/woff2";
            if (lower.endsWith(".woff")) return "font/woff";
            if (lower.endsWith(".ttf")) return "font/ttf";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".ico")) return "image/x-icon";
            return "application/octet-stream";
        }

        private String extractSessionCookie(HttpExchange exchange) {
            List<String> cookies = exchange.getRequestHeaders().get("Cookie");
            if (cookies == null) return null;
            for (String c : cookies) {
                String[] parts = c.split(";");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("FISCALSESSIONID=")) {
                        return part.substring("FISCALSESSIONID=".length()).trim();
                    }
                }
            }
            return null;
        }

        private void sendJson(HttpExchange exchange, int status, Object data) throws IOException {
            String json = JsonUtil.toJson(data);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> map = new HashMap<>();
            if (query == null || query.isEmpty()) return map;
            for (String pair : query.split("&")) {
                int eq = pair.indexOf("=");
                if (eq > 0) {
                    try {
                        String k = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                        String v = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                        map.put(k, v);
                    } catch (Exception ignored) {}
                }
            }
            return map;
        }

        private Long parseId(String str) {
            try {
                if (str.contains("/")) str = str.substring(0, str.indexOf("/"));
                if (str.contains("?")) str = str.substring(0, str.indexOf("?"));
                return Long.parseLong(str.trim());
            } catch (Exception e) {
                return null;
            }
        }

        private AppUser sanitize(AppUser src) {
            AppUser u = new AppUser(src.getUserId(), src.getUsername(), src.getFullName(), src.getEmail(), src.getRole(), src.getStatus(), src.getCreatedAt());
            u.setPasswordHash(null);
            u.setSalt(null);
            return u;
        }
    }
}
