package com.fiscal.servlet;

import com.fiscal.model.ApiResponse;
import com.fiscal.model.TaxType;
import com.fiscal.service.TaxTypeService;
import com.fiscal.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * RESTful Servlet managing TAX_TYPE categories and configured percentage rates.
 */
@WebServlet(urlPatterns = {"/api/tax-types", "/api/tax-types/*"})
public class TaxTypeServlet extends HttpServlet {

    private final TaxTypeService taxTypeService = new TaxTypeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                String search = req.getParameter("search");
                List<TaxType> list = taxTypeService.listTaxTypes(search);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok(list)));
                return;
            }

            Long id = parseId(pathInfo);
            if (id != null) {
                TaxType t = taxTypeService.getTaxType(id);
                if (t != null) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok(t)));
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Tax category with ID " + id + " not found")));
                }
                return;
            }

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Invalid tax type endpoint path")));

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Server error: " + e.getMessage())));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            TaxType payload = JsonUtil.fromJson(req.getReader(), TaxType.class);
            if (payload == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Missing tax type payload")));
                return;
            }

            TaxType created = taxTypeService.createTaxType(payload);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok("Tax category created successfully", created)));

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error(e.getMessage())));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Failed to create tax category: " + e.getMessage())));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        Long id = parseId(pathInfo);

        if (id == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("A valid Tax ID must be specified in the URL")));
            return;
        }

        try {
            TaxType payload = JsonUtil.fromJson(req.getReader(), TaxType.class);
            if (payload == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Missing update payload")));
                return;
            }

            payload.setTaxId(id);
            TaxType updated = taxTypeService.updateTaxType(payload);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok("Tax category updated successfully", updated)));

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error(e.getMessage())));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Failed to update tax category: " + e.getMessage())));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        Long id = parseId(pathInfo);

        if (id == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("A valid Tax ID must be specified in the URL")));
            return;
        }

        try {
            boolean deleted = taxTypeService.deleteTaxType(id);
            if (deleted) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok("Tax category removed successfully", null)));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Tax category not found or deletion prevented")));
            }
        } catch (IllegalStateException e) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error(e.getMessage())));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Failed to delete tax category: " + e.getMessage())));
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
