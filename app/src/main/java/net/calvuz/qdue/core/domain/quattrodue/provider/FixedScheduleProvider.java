package net.calvuz.qdue.core.domain.quattrodue.provider;

import net.calvuz.qdue.core.domain.quattrodue.models.ShiftType;
import net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleEvent;
import net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleTemplate;
import net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleType;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ✅ PROVIDER IMPLEMENTATION: Fixed Schedule Provider for 4-2 Pattern
 *
 * <p>Implements the classic 4-2 work schedule pattern using the existing
 * SchemeManager logic. Generates volatile events based on the 18-day
 * rotation cycle with proper team assignments.</p>
 *
 * <p>Pattern details:</p>
 * <ul>
 *   <li>18-day rotation cycle</li>
 *   <li>3 shifts per day: Morning, Afternoon, Night</li>
 *   <li>9 teams: A, B, C, D, E, F, G, H, I</li>
 *   <li>Each team works 4 days, then rests 2 days</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 5
 */
public class FixedScheduleProvider implements WorkScheduleProvider {

    private static final String TAG = "FixedScheduleProvider";

    // Constants matching existing SchemeManager
    private static final int CYCLE_LENGTH = 18;
    private static final int SHIFTS_PER_DAY = 3;

    // Reference start date for the scheme (from SchemeManager)
    private static final LocalDate REFERENCE_START_DATE = LocalDate.of(2018, 11, 7);

    // Fixed rotation scheme - converted from existing SchemeManager logic
    private static final char[][][] SCHEME = new char[][][]{
            {{'A', 'B'}, {'C', 'D'}, {'E', 'F'}},
            {{'A', 'B'}, {'C', 'D'}, {'E', 'F'}},
            {{'A', 'H'}, {'D', 'I'}, {'G', 'F'}},
            {{'A', 'H'}, {'D', 'I'}, {'G', 'F'}},
            {{'C', 'H'}, {'E', 'I'}, {'G', 'B'}},
            {{'C', 'H'}, {'E', 'I'}, {'G', 'B'}},
            {{'C', 'D'}, {'E', 'F'}, {'A', 'B'}},
            {{'C', 'D'}, {'E', 'F'}, {'A', 'B'}},
            {{'D', 'I'}, {'G', 'F'}, {'A', 'H'}},
            {{'D', 'I'}, {'G', 'F'}, {'A', 'H'}},
            {{'E', 'I'}, {'G', 'B'}, {'C', 'H'}},
            {{'E', 'I'}, {'G', 'B'}, {'C', 'H'}},
            {{'E', 'F'}, {'A', 'B'}, {'C', 'D'}},
            {{'E', 'F'}, {'A', 'B'}, {'C', 'D'}},
            {{'G', 'F'}, {'A', 'H'}, {'D', 'I'}},
            {{'G', 'F'}, {'A', 'H'}, {'D', 'I'}},
            {{'G', 'B'}, {'C', 'H'}, {'E', 'I'}},
            {{'G', 'B'}, {'C', 'H'}, {'E', 'I'}}
    };

    // Shift type mapping - will be populated with actual ShiftType instances
    private Map<Integer, ShiftType> shiftTypeMapping;

    // ==================== CONSTRUCTOR ====================

    public FixedScheduleProvider() {
        initializeShiftTypeMapping();
    }

    /**
     * Initializes the mapping between shift indices and ShiftType instances.
     * This should be called with actual ShiftType instances from the service.
     */
    private void initializeShiftTypeMapping() {
        shiftTypeMapping = new HashMap<>();
        // Will be populated by the service when ShiftTypes are available
    }

    /**
     * Sets the shift type mapping for the provider.
     *
     * @param morningShift Morning shift type
     * @param afternoonShift Afternoon shift type
     * @param nightShift Night shift type
     */
    public void setShiftTypeMapping(ShiftType morningShift, ShiftType afternoonShift, ShiftType nightShift) {
        shiftTypeMapping.put(0, morningShift);    // Morning = index 0
        shiftTypeMapping.put(1, afternoonShift);  // Afternoon = index 1
        shiftTypeMapping.put(2, nightShift);      // Night = index 2

        Log.d(TAG, "Shift type mapping initialized");
    }

    // ==================== WORKSCHEDULEPROVIDER IMPLEMENTATION ====================

    @Override
    public List<WorkScheduleEvent> generateSchedule(LocalDate startDate, LocalDate endDate,
                                                    WorkScheduleTemplate template, String userTeam) {
        List<WorkScheduleEvent> events = new ArrayList<>();

        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            Log.w(TAG, "Invalid date range provided");
            return events;
        }

        if (!supportsTemplate(template)) {
            Log.w(TAG, "Template not supported: " + (template != null ? template.getType() : "null"));
            return events;
        }

        if (shiftTypeMapping.isEmpty()) {
            Log.e(TAG, "Shift type mapping not initialized");
            return events;
        }

        try {
            Log.d(TAG, "Generating fixed schedule from " + startDate + " to " + endDate +
                    " for team: " + userTeam);

            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                List<WorkScheduleEvent> dayEvents = generateScheduleForDate(currentDate, template, userTeam);
                events.addAll(dayEvents);
                currentDate = currentDate.plusDays(1);
            }

            Log.d(TAG, "Generated " + events.size() + " schedule events");

        } catch (Exception e) {
            Log.e(TAG, "Error generating schedule", e);
        }

        return events;
    }

    @Override
    public List<WorkScheduleEvent> generateScheduleForDate(LocalDate date, WorkScheduleTemplate template, String userTeam) {
        List<WorkScheduleEvent> dayEvents = new ArrayList<>();

        if (date == null || !supportsTemplate(template)) {
            return dayEvents;
        }

        try {
            // Calculate cycle index for the date
            int cycleIndex = calculateCycleIndex(date);

            // Generate events for each shift
            for (int shiftIndex = 0; shiftIndex < SHIFTS_PER_DAY; shiftIndex++) {
                ShiftType shiftType = shiftTypeMapping.get(shiftIndex);
                if (shiftType == null) {
                    Log.w(TAG, "No shift type mapped for index: " + shiftIndex);
                    continue;
                }

                // Get teams assigned to this shift on this day
                char[] assignedTeamChars = SCHEME[cycleIndex][shiftIndex];
                List<String> assignedTeams = new ArrayList<>();
                for (char teamChar : assignedTeamChars) {
                    assignedTeams.add(String.valueOf(teamChar));
                }

                // Filter by user team if specified
                if (userTeam != null && !userTeam.trim().isEmpty()) {
                    if (!assignedTeams.contains(userTeam.trim())) {
                        continue; // Skip this shift if user's team is not assigned
                    }
                }

                // Create work schedule event
                WorkScheduleEvent event = createWorkScheduleEvent(date, shiftType, assignedTeams,
                        template, cycleIndex);
                dayEvents.add(event);

                Log.v(TAG, "Created event: " + event.getTitle() + " for teams: " + assignedTeams);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error generating day schedule for " + date, e);
        }

        return dayEvents;
    }

    @Override
    public boolean supportsTemplate(WorkScheduleTemplate template) {
        return template != null &&
                (template.getType() == WorkScheduleType.FIXED_4_2 ||
                        (template.getType() == WorkScheduleType.CUSTOM && template.getCycleDays() == CYCLE_LENGTH));
    }

    @Override
    public String getProviderName() {
        return "Fixed 4-2 Schedule Provider";
    }

    @Override
    public String getProviderDescription() {
        return "Provides work schedules based on the classic 4-2 continuous pattern with 18-day rotation cycle";
    }

    @Override
    public boolean validateTemplate(WorkScheduleTemplate template) {
        if (template == null) {
            return false;
        }

        if (template.getType() == WorkScheduleType.FIXED_4_2) {
            return template.getCycleDays() == CYCLE_LENGTH;
        }

        if (template.getType() == WorkScheduleType.CUSTOM) {
            // For custom templates, validate that cycle length matches
            return template.getCycleDays() == CYCLE_LENGTH &&
                    template.getPatterns() != null &&
                    template.getPatterns().size() == CYCLE_LENGTH;
        }

        return false;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Calculates the cycle index for a given date based on the reference start date.
     *
     * @param date Target date
     * @return Cycle index (0-based, 0 to CYCLE_LENGTH-1)
     */
    private int calculateCycleIndex(LocalDate date) {
        if (date == null) {
            return 0;
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(REFERENCE_START_DATE, date);

        // Handle negative values (dates before reference)
        if (daysBetween < 0) {
            daysBetween = ((daysBetween % CYCLE_LENGTH) + CYCLE_LENGTH) % CYCLE_LENGTH;
        }

        return (int) (daysBetween % CYCLE_LENGTH);
    }

    /**
     * Creates a WorkScheduleEvent for a fixed schedule assignment.
     *
     * @param date Event date
     * @param shiftType Shift type
     * @param assignedTeams Teams assigned to this shift
     * @param template Work schedule template
     * @param cycleIndex Index in the rotation cycle
     * @return Created WorkScheduleEvent
     */
    private WorkScheduleEvent createWorkScheduleEvent(LocalDate date, ShiftType shiftType,
                                                      List<String> assignedTeams, WorkScheduleTemplate template,
                                                      int cycleIndex) {
        // Create event title
        String title = createEventTitle(shiftType, assignedTeams);

        // Create the event using builder pattern
        WorkScheduleEvent.Builder builder = new WorkScheduleEvent.Builder()
                .date(date)
                .shiftType(shiftType)
                .assignedTeams(assignedTeams)
                .title(title)
                .cycleDay(cycleIndex)
                .providerName(getProviderName());

        // Set template information if available
        if (template != null && template.getId() != null) {
            builder.templateId(template.getId().toString());
        }

        // Set description
        String description = createEventDescription(shiftType, assignedTeams, template, cycleIndex);
        builder.description(description);

        return builder.build();
    }

    /**
     * Creates a title for the work schedule event.
     *
     * @param shiftType Shift type
     * @param assignedTeams Teams assigned to this shift
     * @return Event title
     */
    private String createEventTitle(ShiftType shiftType, List<String> assignedTeams) {
        StringBuilder title = new StringBuilder();

        if (shiftType != null) {
            title.append(shiftType.getName());
        }

        if (assignedTeams != null && !assignedTeams.isEmpty()) {
            title.append(" - Squadre ");
            title.append(String.join(", ", assignedTeams));
        }

        title.append(" (4-2)");

        return title.toString();
    }

    /**
     * Creates a description for the work schedule event.
     *
     * @param shiftType Shift type
     * @param assignedTeams Teams assigned to this shift
     * @param template Work schedule template
     * @param cycleIndex Index in cycle
     * @return Event description
     */
    private String createEventDescription(ShiftType shiftType, List<String> assignedTeams,
                                          WorkScheduleTemplate template, int cycleIndex) {
        StringBuilder desc = new StringBuilder();

        if (shiftType != null) {
            desc.append("Turno ").append(shiftType.getName());

            if (!shiftType.isRestPeriod()) {
                desc.append(" (").append(shiftType.getStartTime())
                        .append("-").append(shiftType.getEndTime()).append(")");
                if (shiftType.crossesMidnight()) {
                    desc.append(" - attraversa la mezzanotte");
                }
            }
        }

        if (assignedTeams != null && !assignedTeams.isEmpty()) {
            desc.append("\nSquadre assegnate: ").append(String.join(", ", assignedTeams));
        }

        desc.append("\nGiorno del ciclo: ").append(cycleIndex + 1).append("/").append(CYCLE_LENGTH);
        desc.append("\nSchema: 4-2 Continuo (18 giorni)");

        if (template != null && template.getDescription() != null) {
            desc.append("\n").append(template.getDescription());
        }

        return desc.toString();
    }
}