package com.ucc.attendance.desktop;

import java.io.InputStream;
import java.util.Properties;

/**
 * Resolves the REST API base URL for the desktop client.
 * <p>
 * Priority: {@code -Dapi.url} → {@code API_URL} env → {@code api.properties} →
 * {@code ATTENDIQ_DEV=true} (localhost) → production default.
 */
public final class ApiConfig {

    public static final String PRODUCTION_URL = "https://ucc-attendance-system.onrender.com";
    public static final String LOCAL_DEV_URL = "http://localhost:8080";

    private static final String PROPERTIES_FILE = "api.properties";

    private ApiConfig() {}

    public static String resolveBaseUrl() {
        String sys = System.getProperty("api.url");
        if (isSet(sys)) {
            return normalize(sys);
        }

        String env = System.getenv("API_URL");
        if (isSet(env)) {
            return normalize(env);
        }

        String fromProps = loadFromClasspath();
        if (isSet(fromProps)) {
            return normalize(fromProps);
        }

        if (isLocalDevMode()) {
            return LOCAL_DEV_URL;
        }

        return PRODUCTION_URL;
    }

    private static boolean isLocalDevMode() {
        String dev = System.getenv("ATTENDIQ_DEV");
        return "true".equalsIgnoreCase(dev) || "1".equals(dev);
    }

    private static String loadFromClasspath() {
        try (InputStream in = ApiConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                return null;
            }
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("api.url");
        } catch (Exception e) {
            System.err.println("Could not load " + PROPERTIES_FILE + ": " + e.getMessage());
            return null;
        }
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalize(String url) {
        String trimmed = url.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
