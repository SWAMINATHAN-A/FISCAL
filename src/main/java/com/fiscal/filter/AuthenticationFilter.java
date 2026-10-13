package com.fiscal.filter;

import com.fiscal.model.ApiResponse;
import com.fiscal.model.AppUser;
import com.fiscal.util.JsonUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Filter ensuring that all protected /api/* endpoints require an active server-side session.
 */
@WebFilter("/api/*")
public class AuthenticationFilter implements Filter {

    public static final String SESSION_USER_KEY = "USER_SESSION";

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();
        String contextPath = req.getContextPath();
        String subPath = path.substring(contextPath.length());

        // Allow public authentication endpoints
        if (subPath.equals("/api/auth/login") || subPath.equals("/api/auth/status") || subPath.equals("/api/auth/logout")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        AppUser currentUser = (session != null) ? (AppUser) session.getAttribute(SESSION_USER_KEY) : null;

        if (currentUser == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write(JsonUtil.toJson(ApiResponse.error("Session expired or authentication required. Please sign in.")));
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
