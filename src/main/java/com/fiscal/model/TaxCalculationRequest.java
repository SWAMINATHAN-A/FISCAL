package com.fiscal.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Request payload for authoritative server-side tax calculation.
 */
public class TaxCalculationRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long taxpayerId;
    private Long taxId;
    private BigDecimal taxableAmount; // If null, uses taxpayer.income

    public TaxCalculationRequest() {}

    public Long getTaxpayerId() { return taxpayerId; }
    public void setTaxpayerId(Long taxpayerId) { this.taxpayerId = taxpayerId; }

    public Long getTaxId() { return taxId; }
    public void setTaxId(Long taxId) { this.taxId = taxId; }

    public BigDecimal getTaxableAmount() { return taxableAmount; }
    public void setTaxableAmount(BigDecimal taxableAmount) { this.taxableAmount = taxableAmount; }
}
