package net.calvuz.qdue.core.domain.quattrodue.models;

import androidx.annotation.NonNull;

import net.calvuz.qdue.QDue;

/**
 * ✅ DOMAIN ENUM: Work Schedule Type
 *
 * <p>Defines the different types of work schedule templates supported by the
 * system. Each type determines which WorkScheduleProvider implementation
 * should handle the template generation.</p>
 *
 * <p>Template types:</p>
 * <ul>
 *   <li><strong>FIXED_4_2</strong> - Classic 4-2 continuous pattern (18-day cycle)</li>
 *   <li><strong>FIXED_3_2</strong> - Alternative 3-2 pattern (15-day cycle)</li>
 *   <li><strong>FIXED_5_2</strong> - Standard 5-2 weekly pattern (7-day cycle)</li>
 *   <li><strong>CUSTOM</strong> - User-defined flexible patterns</li>
 *   <li><strong>IMPORT</strong> - Imported from external systems</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 5
 */
public enum WorkScheduleType {

    // ==================== PREDEFINED PATTERNS ====================

    /**
     * Classic 4-2 continuous schedule pattern.
     * - 18-day rotation cycle
     * - 3 shifts per day (Morning, Afternoon, Night)
     * - 9 teams with 4 work days followed by 2 rest days
     */
    FIXED_4_2("4-2 Continuous", "Classic 4-2 pattern with 18-day rotation", 18, true, false),

    /**
     * Alternative 3-2 continuous schedule pattern.
     * - 15-day rotation cycle
     * - 3 shifts per day
     * - Modified rotation with 3 work days followed by 2 rest days
     */
    FIXED_3_2("3-2 Continuous", "Alternative 3-2 pattern with 15-day rotation", 15, true, false),

    /**
     * Standard 5-2 weekly schedule pattern.
     * - 7-day rotation cycle (weekly)
     * - Traditional Monday-Friday work, Saturday-Sunday rest
     * - Suitable for office or administrative work
     */
    FIXED_5_2("5-2 Weekly", "Standard weekly 5-2 pattern", 7, true, false),

    // ==================== FLEXIBLE PATTERNS ====================

    /**
     * User-defined custom schedule patterns.
     * - Variable cycle length
     * - Flexible shift assignments
     * - Fully customizable by users
     */
    CUSTOM("Custom Pattern", "User-defined flexible schedule pattern", 0, false, true),

    /**
     * Templates imported from external systems.
     * - Variable cycle length
     * - Imported configuration
     * - May require adaptation to system format
     */
    IMPORT("Imported Template", "Template imported from external system", 0, false, true);

    // ==================== ENUM PROPERTIES ====================

    private final String displayName;        // Human-readable name
    private final String description;        // Description of the pattern
    private final int defaultCycleDays;      // Default cycle length (0 = variable)
    private final boolean isPredefined;      // System-predefined vs user-customizable
    private final boolean allowsCustomization; // Can be modified by users

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for WorkScheduleType enum values.
     *
     * @param displayName Human-readable name
     * @param description Pattern description
     * @param defaultCycleDays Default cycle length (0 for variable)
     * @param isPredefined Whether this is a system-predefined pattern
     * @param allowsCustomization Whether users can modify this pattern
     */
    WorkScheduleType(String displayName, String description, int defaultCycleDays,
                     boolean isPredefined, boolean allowsCustomization) {
        this.displayName = displayName;
        this.description = description;
        this.defaultCycleDays = defaultCycleDays;
        this.isPredefined = isPredefined;
        this.allowsCustomization = allowsCustomization;
    }

    // ==================== BUSINESS LOGIC METHODS ====================

    /**
     * Checks if this type supports fixed cycle lengths.
     *
     * @return true if cycle length is fixed
     */
    public boolean hasFixedCycle() {
        return defaultCycleDays > 0;
    }

    /**
     * Checks if this type requires a specific provider implementation.
     *
     * @return true if requires specific provider
     */
    public boolean requiresSpecificProvider() {
        return isPredefined;
    }

    /**
     * Gets the provider class name that should handle this type.
     *
     * @return Provider class name
     */
    public String getPreferredProviderName() {
        switch (this) {
            case FIXED_4_2:
            case FIXED_3_2:
            case FIXED_5_2:
                return "FixedScheduleProvider";
            case CUSTOM:
            case IMPORT:
            default:
                return "CustomScheduleProvider";
        }
    }

    /**
     * Validates if a cycle length is appropriate for this type.
     *
     * @param cycleDays Proposed cycle length
     * @return true if cycle length is valid
     */
    public boolean isValidCycleLength(int cycleDays) {
        if (cycleDays <= 0) {
            return false;
        }

        if (hasFixedCycle()) {
            return cycleDays == defaultCycleDays;
        }

        // For flexible types, allow reasonable ranges
        return cycleDays >= 1 && cycleDays <= 365;
    }

    /**
     * Gets all predefined (system) schedule types.
     *
     * @return Array of predefined types
     */
    public static WorkScheduleType[] getPredefinedTypes() {
        return new WorkScheduleType[]{FIXED_4_2, FIXED_3_2, FIXED_5_2};
    }

    /**
     * Gets all flexible (user-customizable) schedule types.
     *
     * @return Array of flexible types
     */
    public static WorkScheduleType[] getFlexibleTypes() {
        return new WorkScheduleType[]{CUSTOM, IMPORT};
    }

    /**
     * Finds a schedule type by its display name (case insensitive).
     *
     * @param displayName Display name to search for
     * @return Matching type, or null if not found
     */
    public static WorkScheduleType findByDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return null;
        }

        for (WorkScheduleType type : values()) {
            if (type.displayName.equalsIgnoreCase(displayName.trim())) {
                return type;
            }
        }

        return null;
    }

    /**
     * Gets the most appropriate type for a given cycle length.
     * Used when importing or auto-detecting schedule patterns.
     *
     * @param cycleDays Cycle length to match
     * @return Best matching type, or CUSTOM if no match
     */
    public static WorkScheduleType getBestMatchForCycle(int cycleDays) {
        for (WorkScheduleType type : getPredefinedTypes()) {
            if (type.defaultCycleDays == cycleDays) {
                return type;
            }
        }

        return CUSTOM;
    }

    // ==================== GETTERS ====================

    /**
     * Gets the human-readable display name.
     *
     * @return Display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the description of this schedule type.
     *
     * @return Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the default cycle length for this type.
     *
     * @return Default cycle days (0 = variable)
     */
    public int getDefaultCycleDays() {
        return defaultCycleDays;
    }

    /**
     * Checks if this is a system-predefined pattern.
     *
     * @return true if predefined
     */
    public boolean isPredefined() {
        return isPredefined;
    }

    /**
     * Checks if users can customize this pattern.
     *
     * @return true if customizable
     */
    public boolean allowsCustomization() {
        return allowsCustomization;
    }

    // ==================== DISPLAY METHODS ====================

    /**
     * Creates a formatted display string with details.
     *
     * @return Formatted display string
     */
    public String getFormattedDisplay() {
        StringBuilder sb = new StringBuilder(displayName);

        if (hasFixedCycle()) {
            sb.append(" (").append(defaultCycleDays).append(" days)");
        }

        if (isPredefined) {
            sb.append(" [System]");
        }

        return sb.toString();
    }

    /**
     * Creates a summary string for logging or debugging.
     *
     * @return Summary string
     */
    public String getSummary() {
        return String.format( QDue.getLocale(), "WorkScheduleType{name='%s', cycle=%d, predefined=%s, customizable=%s}",
                displayName, defaultCycleDays, isPredefined, allowsCustomization);
    }

    @NonNull
    @Override
    public String toString() {
        return displayName;
    }
}