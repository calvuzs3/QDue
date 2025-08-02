package net.calvuz.qdue.core.domain.events.models;

import static net.calvuz.qdue.ui.core.common.utils.Library.getString;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;

/**
 * EventTypeCategory - Internationalized categorization system for EventTypes.
 *
 * <p>Provides logical grouping of EventTypes for UI organization, filtering,
 * and business logic simplification. Each category includes localized display
 * names, representative icons, and semantic grouping for related event types.</p>
 *
 * <p>Categories are designed to:</p>
 * <ul>
 *   <li>Simplify EventType business logic methods</li>
 *   <li>Enable efficient UI filtering and grouping</li>
 *   <li>Provide consistent iconography across the application</li>
 *   <li>Support internationalization for global deployment</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 6
 */
public enum EventTypeCategory {

    /**
     * General events including meetings, training, and miscellaneous activities.
     * Used for standard business and operational events.
     */
    GENERAL(R.string.event_category_general,
            R.string.event_category_general_description,
            R.drawable.ic_rounded_event_24),

    /**
     * Work absence events that affect attendance and scheduling.
     * Includes vacation, sick leave, personal time, and other absence types.
     */
    ABSENCE(R.string.event_category_absence,
            R.string.event_category_absence_description,
            R.drawable.ic_rounded_person_off_24),

    /**
     * Work schedule adjustments including overtime, shift swaps, and compensation.
     * Events that modify the standard work schedule without being absences.
     */
    WORK_ADJUSTMENT(R.string.event_category_work_adjustment,
            R.string.event_category_work_adjustment_description,
            R.drawable.ic_rounded_swap_horiz_24),

    /**
     * Production-related events including stops, maintenance, and emergencies.
     * Events that directly impact production operations and capacity.
     */
    PRODUCTION(R.string.event_category_production,
            R.string.event_category_production_description,
            R.drawable.ic_rounded_factory_24),

    /**
     * Organizational events including meetings, training, and company activities.
     * Events focused on organizational development and communication.
     */
    ORGANIZATIONAL(R.string.event_category_organizational,
            R.string.event_category_organizational_description,
            R.drawable.ic_rounded_business_24),

    /**
     * Compliance and safety events including drills, audits, and regulatory activities.
     * Events required for legal, safety, or regulatory compliance.
     */
    COMPLIANCE(R.string.event_category_compliance,
            R.string.event_category_compliance_description,
            R.drawable.ic_rounded_verified_24);

    // ==================== FIELDS ====================

    @StringRes
    private final int displayNameRes;

    @StringRes
    private final int descriptionRes;

    @DrawableRes
    private final int iconRes;

    // ==================== CONSTRUCTOR ====================

    /**
     * Creates an EventTypeCategory with internationalized strings and icon.
     *
     * @param displayNameRes String resource for localized display name
     * @param descriptionRes String resource for localized description
     * @param iconRes Drawable resource for category icon
     */
    EventTypeCategory(@StringRes int displayNameRes,
                      @StringRes int descriptionRes,
                      @DrawableRes int iconRes) {
        this.displayNameRes = displayNameRes;
        this.descriptionRes = descriptionRes;
        this.iconRes = iconRes;
    }

    // ==================== GETTERS ====================

    /**
     * Get localized display name for this category.
     *
     * @return Localized category name
     */
    public String getDisplayName() {
        return getString(QDue.getContext(), displayNameRes);
    }

    /**
     * Get localized description for this category.
     *
     * @return Localized category description
     */
    public String getDescription() {
        return getString(QDue.getContext(), descriptionRes);
    }

    /**
     * Get drawable icon resource for this category.
     *
     * @return Drawable resource ID
     */
    @DrawableRes
    public int getIconRes() {
        return iconRes;
    }

    // ==================== BUSINESS LOGIC METHODS ====================

    /**
     * Check if this category represents work absence events.
     *
     * @return true if events in this category affect work attendance
     */
    public boolean isWorkAbsence() {
        return this == ABSENCE;
    }

    /**
     * Check if this category represents production-related events.
     *
     * @return true if events in this category affect production operations
     */
    public boolean isProductionRelated() {
        return this == PRODUCTION;
    }

    /**
     * Check if this category represents work schedule modifications.
     *
     * @return true if events in this category modify work schedules
     */
    public boolean isWorkScheduleModification() {
        return this == WORK_ADJUSTMENT || this == ABSENCE;
    }

    /**
     * Check if events in this category typically require management approval.
     *
     * @return true if events in this category usually require approval
     */
    public boolean typicallyRequiresApproval() {
        return this == ABSENCE || this == WORK_ADJUSTMENT;
    }

    /**
     * Check if events in this category are typically all-day events.
     *
     * @return true if events in this category are usually all-day
     */
    public boolean isTypicallyAllDay() {
        return this == ABSENCE || this == ORGANIZATIONAL || this == COMPLIANCE;
    }

    /**
     * Get priority weight for this category for UI sorting and display.
     * Lower numbers indicate higher priority for display ordering.
     *
     * @return Priority weight (0-5, lower = higher priority)
     */
    public int getPriorityWeight() {
        return switch (this) {
            case ABSENCE -> 1; // Highest priority - affects attendance
            case PRODUCTION -> 2; // High priority - affects operations
            case WORK_ADJUSTMENT -> 3; // Medium-high priority - affects schedule
            case COMPLIANCE -> 4; // Medium priority - regulatory importance
            case ORGANIZATIONAL -> 5; // Lower priority - operational but not critical
            case GENERAL -> 6; // Lowest priority - miscellaneous
            default -> 6;
        };
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get all categories that affect work schedules.
     *
     * @return Array of categories that modify work schedules
     */
    public static EventTypeCategory[] getWorkScheduleAffectingCategories() {
        return new EventTypeCategory[] { ABSENCE, WORK_ADJUSTMENT };
    }

    /**
     * Get all categories that typically require approval.
     *
     * @return Array of categories that usually require approval
     */
    public static EventTypeCategory[] getApprovalRequiringCategories() {
        return new EventTypeCategory[] { ABSENCE, WORK_ADJUSTMENT };
    }

    /**
     * Find category by display name (useful for UI selection).
     *
     * @param displayName Localized display name to match
     * @return Matching category or GENERAL if not found
     */
    public static EventTypeCategory findByDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return GENERAL;
        }

        for (EventTypeCategory category : values()) {
            if (displayName.equals(category.getDisplayName()) ||
                    displayName.equalsIgnoreCase(category.getDisplayName())) {
                return category;
            }
        }

        return GENERAL; // Default fallback
    }

    /**
     * Get summary statistics for categories.
     * Useful for analytics and reporting.
     *
     * @return String with category count and types
     */
    public static String getCategorySummary() {
        return "Categories: " + values().length + " types " +
                "(Absence, Production, Work Adjustment, Organizational, Compliance, General)";
    }
}