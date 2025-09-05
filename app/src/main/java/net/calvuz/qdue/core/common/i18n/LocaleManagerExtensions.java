package net.calvuz.qdue.core.common.i18n;

import static net.calvuz.qdue.core.common.i18n.LocaleManager.getCurrentLocale;
import static net.calvuz.qdue.core.common.i18n.LocaleManager.getLocalizedString;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.R;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * LocaleManager Complex Extensions - Template Methods and Dynamic Formatting
 *
 * <p>Extension methods for LocaleManager that handle complex i18n scenarios requiring
 * templates, parameter formatting, and dynamic content generation. These methods
 * complement the simple string resources in strings.xml.</p>
 *
 * <p><strong>Note:</strong> Add these methods to the existing LocaleManager class
 * in net.calvuz.qdue.core.common.i18n.LocaleManager</p>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Calendar Engine i18n Extensions
 * @since Clean Architecture Phase 2
 */
public class LocaleManagerExtensions {

    // ==================== ENUM DISPLAY METHODS (Simple - use strings.xml) ====================

    /**
     * Get localized display name for any enum type using strings.xml.
     * Simple wrapper that delegates to string resources.
     */
    @NonNull
    public static String getEnumDisplayName(@Nullable Context context, @NonNull String prefix, @NonNull String enumKey) {
        return getLocalizedString(context, prefix + "_" + enumKey.toLowerCase(), enumKey);
    }

    // Example usage:
    // getEnumDisplayName(context, "frequency", "DAILY") -> looks for "frequency_daily" in strings.xml
    // getEnumDisplayName(context, "exception_type", "VACATION") -> looks for "exception_type_vacation"

    // Specific enum methods (delegates to string resources)
    @NonNull
    public static String getFrequencyDisplayName(@Nullable Context context, @NonNull String frequencyKey) {
        return getLocalizedString(context, "frequency_" + frequencyKey.toLowerCase(), frequencyKey);
    }

    @NonNull
    public static String getFrequencyDescription(@Nullable Context context, @NonNull String frequencyKey) {
        return getLocalizedString(context, "frequency_desc_" + frequencyKey.toLowerCase(), frequencyKey + " frequency");
    }

    @NonNull
    public static String getEndTypeDisplayName(@Nullable Context context, @NonNull String endTypeKey) {
        return getLocalizedString(context, "end_type_" + endTypeKey.toLowerCase(), endTypeKey);
    }

    @NonNull
    public static String getEndTypeDescription(@Nullable Context context, @NonNull String endTypeKey) {
        return getLocalizedString(context, "end_type_desc_" + endTypeKey.toLowerCase(), endTypeKey + " end type");
    }

    @NonNull
    public static String getExceptionTypeDisplayName(@Nullable Context context, @NonNull String typeKey) {
        return getLocalizedString(context, "exception_type_" + typeKey, typeKey);
    }

    @NonNull
    public static String getExceptionTypeDescription(@Nullable Context context, @NonNull String descriptionKey) {
        return getLocalizedString(context, "exception_desc_" + descriptionKey, descriptionKey);
    }

    @NonNull
    public static String getApprovalStatusDisplayName(@Nullable Context context, @NonNull String statusKey) {
        return getLocalizedString(context, "approval_status_" + statusKey, statusKey);
    }

    @NonNull
    public static String getApprovalStatusDescription(@Nullable Context context, @NonNull String descriptionKey) {
        return getLocalizedString(context, "approval_desc_" + descriptionKey, descriptionKey);
    }

    @NonNull
    public static String getPriorityDisplayName(@Nullable Context context, @NonNull String priorityKey) {
        return getLocalizedString(context, "priority_" + priorityKey, priorityKey);
    }

    @NonNull
    public static String getAssignmentPriorityDisplayName(@Nullable Context context, @NonNull String priorityKey) {
        return getLocalizedString(context, "assignment_priority_" + priorityKey, priorityKey);
    }

    @NonNull
    public static String getAssignmentStatusDisplayName(@Nullable Context context, @NonNull String statusKey) {
        return getLocalizedString(context, "assignment_status_" + statusKey, statusKey);
    }

    @NonNull
    public static String getAssignmentStatusDescription(@Nullable Context context, @NonNull String descriptionKey) {
        return getLocalizedString(context, "assignment_status_desc_" + descriptionKey, descriptionKey);
    }

    // ==================== COMPLEX TEMPLATE METHODS ====================

    /**
     * Get localized recurrence rule description with interval parameter.
     * Template: "Every {interval} {frequency_unit}"
     */
    @NonNull
    public static String getRecurrenceRuleDescriptionWithInterval(@Nullable Context context, @NonNull String ruleTypeKey, int interval) {
        if (context == null) {
            return "Every " + interval + " " + ruleTypeKey.toLowerCase();
        }

        try {
            // Get template from strings.xml: "recurrence_interval_daily" -> "Ogni %d giorni"
            String templateKey = "recurrence_interval_" + ruleTypeKey.toLowerCase();
            String template = context.getString(context.getResources().getIdentifier(
                    templateKey, "string", context.getPackageName()));

            if (template != null && !template.equals(templateKey)) {
                return String.format(getCurrentLocale(context), template, interval);
            }
        } catch (Exception e) {
            // Fallback to default format
        }

        // Fallback
        String frequencyUnit = getFrequencyDisplayName(context, ruleTypeKey);
        String template = getLocalizedString(context, "recurrence_every_template", "Every %d %s");
        return String.format(getCurrentLocale(context), template, interval, frequencyUnit.toLowerCase());
    }

    /**
     * Get localized end condition with count parameter.
     * Template: "Ends after {count} occurrences"
     */
    @NonNull
    public static String getRecurrenceEndConditionWithCount(@Nullable Context context, int count) {
        if (context == null) {
            return "Ends after " + count + " occurrences";
        }

        try {
            String template = context.getString(R.string.recurrence_end_count_template);
            return String.format(getCurrentLocale(context), template, count);
        } catch (Exception e) {
            // Fallback
            return "Ends after " + count + " occurrences";
        }
    }

    /**
     * Get localized end condition with date parameter.
     * Template: "Ends on {formatted_date}"
     */
    @NonNull
    public static String getRecurrenceEndConditionWithDate(@Nullable Context context, @NonNull LocalDate endDate) {
        if (context == null) {
            return "Ends on " + endDate.toString();
        }

        try {
            String template = context.getString(R.string.recurrence_end_date_template);
            String formattedDate = formatDate(context, endDate);
            return String.format(getCurrentLocale(context), template, formattedDate);
        } catch (Exception e) {
            // Fallback
            return "Ends on " + formatDate(context, endDate);
        }
    }

    /**
     * Get localized recurrence end condition (simple).
     * Uses strings.xml: "recurrence_end_never", etc.
     */
    @NonNull
    public static String getRecurrenceEndCondition(@Nullable Context context, @NonNull String conditionKey) {
        return getLocalizedString(context, "recurrence_end_" + conditionKey.toLowerCase(), conditionKey);
    }

    /**
     * Get localized recurrence rule name (simple).
     * Uses strings.xml: "recurrence_rule_quattrodue_cycle", etc.
     */
    @NonNull
    public static String getRecurrenceRuleName(@Nullable Context context, @NonNull String ruleTypeKey) {
        return getLocalizedString(context, "recurrence_rule_" + ruleTypeKey.toLowerCase(), ruleTypeKey);
    }

    /**
     * Get localized recurrence rule description (simple).
     * Uses strings.xml: "recurrence_rule_desc_quattrodue_cycle", etc.
     */
    @NonNull
    public static String getRecurrenceRuleDescription(@Nullable Context context, @NonNull String ruleTypeKey) {
        return getLocalizedString(context, "recurrence_rule_desc_" + ruleTypeKey.toLowerCase(), ruleTypeKey + " pattern");
    }

    // ==================== SHIFT EXCEPTION TEMPLATE METHODS ====================

    /**
     * Get localized default exception title (simple).
     * Uses strings.xml: "default_exception_title_absence_vacation", etc.
     */
    @NonNull
    public static String getDefaultExceptionTitle(@Nullable Context context, @NonNull String exceptionTypeKey) {
        return getLocalizedString(context, "default_exception_title_" + exceptionTypeKey.toLowerCase(), exceptionTypeKey);
    }

    /**
     * Get localized default shift swap title with user reference.
     * Template: "Shift swap with user {userID}"
     */
    @NonNull
    public static String getDefaultShiftSwapTitle(@Nullable Context context, @NonNull Long swapWithUserId) {
        if (context == null) {
            return "Shift swap with user " + swapWithUserId;
        }

        try {
            String template = context.getString(R.string.default_shift_swap_title);
            return String.format(getCurrentLocale(context), template, swapWithUserId);
        } catch (Exception e) {
            // Fallback
            return "Shift swap with user " + swapWithUserId;
        }
    }

    /**
     * Get localized default time reduction title (simple).
     * Uses strings.xml: "default_reduction_title_personal_reduction", etc.
     */
    @NonNull
    public static String getDefaultTimeReductionTitle(@Nullable Context context, @NonNull String reductionTypeKey) {
        return getLocalizedString(context, "default_reduction_title_" + reductionTypeKey, reductionTypeKey);
    }

    // ==================== USER SCHEDULE ASSIGNMENT TEMPLATE METHODS ====================

    /**
     * Get localized user display name template.
     * Template: "User {userID}"
     */
    @NonNull
    public static String getUserDisplayNameTemplate(@Nullable Context context, @NonNull Long userId) {
        if (context == null) {
            return "User " + userId;
        }

        try {
            String template = context.getString(R.string.user_display_template);
            return String.format(getCurrentLocale(context), template, userId);
        } catch (Exception e) {
            // Fallback
            return "User " + userId;
        }
    }

    /**
     * Get localized assignment display title.
     * Template: "{userDisplay} → {teamDisplay}"
     */
    @NonNull
    public static String getAssignmentDisplayTitle(@Nullable Context context, @NonNull String userDisplay, @NonNull String teamDisplay) {
        if (context == null) {
            return userDisplay + " → " + teamDisplay;
        }

        try {
            String template = context.getString(R.string.assignment_display_title);
            return String.format(getCurrentLocale(context), template, userDisplay, teamDisplay);
        } catch (Exception e) {
            // Fallback
            return userDisplay + " → " + teamDisplay;
        }
    }

    /**
     * Get localized permanent assignment period description.
     * Template: "From {startDate} (Permanent)"
     */
    @NonNull
    public static String getAssignmentPermanentPeriod(@Nullable Context context, @NonNull LocalDate startDate) {
        if (context == null) {
            return "From " + startDate + " (Permanent)";
        }

        try {
            String template = context.getString(R.string.assignment_permanent_period);
            String formattedDate = formatDate(context, startDate);
            return String.format(getCurrentLocale(context), template, formattedDate);
        } catch (Exception e) {
            // Fallback
            return "From " + formatDate(context, startDate) + " (Permanent)";
        }
    }

    /**
     * Get localized time period description.
     * Template: "From {startDate} to {endDate}"
     */
    @NonNull
    public static String getAssignmentTimePeriod(@Nullable Context context, @NonNull LocalDate startDate, @Nullable LocalDate endDate) {
        if (context == null) {
            String endDateStr = endDate != null ? endDate.toString() : "";
            return "From " + startDate + " to " + endDateStr;
        }

        try {
            String template = context.getString(R.string.assignment_time_period);
            String formattedStartDate = formatDate(context, startDate);
            String formattedEndDate = endDate != null ? formatDate(context, endDate) : "";
            return String.format(getCurrentLocale(context), template, formattedStartDate, formattedEndDate);
        } catch (Exception e) {
            // Fallback
            String formattedStartDate = formatDate(context, startDate);
            String formattedEndDate = endDate != null ? formatDate(context, endDate) : "";
            return "From " + formattedStartDate + " to " + formattedEndDate;
        }
    }

    /**
     * Get localized standard assignment title (simple).
     * Uses strings.xml: "standard_assignment_title"
     */
    @NonNull
    public static String getStandardAssignmentTitle(@Nullable Context context) {
        return getLocalizedString(context, "standard_assignment_title", "Standard team assignment");
    }

    /**
     * Get localized temporary assignment title (simple).
     * Uses strings.xml: "temporary_assignment_title"
     */
    @NonNull
    public static String getTemporaryAssignmentTitle(@Nullable Context context) {
        return getLocalizedString(context, "temporary_assignment_title", "Temporary assignment");
    }

    /**
     * Get localized team transfer title (simple).
     * Uses strings.xml: "team_transfer_title"
     */
    @NonNull
    public static String getTeamTransferTitle(@Nullable Context context) {
        return getLocalizedString(context, "team_transfer_title", "Team transfer");
    }

    // ==================== UTILITY METHODS ====================





    /**
     * Helper method to format date according to current locale.
     * Uses locale-appropriate date formatting.
     */
    @NonNull
    private static String formatDate(@Nullable Context context, @NonNull LocalDate date) {
        if (context == null) {
            return date.toString();
        }

        try {
            Locale currentLocale = getCurrentLocale(context);
            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(currentLocale);
            return date.format(formatter);
        } catch (Exception e) {
            // Fallback to ISO format
            return date.toString();
        }
    }

    /**
     * Helper method to format date with custom pattern.
     * Uses locale-specific patterns when available.
     */
    @NonNull
    private static String formatDateWithPattern(@Nullable Context context, @NonNull LocalDate date, @NonNull String pattern) {
        if (context == null) {
            return date.format(DateTimeFormatter.ofPattern(pattern));
        }

        try {
            Locale currentLocale = getCurrentLocale(context);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, currentLocale);
            return date.format(formatter);
        } catch (Exception e) {
            // Fallback to default pattern
            return date.format(DateTimeFormatter.ofPattern(pattern));
        }
    }
}