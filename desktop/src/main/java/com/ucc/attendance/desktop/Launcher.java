package com.ucc.attendance.desktop;

/**
 * Launcher class to bypass JavaFX module path verification.
 * Enables running the application from classpath without JVM crashes.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
