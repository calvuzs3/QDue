package net.calvuz.qdue.smartshifts.utils;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Utility class for input validation
 */
@Singleton
public class ValidationHelper {

    @Inject
    public ValidationHelper() {}

    /**
     * Validate cycle length
     */
    public boolean isValidCycleLength(int cycleLength) {
        return cycleLength >= 1 && cycleLength <= 365;
    }

    /**
     * Validate pattern name
     */
    public boolean isValidPatternName(String name) {
        return name != null && name.trim().length() >= 2 && name.trim().length() <= 100;
    }

    /**
     * Validate contact name
     */
    public boolean isValidContactName(String name) {
        return name != null && name.trim().length() >= 1 && name.trim().length() <= 50;
    }

    /**
     * Validate user ID format
     */
    public boolean isValidUserId(String userId) {
        return userId != null && userId.trim().length() > 0;
    }

    /**
     * Validate UUID format
     */
    public boolean isValidUUID(String uuid) {
        if (uuid == null) return false;
        return uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }
}

// =====================================================================

