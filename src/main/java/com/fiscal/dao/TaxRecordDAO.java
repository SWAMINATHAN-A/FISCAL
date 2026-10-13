package com.fiscal.dao;

import com.fiscal.config.DatabaseConfig;
import com.fiscal.model.TaxRecord;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for TAX_RECORD table managing statutory assessments.
 */
public class TaxRecordDAO {

    private static final Logger LOGGER = Logger.getLogger(TaxRecordDAO.class.getName());

    public List<TaxRecord> findAll(String search, Long taxpayerId, Long taxTypeId, String status) {
        List<TaxRecord> list = new ArrayList<>();

        if (DatabaseConfig.isOracleReachable()) {
            StringBuilder sb = new StringBuilder(
                "SELECT tr.TAX_ID, tr.TAXPAYER_ID, tr.TAX_TYPE_ID, tr.TAX_AMOUNT, tr.STATUS, " +
                "tp.NAME AS TAXPAYER_NAME, tp.PAN_NO AS TAXPAYER_PAN, " +
                "tt.TAX_NAME, tt.TAX_RATE, " +
                "NVL((SELECT SUM(p.AMOUNT_PAID) FROM TAX_PAYMENT p WHERE p.TAX_ID = tr.TAX_ID AND p.PAYMENT_STATUS = 'PAID'), 0) AS AMOUNT_PAID " +
                "FROM TAX_RECORD tr " +
                "JOIN TAXPAYER tp ON tr.TAXPAYER_ID = tp.TAXPAYER_ID " +
                "JOIN TAX_TYPE tt ON tr.TAX_TYPE_ID = tt.TAX_TYPE_ID " +
                "WHERE 1=1 "
            );

            List<Object> params = new ArrayList<>();
            if (taxpayerId != null && taxpayerId > 0) {
                sb.append("AND tr.TAXPAYER_ID = ? ");
                params.add(taxpayerId);
            }
            if (taxTypeId != null && taxTypeId > 0) {
                sb.append("AND tr.TAX_TYPE_ID = ? ");
                params.add(taxTypeId);
            }
            if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
                sb.append("AND UPPER(tr.STATUS) = ? ");
                params.add(status.trim().toUpperCase());
            }
            if (search != null && !search.trim().isEmpty()) {
                sb.append("AND (LOWER(tp.NAME) LIKE LOWER(?) OR UPPER(tp.PAN_NO) LIKE UPPER(?)) ");
                params.add("%" + search.trim() + "%");
                params.add("%" + search.trim() + "%");
            }
            sb.append("ORDER BY tr.TAX_ID DESC");

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sb.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRecord(rs));
                    }
                    return list;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Oracle query error in TaxRecordDAO.findAll: " + e.getMessage(), e);
            }
        }

        return list;
    }

    public TaxRecord findById(Long taxId) {
        if (taxId == null) return null;

        if (DatabaseConfig.isOracleReachable()) {
            String sql = "SELECT tr.TAX_ID, tr.TAXPAYER_ID, tr.TAX_TYPE_ID, tr.TAX_AMOUNT, tr.STATUS, " +
                         "tp.NAME AS TAXPAYER_NAME, tp.PAN_NO AS TAXPAYER_PAN, " +
                         "tt.TAX_NAME, tt.TAX_RATE, " +
                         "NVL((SELECT SUM(p.AMOUNT_PAID) FROM TAX_PAYMENT p WHERE p.TAX_ID = tr.TAX_ID AND p.PAYMENT_STATUS = 'PAID'), 0) AS AMOUNT_PAID " +
                         "FROM TAX_RECORD tr " +
                         "JOIN TAXPAYER tp ON tr.TAXPAYER_ID = tp.TAXPAYER_ID " +
                         "JOIN TAX_TYPE tt ON tr.TAX_TYPE_ID = tt.TAX_TYPE_ID " +
                         "WHERE tr.TAX_ID = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, taxId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapRecord(rs);
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Oracle query error in TaxRecordDAO.findById: " + e.getMessage(), e);
            }
        }
        return null;
    }

    public boolean create(TaxRecord record) {
        if (record == null) return false;

        if (DatabaseConfig.isOracleReachable()) {
            String sql = "INSERT INTO TAX_RECORD (TAXPAYER_ID, TAX_TYPE_ID, TAX_AMOUNT, STATUS) VALUES (?, ?, ?, ?)";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, new String[]{"TAX_ID"})) {
                conn.setAutoCommit(false);
                ps.setLong(1, record.getTaxpayerId());
                ps.setLong(2, record.getTaxTypeId());
                ps.setBigDecimal(3, record.getTaxAmount());
                ps.setString(4, record.getStatus() != null ? record.getStatus().toUpperCase() : "PENDING");
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            record.setTaxId(rs.getLong(1));
                        }
                    }
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Oracle insertion error in TaxRecordDAO.create: " + e.getMessage(), e);
            }
        }
        return false;
    }

    public boolean updateStatus(Long taxId, String status) {
        if (taxId == null || status == null) return false;

        if (DatabaseConfig.isOracleReachable()) {
            String sql = "UPDATE TAX_RECORD SET STATUS = ? WHERE TAX_ID = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                ps.setString(1, status.toUpperCase());
                ps.setLong(2, taxId);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Oracle update error in TaxRecordDAO.updateStatus: " + e.getMessage(), e);
            }
        }
        return false;
    }

    public boolean delete(Long taxId) {
        if (taxId == null) return false;

        if (DatabaseConfig.isOracleReachable()) {
            // Delete payments referencing this tax record, then tax record
            String delPay = "DELETE FROM TAX_PAYMENT WHERE TAX_ID = ?";
            String delRec = "DELETE FROM TAX_RECORD WHERE TAX_ID = ?";
            try (Connection conn = DatabaseConfig.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement ps1 = conn.prepareStatement(delPay)) {
                        ps1.setLong(1, taxId);
                        ps1.executeUpdate();
                    }
                    int rows;
                    try (PreparedStatement ps2 = conn.prepareStatement(delRec)) {
                        ps2.setLong(1, taxId);
                        rows = ps2.executeUpdate();
                    }
                    if (rows > 0) {
                        conn.commit();
                        return true;
                    } else {
                        conn.rollback();
                    }
                } catch (SQLException ex) {
                    conn.rollback();
                    throw ex;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Oracle delete error in TaxRecordDAO.delete: " + e.getMessage(), e);
            }
        }
        return false;
    }

    private TaxRecord mapRecord(ResultSet rs) throws SQLException {
        TaxRecord tr = new TaxRecord();
        tr.setTaxId(rs.getLong("TAX_ID"));
        tr.setTaxpayerId(rs.getLong("TAXPAYER_ID"));
        tr.setTaxTypeId(rs.getLong("TAX_TYPE_ID"));
        tr.setTaxAmount(rs.getBigDecimal("TAX_AMOUNT"));
        tr.setStatus(rs.getString("STATUS"));
        tr.setTaxpayerName(rs.getString("TAXPAYER_NAME"));
        tr.setTaxpayerPan(rs.getString("TAXPAYER_PAN"));
        tr.setTaxName(rs.getString("TAX_NAME"));
        tr.setTaxRate(rs.getBigDecimal("TAX_RATE"));
        tr.setAmountPaid(rs.getBigDecimal("AMOUNT_PAID"));
        return tr;
    }
}
