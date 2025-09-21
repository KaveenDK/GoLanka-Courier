package lk.ijse.edu.golankacourier.util;

/**
 * --------------------------------------------
 *
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 9/21/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import java.util.regex.Pattern;

/**
 * Small validation helpers (NIC, phone, password strength).
 * Keep regexes conservative; adapt if you need special cases.
 */
public final class ValidationUtil {

    private ValidationUtil() { /* no-op */ }

    // Sri Lankan NIC old format: 9 digits + V or X (e.g. 861234567V)
    // New format: 12 digits (YYYYXXXXXXXX)
    private static final Pattern OLD_NIC = Pattern.compile("^[0-9]{9}[VvXx]$");
    private static final Pattern NEW_NIC = Pattern.compile("^[0-9]{12}$");

    // Phone: allow +, digits, spaces, dashes; minimum 6 chars
    private static final Pattern PHONE = Pattern.compile("^[+0-9\\-\\s]{6,20}$");

    // Basic password strength: at least 8 chars, contains letter and number (improve as needed)
    private static final Pattern STRONG_PASSWORD = Pattern.compile("^(?=.{8,}$)(?=.*[A-Za-z])(?=.*\\d).*$");

    public static boolean isValidSriLankanNic(String nic) {
        if (nic == null) return false;
        String t = nic.trim();
        return OLD_NIC.matcher(t).matches() || NEW_NIC.matcher(t).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        return PHONE.matcher(phone.trim()).matches();
    }

    public static boolean isStrongPassword(String password) {
        if (password == null) return false;
        return STRONG_PASSWORD.matcher(password).matches();
    }

    /**
     * Utility to normalize phone (remove spaces/dashes). This does not validate.
     */
    public static String normalizePhone(String phone) {
        if (phone == null) return null;
        return phone.replaceAll("[\\s\\-]", "");
    }
}
