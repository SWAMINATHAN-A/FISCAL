package com.fiscal.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Domain entity representing an assessment record in Oracle TAX_RECORD table.
 */
public class TaxRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long taxId;
    private Long taxpayerId;
    private Long taxTypeId;
    private BigDecimal taxAmount;
    private String status; // 'PAID', 'PENDING'

    // Display fields from joins
    private String taxpayerName;
    private String taxpayerPan;
    private String taxName;
    private BigDecimal taxRate;
    private BigDecimal amountPaid;

    public TaxRecord() {}

    public TaxRecord(Long taxId, Long taxpayerId, Long taxTypeId, BigDecimal taxAmount, String status) {
        this.taxId = taxId;
        this.taxpayerId = taxpayerId;
        this.taxTypeId = taxTypeId;
        this.taxAmount = taxAmount;
        this.status = status;
    }

    public Long getTaxId() { return taxId; }
    public void setTaxId(Long taxId) { this.taxId = taxId; }

    public Long getTaxpayerId() { return taxpayerId; }
    public void setTaxpayerId(Long taxpayerId) { this.taxpayerId = taxpayerId; }

    public Long getTaxTypeId() { return taxTypeId; }
    public void setTaxTypeId(Long taxTypeId) { this.taxTypeId = taxTypeId; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTaxpayerName() { return taxpayerName; }
    public void setTaxpayerName(String taxpayerName) { this.taxpayerName = taxpayerName; }

    public String getTaxpayerPan() { return taxpayerPan; }
    public void setTaxpayerPan(String taxpayerPan) { this.taxpayerPan = taxpayerPan; }

    public String getTaxName() { return taxName; }
    public void setTaxName(String taxName) { this.taxName = taxName; }

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
}
