package com.ucc.attendance.desktop;

/**
 * Manages the logged-in session, JWT token, and role.
 */
public class SessionManager {
    private static String jwtToken = null;
    private static String displayName = null;
    private static String identifier = null;
    private static String userRole = null;

    public static synchronized void setSession(String token, String name, String id, String role) {
        jwtToken = token;
        displayName = name;
        identifier = id;
        userRole = role;
    }

    public static synchronized void clearSession() {
        jwtToken = null;
        displayName = null;
        identifier = null;
        userRole = null;
    }

    public static synchronized boolean isLoggedIn() {
        return jwtToken != null;
    }

    public static synchronized String getJwtToken() {
        return jwtToken;
    }

    public static synchronized String getDisplayName() {
        return displayName;
    }

    public static synchronized String getIdentifier() {
        return identifier;
    }

    public static synchronized String getUserRole() {
        return userRole;
    }

    public static synchronized void setDisplayName(String name) {
        displayName = name;
    }

    // Backward compatibility for lecturer portal
    public static synchronized String getLecturerName() {
        return displayName;
    }

    public static synchronized String getLecturerCode() {
        return identifier;
    }
}
