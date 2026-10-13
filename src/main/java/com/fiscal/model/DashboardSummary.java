package com.fiscal.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Composite model encapsulating all live Oracle-backed dashboard statistics.
 */
public class DashboardSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    private int totalTaxpayers;
    private int totalPayments;
    private BigDecimal totalPaidRevenue;
    private BigDecimal totalPendingAmount;
    private BigDecimal totalReversedAmount;
    private BigDecimal grossTurnoverMonitored;

    private List<TaxPayment> recentPayments;
    private List<Taxpayer> recentTaxpayers;
    private List<TaxType> taxCategoriesBreakdown;
    private Map<String, Integer> statusDistribution;

    public DashboardSummary() {}

    public int getTotalTaxpayers() { return totalTaxpayers; }
    public void setTotalTaxpayers(int totalTaxpayers) { this.totalTaxpayers = totalTaxpayers; }

    public int getTotalPayments() { return totalPayments; }
    public void setTotalPayments(int totalPayments) { this.totalPayments = totalPayments; }

    public BigDecimal getTotalPaidRevenue() { return totalPaidRevenue; }
    public void setTotalPaidRevenue(BigDecimal totalPaidRevenue) { this.totalPaidRevenue = totalPaidRevenue; }

    public BigDecimal getTotalPendingAmount() { return totalPendingAmount; }
    public void setTotalPendingAmount(BigDecimal totalPendingAmount) { this.totalPendingAmount = totalPendingAmount; }

    public BigDecimal getTotalReversedAmount() { return totalReversedAmount; }
    public void setTotalReversedAmount(BigDecimal totalReversedAmount) { this.totalReversedAmount = totalReversedAmount; }

    public BigDecimal getGrossTurnoverMonitored() { return grossTurnoverMonitored; }
    public void setGrossTurnoverMonitored(BigDecimal grossTurnoverMonitored) { this.grossTurnoverMonitored = grossTurnoverMonitored; }

    public List<TaxPayment> getRecentPayments() { return recentPayments; }
    public void setRecentPayments(List<TaxPayment> recentPayments) { this.recentPayments = recentPayments; }

    public List<Taxpayer> getRecentTaxpayers() { return recentTaxpayers; }
    public void setRecentTaxpayers(List<Taxpayer> recentTaxpayers) { this.recentTaxpayers = recentTaxpayers; }

    public List<TaxType> getTaxCategoriesBreakdown() { return taxCategoriesBreakdown; }
    public void setTaxCategoriesBreakdown(List<TaxType> taxCategoriesBreakdown) { this.taxCategoriesBreakdown = taxCategoriesBreakdown; }

    public Map<String, Integer> getStatusDistribution() { return statusDistribution; }
    public void setStatusDistribution(Map<String, Integer> statusDistribution) { this.statusDistribution = statusDistribution; }
}
