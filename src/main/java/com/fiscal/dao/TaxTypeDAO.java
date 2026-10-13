package com.fiscal.dao;

import com.fiscal.config.DatabaseConfig;
import com.fiscal.model.TaxPayment;
import com.fiscal.model.TaxType;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Data Access Object for TAX_TYPE table managing tax categories and configured percentage rates.
 * Integrated with live Oracle TAX_TYPE, TAX_RECORD, and TAX_PAYMENT tables.
 */
public class TaxTypeDAO {

    private static final Logger LOGGER = Logger.getLogger(TaxTypeDAO.class.getName());

    private void checkOracleReachability() {
        if (!DatabaseConfig.isOracleReachable() && !DatabaseConfig.isFallbackAllowed()) {
            throw new IllegalStateException("Oracle Database is unreachable and fallback is disabled (db.allowFallback=false).");
        }
    }

    public List<TaxType> findAll(String search) {
        checkOracleReachability();
        List<TaxType> list = new ArrayList<>();

        if (DatabaseConfig.isOracleReachable()) {
            StringBuilder sb = new StringBuilder(
                "SELECT tt.TAX_TYPE_ID, tt.TAX_NAME, tt.TAX_RATE, " +
                "COUNT(p.PAYMENT_ID) AS TRANSACTION_COUNT, " +
                "NVL(SUM(CASE WHEN p.PAYMENT_STATUS = 'PAID' THEN p.AMOUNT_PAID ELSE 0 END), 0) AS TOTAL_COLLECTED " +
                "FROM TAX_TYPE tt " +
                "LEFT JOIN TAX_RECORD tr ON tt.TAX_TYPE_ID = tr.TAX_TYPE_ID " +
                "LEFT JOIN TAX_PAYMENT p ON tr.TAX_ID = p.TAX_ID " +
                "WHERE 1=1 "
            );
            List<Object> params = new ArrayList<>();
            if (search != null && !search.trim().isEmpty()) {
                sb.append("AND LOWER(tt.TAX_NAME) LIKE LOWER(?) ");
                params.add("%" + search.trim() + "%");
            }
            sb.append("GROUP BY tt.TAX_TYPE_ID, tt.TAX_NAME, tt.TAX_RATE ");
            sb.append("ORDER BY tt.TAX_TYPE_ID ASC");

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sb.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TaxType t = mapTaxType(rs);
                        t.setTransactionCount(rs.getInt("TRANSACTION_COUNT"));
                        t.setTotalCollected(rs.getBigDecimal("TOTAL_COLLECTED"));
                        list.add(t);
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
        List<TaxType> filtered = MemoryStore.getTaxTypes().values().stream()
                .filter(t -> sTerm.isEmpty() || t.getTaxName().toLowerCase().contains(sTerm))
                .map(this::cloneTaxType)
                .collect(Collectors.toList());

        for (TaxType t : filtered) {
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            for (TaxPayment p : MemoryStore.getPayments().values()) {
                if (p.getTaxId().equals(t.getTaxId())) {
                    count++;
                    if ("PAID".equalsIgnoreCase(p.getStatus())) {
                        sum = sum.add(p.getAmount());
                    }
                }
            }
            t.setTransactionCount(count);
            t.setTotalCollected(sum);
        }

        filtered.sort(Comparator.comparing(TaxType::getTaxId));
        return filtered;
    }

    public TaxType findById(Long taxId) {
        checkOracleReachability();
        if (taxId == null) return null;

        if (DatabaseConfig.isOracleReachable()) {
            String sql = "SELECT tt.TAX_TYPE_ID, tt.TAX_NAME, tt.TAX_RATE, " +
                         "COUNT(p.PAYMENT_ID) AS TRANSACTION_COUNT, " +
                         "NVL(SUM(CASE WHEN p.PAYMENT_STATUS = 'PAID' THEN p.AMOUNT_PAID ELSE 0 END), 0) AS TOTAL_COLLECTED " +
                         "FROM TAX_TYPE tt " +
                         "LEFT JOIN TAX_RECORD tr ON tt.TAX_TYPE_ID = tr.TAX_TYPE_ID " +
                         "LEFT JOIN TAX_PAYMENT p ON tr.TAX_ID = p.TAX_ID " +
                         "WHERE tt.TAX_TYPE_ID = ? " +
                         "GROUP BY tt.TAX_TYPE_ID, tt.TAX_NAME, tt.TAX_RATE";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, taxId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        TaxType t = mapTaxType(rs);
                        t.setTransactionCount(rs.getInt("TRANSACTION_COUNT"));
                        t.setTotalCollected(rs.getBigDecimal("TOTAL_COLLECTED"));
                        return t;
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Oracle query error in findById: " + e.getMessage(), e);
                if (!DatabaseConfig.isFallbackAllowed()) {
                    throw new RuntimeException("Oracle error: " + e.getMessage(), e);
                }
            }
        }

        TaxType t = MemoryStore.getTaxTypes().get(taxId);
        if (t != null) {
            TaxType res = cloneTaxType(t);
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            for (TaxPayment p : MemoryStore.getPayments().values()) {
                if (p.getTaxId().equals(res.getTaxId())) {
                    count++;
                    if ("PAID".equalsIgnoreCase(p.getStatus())) {
                        sum = sum.add(p.getAmount());
                    }
                }
            }
            res.setTransactionCount(count);
            res.setTotalCollected(sum);
            return res;
        }
        return null;
    }

    public TaxType findByName(String taxName) {
        checkOracleReachability();
        if (taxName == null) return null;
        taxName = taxName.trim();

        if (DatabaseConfig.isOracleReachable()) {
            String sql = "SELECT TAX_TYPE_ID, TAX_NAME, TAX_RATE FROM TAX_TYPE WHERE LOWER(TAX_NAME) = LOWER(?)";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, taxName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapTaxType(rs);
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Oracle query error in findByName: " + e.getMessage(), e);
                if (!DatabaseConfig.isFallbackAllowed()) {
                    throw new RuntimeException("Oracle error: " + e.getMessage(), e);
                }
            }
        }

        for (TaxType t : MemoryStore.getTaxTypes().values()) {
            if (t.getTaxName().equalsIgnoreCase(taxName)) {
                return cloneTaxType(t);
            }
        }
        return null;
    }

    public boolean create(TaxType taxType) {
        checkOracleReachability();
        if (DatabaseConfig.isOracleReachable()) {
            String sql = "INSERT INTO TAX_TYPE (TAX_NAME, TAX_RATE) VALUES (?, ?)";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, new String[]{"TAX_TYPE_ID"})) {
                conn.setAutoCommit(false);
                ps.setString(1, taxType.getTaxName());
                ps.setBigDecimal(2, taxType.getTaxRate());
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            taxType.setTaxId(rs.getLong(1));
                        }
                    }
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Oracle insertion error in create: " + e.getMessage(), e);
                if (!DatabaseConfig.isFallbackAllowed()) {
                    throw new RuntimeException("Oracle insertion error in create: " + e.getMessage(), e);
                }
            }
        }

        taxType.setTaxId(MemoryStore.nextTaxTypeId());
        taxType.setCreatedAt(new Date(System.currentTimeMillis()));
        MemoryStore.getTaxTypes().put(taxType.getTaxId(), cloneTaxType(taxType));
        return true;
    }

    public boolean update(TaxType taxType) {
        checkOracleReachability();
        if (DatabaseConfig.isOracleReachable()) {
            String sql = "UPDATE TAX_TYPE SET TAX_NAME = ?, TAX_RATE = ? WHERE TAX_TYPE_ID = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                ps.setString(1, taxType.getTaxName());
                ps.setBigDecimal(2, taxType.getTaxRate());
                ps.setLong(3, taxType.getTaxId());
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Oracle update error in update: " + e.getMessage(), e);
                if (!DatabaseConfig.isFallbackAllowed()) {
                    throw new RuntimeException("Oracle update error in update: " + e.getMessage(), e);
                }
            }
        }

        TaxType existing = MemoryStore.getTaxTypes().get(taxType.getTaxId());
        if (existing != null) {
            existing.setTaxName(taxType.getTaxName());
            existing.setTaxRate(taxType.getTaxRate());
            existing.setDescription(taxType.getDescription());
            return true;
        }
        return false;
    }

    public boolean hasAssociatedPayments(Long taxId) {
        checkOracleReachability();
        if (DatabaseConfig.isOracleReachable()) {
            String sql = "SELECT COUNT(*) FROM TAX_RECORD WHERE TAX_TYPE_ID = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, taxId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Oracle query error in hasAssociatedPayments: " + e.getMessage(), e);
                if (!DatabaseConfig.isFallbackAllowed()) {
                    throw new RuntimeException("Oracle error: " + e.getMessage(), e);
                }
            }
        }

        for (TaxPayment p : MemoryStore.getPayments().values()) {
            if (p.getTaxId().equals(taxId)) return true;
        }
        return false;
    }

    public boolean delete(Long taxId) {
        checkOracleReachability();
        if (hasAssociatedPayments(taxId)) {
            LOGGER.warning("Refusing to delete TAX_TYPE " + taxId + ": associated records exist.");
            return false;
        }

        if (DatabaseConfig.isOracleReachable()) {
            String sql = "DELETE FROM TAX_TYPE WHERE TAX_TYPE_ID = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                ps.setLong(1, taxId);
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
                    throw new RuntimeException("Oracle delete error in delete: " + e.getMessage(), e);
                }
            }
        }

        return MemoryStore.getTaxTypes().remove(taxId) != null;
    }

    private TaxType mapTaxType(ResultSet rs) throws SQLException {
        TaxType t = new TaxType();
        t.setTaxId(rs.getLong("TAX_TYPE_ID"));
        t.setTaxName(rs.getString("TAX_NAME"));
        t.setTaxRate(rs.getBigDecimal("TAX_RATE"));
        t.setDescription(t.getTaxName());
        t.setCreatedAt(null);
        return t;
    }

    private TaxType cloneTaxType(TaxType src) {
        TaxType t = new TaxType(src.getTaxId(), src.getTaxName(), src.getTaxRate(), src.getDescription());
        t.setCreatedAt(src.getCreatedAt());
        t.setTransactionCount(src.getTransactionCount());
        t.setTotalCollected(src.getTotalCollected());
        return t;
    }
}
