package com.fiscal.service;

import com.fiscal.dao.DashboardDAO;
import com.fiscal.model.DashboardSummary;

/**
 * Service providing aggregated financial and operational metrics for the dashboard.
 */
public class DashboardService {

    private final DashboardDAO dashboardDAO = new DashboardDAO();

    public DashboardSummary getDashboardSummary() {
        return dashboardDAO.getSummary();
    }
}
