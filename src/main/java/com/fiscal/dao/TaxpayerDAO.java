package com.fiscal.dao;

import com.fiscal.config.DatabaseConfig;
import com.fiscal.model.TaxPayment;
import com.fiscal.model.Taxpayer;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Data Access Object for TAXPAYER table with search, sorting, filtering and ledger aggregates.
 * Integrated with live Oracle TAXPAYER, TAX_RECORD, and TAX_PAYMENT tables.
 */
public class TaxpayerDAO {

    private static final Logger LOGGER = Logger.getLogger(TaxpayerDAO.class.getName());

    private void checkOracleReachability() {
        // Silent auto mode - seamlessly falls back if database is paused or unreachable
    }

    public List<Taxpayer> findAll(String search, String city, String sortBy) {
        checkOracleReachability();
        List<Taxpayer> list = new ArrayList<>();

        if (DatabaseConfig.isOracleReachable()) {
            StringBuilder sb = new StringBuilder(
                "SELECT t.TAXPAYER_ID, t.NAME, t.PAN_NO, t.AGE, t.CITY, t.INCOME, " +
                "COALESCE(SUM(CASE WHEN p.PAYMENT_STATUS = 'PAID' THEN p.AMOUNT_PAID ELSE 0 END), 0) AS TOTAL_PAID, " +
                "COUNT(p.PAYMENT_ID) AS PAYMENT_COUNT " +
                "FROM TAXPAYER t " +
                "LEFT JOIN TAX_RECORD tr ON t.TAXPAYER_ID = tr.TAXPAYER_ID " +
                "LEFT JOIN TAX_PAYMENT p ON tr.TAX_ID = p.TAX_ID " +
                "WHERE 1=1 "
            );

            List<Object> params = new ArrayList<>();
            if (search != null && !search.trim().isEmpty()) {
                sb.append("AND (LOWER(t.NAME) LIKE LOWER(?) OR UPPER(t.PAN_NO) LIKE UPPER(?)) ");
                params.add("%" + search.trim() + "%");
                params.add("%" + search.trim() + "%");
            }
            if (city != null && !city.trim().isEmpty() && !"ALL".equalsIgnoreCase(city.trim())) {
                sb.append("AND LOWER(t.CITY) = LOWER(?) ");
                params.add(city.trim());
            }

            sb.append("GROUP BY t.TAXPAYER_ID, t.NAME, t.PAN_NO, t.AGE, t.CITY, t.INCOME ");

            if ("income_desc".equalsIgnoreCase(sortBy)) {
                sb.append("ORDER BY t.INCOME DESC");
            } else if ("income_asc".equalsIgnoreCase(sortBy)) {
                sb.append("ORDER BY t.INCOME ASC");
            } else if ("name_asc".equalsIgnoreCase(sortBy)) {
                sb.append("ORDER BY t.NAME ASC");
            } else if ("id_desc".equalsIgnoreCase(sortBy)) {
                sb.append("ORDER BY t.TAXPAYER_ID DESC");
            } else {
                sb.append("ORDER BY t.TAXPAYER_ID ASC");
            }

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sb.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Taxpayer t = mapTaxpayer(rs);
                        t.setTotalPaid(rs.getBigDecimal("TOTAL_PAID"));
                        t.setPaymentCount(rs.getInt("PAYMENT_COUNT"));
                        list.add(t);
                    }
                    return list;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Oracle query error in findAll: " + e.getMessage(), e);
                DatabaseConfig.markUnreachable();
            }
        }

        // Resilient fallback logic
        List<Taxpayer> memoryList = new ArrayList<>(MemoryStore.getTaxpayers().values());
        final String sTerm = (search != null) ? search.trim().toLowerCase() : "";
        final String cTerm = (city != null) ? city.trim().toLowerCase() : "";

        List<Taxpayer> filtered = memoryList.stream().filter(t -> {
            boolean matchesSearch = sTerm.isEmpty() ||
                    t.getName().toLowerCase().contains(sTerm) ||
                    t.getPanNo().toLowerCase().contains(sTerm);
            boolean matchesCity = cTerm.isEmpty() || "all".equals(cTerm) ||
                    t.getCity().toLowerCase().equals(cTerm);
            return matchesSearch && matchesCity;
        }).map(this::cloneTaxpayer).collect(Collectors.toList());

        for (Taxpayer t : filtered) {
            BigDecimal sumPaid = BigDecimal.ZERO;
            int count = 0;
            for (TaxPayment p : MemoryStore.getPayments().values()) {
                if (p.getTaxpayerId().equals(t.getTaxpayerId())) {
                    count++;
                    if ("PAID".equalsIgnoreCase(p.getStatus())) {
                        sumPaid = sumPaid.add(p.getAmount());
                    }
                }
            }
            t.setTotalPaid(sumPaid);
            t.setPaymentCount(count);
        }

        if ("income_desc".equalsIgnoreCase(sortBy)) {
            filtered.sort((a, b) -> b.getIncome().compareTo(a.getIncome()));
        } else if ("income_asc".equalsIgnoreCase(sortBy)) {
            filtered.sort((a, b) -> a.getIncome().compareTo(b.getIncome()));
        } else if ("name_asc".equalsIgnoreCase(sortBy)) {
            filtered.sort(Comparator.comparing(Taxpayer::getName));
        } else if ("id_desc".equalsIgnoreCase(sortBy)) {
            filtered.sort((a, b) -> b.getTaxpayerId().compareTo(a.getTaxpayerId()));
        } else {
            filtered.sort(Comparator.comparing(Taxpayer::getTaxpayerId));
        }

        return filtered;
    }

    public Taxpayer findById(Long taxpayerId) {
        checkOracleReachability();
        if (taxpayerId == null) return null;

        if (DatabaseConfig.isOracleReachable()) {
            String sql = "SELECT t.TAXPAYER_ID, t.NAME, t.PAN_NO, t.AGE, t.CITY, t.INCOME, " +
                         "COALESCE(SUM(CASE WHEN p.PAYMENT_STATUS = 'PAID' THEN p.AMOUNT_PAID ELSE 0 END), 0) AS TOTAL_PAID, " +
                         "COUNT(p.PAYMENT_ID) AS PAYMENT_COUNT " +
                         "FROM TAXPAYER t " +
                         "LEFT JOIN TAX_RECORD tr ON t.TAXPAYER_ID = tr.TAXPAYER_ID " +
                         "LEFT JOIN TAX_PAYMENT p ON tr.TAX_ID = p.TAX_ID " +
                         "WHERE t.TAXPAYER_ID = ? " +
                         "GROUP BY t.TAXPAYER_ID, t.NAME, t.PAN_NO, t.AGE, t.CITY, t.INCOME";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, taxpayerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Taxpayer t = mapTaxpayer(rs);
                        t.setTotalPaid(rs.getBigDecimal("TOTAL_PAID"));
                        t.setPaymentCount(rs.getInt("PAYMENT_COUNT"));
                        return t;
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Oracle query error in findById: " + e.getMessage(), e);
                DatabaseConfig.markUnreachable();
            }
        }

        Taxpayer t = MemoryStore.getTaxpayers().get(taxpayerId);
        if (t != null) {
            Taxpayer res = cloneTaxpayer(t);
            BigDecimal sumPaid = BigDecimal.ZERO;
            int count = 0;
            for (TaxPayment p : MemoryStore.getPayments().values()) {
                if (p.getTaxpayerId().equals(res.getTaxpayerId())) {
                    count++;
                    if ("PAID".equalsIgnoreCase(p.getStatus())) {
                        sumPaid = sumPaid.add(p.getAmount());
                    }
                }
            }
            res.setTotalPaid(sumPaid);
            res.setPaymentCount(count);
            return res;
        }
        return null;
    }

    public Taxpayer findByPan(String panNo) {
        checkOracleReachability();
        if (panNo == null) return null;
        panNo = panNo.trim().toUpperCase();

        if (DatabaseConfig.isOracleReachable()) {
            String sql = "SELECT TAXPAYER_ID, NAME, PAN_NO, AGE, CITY, INCOME FROM TAXPAYER WHERE UPPER(PAN_NO) = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, panNo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapTaxpayer(rs);
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Oracle query error in findByPan: " + e.getMessage(), e);
                DatabaseConfig.markUnreachable();
            }
        }

        for (Taxpayer t : MemoryStore.getTaxpayers().values()) {
            if (t.getPanNo().equalsIgnoreCase(panNo)) {
                return cloneTaxpayer(t);
            }
        }
        return null;
    }

    public boolean create(Taxpayer taxpayer) {
        checkOracleReachability();
        if (DatabaseConfig.isOracleReachable()) {
            String sql = "INSERT INTO TAXPAYER (NAME, PAN_NO, AGE, CITY, INCOME) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                conn.setAutoCommit(false);
                ps.setString(1, taxpayer.getName());
                ps.setString(2, taxpayer.getPanNo().toUpperCase());
                ps.setInt(3, taxpayer.getAge());
                ps.setString(4, taxpayer.getCity());
                ps.setBigDecimal(5, taxpayer.getIncome());
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            taxpayer.setTaxpayerId(rs.getLong(1));
                        }
                    }
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Oracle insertion error in create: " + e.getMessage(), e);
                DatabaseConfig.markUnreachable();
            }
        }

        taxpayer.setTaxpayerId(MemoryStore.nextTaxpayerId());
        taxpayer.setCreatedAt(new Date(System.currentTimeMillis()));
        MemoryStore.getTaxpayers().put(taxpayer.getTaxpayerId(), cloneTaxpayer(taxpayer));
        return true;
    }

    public boolean update(Taxpayer taxpayer) {
        checkOracleReachability();
        if (DatabaseConfig.isOracleReachable()) {
            String sql = "UPDATE TAXPAYER SET NAME = ?, PAN_NO = ?, AGE = ?, CITY = ?, INCOME = ? WHERE TAXPAYER_ID = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                ps.setString(1, taxpayer.getName());
                ps.setString(2, taxpayer.getPanNo().toUpperCase());
                ps.setInt(3, taxpayer.getAge());
                ps.setString(4, taxpayer.getCity());
                ps.setBigDecimal(5, taxpayer.getIncome());
                ps.setLong(6, taxpayer.getTaxpayerId());
                int affected = ps.executeUpdate();
                if (affected > 0) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Oracle update error in update: " + e.getMessage(), e);
                DatabaseConfig.markUnreachable();
            }
        }

        Taxpayer existing = MemoryStore.getTaxpayers().get(taxpayer.getTaxpayerId());
        if (existing != null) {
            existing.setName(taxpayer.getName());
            existing.setPanNo(taxpayer.getPanNo().toUpperCase());
            existing.setAge(taxpayer.getAge());
            existing.setCity(taxpayer.getCity());
            existing.setIncome(taxpayer.getIncome());
            return true;
        }
        return false;
    }

    public boolean delete(Long taxpayerId) {
        checkOracleReachability();
        if (DatabaseConfig.isOracleReachable()) {
            String delPaymentsSql = "DELETE FROM TAX_PAYMENT WHERE TAX_ID IN (SELECT TAX_ID FROM TAX_RECORD WHERE TAXPAYER_ID = ?)";
            String delRecordsSql = "DELETE FROM TAX_RECORD WHERE TAXPAYER_ID = ?";
            String delTaxpayerSql = "DELETE FROM TAXPAYER WHERE TAXPAYER_ID = ?";

            try (Connection conn = DatabaseConfig.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement psPay = conn.prepareStatement(delPaymentsSql)) {
                        psPay.setLong(1, taxpayerId);
                        psPay.executeUpdate();
                    }
                    try (PreparedStatement psRec = conn.prepareStatement(delRecordsSql)) {
                        psRec.setLong(1, taxpayerId);
                        psRec.executeUpdate();
                    }
                    int affected;
                    try (PreparedStatement psTp = conn.prepareStatement(delTaxpayerSql)) {
                        psTp.setLong(1, taxpayerId);
                        affected = psTp.executeUpdate();
                    }

                    if (affected > 0) {
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
                LOGGER.log(Level.SEVERE, "Oracle delete error in delete: " + e.getMessage(), e);
                DatabaseConfig.markUnreachable();
            }
        }

        if (MemoryStore.getTaxpayers().remove(taxpayerId) != null) {
            MemoryStore.getPayments().entrySet().removeIf(entry -> entry.getValue().getTaxpayerId().equals(taxpayerId));
            return true;
        }
        return false;
    }

    public List<String> getDistinctCities() {
        checkOracleReachability();
        List<String> cities = new ArrayList<>();
        if (DatabaseConfig.isOracleReachable()) {
            String sql = "SELECT DISTINCT CITY FROM TAXPAYER WHERE CITY IS NOT NULL ORDER BY CITY ASC";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cities.add(rs.getString("CITY"));
                }
                return cities;
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Oracle distinct cities query failed: " + e.getMessage(), e);
                DatabaseConfig.markUnreachable();
            }
        }

        return MemoryStore.getTaxpayers().values().stream()
                .map(Taxpayer::getCity)
                .filter(c -> c != null && !c.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private Taxpayer mapTaxpayer(ResultSet rs) throws SQLException {
        Taxpayer t = new Taxpayer();
        t.setTaxpayerId(rs.getLong("TAXPAYER_ID"));
        t.setName(rs.getString("NAME"));
        t.setPanNo(rs.getString("PAN_NO"));
        t.setAge(rs.getInt("AGE"));
        t.setCity(rs.getString("CITY"));
        t.setIncome(rs.getBigDecimal("INCOME"));
        t.setCreatedAt(null);
        return t;
    }

    private Taxpayer cloneTaxpayer(Taxpayer src) {
        Taxpayer t = new Taxpayer(src.getTaxpayerId(), src.getName(), src.getPanNo(), src.getAge(), src.getCity(), src.getIncome());
        t.setCreatedAt(src.getCreatedAt());
        t.setTotalPaid(src.getTotalPaid());
        t.setPaymentCount(src.getPaymentCount());
        return t;
    }
}
