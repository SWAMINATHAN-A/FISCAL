package com.fiscal.model;

import java.io.Serializable;
import java.sql.Date;

/**
 * Domain entity representing an authenticated operator or administrator.
 */
public class AppUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private transient String passwordHash; // Never serialized in JSON output
    private transient String salt;         // Never serialized in JSON output
    private String fullName;
    private String email;
    private String role;     // 'ADMIN', 'OFFICER', 'AUDITOR'
    private Integer active; // 1 for Active, 0 for Inactive
    private String status;   // 'ACTIVE', 'INACTIVE'
    private Date createdAt;

    public AppUser() {}

    public AppUser(Long userId, String username, String fullName, String email, String role, String status, Date createdAt) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.status = status;
        this.active = "ACTIVE".equalsIgnoreCase(status) ? 1 : 0;
        this.createdAt = createdAt;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getFullName() {
        if (fullName != null && !fullName.trim().isEmpty()) return fullName;
        return username != null ? (Character.toUpperCase(username.charAt(0)) + username.substring(1)) : "User";
    }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getActive() { return active != null ? active : ("ACTIVE".equalsIgnoreCase(status) ? 1 : 0); }
    public void setActive(Integer active) {
        this.active = active;
        this.status = (active != null && active == 1) ? "ACTIVE" : "INACTIVE";
    }

    public String getStatus() {
        if (active != null) return (active == 1) ? "ACTIVE" : "INACTIVE";
        return status != null ? status : "ACTIVE";
    }
    public void setStatus(String status) {
        this.status = status;
        this.active = "ACTIVE".equalsIgnoreCase(status) ? 1 : 0;
    }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
