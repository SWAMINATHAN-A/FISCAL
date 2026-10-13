package com.fiscal.service;

import com.fiscal.dao.AppUserDAO;
import com.fiscal.model.AppUser;
import com.fiscal.util.PasswordUtil;

import java.util.logging.Logger;

/**
 * Authentication and Session Security Service.
 */
public class AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());
    private final AppUserDAO userDAO = new AppUserDAO();

    public AppUser authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }

        AppUser user = userDAO.findByUsername(username.trim());
        if (user == null) {
            LOGGER.warning("Authentication failed: Unknown username '" + username + "'");
            return null;
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            LOGGER.warning("Authentication failed: User account is deactivated for '" + username + "'");
            return null;
        }

        boolean valid = PasswordUtil.verifyPassword(password, user.getPasswordHash(), user.getSalt());
        if (!valid) {
            LOGGER.warning("Authentication failed: Invalid credentials for user '" + username + "'");
            return null;
        }

        LOGGER.info("Authentication successful for user '" + username + "' [Role: " + user.getRole() + "]");
        return user;
    }

    public AppUser getUserById(Long userId) {
        return userDAO.findById(userId);
    }
}
