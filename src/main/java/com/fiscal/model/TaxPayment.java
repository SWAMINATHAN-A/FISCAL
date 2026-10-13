package com.fiscal.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;

/**
 * Domain entity representing a payment/ledger entry matching Oracle TAX_PAYMENT table.
 */
public class TaxPayment implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long paymentId;
    private Long taxpayerId;
    private Long taxId;         // Assessment ID (FK to TAX_RECORD.TAX_ID)
    private Long taxTypeId;     // Tax Category ID (from joined TAX_RECORD)
    private BigDecimal amount;  // Maps to AMOUNT_PAID
    private Date paymentDate;
    private String status;      // Maps to PAYMENT_STATUS ('PAID', 'PENDING', 'REVERSED')
    private String paymentMode; // Maps to PAYMENT_MODE ('BANK_TRANSFER', 'CARD', 'UPI', 'CASH', 'ONLINE')
    private String paymentRef;  // e.g. 'FSC-2026-5001'
    private String remarks;

    // Joined fields for display
    private String taxpayerName;
    private String taxpayerPan;
    private String taxName;
    private BigDecimal taxRate;

    public TaxPayment() {}

    public TaxPayment(Long paymentId, Long taxpayerId, Long taxId, BigDecimal amount, Date paymentDate, String status, String paymentRef, String remarks) {
        this.paymentId = paymentId;
        this.taxpayerId = taxpayerId;
        this.taxId = taxId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.status = status;
        this.paymentRef = paymentRef;
        this.remarks = remarks;
    }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public Long getTaxpayerId() { return taxpayerId; }
    public void setTaxpayerId(Long taxpayerId) { this.taxpayerId = taxpayerId; }

    public Long getTaxId() { return taxId; }
    public void setTaxId(Long taxId) { this.taxId = taxId; }

    public Long getTaxTypeId() { return taxTypeId; }
    public void setTaxTypeId(Long taxTypeId) { this.taxTypeId = taxTypeId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getAmountPaid() { return amount; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amount = amountPaid; }

    public Date getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Date paymentDate) { this.paymentDate = paymentDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentStatus() { return status; }
    public void setPaymentStatus(String paymentStatus) { this.status = paymentStatus; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getPaymentRef() { return paymentRef; }
    public void setPaymentRef(String paymentRef) { this.paymentRef = paymentRef; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getTaxpayerName() { return taxpayerName; }
    public void setTaxpayerName(String taxpayerName) { this.taxpayerName = taxpayerName; }

    public String getTaxpayerPan() { return taxpayerPan; }
    public void setTaxpayerPan(String taxpayerPan) { this.taxpayerPan = taxpayerPan; }

    public String getTaxName() { return taxName; }
    public void setTaxName(String taxName) { this.taxName = taxName; }

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }

    public String getMaskedPan() {
        if (taxpayerPan == null || taxpayerPan.length() < 10) return taxpayerPan;
        return taxpayerPan.substring(0, 3) + "••••" + taxpayerPan.substring(7);
    }
}
