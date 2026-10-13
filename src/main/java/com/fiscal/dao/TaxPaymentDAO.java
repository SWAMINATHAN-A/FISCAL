package com.fiscal.dao;

import com.fiscal.config.DatabaseConfig;
import com.fiscal.model.TaxPayment;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Data Access Object for TAX_PAYMENT table managing the tax collection ledger.
 * Integrated with live Oracle TAX_PAYMENT, TAX_RECORD, TAXPAYER, and TAX_TYPE tables.
 */
public class TaxPaymentDAO {

    private static final Logger LOGGER = Logger.getLogger(TaxPaymentDAO.class.getName());

    private void checkOracleReachability() {
        if (!DatabaseConfig.isOracleReachable() && !DatabaseConfig.isFallbackAllowed()) {
            throw new IllegalStateException("Oracle Database is unreachable and fallback is disabled (db.allowFallback=false).");
        }
    }

    public List<TaxPayment> findAll(String search, Long taxpayerId, Long taxId, String status, String sortBy) {
        checkOracleReachability();
        List<TaxPayment> list = new ArrayList<>();

        if (DatabaseConfig.isOracleReachable()) {
            StringBuilder sb = new StringBuilder(
                "SELECT p.PAYMENT_ID, p.TAX_ID, p.AMOUNT_PAID, p.PAYMENT_DATE, p.PAYMENT_MODE, p.PAYMENT_STATUS, " +
                "tr.TAXPAYER_ID, tr.TAX_TYPE_ID, " +
                "tp.NAME AS TAXPAYER_NAME, tp.PAN_NO AS TAXPAYER_PAN, " +
                "tt.TAX_NAME, tt.TAX_RATE " +
                "FROM TAX_PAYMENT p " +
                "JOIN TAX_RECORD tr ON p.TAX_ID = tr.TAX_ID " +
                "JOIN TAXPAYER tp ON tr.TAXPAYER_ID = tp.TAXPAYER_ID " +
                "JOIN TAX_TYPE tt ON tr.TAX_TYPE_ID = tt.TAX_TYPE_ID " +
                "WHERE 1=1 "
            );

            List<Object> params = new ArrayList<>();

            if (taxpayerId != null && taxpayerId > 0) {
                sb.append("AND tr.TAXPAYER_ID = ? ");
                params.add(taxpayerId);
            }
            if (taxId != null && taxId > 0) {
                sb.append("AND (tr.TAX_TYPE_ID = ? OR p.TAX_ID = ?) ");
                params.add(taxId);
                params.add(taxId);
            }
            if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
                sb.append("AND UPPER(p.PAYMENT_STATUS) = ? ");
                params.add(status.trim().toUpperCase());
            }
            if (search != null && !search.trim().isEmpty()) {
                sb.append("AND (LOWER(tp.NAME) LIKE LOWER(?) OR UPPER(tp.PAN_NO) LIKE UPPER(?) OR UPPER(p.PAYMENT_MODE) LIKE UPPER(?)) ");
                String term = "%" + search.trim() + "%";
                params.add(term);
                params.add(term);
                params.add(term);
            }

            if ("amount_desc".equalsIgnoreCase(sortBy)) {
                sb.append("ORDER BY p.AMOUNT_PAID DESC");
            } else if ("amount_asc".equalsIgnoreCase(sortBy)) {
                sb.append("ORDER BY p.AMOUNT_PAID ASC");
            } else if ("date_asc".equalsIgnoreCase(sortBy)) {
                sb.append("ORDER BY p.PAYMENT_DATE ASC, p.PAYMENT_ID ASC");
            } else {
                sb.append("ORDER BY p.PAYMENT_DATE DESC, p.PAYMENT_ID DESC");
            }

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sb.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapPayment(rs));
                    }
                    return list;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Oracle query error in findAll: " + e.getMessage(), e);
                if (!DatabaseConfig.isFallbackAllowed()) {
                    throw new RuntimeException("Oracle error: " + e.getMessage(), e);
                }
            }
        }

        // Resilient fallback logic
        final String sTerm = (search != null) ? search.trim().toLowerCase() : "";
        final String stTerm = (status != null) ? status.trim().toUpperCase() : "";

        List<TaxPayment> filtered = MemoryStore.getPayments().values().stream().filter(p -> {
            if (taxpayerId != null && taxpayerId > 0 && !p.getTaxpayerId().equals(taxpayerId)) return false;
            if (taxId != null && taxId > 0 && !p.getTaxId().equals(taxId)) return false;
            if (!stTerm.isEmpty() && !"ALL".equals(stTerm) && !p.getStatus().equalsIgnoreCase(stTerm)) return false;

            if (!sTerm.isEmpty()) {
                boolean matchName = p.getTaxpayerName() != null && p.getTaxpayerName().toLowerCase().contains(sTerm);
                boolean matchPan = p.getTaxpayerPan() != null && p.getTaxpayerPan().toLowerCase().contains(sTerm);
                boolean matchRef = p.getPaymentRef() != null && p.getPaymentRef().toLowerCase().contains(sTerm);
                if (!matchName && !matchPan && !matchRef) return false;
            }
            return true;
        }).map(this::clonePayment).collect(Collectors.toList());

        if ("amount_desc".equalsIgnoreCase(sortBy)) {
            filtered.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        } else if ("amount_asc".equalsIgnoreCase(sortBy)) {
            filtered.sort((a, b) -> a.getAmount().compareTo(b.getAmount()));
        } else if ("date_asc".equalsIgnoreCase(sortBy)) {
            filtered.sort(Comparator.comparing(TaxPayment::getPaymentDate));
        } else {
            filtered.sort((a, b) -> b.getPaymentDate().compareTo(a.getPaymentDate()));
        }

        return filtered;
    }

    public TaxPayment findById(Long paymentId) {
        checkOracleReachability();
        if (paymentId == null) return null;

        if (DatabaseConfig.isOracleReachable()) {
            String sql = "SELECT p.PAYMENT_ID, p.TAX_ID, p.AMOUNT_PAID, p.PAYMENT_DATE, p.PAYMENT_MODE, p.PAYMENT_STATUS, " +
                         "tr.TAXPAYER_ID, tr.TAX_TYPE_ID, " +
                         "tp.NAME AS TAXPAYER_NAME, tp.PAN_NO AS TAXPAYER_PAN, " +
                         "tt.TAX_NAME, tt.TAX_RATE " +
                         "FROM TAX_PAYMENT p " +
                         "JOIN TAX_RECORD tr ON p.TAX_ID = tr.TAX_ID " +
                         "JOIN TAXPAYER tp ON tr.TAXPAYER_ID = tp.TAXPAYER_ID " +
                         "JOIN TAX_TYPE tt ON tr.TAX_TYPE_ID = tt.TAX_TYPE_ID " +
                         "WHERE p.PAYMENT_ID = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, paymentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapPayment(rs);
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Oracle query error in findById: " + e.getMessage(), e);
                if (!DatabaseConfig.isFallbackAllowed()) {
                    throw new RuntimeException("Oracle error: " + e.getMessage(), e);
                }
            }
        }

        TaxPayment p = MemoryStore.getPayments().get(paymentId);
        return p != null ? clonePayment(p) : null;
    }

    public List<TaxPayment> findByTaxpayerId(Long taxpayerId) {
        return findAll(null, taxpayerId, null, null, "date_desc");
    }

    public boolean create(TaxPayment payment) {
        checkOracleReachability();
        if (payment.getPaymentRef() == null || payment.getPaymentRef().trim().isEmpty()) {
            payment.setPaymentRef("FSC-" + java.time.Year.now().getValue() + "-" + (System.currentTimeMillis() % 100000));
        }

        if (DatabaseConfig.isOracleReachable()) {
            try (Connection conn = DatabaseConfig.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    Long taxRecordId = null;

                    // 1. Resolve or establish corresponding TAX_RECORD in Oracle
                    Long taxTypeId = payment.getTaxTypeId() != null ? payment.getTaxTypeId() : payment.getTaxId();

                    // If taxpayerId is provided, check if payment.getTaxId() points to an assessment for THIS taxpayer
                    if (payment.getTaxpayerId() != null && payment.getTaxId() != null && payment.getTaxId() > 0) {
                        try (PreparedStatement checkPs = conn.prepareStatement(
                                "SELECT TAX_ID FROM TAX_RECORD WHERE TAX_ID = ? AND TAXPAYER_ID = ?")) {
                            checkPs.setLong(1, payment.getTaxId());
                            checkPs.setLong(2, payment.getTaxpayerId());
                            try (ResultSet rs = checkPs.executeQuery()) {
                                if (rs.next()) {
                                    taxRecordId = rs.getLong(1);
                                }
                            }
                        }
                    }

                    // Look up pending assessment for this taxpayer matching taxTypeId
                    if (taxRecordId == null && payment.getTaxpayerId() != null && taxTypeId != null) {
                        try (PreparedStatement checkRec = conn.prepareStatement(
                                "SELECT TAX_ID FROM TAX_RECORD WHERE TAXPAYER_ID = ? AND TAX_TYPE_ID = ? AND STATUS = 'PENDING' ORDER BY TAX_ID ASC")) {
                            checkRec.setLong(1, payment.getTaxpayerId());
                            checkRec.setLong(2, taxTypeId);
                            try (ResultSet rs = checkRec.executeQuery()) {
                                if (rs.next()) {
                                    taxRecordId = rs.getLong(1);
                                }
                            }
                        }
                    }

                    if (taxRecordId == null && payment.getTaxpayerId() != null && taxTypeId != null) {
                        String createRecSql = "INSERT INTO TAX_RECORD (TAXPAYER_ID, TAX_TYPE_ID, TAX_AMOUNT, STATUS) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement recPs = conn.prepareStatement(createRecSql, Statement.RETURN_GENERATED_KEYS)) {
                            recPs.setLong(1, payment.getTaxpayerId());
                            recPs.setLong(2, taxTypeId);
                            recPs.setBigDecimal(3, payment.getAmount());
                            recPs.setString(4, payment.getStatus() != null ? payment.getStatus().toUpperCase() : "PAID");
                            recPs.executeUpdate();
                            try (ResultSet rs = recPs.getGeneratedKeys()) {
                                if (rs.next()) {
                                    taxRecordId = rs.getLong(1);
                                }
                            }
                        }
                    }

                    if (taxRecordId == null) {
                        throw new SQLException("Unable to locate or create parent TAX_RECORD assessment for payment.");
                    }

                    payment.setTaxId(taxRecordId);

                    // 2. Insert into live Oracle TAX_PAYMENT
                    String insPaySql = "INSERT INTO TAX_PAYMENT (TAX_ID, AMOUNT_PAID, PAYMENT_DATE, PAYMENT_MODE, PAYMENT_STATUS) " +
                                       "VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?)";
                    try (PreparedStatement psPay = conn.prepareStatement(insPaySql, Statement.RETURN_GENERATED_KEYS)) {
                        psPay.setLong(1, taxRecordId);
                        psPay.setBigDecimal(2, payment.getAmount());
                        String mode = (payment.getPaymentMode() != null && !payment.getPaymentMode().trim().isEmpty())
                                ? payment.getPaymentMode() : "BANK_TRANSFER";
                        psPay.setString(3, mode);
                        String st = (payment.getStatus() != null && !payment.getStatus().trim().isEmpty())
                                ? payment.getStatus().toUpperCase() : "PAID";
                        psPay.setString(4, st);

                        int rows = psPay.executeUpdate();
                        if (rows > 0) {
                            try (ResultSet rs = psPay.getGeneratedKeys()) {
                                if (rs.next()) {
                                    payment.setPaymentId(rs.getLong(1));
                                }
                            }

                            if ("PAID".equalsIgnoreCase(st)) {
                                try (PreparedStatement updRec = conn.prepareStatement(
                                        "UPDATE TAX_RECORD SET STATUS = 'PAID' WHERE TAX_ID = ? AND TAX_AMOUNT <= " +
                                        "(SELECT COALESCE(SUM(AMOUNT_PAID), 0) FROM TAX_PAYMENT WHERE TAX_ID = ? AND PAYMENT_STATUS = 'PAID')")) {
                                    updRec.setLong(1, taxRecordId);
                                    updRec.setLong(2, taxRecordId);
                                    updRec.executeUpdate();
                                }
                            }

                            conn.commit();
                            return true;
                        } else {
                            conn.rollback();
                        }
                    }
                } catch (SQLException ex) {
                    conn.rollback();
                    throw ex;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Oracle insertion error in create: " + e.getMessage(), e);
                if (!DatabaseConfig.isFallbackAllowed()) {
                    throw new RuntimeException("Oracle error: " + e.getMessage(), e);
                }
            }
        }

        // Resilient fallback logic
        payment.setPaymentId(MemoryStore.nextPaymentId());
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(new Date(System.currentTimeMillis()));
        }
        if (payment.getStatus() == null) {
            payment.setStatus("PAID");
        }
        MemoryStore.savePayment(clonePayment(payment));
        return true;
    }

    public boolean updateStatus(Long paymentId, String status, String remarks) {
        checkOracleReachability();
        if (paymentId == null || status == null) return false;
        status = status.trim().toUpperCase();

        if (DatabaseConfig.isOracleReachable()) {
            String sql = "UPDATE TAX_PAYMENT SET PAYMENT_STATUS = ? WHERE PAYMENT_ID = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                ps.setString(1, status);
                ps.setLong(2, paymentId);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    if ("PAID".equalsIgnoreCase(status)) {
                        String syncRec = "UPDATE TAX_RECORD SET STATUS = 'PAID' WHERE TAX_ID = " +
                                         "(SELECT TAX_ID FROM TAX_PAYMENT WHERE PAYMENT_ID = ?)";
                        try (PreparedStatement ps2 = conn.prepareStatement(syncRec)) {
                            ps2.setLong(1, paymentId);
                            ps2.executeUpdate();
                        }
                    }
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Oracle update error in updateStatus: " + e.getMessage(), e);
                if (!DatabaseConfig.isFallbackAllowed()) {
                    throw new RuntimeException("Oracle error: " + e.getMessage(), e);
                }
            }
        }

        TaxPayment p = MemoryStore.getPayments().get(paymentId);
        if (p != null) {
            p.setStatus(status);
            if (remarks != null && !remarks.trim().isEmpty()) {
                p.setRemarks(remarks.trim());
            }
            return true;
        }
        return false;
    }

    public boolean delete(Long paymentId) {
        checkOracleReachability();
        if (paymentId == null) return false;

        if (DatabaseConfig.isOracleReachable()) {
            String sql = "DELETE FROM TAX_PAYMENT WHERE PAYMENT_ID = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                ps.setLong(1, paymentId);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Oracle delete error in delete: " + e.getMessage(), e);
                if (!DatabaseConfig.isFallbackAllowed()) {
                    throw new RuntimeException("Oracle error: " + e.getMessage(), e);
                }
            }
        }

        return MemoryStore.getPayments().remove(paymentId) != null;
    }

    private TaxPayment mapPayment(ResultSet rs) throws SQLException {
        TaxPayment p = new TaxPayment();
        p.setPaymentId(rs.getLong("PAYMENT_ID"));
        p.setTaxId(rs.getLong("TAX_ID"));
        p.setAmount(rs.getBigDecimal("AMOUNT_PAID"));
        p.setPaymentDate(rs.getDate("PAYMENT_DATE"));
        p.setPaymentMode(rs.getString("PAYMENT_MODE"));
        p.setStatus(rs.getString("PAYMENT_STATUS"));
        p.setPaymentRef("FSC-2026-" + rs.getLong("PAYMENT_ID"));

        p.setTaxpayerId(rs.getLong("TAXPAYER_ID"));
        p.setTaxTypeId(rs.getLong("TAX_TYPE_ID"));
        p.setTaxpayerName(rs.getString("TAXPAYER_NAME"));
        p.setTaxpayerPan(rs.getString("TAXPAYER_PAN"));
        p.setTaxName(rs.getString("TAX_NAME"));
        p.setTaxRate(rs.getBigDecimal("TAX_RATE"));
        return p;
    }

    private TaxPayment clonePayment(TaxPayment src) {
        TaxPayment p = new TaxPayment(src.getPaymentId(), src.getTaxpayerId(), src.getTaxId(), src.getAmount(),
                src.getPaymentDate(), src.getStatus(), src.getPaymentRef(), src.getRemarks());
        p.setPaymentMode(src.getPaymentMode());
        p.setTaxTypeId(src.getTaxTypeId());
        p.setTaxpayerName(src.getTaxpayerName());
        p.setTaxpayerPan(src.getTaxpayerPan());
        p.setTaxName(src.getTaxName());
        p.setTaxRate(src.getTaxRate());
        return p;
    }
}
