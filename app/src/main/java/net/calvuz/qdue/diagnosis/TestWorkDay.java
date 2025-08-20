package net.calvuz.qdue.diagnosis;

import static net.calvuz.qdue.core.common.Lib.isDebugBuild;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.core.di.impl.ServiceProviderImpl;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleShift;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;

import java.time.LocalDate;

/**
 * TestWorkDay - Test a work day after RecurrenceRule fix
 */
public class TestWorkDay {

    private static final String TAG = "TestWorkDay";

    /**
     * Add debug menu option to test work day (for debug builds)
     */
    public static void addDebugMenuOption(Activity activity, android.view.Menu menu) {
        if (isDebugBuild(activity)) {
            menu.add(0, 44444, 0, "🔧 Test Work Day")
                    .setOnMenuItemClickListener(item -> {
                        new Thread(() -> {
                            String results = TestWorkDay.testWorkDayAfterFix(
                                    activity.getApplicationContext());
                            android.util.Log.v("TEST_WORK_DAY", results);
                        }).start();
                        return true;
                    });
        }
    }

    /**
     * Test work day generation after RecurrenceRule fix
     */
    public static String testWorkDayAfterFix(Context context) {
        StringBuilder report = new StringBuilder();

        report.append("=== WORK DAY TEST AFTER RECURRENCERULE FIX ===\n");

        try {
            ServiceProvider serviceProvider = new ServiceProviderImpl(context);
            WorkScheduleRepository repository = serviceProvider.getWorkScheduleService();

            // Test multiple dates to find work days
            LocalDate baseDate = LocalDate.of(2025, 8, 17);

            report.append("Testing next 10 days to find work days:\n");
            report.append("Date       | Cycle | RecRule | Working | Shifts | Teams | Status\n");
            report.append("-----------|-------|---------|---------|--------|-------|--------\n");

            for (int i = 0; i < 10; i++) {
                LocalDate testDate = baseDate.plusDays(i);

                try {
                    // Get cycle position
                    int dayInCycle = repository.getDayInCycle(testDate).join().getData();

                    // Check actual RecurrenceRule.appliesTo() result
                    // We need to simulate this by checking if any assignment's rule applies
                    boolean recRuleApplies = false;
                    try {
                        // This would test if the RecurrenceRule thinks this date should generate shifts
                        WorkScheduleDay schedule = repository.getWorkScheduleForDate(testDate, 1L).join().getData();
                        // If we got shifts generated, then RecurrenceRule.appliesTo() returned true
                        recRuleApplies = schedule != null && !schedule.getShifts().isEmpty();
                    } catch (Exception e) {
                        recRuleApplies = false;
                    }

                    // Check actual isWorkingDayForTeam() result
                    boolean repositoryWorking = false;
                    try {
                        var teamA = repository.getTeamForUser(1L).join().getData();
                        if (teamA != null) {
                            repositoryWorking = repository.isWorkingDayForTeam(testDate, teamA).join().getData();
                        }
                    } catch (Exception e) {
                        repositoryWorking = false;
                    }

                    // Check what SHOULD work (using our fixed pattern)
                    boolean shouldWork = isWorkDayInQuattroDuePattern(dayInCycle);

                    // Get actual schedule result
                    WorkScheduleDay schedule = repository.getWorkScheduleForDate(testDate, 1L).join().getData();

                    int shiftCount = schedule != null ? schedule.getShifts().size() : 0;
                    int teamCount = 0;

                    if (schedule != null) {
                        for (WorkScheduleShift shift : schedule.getShifts()) {
                            teamCount += shift.getTeams().size();
                        }
                    }

                    String status;
                    if (shouldWork && shiftCount > 0 && teamCount > 0) {
                        status = "✅ PERFECT";
                    } else if (shouldWork && shiftCount > 0 && teamCount == 0) {
                        status = "❌ NO TEAMS";
                    } else if (shouldWork && shiftCount == 0) {
                        status = "❌ NO SHIFTS";
                    } else if (!shouldWork && shiftCount == 0) {
                        status = "✅ REST OK";
                    } else if (!shouldWork && shiftCount > 0) {
                        status = "⚠️ EXTRA SHIFTS";
                    } else {
                        status = "⚠️ UNEXPECTED";
                    }

                    report.append(String.format("%-10s | %5d | %-7s | %-7s | %6d | %5d | %s\n",
                            testDate, dayInCycle, recRuleApplies, repositoryWorking, shiftCount, teamCount, status));

                    // Detail the first work day found
                    if (shouldWork && shiftCount > 0) {
                        report.append("  >>> WORK DAY DETAILS:\n");
                        for (int j = 0; j < schedule.getShifts().size(); j++) {
                            WorkScheduleShift shift = schedule.getShifts().get(j);
                            report.append("    Shift ").append(j + 1).append(": ");

                            if (shift.getShift() != null) {
                                report.append(shift.getShift().getName()).append(" ");
                            }

                            report.append("(").append(shift.getTeams().size()).append(" teams");

                            if (shift.getTeams().isEmpty()) {
                                report.append(" - ❌ MISSING TEAMS!");
                            } else {
                                report.append(" - ✅");
                                for (var team : shift.getTeams()) {
                                    report.append(" ").append(team.getName());
                                }
                            }
                            report.append(")\n");
                        }
                    }

                } catch (Exception e) {
                    report.append(String.format("%-10s | ERROR | %s\n", testDate, e.getMessage()));
                }
            }

            // Summary analysis
            report.append("\nANALYSIS:\n");
            report.append("Looking for the pattern:\n");
            report.append("- Work days should have shifts WITH teams (✅ PERFECT)\n");
            report.append("- Rest days should have no shifts (✅ REST OK)\n");
            report.append("- If work days have shifts but no teams (❌ NO TEAMS) → Need RecurrenceCalculator fix\n");
            report.append("- If work days have no shifts (❌ NO SHIFTS) → RecurrenceRule still has issues\n");

        } catch (Exception e) {
            report.append("❌ Work day test failed: ").append(e.getMessage()).append("\n");
        }

        report.append("\n=== END WORK DAY TEST ===\n");

        String finalReport = report.toString();
        Log.i(TAG, finalReport);

        return finalReport;
    }

    /**
     * Determine if a cycle day should be a work day in QuattroDue pattern
     */
    private static boolean isWorkDayInQuattroDuePattern(int cyclePosition) {
        // Convert 0-based to 1-based for our pattern
        int dayInCycle = cyclePosition + 1;

        // QuattroDue 4-2 pattern: work days 1-4, 7-10, 13-16
        return (dayInCycle >= 1 && dayInCycle <= 4) ||   // Work period 1
                (dayInCycle >= 7 && dayInCycle <= 10) ||  // Work period 2
                (dayInCycle >= 13 && dayInCycle <= 16);   // Work period 3
    }
}