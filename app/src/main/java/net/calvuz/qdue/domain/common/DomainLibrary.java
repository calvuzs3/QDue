package net.calvuz.qdue.domain.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DomainLibrary {

    // ==================== LOCALIZED LOGGING METHODS ====================

    public static void logDebug(@NonNull String message) {
        // Domain logging - simplified for clean architecture
        System.out.println("[DEBUG] ExceptionResolver: " + message);
    }

    public static void logVerbose(@NonNull String message) {
        // Domain logging - simplified for clean architecture
        System.out.println("[VERBOSE] ExceptionResolver: " + message);
    }

    public static void logWarning(@NonNull String message) {
        // Domain logging - simplified for clean architecture
        System.out.println("[WARNING] ExceptionResolver: " + message);
    }

    public static void logError(@NonNull String message, @Nullable Exception e) {
        // Domain logging - simplified for clean architecture
        System.out.println("[ERROR] ExceptionResolver: " + message +
                (e != null ? " - " + e.getMessage() : ""));
    }
}
