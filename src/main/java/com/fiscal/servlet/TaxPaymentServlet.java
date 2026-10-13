package com.fiscal.servlet;

import com.fiscal.model.ApiResponse;
import com.fiscal.model.TaxPayment;
import com.fiscal.service.TaxPaymentService;
import com.fiscal.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * RESTful Servlet managing TAX_PAYMENT ledger entries, status transitions, and history.
 */
@WebServlet(urlPatterns = {"/api/payments", "/api/payments/*"})
public class TaxPaymentServlet extends HttpServlet {

    private final TaxPaymentService paymentService = new TaxPaymentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                String search = req.getParameter("search");
                String tpParam = req.getParameter("taxpayerId");
                String ttParam = req.getParameter("taxId");
                String status = req.getParameter("status");
                String sort = req.getParameter("sort");

                Long taxpayerId = (tpParam != null && !tpParam.trim().isEmpty()) ? Long.parseLong(tpParam.trim()) : null;
                Long taxId = (ttParam != null && !ttParam.trim().isEmpty()) ? Long.parseLong(ttParam.trim()) : null;

                List<TaxPayment> list = paymentService.listPayments(search, taxpayerId, taxId, status, sort);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok(list)));
                return;
            }

            Long id = parseId(pathInfo);
            if (id != null) {
                TaxPayment p = paymentService.getPayment(id);
                if (p != null) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok(p)));
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Payment with ID " + id + " not found")));
                }
                return;
            }

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Invalid payment endpoint path")));

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Server error: " + e.getMessage())));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        // Check if this is a status update endpoint: /api/payments/{id}/status
        if (pathInfo != null && pathInfo.endsWith("/status")) {
            handleStatusUpdate(pathInfo, req, resp);
            return;
        }

        try {
            TaxPayment payload = JsonUtil.fromJson(req.getReader(), TaxPayment.class);
            if (payload == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Missing payment payload")));
                return;
            }

            TaxPayment recorded = paymentService.recordPayment(payload);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok("Tax payment recorded and committed to ledger", recorded)));

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error(e.getMessage())));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Failed to commit payment: " + e.getMessage())));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleStatusUpdate(String pathInfo, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Format: /{id}/status
        String idStr = pathInfo.substring(1, pathInfo.lastIndexOf("/status"));
        Long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Invalid Payment ID in path")));
            return;
        }

        try {
            Map<String, String> body = JsonUtil.fromJson(req.getReader(), Map.class);
            if (body == null || !body.containsKey("status")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Field 'status' is required")));
                return;
            }

            String newStatus = body.get("status");
            String remarks = body.get("remarks");

            TaxPayment updated = paymentService.updatePaymentStatus(id, newStatus, remarks);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok("Payment status updated to " + newStatus, updated)));

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error(e.getMessage())));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Failed to update status: " + e.getMessage())));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        Long id = parseId(pathInfo);

        if (id == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("A valid Payment ID must be specified in the URL")));
            return;
        }

        try {
            boolean deleted = paymentService.deletePayment(id);
            if (deleted) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok("Payment record removed from ledger", null)));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Payment with ID " + id + " not found")));
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Failed to delete payment record: " + e.getMessage())));
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
