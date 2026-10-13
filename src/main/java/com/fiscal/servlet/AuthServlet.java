package com.fiscal.servlet;

import com.fiscal.filter.AuthenticationFilter;
import com.fiscal.model.ApiResponse;
import com.fiscal.model.AppUser;
import com.fiscal.service.AuthService;
import com.fiscal.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

/**
 * Servlet handling login, session establishment, session rotation, logout, and identity inspection.
 */
@WebServlet(urlPatterns = {"/api/auth/login", "/api/auth/logout", "/api/auth/me", "/api/auth/status"})
public class AuthServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();

        if ("/api/auth/login".equals(path)) {
            handleLogin(req, resp);
        } else if ("/api/auth/logout".equals(path)) {
            handleLogout(req, resp);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Endpoint not found")));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();

        if ("/api/auth/me".equals(path) || "/api/auth/status".equals(path)) {
            handleCheckStatus(req, resp);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Endpoint not found")));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Map<String, String> payload = JsonUtil.fromJson(req.getReader(), Map.class);
            if (payload == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Missing login credentials")));
                return;
            }

            String username = payload.get("username");
            String password = payload.get("password");

            if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Both username and password are required")));
                return;
            }

            AppUser user = authService.authenticate(username, password);
            if (user == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Invalid credentials or account inactive.")));
                return;
            }

            // Invalidate any pre-existing session to prevent session fixation attacks
            HttpSession oldSession = req.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }

            // Create new pristine session and store authenticated principal
            HttpSession newSession = req.getSession(true);
            newSession.setMaxInactiveInterval(1800); // 30 minutes
            newSession.setAttribute(AuthenticationFilter.SESSION_USER_KEY, user);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok("Login successful", sanitizeUser(user))));

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("An error occurred during authentication")));
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.removeAttribute(AuthenticationFilter.SESSION_USER_KEY);
            session.invalidate();
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok("Logged out successfully", null)));
    }

    private void handleCheckStatus(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        AppUser user = (session != null) ? (AppUser) session.getAttribute(AuthenticationFilter.SESSION_USER_KEY) : null;

        if (user != null) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok("Session active", sanitizeUser(user))));
        } else {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(new ApiResponse<>(false, "Unauthenticated", null)));
        }
    }

    private AppUser sanitizeUser(AppUser src) {
        AppUser u = new AppUser(src.getUserId(), src.getUsername(), src.getFullName(), src.getEmail(), src.getRole(), src.getStatus(), src.getCreatedAt());
        u.setPasswordHash(null);
        u.setSalt(null);
        return u;
    }
}
