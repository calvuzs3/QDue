package net.calvuz.qdue.core.domain.events.models;

import static net.calvuz.qdue.ui.core.common.utils.Library.getString;

import android.graphics.Color;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;

/**
 * Enhanced EventType enum with drawable icon support.
 *
 * <p>Each EventType now includes:</p>
 * <ul>
 *   <li>Display name (localized string resource)</li>
 *   <li>Color (for UI theming and backgrounds)</li>
 *   <li>Emoji (for quick visual identification)</li>
 *   <li>Icon drawable (ic_rounded_* format)</li>
 *   <li>Business logic flags (affects schedule, requires approval)</li>
 * </ul>
 *
 * <p>Icons follow the naming convention: ic_rounded_&lt;descriptive_name&gt;_24</p>
 *
 * @author Calendar App Team
 * @version 2.0 - Added drawable icon support
 * @since Database Version 5
 */
public enum EventType {

    // ==================== GENERAL EVENTS ====================

    GENERAL(R.string.event_action_general,
            Color.GRAY,
            "📅",
            R.drawable.ic_rounded_event_24,
            EventTypeCategory.GENERAL),

    MEETING(R.string.event_action_meeting,
            Color.BLUE,
            "🤝",
            R.drawable.ic_rounded_groups_24,
            EventTypeCategory.ORGANIZATIONAL),

    TRAINING(R.string.event_action_training,
            Color.GREEN,
            "📚",
            R.drawable.ic_rounded_school_24,
            EventTypeCategory.ORGANIZATIONAL),

    // ==================== PRODUCTION EVENTS ====================

    MAINTENANCE(R.string.event_action_maintenance,
            Color.LTGRAY,
            "🔧",
            R.drawable.ic_rounded_build_24,
            EventTypeCategory.PRODUCTION),

    EMERGENCY(R.string.event_action_emergency,
            Color.RED,
            "🚨",
            R.drawable.ic_rounded_emergency_24,
            EventTypeCategory.PRODUCTION),

    // ==================== PRODUCTION STOPS ====================

    STOP_PLANNED(R.string.event_action_planned_stop,
            Color.parseColor("#FF6B35"),
            "⏸️",
            R.drawable.ic_rounded_pause_circle_24,
            EventTypeCategory.PRODUCTION),

    STOP_UNPLANNED(R.string.event_action_unplanned_stop,
            Color.parseColor("#D32F2F"),
            "🚨",
            R.drawable.ic_rounded_error_24,
            EventTypeCategory.PRODUCTION),

    STOP_SHORTAGE_ORDERS(R.string.event_type_stop_shortage_orders,
            Color.parseColor("#FF6B35"),
            "📦❌",
            R.drawable.ic_rounded_inventory_24,
            EventTypeCategory.PRODUCTION),

    STOP_SHORTAGE_RAW_MATERIALS(R.string.event_type_stop_shortage_raw_materials,
            Color.parseColor("#FF8C42"),
            "📦",
            R.drawable.ic_rounded_warehouse_24,
            EventTypeCategory.PRODUCTION),

    // ==================== SAFETY AND COMPLIANCE ====================

    SAFETY_DRILL(R.string.event_type_saefty_drill,
            Color.parseColor("#96CEB4"),
            "🛡️",
            R.drawable.ic_rounded_shield_24,
            EventTypeCategory.COMPLIANCE),

    AUDIT(R.string.event_type_audit,
            Color.parseColor("#FFEAA7"),
            "📋",
            R.drawable.ic_rounded_fact_check_24,
            EventTypeCategory.COMPLIANCE),

    // ==================== WORK EVENTS (Shift-Related) ====================

    OVERTIME(R.string.event_action_overtime,
            Color.parseColor("#7B1FA2"),
            "⏰",
            R.drawable.ic_rounded_schedule_24,
            EventTypeCategory.WORK_ADJUSTMENT,
            false, true),

    SHIFT_CHANGE(R.string.event_type_shift_change,
            Color.parseColor("#4ECDC4"),
            "🔄",
            R.drawable.ic_rounded_swap_horiz_24,
            EventTypeCategory.WORK_ADJUSTMENT),

    SHIFT_SWAP(R.string.event_action_shift_swap,
            Color.parseColor("#607D8B"),
            "🔄",
            R.drawable.ic_rounded_swap_calls_24,
            EventTypeCategory.WORK_ADJUSTMENT,
            false, true),

    COMPENSATION(R.string.event_action_compensation,
            Color.parseColor("#795548"),
            "⚖️",
            R.drawable.ic_rounded_balance_24,
            EventTypeCategory.WORK_ADJUSTMENT,
            false, true),

    // ==================== USER ABSENCE EVENTS ====================

    VACATION(R.string.event_action_vacation,
            Color.parseColor("#4CAF50"),
            "🏖️",
            R.drawable.ic_rounded_beach_access_24,
            EventTypeCategory.ABSENCE,
            true, true),

    SICK_LEAVE(R.string.event_action_sick_leave,
            Color.parseColor("#F44336"),
            "🏥",
            R.drawable.ic_rounded_local_hospital_24,
            EventTypeCategory.ABSENCE,
            true, false),

    PERSONAL_LEAVE(R.string.event_action_personal_leave,
            Color.parseColor("#2196F3"),
            "📄",
            R.drawable.ic_rounded_person_24,
            EventTypeCategory.ABSENCE,
            true, true),

    SPECIAL_LEAVE(R.string.event_action_special_leave,
            Color.parseColor("#9C27B0"),
            "♿",
            R.drawable.ic_rounded_accessible_24,
            EventTypeCategory.ABSENCE,
            true, false),

    SYNDICATE_LEAVE(R.string.event_action_syndicate_leave,
            Color.parseColor("#FF9800"),
            "🏛️",
            R.drawable.ic_rounded_account_balance_24,
            EventTypeCategory.ABSENCE,
            true, true),

    // ==================== CUSTOM/IMPORTED EVENTS ====================

    IMPORTED(R.string.event_type_imported,
            Color.parseColor("#DDA0DD"),
            "📥",
            R.drawable.ic_rounded_download_24,
            EventTypeCategory.GENERAL),

    OTHER(R.string.event_type_other,
            Color.parseColor("#DDA0DD"),
            "📥",
            R.drawable.ic_rounded_more_horiz_24,
            EventTypeCategory.GENERAL);

    // ==================== FIELDS ====================

    @StringRes
    private final int displayName;

    private final int color;
    private final String emoji;

    @DrawableRes
    private final int iconRes;

    private final EventTypeCategory category; // ✅ NEW: Category classification

    private final boolean affectsWorkSchedule;
    private final boolean requiresApproval;

    // ==================== CONSTRUCTORS ====================

    /**
     * Enhanced constructor with drawable icon, category, and business logic
     */
    EventType(@StringRes int displayName, int color, String emoji, @DrawableRes int iconRes,
              EventTypeCategory category, boolean affectsWorkSchedule, boolean requiresApproval) {
        this.displayName = displayName;
        this.color = color;
        this.emoji = emoji;
        this.iconRes = iconRes;
        this.category = category;
        this.affectsWorkSchedule = affectsWorkSchedule;
        this.requiresApproval = requiresApproval;
    }

    /**
     * Constructor with drawable icon and category, default business logic
     */
    EventType(@StringRes int displayName, int color, String emoji, @DrawableRes int iconRes,
              EventTypeCategory category) {
        this.displayName = displayName;
        this.color = color;
        this.emoji = emoji;
        this.iconRes = iconRes;
        this.category = category;
        this.affectsWorkSchedule = category.isWorkScheduleModification();
        this.requiresApproval = category.typicallyRequiresApproval();
    }

    /**
     * Legacy constructor for backward compatibility (without category)
     * @deprecated Use constructor with EventTypeCategory parameter
     */
    @Deprecated
    EventType(@StringRes int displayName, int color, String emoji, @DrawableRes int iconRes,
              boolean affectsWorkSchedule, boolean requiresApproval) {
        this.displayName = displayName;
        this.color = color;
        this.emoji = emoji;
        this.iconRes = iconRes;
        this.category = EventTypeCategory.GENERAL; // Default category
        this.affectsWorkSchedule = affectsWorkSchedule;
        this.requiresApproval = requiresApproval;
    }

    /**
     * Legacy constructor for backward compatibility (without category)
     * @deprecated Use constructor with EventTypeCategory parameter
     */
    @Deprecated
    EventType(@StringRes int displayName, int color, String emoji, @DrawableRes int iconRes) {
        this.displayName = displayName;
        this.color = color;
        this.emoji = emoji;
        this.iconRes = iconRes;
        this.category = EventTypeCategory.GENERAL; // Default category
        this.affectsWorkSchedule = false;
        this.requiresApproval = false;
    }

    /**
     * Legacy constructor for backward compatibility (without icon and category)
     * @deprecated Use constructor with iconRes and EventTypeCategory parameters
     */
    @Deprecated
    EventType(@StringRes int displayName, int color, String emoji,
              boolean affectsWorkSchedule, boolean requiresApproval) {
        this.displayName = displayName;
        this.color = color;
        this.emoji = emoji;
        this.iconRes = R.drawable.ic_rounded_event_24; // Default icon
        this.category = EventTypeCategory.GENERAL; // Default category
        this.affectsWorkSchedule = affectsWorkSchedule;
        this.requiresApproval = requiresApproval;
    }

    /**
     * Legacy constructor for backward compatibility (without icon and category)
     * @deprecated Use constructor with iconRes and EventTypeCategory parameters
     */
    @Deprecated
    EventType(@StringRes int displayName, int color, String emoji) {
        this.displayName = displayName;
        this.color = color;
        this.emoji = emoji;
        this.iconRes = R.drawable.ic_rounded_event_24; // Default icon
        this.category = EventTypeCategory.GENERAL; // Default category
        this.affectsWorkSchedule = false;
        this.requiresApproval = false;
    }

    // ==================== GETTERS ====================

    /**
     * Get localized display name
     */
    public String getDisplayName() {
        return getString(QDue.getContext(), displayName);
    }

    /**
     * Get color for UI theming
     */
    public int getColor() {
        return color;
    }

    /**
     * Get emoji for quick visual identification
     */
    public String getEmoji() {
        return emoji;
    }

    /**
     * ✅ NEW: Get drawable icon resource
     * @return Drawable resource ID following ic_rounded_* naming convention
     */
    @DrawableRes
    public int getIconRes() {
        return iconRes;
    }

    /**
     * ✅ NEW: Get event category for grouping and business logic
     * @return EventTypeCategory for this event type
     */
    public EventTypeCategory getCategory() {
        return category;
    }

    /**
     * Check if this event type affects work schedule
     */
    public boolean affectsWorkSchedule() {
        return affectsWorkSchedule;
    }

    /**
     * Check if this event type requires approval
     */
    public boolean requiresApproval() {
        return requiresApproval;
    }

    // ==================== BUSINESS LOGIC METHODS ====================

    /**
     * Get text color based on background color for readability
     */
    public int getTextColor() {
        // Calculate luminance and return black or white text
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Check if this is a production stop event
     * ✅ SIMPLIFIED: Uses category-based logic
     */
    public boolean isProductionStop() {
        return category == EventTypeCategory.PRODUCTION &&
                (this == STOP_PLANNED || this == STOP_UNPLANNED ||
                        this == STOP_SHORTAGE_ORDERS || this == STOP_SHORTAGE_RAW_MATERIALS);
    }

    /**
     * Check if this is a work absence event
     * ✅ SIMPLIFIED: Uses category-based logic
     */
    public boolean isWorkAbsence() {
        return category.isWorkAbsence();
    }

    /**
     * Check if this event should be all-day by default
     * ✅ SIMPLIFIED: Uses category-based logic with specific overrides
     */
    public boolean isDefaultAllDay() {
        return category.isTypicallyAllDay() ||
                this == TRAINING ||
                this == AUDIT ||
                this == SAFETY_DRILL;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get all EventTypes belonging to a specific category.
     *
     * @param category The category to filter by
     * @return Array of EventTypes in the specified category
     */
    public static EventType[] getEventTypesByCategory(EventTypeCategory category) {
        return java.util.Arrays.stream(values())
                .filter(eventType -> eventType.getCategory() == category)
                .toArray(EventType[]::new);
    }

    /**
     * Get all categories that have at least one EventType.
     *
     * @return Array of categories that are actually used
     */
    public static EventTypeCategory[] getUsedCategories() {
        return java.util.Arrays.stream(EventTypeCategory.values())
                .filter(category -> java.util.Arrays.stream(values())
                        .anyMatch(eventType -> eventType.getCategory() == category))
                .toArray(EventTypeCategory[]::new);
    }

    /**
     * Find EventType by display name with category context.
     *
     * @param displayName Localized display name to match
     * @param preferredCategory Preferred category for disambiguation
     * @return Matching EventType or GENERAL if not found
     */
    public static EventType findByDisplayName(String displayName, EventTypeCategory preferredCategory) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return GENERAL;
        }

        // First try: exact match in preferred category
        if (preferredCategory != null) {
            for (EventType eventType : getEventTypesByCategory(preferredCategory)) {
                if (displayName.equals(eventType.getDisplayName()) ||
                        displayName.equalsIgnoreCase(eventType.getDisplayName())) {
                    return eventType;
                }
            }
        }

        // Second try: any category
        for (EventType eventType : values()) {
            if (displayName.equals(eventType.getDisplayName()) ||
                    displayName.equalsIgnoreCase(eventType.getDisplayName())) {
                return eventType;
            }
        }

        return GENERAL; // Default fallback
    }

    /**
     * Get summary statistics for EventTypes by category.
     *
     * @return String with category distribution
     */
    public static String getCategoryDistribution() {
        StringBuilder summary = new StringBuilder("EventType Distribution:\n");

        for (EventTypeCategory category : EventTypeCategory.values()) {
            long count = java.util.Arrays.stream(values())
                    .filter(eventType -> eventType.getCategory() == category)
                    .count();
            summary.append("- ").append(category.getDisplayName())
                    .append(": ").append(count).append(" types\n");
        }

        return summary.toString();
    }
}