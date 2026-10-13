package com.fiscal.dao;

import com.fiscal.config.DatabaseConfig;
import com.fiscal.model.AppUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Oracle APP_USER table:
 * USER_ID (IDENTITY), USERNAME, EMAIL, PASSWORD_HASH, ROLE, ACTIVE, CREATED_AT
 */
public class AppUserDAO {

    private static final Logger LOGGER = Logger.getLogger(AppUserDAO.class.getName());

    public AppUser findByUsername(String username) {
        if (username == null) return null;
        username = username.trim();

        if (DatabaseConfig.isOracleReachable()) {
            String sql = "SELECT USER_ID, USERNAME, FULL_NAME, EMAIL, PASSWORD_HASH, SALT, ROLE, STATUS, ACTIVE, CREATED_AT " +
                         "FROM APP_USER WHERE LOWER(USERNAME) = LOWER(?)";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapUser(rs);
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Oracle query error in findByUsername: " + e.getMessage(), e);
                if (!DatabaseConfig.isFallbackAllowed()) {
                    throw new IllegalStateException("Database error fetching user: " + e.getMessage(), e);
                }
            }
        }

        if (DatabaseConfig.isFallbackAllowed()) {
            for (AppUser user : MemoryStore.getUsers().values()) {
                if (user.getUsername().equalsIgnoreCase(username)) {
                    return cloneUser(user);
                }
            }
        }
        return null;
    }

    public AppUser findById(Long userId) {
        if (userId == null) return null;

        if (DatabaseConfig.isOracleReachable()) {
            String sql = "SELECT USER_ID, USERNAME, FULL_NAME, EMAIL, PASSWORD_HASH, SALT, ROLE, STATUS, ACTIVE, CREATED_AT " +
                         "FROM APP_USER WHERE USER_ID = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapUser(rs);
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Oracle query error in findById: " + e.getMessage(), e);
                if (!DatabaseConfig.isFallbackAllowed()) {
                    throw new IllegalStateException("Database error fetching user by ID: " + e.getMessage(), e);
                }
            }
        }

        if (DatabaseConfig.isFallbackAllowed()) {
            AppUser user = MemoryStore.getUsers().get(userId);
            return user != null ? cloneUser(user) : null;
        }
        return null;
    }

    public List<AppUser> findAll() {
        List<AppUser> list = new ArrayList<>();
        if (DatabaseConfig.isOracleReachable()) {
            String sql = "SELECT USER_ID, USERNAME, FULL_NAME, EMAIL, PASSWORD_HASH, SALT, ROLE, STATUS, ACTIVE, CREATED_AT " +
                         "FROM APP_USER ORDER BY USER_ID ASC";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapUser(rs));
                }
                return list;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Oracle query error in findAll: " + e.getMessage(), e);
                if (!DatabaseConfig.isFallbackAllowed()) {
                    throw new IllegalStateException("Database error listing users: " + e.getMessage(), e);
                }
            }
        }

        if (DatabaseConfig.isFallbackAllowed()) {
            for (AppUser user : MemoryStore.getUsers().values()) {
                list.add(cloneUser(user));
            }
            list.sort((a, b) -> a.getUserId().compareTo(b.getUserId()));
        }
        return list;
    }

    public boolean create(AppUser user) {
        if (DatabaseConfig.isOracleReachable()) {
            String sql = "INSERT INTO APP_USER (USERNAME, EMAIL, PASSWORD_HASH, ROLE, ACTIVE, CREATED_AT) " +
                         "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getEmail());
                ps.setString(3, user.getPasswordHash());
                ps.setString(4, "ADMIN".equalsIgnoreCase(user.getRole()) ? "ADMIN" : "USER");
                ps.setInt(5, user.getActive() != null ? user.getActive() : 1);
                int affected = ps.executeUpdate();
                if (affected > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            user.setUserId(rs.getLong(1));
                        }
                    }
                    return true;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Oracle insertion error in create: " + e.getMessage(), e);
                if (!DatabaseConfig.isFallbackAllowed()) {
                    throw new IllegalStateException("Database error creating user: " + e.getMessage(), e);
                }
            }
        }

        if (DatabaseConfig.isFallbackAllowed()) {
            user.setUserId(MemoryStore.nextUserId());
            user.setCreatedAt(new Date(System.currentTimeMillis()));
            if (user.getStatus() == null) user.setStatus("ACTIVE");
            MemoryStore.getUsers().put(user.getUserId(), cloneUser(user));
            return true;
        }
        return false;
    }

    public boolean update(AppUser user) {
        if (DatabaseConfig.isOracleReachable()) {
            String sql = "UPDATE APP_USER SET EMAIL = ?, ROLE = ?, ACTIVE = ? WHERE USER_ID = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user.getEmail());
                ps.setString(2, "ADMIN".equalsIgnoreCase(user.getRole()) ? "ADMIN" : "USER");
                ps.setInt(3, user.getActive() != null ? user.getActive() : 1);
                ps.setLong(4, user.getUserId());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Oracle update error in update: " + e.getMessage(), e);
                if (!DatabaseConfig.isFallbackAllowed()) {
                    throw new IllegalStateException("Database error updating user: " + e.getMessage(), e);
                }
            }
        }

        if (DatabaseConfig.isFallbackAllowed()) {
            AppUser existing = MemoryStore.getUsers().get(user.getUserId());
            if (existing != null) {
                existing.setEmail(user.getEmail());
                existing.setRole(user.getRole());
                existing.setActive(user.getActive());
                return true;
            }
        }
        return false;
    }

    public boolean delete(Long userId) {
        if (DatabaseConfig.isOracleReachable()) {
            String sql = "DELETE FROM APP_USER WHERE USER_ID = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, userId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Oracle delete error in delete: " + e.getMessage(), e);
                if (!DatabaseConfig.isFallbackAllowed()) {
                    throw new IllegalStateException("Database error deleting user: " + e.getMessage(), e);
                }
            }
        }
        if (DatabaseConfig.isFallbackAllowed()) {
            return MemoryStore.getUsers().remove(userId) != null;
        }
        return false;
    }

    private AppUser mapUser(ResultSet rs) throws SQLException {
        AppUser u = new AppUser();
        u.setUserId(rs.getLong("USER_ID"));
        u.setUsername(rs.getString("USERNAME"));
        try { u.setFullName(rs.getString("FULL_NAME")); } catch (Exception ignored) {}
        u.setEmail(rs.getString("EMAIL"));
        u.setPasswordHash(rs.getString("PASSWORD_HASH"));
        try { u.setSalt(rs.getString("SALT")); } catch (Exception ignored) {}
        u.setRole(rs.getString("ROLE"));
        try {
            String st = rs.getString("STATUS");
            u.setStatus(st != null ? st : "ACTIVE");
        } catch (Exception ignored) {
            u.setStatus("ACTIVE");
        }
        int active = rs.getInt("ACTIVE");
        u.setActive(active);
        Timestamp ts = rs.getTimestamp("CREATED_AT");
        if (ts != null) {
            u.setCreatedAt(new Date(ts.getTime()));
        }
        return u;
    }

    private AppUser cloneUser(AppUser src) {
        AppUser u = new AppUser(src.getUserId(), src.getUsername(), src.getFullName(), src.getEmail(), src.getRole(), src.getStatus(), src.getCreatedAt());
        u.setPasswordHash(src.getPasswordHash());
        u.setSalt(src.getSalt());
        u.setActive(src.getActive());
        return u;
    }
}
