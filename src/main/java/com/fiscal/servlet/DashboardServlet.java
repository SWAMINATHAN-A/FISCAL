package com.fiscal.servlet;

import com.fiscal.model.ApiResponse;
import com.fiscal.model.DashboardSummary;
import com.fiscal.service.DashboardService;
import com.fiscal.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Servlet delivering aggregate financial dashboard statistics.
 */
@WebServlet("/api/dashboard/summary")
public class DashboardServlet extends HttpServlet {

    private final DashboardService dashboardService = new DashboardService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            DashboardSummary summary = dashboardService.getDashboardSummary();
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok(summary)));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Failed to generate dashboard summary: " + e.getMessage())));
        }
    }
}
