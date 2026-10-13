package com.fiscal.servlet;

import com.fiscal.dao.AppUserDAO;
import com.fiscal.filter.AuthenticationFilter;
import com.fiscal.model.ApiResponse;
import com.fiscal.model.AppUser;
import com.fiscal.util.JsonUtil;
import com.fiscal.util.PasswordUtil;
import com.fiscal.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Administrative Servlet for Operator Account Governance.
 * Enforces role-based authorization: Only users with 'ADMIN' role may access.
 */
@WebServlet(urlPatterns = {"/api/users", "/api/users/*"})
public class UserServlet extends HttpServlet {

    private final AppUserDAO userDAO = new AppUserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!checkAdmin(req, resp)) return;

        try {
            List<AppUser> users = userDAO.findAll();
            for (AppUser u : users) {
                u.setPasswordHash(null);
                u.setSalt(null);
            }
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok(users)));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Failed to list users: " + e.getMessage())));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!checkAdmin(req, resp)) return;

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> body = JsonUtil.fromJson(req.getReader(), Map.class);
            if (body == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Missing payload")));
                return;
            }

            String username = body.get("username");
            String password = body.get("password");
            String fullName = body.get("fullName");
            String email = body.get("email");
            String role = body.get("role");

            if (!ValidationUtil.isNonEmpty(username) || !ValidationUtil.isNonEmpty(password) ||
                !ValidationUtil.isNonEmpty(fullName) || !ValidationUtil.isValidEmail(email)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("All user fields are required, and email must be valid")));
                return;
            }

            if (userDAO.findByUsername(username) != null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Username already exists")));
                return;
            }

            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hashPassword(password, salt);

            AppUser newUser = new AppUser();
            newUser.setUsername(username.trim());
            newUser.setPasswordHash(hash);
            newUser.setSalt(salt);
            newUser.setFullName(fullName.trim());
            newUser.setEmail(email.trim());
            newUser.setRole(role != null ? role.trim().toUpperCase() : "OFFICER");
            newUser.setStatus("ACTIVE");

            boolean created = userDAO.create(newUser);
            if (created) {
                newUser.setPasswordHash(null);
                newUser.setSalt(null);
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok("User account created successfully", newUser)));
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Failed to create user account")));
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Server error: " + e.getMessage())));
        }
    }

    private boolean checkAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        AppUser user = (session != null) ? (AppUser) session.getAttribute(AuthenticationFilter.SESSION_USER_KEY) : null;
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Forbidden: Administrative privilege required")));
            return false;
        }
        return true;
    }
}
