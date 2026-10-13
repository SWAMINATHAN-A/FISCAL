package com.fiscal.servlet;

import com.fiscal.model.ApiResponse;
import com.fiscal.model.Taxpayer;
import com.fiscal.service.TaxpayerService;
import com.fiscal.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * RESTful Servlet managing TAXPAYER CRUD operations, search, and demographics.
 */
@WebServlet(urlPatterns = {"/api/taxpayers", "/api/taxpayers/*"})
public class TaxpayerServlet extends HttpServlet {

    private final TaxpayerService taxpayerService = new TaxpayerService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // List taxpayers
                String search = req.getParameter("search");
                String city = req.getParameter("city");
                String sort = req.getParameter("sort");
                List<Taxpayer> list = taxpayerService.listTaxpayers(search, city, sort);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok(list)));
                return;
            }

            if ("/cities".equalsIgnoreCase(pathInfo)) {
                List<String> cities = taxpayerService.listCities();
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok(cities)));
                return;
            }

            // Single taxpayer by ID
            Long id = parseId(pathInfo);
            if (id != null) {
                Taxpayer t = taxpayerService.getTaxpayer(id);
                if (t != null) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok(t)));
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Taxpayer with ID " + id + " not found")));
                }
                return;
            }

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Invalid taxpayer endpoint path")));

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Server error: " + e.getMessage())));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Taxpayer payload = JsonUtil.fromJson(req.getReader(), Taxpayer.class);
            if (payload == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Missing taxpayer payload")));
                return;
            }

            Taxpayer created = taxpayerService.createTaxpayer(payload);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok("Taxpayer registered successfully", created)));

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error(e.getMessage())));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Failed to register taxpayer: " + e.getMessage())));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        Long id = parseId(pathInfo);

        if (id == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("A valid taxpayer ID must be specified in the URL")));
            return;
        }

        try {
            Taxpayer payload = JsonUtil.fromJson(req.getReader(), Taxpayer.class);
            if (payload == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Missing update payload")));
                return;
            }

            payload.setTaxpayerId(id);
            Taxpayer updated = taxpayerService.updateTaxpayer(payload);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok("Taxpayer updated successfully", updated)));

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error(e.getMessage())));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Failed to update taxpayer: " + e.getMessage())));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        Long id = parseId(pathInfo);

        if (id == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("A valid taxpayer ID must be specified in the URL")));
            return;
        }

        try {
            boolean deleted = taxpayerService.deleteTaxpayer(id);
            if (deleted) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok("Taxpayer record removed successfully", null)));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Taxpayer with ID " + id + " does not exist")));
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Failed to delete taxpayer: " + e.getMessage())));
        }
    }

    private Long parseId(String pathInfo) {
        if (pathInfo == null || pathInfo.length() <= 1) return null;
        try {
            String part = pathInfo.substring(1);
            if (part.contains("/")) part = part.substring(0, part.indexOf("/"));
            return Long.parseLong(part);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
