package com.fiscal.dao;

import com.fiscal.config.DatabaseConfig;
import com.fiscal.model.DashboardSummary;
import com.fiscal.model.TaxPayment;
import com.fiscal.model.TaxType;
import com.fiscal.model.Taxpayer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object computing aggregated financial summaries directly from Oracle.
 */
public class DashboardDAO {

    private static final Logger LOGGER = Logger.getLogger(DashboardDAO.class.getName());
    private final TaxpayerDAO taxpayerDAO = new TaxpayerDAO();
    private final TaxTypeDAO taxTypeDAO = new TaxTypeDAO();
    private final TaxPaymentDAO paymentDAO = new TaxPaymentDAO();

    private void checkOracleReachability() {
        // Silent auto mode - seamlessly falls back if database is paused or unreachable
    }

    public DashboardSummary getSummary() {
        checkOracleReachability();
        DashboardSummary summary = new DashboardSummary();

        if (DatabaseConfig.isOracleReachable()) {
            try (Connection conn = DatabaseConfig.getConnection()) {
                // 1. Taxpayer count
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM TAXPAYER");
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) summary.setTotalTaxpayers(rs.getInt(1));
                }

                // 2. Gross monitored income across all registered taxpayers
                try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(INCOME), 0) FROM TAXPAYER");
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) summary.setGrossTurnoverMonitored(rs.getBigDecimal(1));
                }

                // 3. Payment counts and totals by status
                String pSql = "SELECT " +
                              "(SELECT COUNT(*) FROM TAX_PAYMENT) AS TOTAL_CNT, " +
                              "(SELECT COALESCE(SUM(AMOUNT_PAID), 0) FROM TAX_PAYMENT WHERE PAYMENT_STATUS = 'PAID') AS PAID_SUM, " +
                              "(SELECT COALESCE(SUM(TAX_AMOUNT), 0) FROM TAX_RECORD WHERE STATUS = 'PENDING') AS PENDING_SUM, " +
                              "(SELECT COALESCE(SUM(AMOUNT_PAID), 0) FROM TAX_PAYMENT WHERE PAYMENT_STATUS = 'REVERSED') AS REVERSED_SUM " +
                              "FROM DUAL";
                try (PreparedStatement ps = conn.prepareStatement(pSql);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        summary.setTotalPayments(rs.getInt("TOTAL_CNT"));
                        summary.setTotalPaidRevenue(rs.getBigDecimal("PAID_SUM"));
                        summary.setTotalPendingAmount(rs.getBigDecimal("PENDING_SUM"));
                        summary.setTotalReversedAmount(rs.getBigDecimal("REVERSED_SUM"));
                    }
                }

                // 4. Status distribution
                Map<String, Integer> dist = new HashMap<>();
                dist.put("PAID", 0);
                dist.put("PENDING", 0);
                dist.put("REVERSED", 0);
                try (PreparedStatement ps = conn.prepareStatement("SELECT PAYMENT_STATUS AS STATUS, COUNT(*) AS CNT FROM TAX_PAYMENT GROUP BY PAYMENT_STATUS");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String st = rs.getString("STATUS");
                        if (st != null) {
                            dist.put(st.toUpperCase(), rs.getInt("CNT"));
                        }
                    }
                }
                summary.setStatusDistribution(dist);

                // 5. Recent records
                summary.setRecentPayments(paymentDAO.findAll(null, null, null, null, "date_desc").stream().limit(6).toList());
                summary.setRecentTaxpayers(taxpayerDAO.findAll(null, null, "id_desc").stream().limit(6).toList());
                summary.setTaxCategoriesBreakdown(taxTypeDAO.findAll(null));

                return summary;

            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Oracle dashboard aggregation error: " + e.getMessage(), e);
                DatabaseConfig.markUnreachable();
            }
        }

        // Resilient fallback logic
        summary.setTotalTaxpayers(MemoryStore.getTaxpayers().size());

        BigDecimal grossIncome = BigDecimal.ZERO;
        for (Taxpayer t : MemoryStore.getTaxpayers().values()) {
            if (t.getIncome() != null) grossIncome = grossIncome.add(t.getIncome());
        }
        summary.setGrossTurnoverMonitored(grossIncome);

        summary.setTotalPayments(MemoryStore.getPayments().size());

        BigDecimal paidSum = BigDecimal.ZERO;
        BigDecimal pendingSum = BigDecimal.ZERO;
        BigDecimal reversedSum = BigDecimal.ZERO;
        Map<String, Integer> dist = new HashMap<>();
        dist.put("PAID", 0);
        dist.put("PENDING", 0);
        dist.put("REVERSED", 0);

        for (TaxPayment p : MemoryStore.getPayments().values()) {
            String s = p.getStatus() != null ? p.getStatus().toUpperCase() : "PAID";
            dist.put(s, dist.getOrDefault(s, 0) + 1);

            if ("PAID".equals(s)) {
                paidSum = paidSum.add(p.getAmount());
            } else if ("PENDING".equals(s)) {
                pendingSum = pendingSum.add(p.getAmount());
            } else if ("REVERSED".equals(s)) {
                reversedSum = reversedSum.add(p.getAmount());
            }
        }

        summary.setTotalPaidRevenue(paidSum);
        summary.setTotalPendingAmount(pendingSum);
        summary.setTotalReversedAmount(reversedSum);
        summary.setStatusDistribution(dist);

        summary.setRecentPayments(paymentDAO.findAll(null, null, null, null, "date_desc").stream().limit(6).toList());
        summary.setRecentTaxpayers(taxpayerDAO.findAll(null, null, "id_desc").stream().limit(6).toList());
        summary.setTaxCategoriesBreakdown(taxTypeDAO.findAll(null));

        return summary;
    }
}
