package com.fiscal.dao;

import com.fiscal.model.AppUser;
import com.fiscal.model.TaxPayment;
import com.fiscal.model.TaxType;
import com.fiscal.model.Taxpayer;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe In-Memory persistent engine seeded with identical dataset from 03_seed_data.sql.
 * Used for offline demonstrations and academic testing when local Oracle DB service is inactive.
 */
public final class MemoryStore {

    private static final Map<Long, AppUser> USERS = new ConcurrentHashMap<>();
    private static final Map<Long, Taxpayer> TAXPAYERS = new ConcurrentHashMap<>();
    private static final Map<Long, TaxType> TAX_TYPES = new ConcurrentHashMap<>();
    private static final Map<Long, TaxPayment> PAYMENTS = new ConcurrentHashMap<>();

    private static final AtomicLong USER_ID_GEN = new AtomicLong(104);
    private static final AtomicLong TAXPAYER_ID_GEN = new AtomicLong(1011);
    private static final AtomicLong TAX_TYPE_ID_GEN = new AtomicLong(207);
    private static final AtomicLong PAYMENT_ID_GEN = new AtomicLong(5011);

    static {
        seedInitialData();
    }

    private MemoryStore() {}

    private static void seedInitialData() {
        // 1. Users
        AppUser admin = new AppUser(101L, "admin", "Arpit Raj (Chief Administrator)", "admin@fiscal.internal", "ADMIN", "ACTIVE", Date.valueOf("2026-01-10"));
        admin.setPasswordHash("567fe3eba4137898a0937d43028963e535e80ab460b12001bbf07f31587db107");
        admin.setSalt("a1b2c3d4e5f67890");
        USERS.put(admin.getUserId(), admin);

        AppUser officer = new AppUser(102L, "officer", "Siddharth Varma (Assessment Officer)", "officer@fiscal.internal", "OFFICER", "ACTIVE", Date.valueOf("2026-01-15"));
        officer.setPasswordHash("9f7d639f050edfb3a0bde235379f94fa97479a34073249a18ff9f279c80da28e");
        officer.setSalt("b2c3d4e5f6a17890");
        USERS.put(officer.getUserId(), officer);

        AppUser auditor = new AppUser(103L, "auditor", "Nandini Sen (Compliance Auditor)", "auditor@fiscal.internal", "AUDITOR", "ACTIVE", Date.valueOf("2026-02-01"));
        auditor.setPasswordHash("9534df533ff1615ab57fdeac16f32f00e758f9ce063df9eebf9e98bbecb0046d");
        auditor.setSalt("c3d4e5f6a1b27890");
        USERS.put(auditor.getUserId(), auditor);

        // 2. Tax Types
        TAX_TYPES.put(201L, new TaxType(201L, "Income Tax", new BigDecimal("15.00"), "Statutory personal and salaried income taxation baseline assessment"));
        TAX_TYPES.put(202L, new TaxType(202L, "Goods and Services Tax (GST)", new BigDecimal("18.00"), "Standard consolidated commercial turnover and services assessment rate"));
        TAX_TYPES.put(203L, new TaxType(203L, "Capital Gains Tax", new BigDecimal("12.50"), "Taxation on long-term securities and real estate asset disposition gains"));
        TAX_TYPES.put(204L, new TaxType(204L, "Corporate Tax", new BigDecimal("22.00"), "Operational net income assessment for registered corporate entities"));
        TAX_TYPES.put(205L, new TaxType(205L, "Professional Tax", new BigDecimal("5.00"), "Municipal assessment on professional certification and trade practice"));
        TAX_TYPES.put(206L, new TaxType(206L, "Wealth & Luxury Surcharge", new BigDecimal("8.50"), "High net-worth asset assessment and luxury consumption tax tier"));

        for (TaxType t : TAX_TYPES.values()) {
            t.setCreatedAt(Date.valueOf("2026-01-01"));
        }

        // 3. Taxpayers
        seedTaxpayer(1001L, "Aarav Sharma", "ABCPS1234F", 34, "Mumbai", "1850000.00", "2026-01-12");
        seedTaxpayer(1002L, "Priya Iyer", "BKDPI5678K", 29, "Bengaluru", "2400000.00", "2026-01-14");
        seedTaxpayer(1003L, "Rohan Mehta", "CLPRM9012M", 45, "New Delhi", "3200000.00", "2026-01-18");
        seedTaxpayer(1004L, "Ananya Deshmukh", "DMKAD3456P", 38, "Pune", "1450000.00", "2026-01-22");
        seedTaxpayer(1005L, "Vikramaditya Rao", "EFGVR7890Q", 52, "Hyderabad", "4800000.00", "2026-01-25");
        seedTaxpayer(1006L, "Sneha Kulkarni", "GHISK2345T", 27, "Chennai", "1200000.00", "2026-02-02");
        seedTaxpayer(1007L, "Kabir Singhania", "JKLKS6789W", 41, "Kolkata", "2950000.00", "2026-02-10");
        seedTaxpayer(1008L, "Meera Nambiar", "MNPMN1122Z", 33, "Kochi", "1600000.00", "2026-02-18");
        seedTaxpayer(1009L, "Devansh Verma", "OPQDV3344B", 48, "Ahmedabad", "3750000.00", "2026-03-01");
        seedTaxpayer(1010L, "Ishita Sen", "RSTIS5566D", 31, "Jaipur", "1380000.00", "2026-03-10");

        // 4. Payments
        seedPayment(5001L, 1001L, 201L, "277500.00", "2026-02-15", "PAID", "FSC-2026-5001", "FY25-26 Annual Advance Income Tax Installment");
        seedPayment(5002L, 1002L, 202L, "108000.00", "2026-02-20", "PAID", "FSC-2026-5002", "Q3 Goods & Services Tax Settlement");
        seedPayment(5003L, 1003L, 203L, "100000.00", "2026-02-28", "PAID", "FSC-2026-5003", "Long-term equity asset capital gains filing");
        seedPayment(5004L, 1005L, 204L, "528000.00", "2026-03-05", "PAID", "FSC-2026-5004", "Corporate Operational Tax Installment 1 of 2");
        seedPayment(5005L, 1005L, 204L, "528000.00", "2026-03-25", "PENDING", "FSC-2026-5005", "Corporate Operational Tax Installment 2 of 2 (Awaiting Clearance)");
        seedPayment(5006L, 1004L, 201L, "217500.00", "2026-03-12", "PAID", "FSC-2026-5006", "Self-assessment annual income tax remittance");
        seedPayment(5007L, 1006L, 205L, "60000.00", "2026-03-18", "PAID", "FSC-2026-5007", "Municipal professional statutory tax deduction");
        seedPayment(5008L, 1007L, 206L, "42500.00", "2026-03-22", "PENDING", "FSC-2026-5008", "Luxury asset acquisition surcharge - under verification");
        seedPayment(5009L, 1009L, 201L, "562500.00", "2026-03-29", "PAID", "FSC-2026-5009", "Advance tax fourth quarter remittance");
        seedPayment(5010L, 1008L, 201L, "240000.00", "2026-03-02", "REVERSED", "FSC-2026-5010", "Reversed due to bank reconciliation duplicate batch transmission");
    }

    private static void seedTaxpayer(Long id, String name, String pan, int age, String city, String income, String date) {
        Taxpayer t = new Taxpayer(id, name, pan, age, city, new BigDecimal(income));
        t.setCreatedAt(Date.valueOf(date));
        TAXPAYERS.put(id, t);
    }

    private static void seedPayment(Long id, Long taxpayerId, Long taxId, String amount, String date, String status, String ref, String remarks) {
        TaxPayment p = new TaxPayment(id, taxpayerId, taxId, new BigDecimal(amount), Date.valueOf(date), status, ref, remarks);
        enrichPayment(p);
        PAYMENTS.put(id, p);
    }

    private static void enrichPayment(TaxPayment p) {
        Taxpayer tp = TAXPAYERS.get(p.getTaxpayerId());
        if (tp != null) {
            p.setTaxpayerName(tp.getName());
            p.setTaxpayerPan(tp.getPanNo());
        }
        TaxType tt = TAX_TYPES.get(p.getTaxId());
        if (tt != null) {
            p.setTaxName(tt.getTaxName());
            p.setTaxRate(tt.getTaxRate());
        }
    }

    // Accessors and Mutators
    public static Map<Long, AppUser> getUsers() { return USERS; }
    public static Map<Long, Taxpayer> getTaxpayers() { return TAXPAYERS; }
    public static Map<Long, TaxType> getTaxTypes() { return TAX_TYPES; }
    public static Map<Long, TaxPayment> getPayments() { return PAYMENTS; }

    public static long nextUserId() { return USER_ID_GEN.getAndIncrement(); }
    public static long nextTaxpayerId() { return TAXPAYER_ID_GEN.getAndIncrement(); }
    public static long nextTaxTypeId() { return TAX_TYPE_ID_GEN.getAndIncrement(); }
    public static long nextPaymentId() { return PAYMENT_ID_GEN.getAndIncrement(); }

    public static void savePayment(TaxPayment p) {
        enrichPayment(p);
        PAYMENTS.put(p.getPaymentId(), p);
    }
}
