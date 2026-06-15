package com.ucc.attendance.util;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class DeviceFingerprintUtil {

    private DeviceFingerprintUtil() {
    }

    public static String resolve(String clientDeviceId, String legacyFingerprint, HttpServletRequest request) {
        if (clientDeviceId != null && !clientDeviceId.isBlank()) {
            String ua = nullToEmpty(request.getHeader("User-Agent"));
            String ip = nullToEmpty(request.getRemoteAddr());
            return sha256(clientDeviceId.trim() + "|" + ua + "|" + ip);
        }
        if (legacyFingerprint != null && !legacyFingerprint.isBlank()) {
            return legacyFingerprint.trim();
        }
        String ua = nullToEmpty(request.getHeader("User-Agent"));
        String ip = nullToEmpty(request.getRemoteAddr());
        return sha256(ua + "|" + ip);
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
