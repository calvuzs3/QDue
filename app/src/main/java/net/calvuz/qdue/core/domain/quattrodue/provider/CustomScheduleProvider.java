package net.calvuz.qdue.core.domain.quattrodue.provider;

import net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleEvent;
import net.calvuz.qdue.core.domain.quattrodue.models.WorkSchedulePattern;
import net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleTemplate;
import net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleType;
import net.calvuz.qdue.core.domain.quattrodue.models.WorkShiftAssignment;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ PROVIDER IMPLEMENTATION: Custom Schedule Provider
 *
 * <p>Implements custom user-defined work schedule patterns. Allows users to
 * create their own rotation patterns with custom shift assignments and
 * team rotations.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>User-defined cycle lengths</li>
 *   <li>Custom shift type assignments</li>
 *   <li>Flexible team rotation patterns</li>
 *   <li>Support for non-continuous schedules</li>
 *   <li>Enhanced Builder pattern usage</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 2.0
 * @since Database Version 5
 */
public class CustomScheduleProvider implements WorkScheduleProvider {

    private static final String TAG = "CustomScheduleProvider";

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

        try {
            Log.d(TAG, "Generating custom schedule from " + startDate + " to " + endDate +
                    " for team: " + userTeam + ", template: " + template.getName());

            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                List<WorkScheduleEvent> dayEvents = generateScheduleForDate(currentDate, template, userTeam);
                events.addAll(dayEvents);
                currentDate = currentDate.plusDays(1);
            }

            Log.d(TAG, "Generated " + events.size() + " custom schedule events");

        } catch (Exception e) {
            Log.e(TAG, "Error generating custom schedule", e);
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
            // Calculate which pattern to use based on cycle
            int daysSinceStart = calculateDaysSinceTemplateStart(date, template);
            int cycleIndex = daysSinceStart % template.getCycleDays();

            // Get the pattern for this day
            WorkSchedulePattern pattern = template.getPatternForDay(cycleIndex);
            if (pattern == null) {
                Log.w(TAG, "No pattern found for cycle day " + cycleIndex);
                return dayEvents;
            }

            // Generate events for each shift assignment in the pattern
            List<WorkShiftAssignment> assignments = pattern.getShiftAssignments();
            if (assignments != null) {
                for (WorkShiftAssignment assignment : assignments) {
                    // Filter by user team if specified
                    if (userTeam != null && !userTeam.trim().isEmpty()) {
                        if (!assignment.hasTeam(userTeam.trim())) {
                            continue; // Skip this assignment if user's team is not assigned
                        }
                    }

                    // Create work schedule event using improved builder pattern
                    WorkScheduleEvent event = createCustomWorkScheduleEvent(date, assignment,
                            template, cycleIndex);
                    dayEvents.add(event);

                    Log.v(TAG, "Created custom event: " + event.getTitle() +
                            " for teams: " + assignment.getTeams());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error generating custom day schedule for " + date, e);
        }

        return dayEvents;
    }

    @Override
    public boolean supportsTemplate(WorkScheduleTemplate template) {
        return template != null &&
                template.getType() == WorkScheduleType.CUSTOM &&
                template.isUserDefined() &&
                template.getPatterns() != null &&
                !template.getPatterns().isEmpty();
    }

    @Override
    public String getProviderName() {
        return "Custom Schedule Provider";
    }

    @Override
    public String getProviderDescription() {
        return "Provides work schedules based on user-defined custom patterns and rotations";
    }

    @Override
    public boolean validateTemplate(WorkScheduleTemplate template) {
        if (template == null) {
            return false;
        }

        if (template.getType() != WorkScheduleType.CUSTOM) {
            return false;
        }

        if (!template.isUserDefined()) {
            return false;
        }

        if (template.getCycleDays() <= 0) {
            return false;
        }

        List<WorkSchedulePattern> patterns = template.getPatterns();
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }

        // Validate that we have enough patterns for the cycle
        if (patterns.size() < template.getCycleDays()) {
            Log.w(TAG, "Not enough patterns for cycle days: " + patterns.size() + "/" + template.getCycleDays());
            return false;
        }

        // Validate each pattern
        for (WorkSchedulePattern pattern : patterns) {
            if (pattern == null || !pattern.isValid()) {
                return false;
            }
        }

        return true;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Calculates days since the template was created/started.
     * For custom templates, we use the template creation date as reference.
     *
     * @param date Target date
     * @param template Work schedule template
     * @return Days since template start
     */
    private int calculateDaysSinceTemplateStart(LocalDate date, WorkScheduleTemplate template) {
        LocalDate referenceDate = template.getCreatedAt();
        if (referenceDate == null) {
            // Fallback to a fixed reference date
            referenceDate = LocalDate.of(2025, 1, 1);
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(referenceDate, date);

        // Handle negative values (dates before reference)
        if (daysBetween < 0) {
            int cycleDays = template.getCycleDays();
            daysBetween = ((daysBetween % cycleDays) + cycleDays) % cycleDays;
        }

        return (int) daysBetween;
    }

    /**
     * Creates a WorkScheduleEvent for a custom shift assignment using improved Builder pattern.
     *
     * @param date Event date
     * @param assignment Shift assignment
     * @param template Work schedule template
     * @param cycleDay Day in the cycle
     * @return Created WorkScheduleEvent
     */
    private WorkScheduleEvent createCustomWorkScheduleEvent(LocalDate date, WorkShiftAssignment assignment,
                                                            WorkScheduleTemplate template, int cycleDay) {
        // Create event title
        String title = createCustomEventTitle(assignment, template);

        // Create event description
        String description = createCustomEventDescription(assignment, template, cycleDay);

        // Create the event using enhanced Builder pattern
        WorkScheduleEvent.Builder builder = new WorkScheduleEvent.Builder()
                .date(date)
                .shiftType(assignment.getShiftType())
                .assignedTeams(assignment.getTeams())
                .title(title)
                .description(description)
                .cycleDay(cycleDay)
                .providerName(getProviderName());

        // Set template information if available
        if (template != null && template.getId() != null) {
            builder.templateId(template.getId().toString());
        }

        // Set special flags based on assignment
        if (assignment.isOvertime()) {
            builder.overtime(true);
        }

        return builder.build();
    }

    /**
     * Creates a title for the custom work schedule event.
     *
     * @param assignment Shift assignment
     * @param template Work schedule template
     * @return Event title
     */
    private String createCustomEventTitle(WorkShiftAssignment assignment, WorkScheduleTemplate template) {
        StringBuilder title = new StringBuilder();

        if (assignment.getShiftType() != null) {
            title.append(assignment.getShiftType().getName());
        }

        if (assignment.getTeams() != null && !assignment.getTeams().isEmpty()) {
            title.append(" - Squadre ");
            title.append(String.join(", ", assignment.getTeams()));
        }

        if (template != null && template.getName() != null) {
            title.append(" (").append(template.getName()).append(")");
        }

        // Add special indicators
        List<String> indicators = new ArrayList<>();
        if (assignment.isOvertime()) indicators.add("ST");
        if (assignment.isMandatory()) indicators.add("OBB");
        if (assignment.isTemporary()) indicators.add("TMP");

        if (!indicators.isEmpty()) {
            title.append(" [").append(String.join("|", indicators)).append("]");
        }

        return title.toString();
    }

    /**
     * Creates a description for the custom work schedule event.
     *
     * @param assignment Shift assignment
     * @param template Work schedule template
     * @param cycleDay Day in cycle
     * @return Event description
     */
    private String createCustomEventDescription(WorkShiftAssignment assignment,
                                                WorkScheduleTemplate template, int cycleDay) {
        StringBuilder desc = new StringBuilder();

        if (assignment.getShiftType() != null) {
            desc.append("Turno ").append(assignment.getShiftType().getName());

            if (!assignment.getShiftType().isRestPeriod()) {
                desc.append(" (").append(assignment.getShiftType().getStartTime())
                        .append("-").append(assignment.getShiftType().getEndTime()).append(")");
                if (assignment.getShiftType().crossesMidnight()) {
                    desc.append(" - attraversa la mezzanotte");
                }
            }
        }

        if (assignment.getTeams() != null && !assignment.getTeams().isEmpty()) {
            desc.append("\nSquadre assegnate: ").append(String.join(", ", assignment.getTeams()));
        }

        if (template != null) {
            desc.append("\nGiorno del ciclo: ").append(cycleDay + 1).append("/").append(template.getCycleDays());
            desc.append("\nSchema: ").append(template.getName());
            if (template.getDescription() != null) {
                desc.append("\n").append(template.getDescription());
            }
        }

        // Add assignment-specific information
        if (assignment.hasSpecialFlags()) {
            desc.append("\n\nDettagli:");
            if (assignment.isOvertime()) {
                desc.append("\n• Straordinario");
            }
            if (assignment.isMandatory()) {
                desc.append("\n• Turno obbligatorio");
            }
            if (assignment.isTemporary()) {
                desc.append("\n• Assegnazione temporanea");
            }
            if (assignment.isModified()) {
                desc.append("\n• Turno modificato");
                if (assignment.getModificationReason() != null) {
                    desc.append(" (").append(assignment.getModificationReason()).append(")");
                }
            }
        }

        // Add assignment notes if present
        if (assignment.getAssignmentNotes() != null && !assignment.getAssignmentNotes().trim().isEmpty()) {
            desc.append("\n\nNote: ").append(assignment.getAssignmentNotes());
        }

        return desc.toString();
    }

    /**
     * Creates a sample custom template for testing or demonstration purposes.
     *
     * @param templateName Name for the template
     * @param cycleDays Number of days in the cycle
     * @return Sample WorkScheduleTemplate
     */
    public static WorkScheduleTemplate createSampleCustomTemplate(String templateName, int cycleDays) {
        WorkScheduleTemplate template = new WorkScheduleTemplate(
                templateName,
                "Template custom di esempio con " + cycleDays + " giorni di ciclo",
                cycleDays,
                new ArrayList<>()
        );

        template.setUserDefined(true);
        template.setRequiresTeamAssignment(true);
        template.setMinTeamsPerShift(1);
        template.setMaxTeamsPerShift(3);

        // Add some default supported teams
        List<String> supportedTeams = new ArrayList<>();
        supportedTeams.add("A");
        supportedTeams.add("B");
        supportedTeams.add("C");
        template.setSupportedTeams(supportedTeams);

        return template;
    }
}