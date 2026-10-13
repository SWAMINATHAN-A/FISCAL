package com.fiscal.servlet;

import com.fiscal.model.ApiResponse;
import com.fiscal.model.TaxCalculationRequest;
import com.fiscal.model.TaxCalculationResponse;
import com.fiscal.service.TaxCalculationService;
import com.fiscal.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * RESTful Servlet executing authoritative server-side tax calculations and assessments.
 */
@WebServlet("/api/tax/calculate")
public class TaxCalculationServlet extends HttpServlet {

    private final TaxCalculationService calculationService = new TaxCalculationService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            TaxCalculationRequest request = JsonUtil.fromJson(req.getReader(), TaxCalculationRequest.class);
            if (request == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Missing tax calculation payload")));
                return;
            }

            TaxCalculationResponse result = calculationService.calculate(request);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok("Tax calculation computed successfully", result)));

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error(e.getMessage())));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.error("Failed to perform tax assessment: " + e.getMessage())));
        }
    }
}
