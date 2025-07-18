package net.calvuz.qdue.events.actions;

import static net.calvuz.qdue.events.metadata.EventMetadataManager.deriveEventActionFromEvent;

import net.calvuz.qdue.events.dao.EventDao;
import net.calvuz.qdue.events.metadata.EventMetadataManager;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.models.EventPriority;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * EventActionManager - Centralized Business Logic for Event Actions
 *
 * This manager provides centralized business logic for event actions, completely
 * separated from UI concerns. It handles validation, event creation, and business
 * rule enforcement.
 */
public class EventActionManager {

    private static final String TAG = "EventActionManager";

    // ==================== CORE BUSINESS METHODS ====================

    /**
     * Check if an action can be performed on a specific date by a specific user.
     */
    public static boolean canPerformAction(EventAction action, LocalDate date, Long userId) {
        if (action == null || date == null) {
            return false;
        }

        try {
            // Check date-specific rules
            if (!action.canPerformOnDate(date)) {
                Log.d(TAG, "Action " + action + " cannot be performed on date " + date);
                return false;
            }

            // Check user-specific rules
            if (!action.canPerformByUser(userId)) {
                Log.d(TAG, "(SKIPPED) Action " + action + " cannot be performed by user " + userId);
                //return false;  // now skipped
            }

            // Check advance notice requirements
            LocalDate today = LocalDate.now();
            int daysUntilEvent = (int) java.time.temporal.ChronoUnit.DAYS.between(today, date);
            int requiredNotice = action.getMinimumAdvanceNoticeDays();

            if (daysUntilEvent < requiredNotice) {
                Log.d(TAG, "Action " + action + " requires " + requiredNotice + " days notice, but only " + daysUntilEvent + " days available");
                return false;
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error checking if action can be performed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Create a LocalEvent from an EventAction with business logic applied.
     */
    public static LocalEvent createEventFromAction(EventAction action, LocalDate date, Long userId) {
        if (!canPerformAction(action, date, userId)) {
            throw new IllegalArgumentException("Action " + action + " cannot be performed on " + date + " by user " + userId);
        }

        try {
            LocalEvent event = new LocalEvent();

            // Basic properties
            event.setId(generateEventId(action, date));
            event.setTitle(action.getDisplayName());
            event.setEventType(action.getMappedEventType());
            event.setPriority(action.getDefaultPriority());
            event.setAllDay(action.isDefaultAllDay());

            // Set timing
            if (action.isDefaultAllDay()) {
                event.setStartTime(date.atStartOfDay());
                event.setEndTime(date.atTime(23, 59, 59));
            } else {
                event.setStartTime(date.atTime(action.getDefaultStartTime()));
                event.setEndTime(date.atTime(action.getDefaultEndTime()));
            }

            // Apply business-specific logic
            applyBusinessLogic(event, action, userId);

            // Set metadata
            event.setLastUpdated(LocalDateTime.now());
            event.setCustomProperties(createBusinessMetadata(action, userId));

            Log.d(TAG, "Event created from action " + action + ": " + event.getId());
            return event;

        } catch (Exception e) {
            Log.e(TAG, "Error creating event from action: " + e.getMessage());
            throw new RuntimeException("Failed to create event from action", e);
        }
    }

    /**
     * Get all actions available for a specific user.
     */
    public static List<EventAction> getAvailableActionsForUser(Long userId) {
        List<EventAction> availableActions = new ArrayList<>();

        for (EventAction action : EventAction.values()) {
            if (action.canPerformByUser(userId)) {
                availableActions.add(action);
            }
        }

        return availableActions;
    }

    /**
     * Get actions by category.
     */
    public static List<EventAction> getActionsByCategory(EventActionCategory category) {
        List<EventAction> actions = new ArrayList<>();

        for (EventAction action : EventAction.values()) {
            if (action.getCategory() == category) {
                actions.add(action);
            }
        }

        return actions;
    }

    /**
     * Validate action constraints for a date range.
     */
    public static boolean validateActionForDateRange(EventAction action, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return false;
        }

        long durationDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        int maxDuration = action.getMaximumDurationDays();

        return durationDays <= maxDuration;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Check for conflicting events on the same date.
     */
    public static boolean hasConflictingEvents(EventAction action, LocalDate date, Long userId, EventDao eventDao) {
        try {
            List<LocalEvent> existingEvents = eventDao.getEventsForDate(date.atStartOfDay(), date.atTime(23, 59, 59)); // .getEventsByDateAndUser(date, userId);
            return hasActionConflicts(action, existingEvents);
        } catch (Exception e) {
            Log.e(TAG, "Error checking conflicting events: " + e.getMessage());
            return false;
        }
    }

    /**
     * Analyze conflicts between EventAction and existing events.
     */
    private static boolean hasActionConflicts(EventAction newAction, List<LocalEvent> existingEvents) {
        for (LocalEvent existingEvent : existingEvents) {
            EventAction existingAction = EventMetadataManager.deriveEventActionFromEvent(existingEvent);
            if (existingAction != null && areActionsConflicting(newAction, existingAction)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Define conflict rules between EventActions.
     */
    private static boolean areActionsConflicting(EventAction action1, EventAction action2) {
        if (action1 == action2) return true; // Same action = conflict

        // Define conflict matrix
        switch (action1.getCategory()) {
            case ABSENCE:
                return action2.getCategory() == EventActionCategory.ABSENCE || // Two absences conflict
                        action2.getCategory() == EventActionCategory.WORK_ADJUSTMENT; // Absence conflicts with work

            case WORK_ADJUSTMENT:
                return action2.getCategory() == EventActionCategory.ABSENCE || // Work conflicts with absence
                        (action2.getCategory() == EventActionCategory.WORK_ADJUSTMENT &&
                                action1 != action2); // Different work adjustments conflict

            case PRODUCTION:
                return action2.getCategory() == EventActionCategory.PRODUCTION && // Multiple production events
                        (action1 == EventAction.EMERGENCY || action2 == EventAction.EMERGENCY); // Emergency conflicts

            default:
                return false; // DEVELOPMENT and GENERAL usually don't conflict
        }
    }

    /**
     * Get detailed conflict information for user feedback.
     */
    public static ConflictAnalysis analyzeConflicts(EventAction action, LocalDate date, Long userId, EventDao eventDao) {
        List<LocalEvent> existingEvents = eventDao.getEventsForDate(date.atStartOfDay(), date.atTime(23, 59, 59));
        List<EventConflict> conflicts = new ArrayList<>();

        for (LocalEvent existingEvent : existingEvents) {
            EventAction existingAction = deriveEventActionFromEvent(existingEvent);
            if (existingAction != null && areActionsConflicting(action, existingAction)) {
                conflicts.add(new EventConflict(existingEvent, existingAction,
                        getConflictReason(action, existingAction)));
            }
        }

        return new ConflictAnalysis(conflicts);
    }

    /**
     * Get specific reason why two EventActions conflict.
     *
     * @param newAction The action being attempted
     * @param existingAction The conflicting existing action
     * @return Human-readable conflict reason
     */
    private static String getConflictReason(EventAction newAction, EventAction existingAction) {
        // Same action conflict
        if (newAction == existingAction) {
            return "Azione duplicata nella stessa giornata";
        }

        // Category-based conflicts
        EventActionCategory newCategory = newAction.getCategory();
        EventActionCategory existingCategory = existingAction.getCategory();

        // Absence conflicts
        if (newCategory == EventActionCategory.ABSENCE && existingCategory == EventActionCategory.ABSENCE) {
            return getAbsenceConflictReason(newAction, existingAction);
        }

        // Absence vs Work conflicts
        if (newCategory == EventActionCategory.ABSENCE && existingCategory == EventActionCategory.WORK_ADJUSTMENT) {
            return "Non puoi essere assente e lavorare straordinario lo stesso giorno";
        }

        if (newCategory == EventActionCategory.WORK_ADJUSTMENT && existingCategory == EventActionCategory.ABSENCE) {
            return "Non puoi lavorare straordinario quando sei assente";
        }

        // Work adjustment conflicts
        if (newCategory == EventActionCategory.WORK_ADJUSTMENT && existingCategory == EventActionCategory.WORK_ADJUSTMENT) {
            return getWorkAdjustmentConflictReason(newAction, existingAction);
        }

        // Production conflicts
        if (newCategory == EventActionCategory.PRODUCTION && existingCategory == EventActionCategory.PRODUCTION) {
            return getProductionConflictReason(newAction, existingAction);
        }

        // Emergency conflicts
        if (newAction == EventAction.EMERGENCY || existingAction == EventAction.EMERGENCY) {
            return "Emergenza in conflitto con altre attività";
        }

        // Generic conflict
        return "Conflitto tra " + newAction.getDisplayName() + " e " + existingAction.getDisplayName();
    }

    /**
     * Get specific reason for absence conflicts.
     */
    private static String getAbsenceConflictReason(EventAction newAction, EventAction existingAction) {
        // Specific absence combinations
        if ((newAction == EventAction.VACATION && existingAction == EventAction.SICK_LEAVE) ||
                (newAction == EventAction.SICK_LEAVE && existingAction == EventAction.VACATION)) {
            return "Non puoi essere in ferie e malattia contemporaneamente";
        }

        if ((newAction == EventAction.VACATION && existingAction == EventAction.PERSONAL_LEAVE) ||
                (newAction == EventAction.PERSONAL_LEAVE && existingAction == EventAction.VACATION)) {
            return "Ferie e permesso personale si sovrappongono";
        }

        if ((newAction == EventAction.SICK_LEAVE && existingAction == EventAction.TRAINING) ||
                (newAction == EventAction.TRAINING && existingAction == EventAction.SICK_LEAVE)) {
            return "Non puoi seguire formazione durante malattia";
        }

        if (newAction == EventAction.SPECIAL_LEAVE || existingAction == EventAction.SPECIAL_LEAVE) {
            return "Permesso Legge 104 esclusivo per la giornata";
        }

        // Generic absence conflict
        return "Multiple assenze nella stessa giornata non consentite";
    }

    /**
     * Get specific reason for work adjustment conflicts.
     */
    private static String getWorkAdjustmentConflictReason(EventAction newAction, EventAction existingAction) {
        if ((newAction == EventAction.OVERTIME && existingAction == EventAction.SHIFT_SWAP) ||
                (newAction == EventAction.SHIFT_SWAP && existingAction == EventAction.OVERTIME)) {
            return "Straordinario e cambio turno si escludono a vicenda";
        }

        if ((newAction == EventAction.OVERTIME && existingAction == EventAction.COMPENSATION) ||
                (newAction == EventAction.COMPENSATION && existingAction == EventAction.OVERTIME)) {
            return "Straordinario e recupero non possono coesistere";
        }

        if ((newAction == EventAction.SHIFT_SWAP && existingAction == EventAction.COMPENSATION) ||
                (newAction == EventAction.COMPENSATION && existingAction == EventAction.SHIFT_SWAP)) {
            return "Cambio turno e recupero si sovrappongono";
        }

        return "Aggiustamenti turno multipli non consentiti";
    }

    /**
     * Get specific reason for production conflicts.
     */
    private static String getProductionConflictReason(EventAction newAction, EventAction existingAction) {
        // Emergency always conflicts with planned activities
        if (newAction == EventAction.EMERGENCY || existingAction == EventAction.EMERGENCY) {
            return "Emergenza interrompe altre attività produttive";
        }

        // Multiple stops conflict
        if ((newAction == EventAction.PLANNED_STOP && existingAction == EventAction.UNPLANNED_STOP) ||
                (newAction == EventAction.UNPLANNED_STOP && existingAction == EventAction.PLANNED_STOP)) {
            return "Fermata pianificata e non pianificata in conflitto";
        }

        // Maintenance during stops
        if ((newAction == EventAction.MAINTENANCE &&
                (existingAction == EventAction.PLANNED_STOP || existingAction == EventAction.UNPLANNED_STOP)) ||
                (existingAction == EventAction.MAINTENANCE &&
                        (newAction == EventAction.PLANNED_STOP || newAction == EventAction.UNPLANNED_STOP))) {
            return "Manutenzione e fermata impianto si sovrappongono";
        }

        return "Attività produttive multiple in conflitto";
    }
    /// ///////
    /**
     * Apply action-specific business logic to the event.
     */
    private static void applyBusinessLogic(LocalEvent event, EventAction action, Long userId) {
        // Adjust priority based on timing
        if (action.isUrgentByNature()) {
            LocalDate today = LocalDate.now();
            LocalDate eventDate = event.getStartTime().toLocalDate();
            if (eventDate.equals(today) || eventDate.equals(today.plusDays(1))) {
                event.setPriority(EventPriority.URGENT);
            }
        }

        // Set description based on action type
        String description = generateDescriptionForAction(action);
        if (action.requiresApproval()) {
            description += " (Richiede approvazione)";
        }
        event.setDescription(description);

        // Apply location if relevant
        if (action.affectsProduction()) {
            event.setLocation("Area Produzione");
        }
    }

    /**
     * Create business metadata for the event.
     */
    private static Map<String, String> createBusinessMetadata(EventAction action, Long userId) {
        Map<String, String> metadata = new HashMap<>();

        // Core action metadata
        metadata.put("event_action", action.name());
        metadata.put("action_category", action.getCategory().name());
        metadata.put("created_from_action", "true");
        metadata.put("creation_timestamp", String.valueOf(System.currentTimeMillis()));

        if (userId != null) {
            metadata.put("created_by_user", String.valueOf(userId));
        }

        // Business rule metadata
        metadata.put("requires_approval", String.valueOf(action.requiresApproval()));
        metadata.put("affects_work_schedule", String.valueOf(action.affectsWorkSchedule()));
        metadata.put("affects_production", String.valueOf(action.affectsProduction()));
        metadata.put("requires_shift_coverage", String.valueOf(action.requiresShiftCoverage()));
        metadata.put("creates_work_absence", String.valueOf(action.createsWorkAbsence()));

        // Turn exception compatibility
        metadata.put("turn_exception_type", action.getTurnExceptionTypeName());

        return metadata;
    }

    /**
     * Generate event ID based on action and date.
     */
    private static String generateEventId(EventAction action, LocalDate date) {
        String dateStr = date.toString().replace("-", "");
        String actionStr = action.name().toLowerCase();
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);

        return String.format("action_%s_%s_%s", actionStr, dateStr, randomSuffix);
    }

    /**
     * Generate appropriate description for the action.
     */
    private static String generateDescriptionForAction(EventAction action) {
        switch (action) {
            case VACATION:
                return "Periodo di ferie programmato";
            case SICK_LEAVE:
                return "Assenza per malattia";
            case OVERTIME:
                return "Lavoro straordinario";
            case EMERGENCY:
                return "Situazione di emergenza";
            case TRAINING:
                return "Attività di formazione";
            case MAINTENANCE:
                return "Attività di manutenzione programmata";
            default:
                return action.getDisplayName();
        }
    }
}

