package com.fiscal.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;

/**
 * Domain entity representing a tax category matching Oracle TAX_TYPE table.
 */
public class TaxType implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long taxId;
    private String taxName;
    private BigDecimal taxRate; // In percentage, e.g. 15.00 for 15%
    private String description;
    private Date createdAt;

    // Aggregate statistics
    private Integer transactionCount;
    private BigDecimal totalCollected;

    public TaxType() {}

    public TaxType(Long taxId, String taxName, BigDecimal taxRate, String description) {
        this.taxId = taxId;
        this.taxName = taxName;
        this.taxRate = taxRate;
        this.description = description;
    }

    public Long getTaxId() { return taxId; }
    public void setTaxId(Long taxId) { this.taxId = taxId; }

    public Long getTaxTypeId() { return taxId; }
    public void setTaxTypeId(Long taxTypeId) { this.taxId = taxTypeId; }

    public String getTaxName() { return taxName; }
    public void setTaxName(String taxName) { this.taxName = taxName; }

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Integer getTransactionCount() { return transactionCount; }
    public void setTransactionCount(Integer transactionCount) { this.transactionCount = transactionCount; }

    public BigDecimal getTotalCollected() { return totalCollected; }
    public void setTotalCollected(BigDecimal totalCollected) { this.totalCollected = totalCollected; }
}
