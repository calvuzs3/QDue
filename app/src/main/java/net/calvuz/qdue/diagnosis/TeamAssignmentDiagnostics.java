package net.calvuz.qdue.diagnosis;

import static net.calvuz.qdue.core.common.Lib.isDebugBuild;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.core.di.impl.ServiceProviderImpl;
import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleShift;
import net.calvuz.qdue.domain.calendar.repositories.RecurrenceRuleRepository;
import net.calvuz.qdue.domain.calendar.repositories.UserScheduleAssignmentRepository;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;

import java.time.LocalDate;

/**
 * TeamAssignmentDiagnostics - Deep Dive into Team Assignment Logic
 *
 * <p>Specifically diagnoses why teams are not being assigned to generated shifts.
 * Tests the complete chain: UserAssignment → RecurrenceRule → Team Assignment.</p>
 */
public class TeamAssignmentDiagnostics {

    private static final String TAG = "TeamAssignmentDx";

    /**
     * Add debug menu option to run diagnostics (for debug builds)
     */
    public static void addDebugMenuOption(Activity activity, android.view.Menu menu) {
        if (isDebugBuild(activity)) {
            menu.add(0, 11111, 0, "🔧 Team Assignment")
                    .setOnMenuItemClickListener(item -> {
                        new Thread(() -> {
                            String results = TeamAssignmentDiagnostics.diagnoseTeamAssignmentChain(
                                    activity.getApplicationContext());
                            android.util.Log.v("TEAM_ASSIGNMENT_DX", results);
                        }).start();
                        return true;
                    });
        }
    }

    /**
     * Comprehensive diagnosis of team assignment chain
     */
    public static String diagnoseTeamAssignmentChain(Context context) {
        StringBuilder report = new StringBuilder();

        report.append("=== TEAM ASSIGNMENT CHAIN DIAGNOSIS ===\n");
        report.append("Investigating why teams are not assigned to shifts\n\n");

        try {
            ServiceProvider serviceProvider = new ServiceProviderImpl(context);
            WorkScheduleRepository workRepo = serviceProvider.getWorkScheduleService();

            Long testUserId = 1L;
            LocalDate testDate = LocalDate.now().plusDays(1); // Tomorrow

            report.append("Test Parameters:\n");
            report.append("  User ID: ").append(testUserId).append("\n");
            report.append("  Date: ").append(testDate).append("\n\n");

            // Step 1: Check User Assignment
            report.append("STEP 1: User Assignment Analysis\n");
            report.append("------------------------------------\n");

            UserScheduleAssignmentRepository assignmentRepo =
                    serviceProvider.getCalendarServiceProvider().getUserScheduleAssignmentRepository();

            UserScheduleAssignment assignment = assignmentRepo
                    .getActiveAssignmentForUser(testUserId, testDate).join().getData();

            if (assignment != null) {
                report.append("✅ Found active assignment:\n");
                report.append("  Assignment ID: ").append(assignment.getId()).append("\n");
                report.append("  Team ID: ").append(assignment.getTeamId()).append("\n");
                report.append("  Recurrence Rule ID: ").append(assignment.getRecurrenceRuleId()).append("\n");
                report.append("  Priority: ").append(assignment.getPriority()).append("\n");
                report.append("  Period: ").append(assignment.getStartDate())
                        .append(" to ").append(assignment.getEndDate()).append("\n");
            } else {
                report.append("❌ No active assignment found for user\n");
                report.append("  This could be why no teams are assigned!\n");
                return report.toString();
            }

            // Step 2: Check RecurrenceRule
            report.append("\nSTEP 2: RecurrenceRule Analysis\n");
            report.append("----------------------------------\n");

            RecurrenceRuleRepository ruleRepo =
                    serviceProvider.getCalendarServiceProvider().getRecurrenceRuleRepository();

            RecurrenceRule rule = ruleRepo.getRecurrenceRuleById(assignment.getRecurrenceRuleId()).join();

            if (rule != null) {
                report.append("✅ Found recurrence rule:\n");
                report.append("  Rule ID: ").append(rule.getId()).append("\n");
                report.append("  Frequency: ").append(rule.getFrequency()).append("\n");
                report.append("  Interval: ").append(rule.getInterval()).append("\n");
                report.append("  Cycle Length: ").append(rule.getCycleLength()).append("\n");
                report.append("  Work Days: ").append(rule.getWorkDays()).append("\n");

                // Test if rule applies to test date
                boolean appliesTo = rule.appliesTo(testDate);
                report.append("  Applies to ").append(testDate).append(": ").append(appliesTo).append("\n");

                if (!appliesTo) {
                    report.append("❌ Rule does not apply to test date - this explains empty schedule!\n");
                }
            } else {
                report.append("❌ RecurrenceRule not found: ").append(assignment.getRecurrenceRuleId()).append("\n");
                return report.toString();
            }

            // Step 3: Check if rule applies to test date
            report.append("\nSTEP 3: RecurrenceRule Application Test\n");
            report.append("---------------------------------------\n");

            boolean appliesTo = rule.appliesTo(testDate);
            report.append("Rule applies to ").append(testDate).append(": ").append(appliesTo).append("\n");

            if (!appliesTo) {
                report.append("❌ Rule does not apply to test date!\n");
                report.append("🔍 This could explain why no shifts are generated\n");
            } else {
                report.append("✅ Rule applies - shifts should be generated\n");
            }

            // Step 4: Direct Repository Chain Test
            report.append("\nSTEP 4: Repository Chain Test\n");
            report.append("------------------------------\n");

            try {
                // Test the exact same call that's failing in the main test
                WorkScheduleDay finalSchedule = workRepo.getWorkScheduleForDate(testDate, testUserId).join().getData();

                if (finalSchedule != null) {
                    report.append("✅ WorkScheduleRepository returned schedule:\n");
                    report.append("  Date: ").append(finalSchedule.getDate()).append("\n");
                    report.append("  Shifts count: ").append(finalSchedule.getShifts().size()).append("\n");

                    for (int i = 0; i < finalSchedule.getShifts().size(); i++) {
                        WorkScheduleShift shift = finalSchedule.getShifts().get(i);
                        report.append("  Shift ").append(i + 1).append(":\n");

                        if (shift.getShift() != null) {
                            report.append("    Shift Name: ").append(shift.getShift().getName()).append("\n");
                        } else {
                            report.append("    ❌ Shift is NULL!\n");
                        }

                        report.append("    Teams: ").append(shift.getTeams().size()).append("\n");

                        if (shift.getTeams().isEmpty()) {
                            report.append("    ❌ NO TEAMS ASSIGNED!\n");
                            report.append("    🎯 ROOT CAUSE: Teams not assigned during generation\n");
                        } else {
                            for (Team team : shift.getTeams()) {
                                report.append("      Team: ").append(team.getName()).append(" (").append(team.getId()).append(")\n");
                            }
                        }
                    }
                } else {
                    report.append("❌ WorkScheduleRepository returned null\n");
                }

            } catch (Exception e) {
                report.append("❌ Repository chain test failed: ").append(e.getMessage()).append("\n");
            }

            // Step 5: QuattroDue Pattern Analysis
            report.append("\nSTEP 5: QuattroDue Pattern Verification\n");
            report.append("----------------------------------------\n");

            if (rule.getFrequency() == RecurrenceRule.Frequency.QUATTRODUE_CYCLE) {
                report.append("Analyzing QuattroDue pattern logic...\n");

                // Check cycle position
                int dayInCycle = workRepo.getDayInCycle(testDate).join().getData();
                report.append("Day in cycle: ").append(dayInCycle).append(" (0-17)\n");

                // Check if team should be working
                Team userTeam = workRepo.getTeamForUser(testUserId).join().getData();
                boolean teamWorking = workRepo.isWorkingDayForTeam(testDate, userTeam).join().getData();
                report.append("Team ").append(userTeam.getName()).append(" should work: ").append(teamWorking).append("\n");

                if (!teamWorking) {
                    report.append("❌ QuattroDue logic says team is NOT working\n");
                    report.append("🔍 This could be a pattern calculation error\n");
                }
            }

        } catch (Exception e) {
            report.append("❌ Team assignment diagnosis failed: ").append(e.getMessage()).append("\n");
        }

        report.append("\n=== END TEAM ASSIGNMENT DIAGNOSIS ===\n");

        String finalReport = report.toString();
        Log.i(TAG, finalReport);

        return finalReport;
    }

    /**
     * Test specific dates for team assignment pattern
     */
    public static String testMultipleDatesTeamAssignment(Context context) {
        StringBuilder report = new StringBuilder();

        report.append("=== MULTIPLE DATES TEAM ASSIGNMENT TEST ===\n");

        try {
            ServiceProvider serviceProvider = new ServiceProviderImpl(context);
            WorkScheduleRepository repository = serviceProvider.getWorkScheduleService();

            Long testUserId = 1L;
            LocalDate startDate = LocalDate.now();

            report.append("Testing team assignment pattern for next 10 days:\n");
            report.append("Date       | Cycle | Teams | Should Work | Pattern Issue?\n");
            report.append("-----------|-------|-------|-------------|---------------\n");

            for (int i = 0; i < 10; i++) {
                LocalDate testDate = startDate.plusDays(i);

                try {
                    int dayInCycle = repository.getDayInCycle(testDate).join().getData();
                    WorkScheduleDay schedule = repository.getWorkScheduleForDate(testDate, testUserId).join().getData();

                    int totalTeams = 0;
                    if (schedule != null) {
                        for (WorkScheduleShift shift : schedule.getShifts()) {
                            totalTeams += shift.getTeams().size();
                        }
                    }

                    Team userTeam = repository.getTeamForUser(testUserId).join().getData();
                    boolean shouldWork = repository.isWorkingDayForTeam(testDate, userTeam).join().getData();

                    String issue = "";
                    if (shouldWork && totalTeams == 0) {
                        issue = "❌ Should work but no teams";
                    } else if (!shouldWork && totalTeams > 0) {
                        issue = "⚠️ Shouldn't work but has teams";
                    } else if (shouldWork && totalTeams > 0) {
                        issue = "✅ Correct";
                    } else {
                        issue = "✅ Rest day";
                    }

                    report.append(String.format("%-10s | %5d | %5d | %-11s | %s\n",
                            testDate, dayInCycle, totalTeams, shouldWork, issue));

                } catch (Exception e) {
                    report.append(String.format("%-10s | ERROR | %s\n", testDate, e.getMessage()));
                }
            }

        } catch (Exception e) {
            report.append("❌ Multiple dates test failed: ").append(e.getMessage()).append("\n");
        }

        report.append("\n=== END MULTIPLE DATES TEST ===\n");

        String finalReport = report.toString();
        Log.i(TAG + "_MULTIPLE", finalReport);

        return finalReport;
    }

    /**
     * Quick team assignment fix test
     */
    public static String quickTeamAssignmentFix(Context context) {
        StringBuilder report = new StringBuilder();
        report.append("=== QUICK TEAM ASSIGNMENT FIX TEST ===\n");

        // Test if we can manually assign teams to verify the hypothesis
        // This would help confirm if the issue is in team assignment logic

        report.append("Testing manual team assignment...\n");
        report.append("(Implementation would require access to internal shift building methods)\n");

        report.append("=== END QUICK FIX TEST ===\n");
        return report.toString();
    }
}