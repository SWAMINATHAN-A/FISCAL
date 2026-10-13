package com.fiscal.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Authoritative response payload from tax calculation engine.
 */
public class TaxCalculationResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long taxpayerId;
    private String taxpayerName;
    private String panNo;
    private Long taxId;
    private String taxName;
    private BigDecimal taxRate;
    private BigDecimal applicableAmount;
    private BigDecimal calculatedTax;
    private String calculationFormula;
    private String timestamp;

    public TaxCalculationResponse() {}

    public Long getTaxpayerId() { return taxpayerId; }
    public void setTaxpayerId(Long taxpayerId) { this.taxpayerId = taxpayerId; }

    public String getTaxpayerName() { return taxpayerName; }
    public void setTaxpayerName(String taxpayerName) { this.taxpayerName = taxpayerName; }

    public String getPanNo() { return panNo; }
    public void setPanNo(String panNo) { this.panNo = panNo; }

    public Long getTaxId() { return taxId; }
    public void setTaxId(Long taxId) { this.taxId = taxId; }

    public String getTaxName() { return taxName; }
    public void setTaxName(String taxName) { this.taxName = taxName; }

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }

    public BigDecimal getApplicableAmount() { return applicableAmount; }
    public void setApplicableAmount(BigDecimal applicableAmount) { this.applicableAmount = applicableAmount; }

    public BigDecimal getCalculatedTax() { return calculatedTax; }
    public void setCalculatedTax(BigDecimal calculatedTax) { this.calculatedTax = calculatedTax; }

    public String getCalculationFormula() { return calculationFormula; }
    public void setCalculationFormula(String calculationFormula) { this.calculationFormula = calculationFormula; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
