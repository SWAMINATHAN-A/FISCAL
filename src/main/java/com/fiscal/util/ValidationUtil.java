package com.fiscal.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Enterprise validation and sanitization utility for tax records and user inputs.
 */
public final class ValidationUtil {

    // Standard statutory PAN format (5 letters + 4 digits + 1 letter) or demo alphanumeric PAN (e.g. DEMO00001)
    private static final Pattern PAN_PATTERN = Pattern.compile("^([A-Z]{5}[0-9]{4}[A-Z]|[A-Z0-9]{5,10})$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private ValidationUtil() {}

    /**
     * Validates statutory or demo PAN number format.
     */
    public static boolean isValidPan(String pan) {
        if (pan == null) return false;
        return PAN_PATTERN.matcher(pan.trim().toUpperCase()).matches();
    }

    /**
     * Normalizes PAN number to uppercase trimmed format.
     */
    public static String normalizePan(String pan) {
        if (pan == null) return null;
        return pan.trim().toUpperCase();
    }

    /**
     * Validates taxpayer legal age boundary.
     */
    public static boolean isValidAge(Integer age) {
        return age != null && age >= 18 && age <= 120;
    }

    /**
     * Validates non-negative financial amount.
     */
    public static boolean isNonNegativeAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * Validates positive financial amount (> 0).
     */
    public static boolean isPositiveAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Validates statutory tax percentage rate (0.00% to 100.00%).
     */
    public static boolean isValidTaxRate(BigDecimal rate) {
        return rate != null && rate.compareTo(BigDecimal.ZERO) >= 0 && rate.compareTo(new BigDecimal("100.00")) <= 0;
    }

    /**
     * Validates email format.
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validates payment status token.
     */
    public static boolean isValidPaymentStatus(String status) {
        if (status == null) return false;
        String s = status.trim().toUpperCase();
        return "PAID".equals(s) || "PENDING".equals(s) || "REVERSED".equals(s);
    }

    /**
     * Validates non-empty string.
     */
    public static boolean isNonEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * Sanitizes untrusted text to prevent Cross-Site Scripting (XSS).
     */
    public static String escapeHtml(String input) {
        if (input == null) return null;
        StringBuilder out = new StringBuilder(Math.max(16, input.length()));
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') {
                out.append("&quot;");
            } else if (c == '&') {
                out.append("&amp;");
            } else if (c == '\'') {
                out.append("&#x27;");
            } else if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
