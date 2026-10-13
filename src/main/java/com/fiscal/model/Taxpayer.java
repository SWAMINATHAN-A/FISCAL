package com.fiscal.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;

/**
 * Domain entity representing a registered taxpayer matching Oracle TAXPAYER table.
 */
public class Taxpayer implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long taxpayerId;
    private String name;
    private String panNo;
    private Integer age;
    private String city;
    private BigDecimal income;
    private Date createdAt;

    // Computed / Ledger aggregation fields (when joined with payments)
    private BigDecimal totalPaid;
    private Integer paymentCount;

    public Taxpayer() {}

    public Taxpayer(Long taxpayerId, String name, String panNo, Integer age, String city, BigDecimal income) {
        this.taxpayerId = taxpayerId;
        this.name = name;
        this.panNo = panNo;
        this.age = age;
        this.city = city;
        this.income = income;
    }

    public Long getTaxpayerId() { return taxpayerId; }
    public void setTaxpayerId(Long taxpayerId) { this.taxpayerId = taxpayerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPanNo() { return panNo; }
    public void setPanNo(String panNo) { this.panNo = panNo; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public BigDecimal getIncome() { return income; }
    public void setIncome(BigDecimal income) { this.income = income; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public BigDecimal getTotalPaid() { return totalPaid; }
    public void setTotalPaid(BigDecimal totalPaid) { this.totalPaid = totalPaid; }

    public Integer getPaymentCount() { return paymentCount; }
    public void setPaymentCount(Integer paymentCount) { this.paymentCount = paymentCount; }

    /**
     * Masked PAN helper for secure display (e.g. ABCPS1234F -> ABC••••••F)
     */
    public String getMaskedPan() {
        if (panNo == null || panNo.length() < 10) return panNo;
        return panNo.substring(0, 3) + "••••" + panNo.substring(7);
    }
}
