package net.calvuz.qdue.domain.calendar.engines;

import static net.calvuz.qdue.domain.common.DomainLibrary.logDebug;
import static net.calvuz.qdue.domain.common.DomainLibrary.logError;
import static net.calvuz.qdue.domain.common.DomainLibrary.logVerbose;
import static net.calvuz.qdue.domain.common.DomainLibrary.logWarning;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.calendar.models.ShiftException;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleShift;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.common.i18n.DomainLocalizer;
import net.calvuz.qdue.domain.common.models.LocalizableDomainModel;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ExceptionResolver - Pure Algorithm for Priority-Based Shift Exception Resolution
 *
 * <p>Advanced exception resolution algorithm for handling conflicts, overrides, and
 * modifications to work schedules. Implements priority-based conflict resolution with
 * comprehensive business rules for exception handling. This is a pure domain algorithm
 * with no external dependencies, following clean architecture principles.</p>
 *
 * <h3>Clean Architecture Compliance:</h3>
 * <ul>
 *   <li><strong>Pure Algorithm</strong>: No external dependencies or infrastructure concerns</li>
 *   <li><strong>Framework Agnostic</strong>: No Android or database dependencies</li>
 *   <li><strong>Testable</strong>: Completely isolated and unit testable</li>
 *   <li><strong>Stateless</strong>: All required data passed as parameters</li>
 * </ul>
 *
 * <h3>Exception Types Supported:</h3>
 * <ul>
 *   <li><strong>ABSENCE_VACATION</strong>: Personal time off (highest priority)</li>
 *   <li><strong>ABSENCE_SICK</strong>: Medical absence (highest priority)</li>
 *   <li><strong>ABSENCE_SPECIAL</strong>: Special leave (highest priority)</li>
 *   <li><strong>CHANGE_COMPANY</strong>: Company-mandated shift changes (high priority)</li>
 *   <li><strong>CHANGE_SWAP</strong>: Team member swap (medium priority)</li>
 *   <li><strong>CHANGE_SPECIAL</strong>: Special event coverage (medium priority)</li>
 *   <li><strong>REDUCTION_PERSONAL</strong>: Personal time reduction (medium priority)</li>
 *   <li><strong>REDUCTION_ROL</strong>: ROL reductions (medium priority)</li>
 *   <li><strong>REDUCTION_UNION</strong>: Union-related time off (medium priority)</li>
 *   <li><strong>CUSTOM</strong>: Custom exceptions (low priority)</li>
 * </ul>
 *
 * <h3>Priority Resolution Rules:</h3>
 * <ul>
 *   <li><strong>Higher Priority Wins</strong>: Exceptions with higher priority override lower ones</li>
 *   <li><strong>Temporal Precedence</strong>: Later exceptions override earlier ones at same priority</li>
 *   <li><strong>Conflict Detection</strong>: Automatic detection and resolution of conflicts</li>
 * </ul>
 *
 * <h3>Business Rules:</h3>
 * <ul>
 *   <li><strong>Absence Exceptions</strong>: Remove all shifts for the user</li>
 *   <li><strong>Replacement Exceptions</strong>: Replace shift with alternative</li>
 *   <li><strong>Addition Exceptions</strong>: Add extra shifts to existing schedule</li>
 *   <li><strong>Time Modifications</strong>: Adjust shift timing without changing type</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 3.1.0 - Pure Domain Algorithm
 * @since Step 3 - Clean Architecture Implementation
 */
public class ExceptionResolver extends LocalizableDomainModel {

    private static final String LOCALIZATION_SCOPE = "exception_resolver";

    // Exception priority mapping (higher number = higher priority)
    private final Map<ShiftException.ExceptionType, Integer> mExceptionPriorities;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for creating exception resolver with optional localization.
     *
     * @param localizer DomainLocalizer for i18n support (can be null)
     */
    public ExceptionResolver(@Nullable DomainLocalizer localizer) {
        super(localizer, LOCALIZATION_SCOPE);
        this.mExceptionPriorities = initializeExceptionPriorities();
    }

    // ==================== LOCALIZABLE IMPLEMENTATION ====================

    /**
     * Create a copy of this ExceptionResolver with localizer injected.
     *
     * @param localizer DomainLocalizer to inject
     * @return New ExceptionResolver instance with localizer support
     */
    @Override
    @NonNull
    public ExceptionResolver withLocalizer(@NonNull DomainLocalizer localizer) {
        return new ExceptionResolver(localizer);
    }

    // ==================== CORE EXCEPTION RESOLUTION ====================

    /**
     * Apply exceptions to a base work schedule with priority-based resolution.
     *
     * <p>Pure algorithm that takes all required data as parameters and returns
     * the modified schedule without any external dependencies.</p>
     *
     * @param baseSchedule Original work schedule day
     * @param exceptions List of domain exceptions to apply
     * @param userTeamMappings Map of userId to Team for team assignment lookups
     * @param replacementShifts Map of shiftId to WorkScheduleShift for replacements
     * @return Modified WorkScheduleDay with exceptions applied
     */
    @NonNull
    public WorkScheduleDay applyExceptions(@NonNull WorkScheduleDay baseSchedule,
                                           @NonNull List<ShiftException> exceptions,
                                           @NonNull Map<Long, Team> userTeamMappings,
                                           @NonNull Map<String, WorkScheduleShift> replacementShifts) {
        try {
            logDebug("Applying " + exceptions.size() + " exceptions to schedule for: " +
                    baseSchedule.getDate());

            if (exceptions.isEmpty()) {
                return baseSchedule; // No changes needed
            }

            // Filter and validate exceptions
            List<ShiftException> validExceptions = filterValidExceptions(exceptions, baseSchedule.getDate());

            if (validExceptions.isEmpty()) {
                return baseSchedule;
            }

            // Sort exceptions by priority and temporal order
            List<ShiftException> sortedExceptions = sortExceptionsByPriority(validExceptions);

            // Create mutable copy of schedule for modifications
            WorkScheduleDay modifiedSchedule = cloneScheduleDay(baseSchedule);

            // Apply each exception in priority order
            for (ShiftException exception : sortedExceptions) {
                modifiedSchedule = applyIndividualException(modifiedSchedule, exception,
                        userTeamMappings, replacementShifts);
            }

            // Validate final schedule consistency
            validateScheduleConsistency(modifiedSchedule);

            logDebug("Successfully applied exceptions. Final schedule has " +
                    modifiedSchedule.getShifts().size() + " shifts");

            return modifiedSchedule;

        } catch (Exception e) {
            logError("Error applying exceptions to schedule", e);
            return baseSchedule; // Return original schedule on error
        }
    }

    /**
     * Apply single exception to work schedule with conflict detection.
     *
     * @param schedule Current work schedule
     * @param exception Domain exception to apply
     * @param userTeamMappings Map of userId to Team for team lookups
     * @param replacementShifts Map of shiftId to WorkScheduleShift for replacements
     * @return Modified work schedule
     */
    @NonNull
    public WorkScheduleDay applyIndividualException(@NonNull WorkScheduleDay schedule,
                                                    @NonNull ShiftException exception,
                                                    @NonNull Map<Long, Team> userTeamMappings,
                                                    @NonNull Map<String, WorkScheduleShift> replacementShifts) {
        try {
            logVerbose("Applying exception: " + exception.getType() +
                    " for user: " + exception.getUserId());

            switch (exception.getType()) {
                case ABSENCE_VACATION:
                case ABSENCE_SICK:
                case ABSENCE_SPECIAL:
                    return applyAbsenceException(schedule, exception, userTeamMappings);

                case CHANGE_COMPANY:
                    return applyCompanyChangeException(schedule, exception, userTeamMappings, replacementShifts);

                case CHANGE_SWAP:
                    return applyShiftSwapException(schedule, exception, userTeamMappings);

                case CHANGE_SPECIAL:
                    return applySpecialChangeException(schedule, exception, userTeamMappings, replacementShifts);

                case REDUCTION_PERSONAL:
                case REDUCTION_ROL:
                case REDUCTION_UNION:
                    return applyTimeReductionException(schedule, exception, userTeamMappings);

                case CUSTOM:
                    return applyCustomException(schedule, exception, userTeamMappings, replacementShifts);

                default:
                    logWarning("Unknown exception type: " + exception.getType());
                    return schedule;
            }

        } catch (Exception e) {
            logError("Error applying individual exception", e);
            return schedule;
        }
    }

    /**
     * Detect conflicts between multiple exceptions.
     *
     * @param exceptions List of domain exceptions to check
     * @param date Target date
     * @return ConflictReport with detected conflicts
     */
    @NonNull
    public ConflictReport detectConflicts(@NonNull List<ShiftException> exceptions,
                                          @NonNull LocalDate date) {
        ConflictReport report = new ConflictReport();

        try {
            // Group exceptions by user and check for conflicts
            Map<Long, List<ShiftException>> exceptionsByUser = exceptions.stream()
                    .collect(Collectors.groupingBy(ShiftException::getUserId));

            for (Map.Entry<Long, List<ShiftException>> entry : exceptionsByUser.entrySet()) {
                Long userId = entry.getKey();
                List<ShiftException> userExceptions = entry.getValue();

                // Check for conflicts within user's exceptions
                List<ExceptionConflict> userConflicts = detectUserConflicts(userId, userExceptions);
                report.addConflicts(userConflicts);
            }

            // Check for team-level conflicts (multiple people affecting same shift)
            List<ExceptionConflict> teamConflicts = detectTeamConflicts(exceptions);
            report.addConflicts(teamConflicts);

            logDebug("Conflict detection complete. Found " + report.getConflictCount() + " conflicts");

        } catch (Exception e) {
            logError("Error detecting conflicts", e);
        }

        return report;
    }

    // ==================== EXCEPTION TYPE HANDLERS ====================

    private WorkScheduleDay applyAbsenceException(@NonNull WorkScheduleDay schedule,
                                                  @NonNull ShiftException exception,
                                                  @NonNull Map<Long, Team> userTeamMappings) {
        // Remove all shifts for the user on this day
        Long userId = exception.getUserId();

        List<WorkScheduleShift> remainingShifts = schedule.getShifts().stream()
                .filter(shift -> !isShiftAssignedToUser(shift, userId, userTeamMappings))
                .collect(Collectors.toList());

        WorkScheduleDay.Builder builder = WorkScheduleDay.builder(schedule.getDate());

        for (WorkScheduleShift shift : remainingShifts) {
            builder.addShift(shift);
        }

        WorkScheduleDay modifiedSchedule = builder.build();

        logVerbose("Applied absence exception for user " + userId +
                ". Removed " + (schedule.getShifts().size() - remainingShifts.size()) + " shifts");

        return modifiedSchedule;
    }

    private WorkScheduleDay applyCompanyChangeException(@NonNull WorkScheduleDay schedule,
                                                        @NonNull ShiftException exception,
                                                        @NonNull Map<Long, Team> userTeamMappings,
                                                        @NonNull Map<String, WorkScheduleShift> replacementShifts) {
        // Replace user's shifts with new shift from company directive
        WorkScheduleDay modifiedSchedule = applyAbsenceException(schedule, exception, userTeamMappings);

        if (exception.getNewShiftId() != null) {
            WorkScheduleShift replacementShift = createReplacementShiftFromException(
                    exception, replacementShifts, userTeamMappings);

            if (replacementShift != null) {
                WorkScheduleDay.Builder builder = WorkScheduleDay.builder(modifiedSchedule.getDate())
                        .copyFrom(modifiedSchedule)
                        .addShift(replacementShift);

                modifiedSchedule = builder.build();

                logVerbose("Applied company change exception with replacement shift: " +
                        exception.getNewShiftId());
            }
        }

        return modifiedSchedule;
    }

    private WorkScheduleDay applyShiftSwapException(@NonNull WorkScheduleDay schedule,
                                                    @NonNull ShiftException exception,
                                                    @NonNull Map<Long, Team> userTeamMappings) {
        // Swap shifts between two users
        Long userId1 = exception.getUserId();
        Long userId2 = exception.getSwapWithUserId();

        if (userId2 == null) {
            logWarning("Shift swap exception missing swap target user");
            return schedule;
        }

        // Find shifts for both users
        List<WorkScheduleShift> user1Shifts = findShiftsForUser(schedule, userId1, userTeamMappings);
        List<WorkScheduleShift> user2Shifts = findShiftsForUser(schedule, userId2, userTeamMappings);

        WorkScheduleDay.Builder builder = WorkScheduleDay.builder(schedule.getDate());

        // Copy all other shifts unchanged
        for (WorkScheduleShift shift : schedule.getShifts()) {
            if (!isShiftAssignedToUser(shift, userId1, userTeamMappings) &&
                    !isShiftAssignedToUser(shift, userId2, userTeamMappings)) {
                builder.addShift(shift);
            }
        }

        // Get teams for both users
        Team user1Team = userTeamMappings.get(userId1);
        Team user2Team = userTeamMappings.get(userId2);

        // Add swapped shifts with correct team assignments
        for (WorkScheduleShift shift : user1Shifts) {
            if (user2Team != null) {
                WorkScheduleShift swappedShift = createShiftWithNewTeam(shift, user2Team);
                builder.addShift(swappedShift);
            }
        }

        for (WorkScheduleShift shift : user2Shifts) {
            if (user1Team != null) {
                WorkScheduleShift swappedShift = createShiftWithNewTeam(shift, user1Team);
                builder.addShift(swappedShift);
            }
        }

        logVerbose("Applied shift swap between users " + userId1 + " and " + userId2);

        return builder.build();
    }

    private WorkScheduleDay applySpecialChangeException(@NonNull WorkScheduleDay schedule,
                                                        @NonNull ShiftException exception,
                                                        @NonNull Map<Long, Team> userTeamMappings,
                                                        @NonNull Map<String, WorkScheduleShift> replacementShifts) {
        // Apply special event coverage changes
        return applyCompanyChangeException(schedule, exception, userTeamMappings, replacementShifts);
    }

    private WorkScheduleDay applyTimeReductionException(@NonNull WorkScheduleDay schedule,
                                                        @NonNull ShiftException exception,
                                                        @NonNull Map<Long, Team> userTeamMappings) {
        // Modify existing shift timing for user
        Long userId = exception.getUserId();

        WorkScheduleDay.Builder builder = WorkScheduleDay.builder(schedule.getDate());

        for (WorkScheduleShift shift : schedule.getShifts()) {
            if (isShiftAssignedToUser(shift, userId, userTeamMappings)) {
                // Apply time changes to user's shift
                WorkScheduleShift modifiedShift = applyTimeChangesToShift(shift, exception);
                builder.addShift(modifiedShift);
            } else {
                // Keep other shifts unchanged
                builder.addShift(shift);
            }
        }

        logVerbose("Applied time reduction exception for user " + userId);

        return builder.build();
    }

    private WorkScheduleDay applyCustomException(@NonNull WorkScheduleDay schedule,
                                                 @NonNull ShiftException exception,
                                                 @NonNull Map<Long, Team> userTeamMappings,
                                                 @NonNull Map<String, WorkScheduleShift> replacementShifts) {
        // Handle custom exception types based on metadata
        String customType = exception.getMetadata("custom_type");

        if ("extra_shift".equals(customType)) {
            return applyExtraShiftLogic(schedule, exception, replacementShifts);
        } else if ("override".equals(customType)) {
            return applyCompanyChangeException(schedule, exception, userTeamMappings, replacementShifts);
        } else {
            logWarning("Unknown custom exception type: " + customType);
            return schedule;
        }
    }

    private WorkScheduleDay applyExtraShiftLogic(@NonNull WorkScheduleDay schedule,
                                                 @NonNull ShiftException exception,
                                                 @NonNull Map<String, WorkScheduleShift> replacementShifts) {
        // Add extra shift for user
        WorkScheduleDay.Builder builder = WorkScheduleDay.builder(schedule.getDate())
                .copyFrom(schedule);

        if (exception.getNewShiftId() != null) {
            WorkScheduleShift extraShift = replacementShifts.get(exception.getNewShiftId());

            if (extraShift == null && exception.getNewStartTime() != null && exception.getNewEndTime() != null) {
                // Create shift from exception data
                extraShift = WorkScheduleShift.builder()
                        .startTime(exception.getNewStartTime())
                        .endTime(exception.getNewEndTime())
                        .build();
            }

            if (extraShift != null) {
                builder.addShift(extraShift);
                logVerbose("Applied extra shift exception: " + exception.getNewShiftId());
            }
        }

        return builder.build();
    }

    // ==================== HELPER METHODS ====================

    private Map<ShiftException.ExceptionType, Integer> initializeExceptionPriorities() {
        Map<ShiftException.ExceptionType, Integer> priorities = new HashMap<>();

        // Highest priority (10-9)
        priorities.put(ShiftException.ExceptionType.ABSENCE_VACATION, 10);
        priorities.put(ShiftException.ExceptionType.ABSENCE_SICK, 10);
        priorities.put(ShiftException.ExceptionType.ABSENCE_SPECIAL, 10);

        // High priority (8-7)
        priorities.put(ShiftException.ExceptionType.CHANGE_COMPANY, 8);
        priorities.put(ShiftException.ExceptionType.CHANGE_SPECIAL, 7);

        // Medium priority (6-5)
        priorities.put(ShiftException.ExceptionType.CHANGE_SWAP, 6);
        priorities.put(ShiftException.ExceptionType.REDUCTION_PERSONAL, 5);
        priorities.put(ShiftException.ExceptionType.REDUCTION_ROL, 5);
        priorities.put(ShiftException.ExceptionType.REDUCTION_UNION, 5);

        // Low priority (4-1)
        priorities.put(ShiftException.ExceptionType.CUSTOM, 4);

        return priorities;
    }

    private List<ShiftException> filterValidExceptions(@NonNull List<ShiftException> exceptions,
                                                       @NonNull LocalDate date) {
        return exceptions.stream()
                .filter(exception -> exception.appliesTo(date))
                .filter(ShiftException::isEffective)
                .filter(this::isExceptionTypeSupported)
                .collect(Collectors.toList());
    }

    private boolean isExceptionTypeSupported(@NonNull ShiftException exception) {
        return mExceptionPriorities.containsKey(exception.getType());
    }

    private List<ShiftException> sortExceptionsByPriority(@NonNull List<ShiftException> exceptions) {
        return exceptions.stream()
                .sorted((e1, e2) -> {
                    // Primary sort: Priority (higher first)
                    int priority1 = mExceptionPriorities.getOrDefault(e1.getType(), 0);
                    int priority2 = mExceptionPriorities.getOrDefault(e2.getType(), 0);

                    int priorityComparison = Integer.compare(priority2, priority1);
                    if (priorityComparison != 0) {
                        return priorityComparison;
                    }

                    // Secondary sort: Creation time (later first)
                    return Long.compare(e2.getCreatedAt(), e1.getCreatedAt());
                })
                .collect(Collectors.toList());
    }

    private WorkScheduleDay cloneScheduleDay(@NonNull WorkScheduleDay original) {
        return WorkScheduleDay.builder(original.getDate())
                .copyFrom(original)
                .build();
    }

    /**
     * Check if shift is assigned to specific user using provided team mappings.
     *
     * @param shift WorkScheduleShift to check
     * @param userId User ID to check assignment for
     * @param userTeamMappings Map of userId to Team for lookups
     * @return true if shift is assigned to user, false otherwise
     */
    private boolean isShiftAssignedToUser(@NonNull WorkScheduleShift shift,
                                          @NonNull Long userId,
                                          @NonNull Map<Long, Team> userTeamMappings) {
        Team userTeam = userTeamMappings.get(userId);
        if (userTeam == null) {
            return false;
        }

        return shift.getTeams().stream()
                .anyMatch(team -> Objects.equals(team.getId(), userTeam.getId()));
    }

    private List<WorkScheduleShift> findShiftsForUser(@NonNull WorkScheduleDay schedule,
                                                      @NonNull Long userId,
                                                      @NonNull Map<Long, Team> userTeamMappings) {
        return schedule.getShifts().stream()
                .filter(shift -> isShiftAssignedToUser(shift, userId, userTeamMappings))
                .collect(Collectors.toList());
    }

    @Nullable
    private WorkScheduleShift createReplacementShiftFromException(@NonNull ShiftException exception,
                                                                  @NonNull Map<String, WorkScheduleShift> replacementShifts,
                                                                  @NonNull Map<Long, Team> userTeamMappings) {
        if (exception.getNewShiftId() == null) {
            return null;
        }

        try {
            // Try to get replacement shift from provided map
            WorkScheduleShift baseShift = replacementShifts.get(exception.getNewShiftId());

            if (baseShift != null) {
                return baseShift;
            }

            // Create from exception data if no replacement found
            LocalTime startTime = exception.getNewStartTime() != null ?
                    exception.getNewStartTime() : getDefaultStartTime();
            LocalTime endTime = exception.getNewEndTime() != null ?
                    exception.getNewEndTime() : getDefaultEndTime();

            WorkScheduleShift.Builder shiftBuilder = WorkScheduleShift.builder()
                    .startTime(startTime)
                    .endTime(endTime);

            // Add user's team if available
            Team userTeam = userTeamMappings.get(exception.getUserId());
            if (userTeam != null) {
                shiftBuilder.addTeam(userTeam);
            }

            return shiftBuilder.build();

        } catch (Exception e) {
            logError("Error creating replacement shift", e);
            return null;
        }
    }

    private WorkScheduleShift createShiftWithNewTeam(@NonNull WorkScheduleShift originalShift,
                                                     @NonNull Team newTeam) {
        return WorkScheduleShift.builder()
                .copyFrom(originalShift)
                .clearTeams()
                .addTeam(newTeam)
                .build();
    }

    private LocalTime getDefaultStartTime() {
        return LocalTime.of(8, 0);
    }

    private LocalTime getDefaultEndTime() {
        return LocalTime.of(16, 0);
    }

    private WorkScheduleShift applyTimeChangesToShift(@NonNull WorkScheduleShift original,
                                                      @NonNull ShiftException exception) {
        LocalTime newStartTime = exception.getNewStartTime() != null ?
                exception.getNewStartTime() : original.getStartTime();
        LocalTime newEndTime = exception.getNewEndTime() != null ?
                exception.getNewEndTime() : original.getEndTime();

        return WorkScheduleShift.builder()
                .copyFrom(original)
                .startTime(newStartTime)
                .endTime(newEndTime)
                .build();
    }

    private List<ExceptionConflict> detectUserConflicts(@NonNull Long userId,
                                                        @NonNull List<ShiftException> userExceptions) {
        List<ExceptionConflict> conflicts = new ArrayList<>();

        // Check for conflicting exception types for same user on same day
        for (int i = 0; i < userExceptions.size(); i++) {
            for (int j = i + 1; j < userExceptions.size(); j++) {
                ShiftException exception1 = userExceptions.get(i);
                ShiftException exception2 = userExceptions.get(j);

                if (areExceptionsConflicting(exception1, exception2)) {
                    String conflictDescription = localize("conflict.user_exceptions",
                            "Conflicting exceptions for user " + userId,
                            userId.toString());

                    conflicts.add(new ExceptionConflict(exception1, exception2, conflictDescription));
                }
            }
        }

        return conflicts;
    }

    private List<ExceptionConflict> detectTeamConflicts(@NonNull List<ShiftException> exceptions) {
        // Implementation would check for team-level conflicts
        // For now, return empty list as this requires more complex team analysis
        return new ArrayList<>();
    }

    private boolean areExceptionsConflicting(@NonNull ShiftException exception1,
                                             @NonNull ShiftException exception2) {
        // Define conflicting exception type combinations
        ShiftException.ExceptionType type1 = exception1.getType();
        ShiftException.ExceptionType type2 = exception2.getType();

        // Absence conflicts with any working exception
        if (exception1.isAbsence() && !exception2.isAbsence()) {
            return true;
        }

        if (exception2.isAbsence() && !exception1.isAbsence()) {
            return true;
        }

        // Multiple time modifications conflict
        if (exception1.isTimeReduction() && exception2.isTimeReduction()) {
            return true;
        }

        return false;
    }

    private void validateScheduleConsistency(@NonNull WorkScheduleDay schedule) {
        // Perform basic consistency checks
        for (WorkScheduleShift shift : schedule.getShifts()) {
            if (shift.getStartTime().isAfter(shift.getEndTime()) && !isOvernightShift(shift)) {
                logWarning("Invalid shift timing detected: start after end");
            }

            if (shift.getTeams().isEmpty()) {
                logWarning("Shift with no assigned teams detected");
            }
        }
    }

    private boolean isOvernightShift(@NonNull WorkScheduleShift shift) {
        // Check if shift crosses midnight
        return shift.getStartTime().isAfter(shift.getEndTime());
    }

    // ==================== LOCALIZED DISPLAY METHODS ====================

    /**
     * Get localized conflict resolution message.
     */
    @NonNull
    public String getConflictResolutionMessage(@NonNull List<ExceptionConflict> conflicts) {
        if (conflicts.isEmpty()) {
            return localize("resolution.no_conflicts",
                    "No conflicts detected",
                    String.valueOf(conflicts.size()));
        }

        return localize("resolution.conflicts_found",
                "Found " + conflicts.size() + " conflicts",
                String.valueOf(conflicts.size()));
    }

    /**
     * Get localized exception priority name.
     */
    @NonNull
    public String getExceptionPriorityName(@NonNull ShiftException.ExceptionType type) {
        int priority = mExceptionPriorities.getOrDefault(type, 0);
        String priorityKey = getPriorityKey(priority);

        return localize("priority." + priorityKey,
                priorityKey.toUpperCase(),
                String.valueOf(priority));
    }

    private String getPriorityKey(int priority) {
        if (priority >= 9) return "highest";
        if (priority >= 7) return "high";
        if (priority >= 5) return "medium";
        return "low";
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Create ExceptionResolver without localization support.
     */
    @NonNull
    public static ExceptionResolver create() {
        return new ExceptionResolver(null);
    }

    /**
     * Create ExceptionResolver with localization support.
     */
    @NonNull
    public static ExceptionResolver create(@NonNull DomainLocalizer localizer) {
        return new ExceptionResolver(localizer);
    }

    // ==================== INNER CLASSES ====================

    public static class ConflictReport {
        private final List<ExceptionConflict> conflicts = new ArrayList<>();

        public void addConflict(@NonNull ExceptionConflict conflict) {
            conflicts.add(conflict);
        }

        public void addConflicts(@NonNull List<ExceptionConflict> conflicts) {
            this.conflicts.addAll(conflicts);
        }

        @NonNull
        public List<ExceptionConflict> getConflicts() {
            return new ArrayList<>(conflicts);
        }

        public int getConflictCount() {
            return conflicts.size();
        }

        public boolean hasConflicts() {
            return !conflicts.isEmpty();
        }
    }

    public static class ExceptionConflict {
        @NonNull public final ShiftException exception1;
        @NonNull public final ShiftException exception2;
        @NonNull public final String description;

        public ExceptionConflict(@NonNull ShiftException exception1,
                                 @NonNull ShiftException exception2,
                                 @NonNull String description) {
            this.exception1 = exception1;
            this.exception2 = exception2;
            this.description = description;
        }
    }
}