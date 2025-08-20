package net.calvuz.qdue.diagnosis;

import static net.calvuz.qdue.core.common.Lib.isDebugBuild;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.core.di.impl.ServiceProviderImpl;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.quattrodue.Costants;

import java.time.LocalDate;

/**
 * QuattroDuePatternVerification - Verify QuattroDue 4-2 Pattern Logic
 *
 * <p>Specifically tests the QuattroDue pattern calculation to verify if
 * the day-in-cycle logic correctly determines working days for Team A.</p>
 */
public class QuattroDuePatternVerification {

    private static final String TAG = "QuattroDuePatternVerify";

    /**
     * Add debug menu option to run diagnostics (for debug builds)
     */
    public static void addDebugMenuOption(Activity activity, android.view.Menu menu) {
        if (isDebugBuild(activity)) {
            menu.add(0, 22222, 0, "🔧 QuattroDue Pattern")
                    .setOnMenuItemClickListener(item -> {
                        new Thread(() -> {
                            String results = QuattroDuePatternVerification.verifyQuattroDuePattern(
                                    activity.getApplicationContext());
                            android.util.Log.v("QUATTRODUE_PATTERN_VERIFY", results);
                        }).start();
                        return true;
                    });
        }
    }

    /**
     * Comprehensive verification of QuattroDue pattern logic
     */
    public static String verifyQuattroDuePattern(Context context) {
        StringBuilder report = new StringBuilder();

        report.append("=== QUATTRODUE PATTERN VERIFICATION ===\n");
        report.append("Testing QuattroDue 4-2 pattern for Team A\n\n");

        try {
            ServiceProvider serviceProvider = new ServiceProviderImpl(context);
            WorkScheduleRepository repository = serviceProvider.getWorkScheduleService();

            // Get Team A
            Team teamA = repository.getTeamForUser(1L).join().getData();
            if (teamA == null) {
                report.append("❌ Team A not found!\n");
                return report.toString();
            }

            report.append("Team A: ").append(teamA.getName()).append(" (").append(teamA.getId()).append(")\n\n");

            // Test the complete 18-day cycle
            report.append("COMPLETE 18-DAY CYCLE ANALYSIS:\n");
            report.append("Day | Date       | Cycle | Should Work? | Actual Logic | Match?\n");
            report.append("----|------------|-------|--------------|--------------|-------\n");

            LocalDate baseDate = LocalDate.of(2025, 8, 17); // Start from a known date

            // Get the QD_SCHEME to determine the correct pattern
            char[][][] scheme = Costants.QD_SCHEME;

            for (int day = 0; day < 18; day++) {
                LocalDate testDate = baseDate.plusDays(day);

                try {
                    // Get day in cycle from repository
                    int dayInCycle = repository.getDayInCycle(testDate).join().getData();

                    // Get working status from repository
                    boolean repositoryWorking = repository.isWorkingDayForTeam(testDate, teamA).join().getData();

                    // Calculate expected working status from QD_SCHEME
                    boolean expectedWorking = isTeamAWorkingInScheme(dayInCycle, scheme);

                    // Check if they match
                    boolean matches = repositoryWorking == expectedWorking;
                    String matchStatus = matches ? "✅" : "❌";

                    report.append(String.format("%3d | %s | %5d | %-12s | %-12s | %s\n",
                            day, testDate, dayInCycle,
                            expectedWorking ? "YES" : "NO",
                            repositoryWorking ? "YES" : "NO",
                            matchStatus));

                    // Flag the specific problematic day
                    if (dayInCycle == 10) {
                        report.append("    >>> THIS IS OUR PROBLEM DATE <<<\n");
                        if (!matches) {
                            report.append("    🎯 MISMATCH CONFIRMED: Expected=")
                                    .append(expectedWorking).append(", Actual=")
                                    .append(repositoryWorking).append("\n");
                        }
                    }

                } catch (Exception e) {
                    report.append(String.format("%3d | %s | ERROR | %s\n", day, testDate, e.getMessage()));
                }
            }

            // Analyze the pattern
            report.append("\nPATTERN ANALYSIS:\n");
            report.append("Expected QuattroDue 4-2 pattern for Team A:\n");

            StringBuilder expectedPattern = new StringBuilder();
            StringBuilder actualPattern = new StringBuilder();

            int expectedWorkDays = 0;
            int actualWorkDays = 0;

            for (int day = 0; day < 18; day++) {
                LocalDate testDate = baseDate.plusDays(day);
                int dayInCycle = repository.getDayInCycle(testDate).join().getData();

                boolean expectedWorking = isTeamAWorkingInScheme(dayInCycle, scheme);
                boolean actualWorking = repository.isWorkingDayForTeam(testDate, teamA).join().getData();

                expectedPattern.append(expectedWorking ? "W" : "R");
                actualPattern.append(actualWorking ? "W" : "R");

                if (expectedWorking) expectedWorkDays++;
                if (actualWorking) actualWorkDays++;
            }

            report.append("Expected: ").append(expectedPattern.toString()).append(" (").append(expectedWorkDays).append(" work days)\n");
            report.append("Actual  : ").append(actualPattern.toString()).append(" (").append(actualWorkDays).append(" work days)\n");

            if (expectedPattern.toString().equals(actualPattern.toString())) {
                report.append("✅ Patterns match perfectly!\n");
            } else {
                report.append("❌ PATTERN MISMATCH - This is the bug!\n");

                // Find specific mismatches
                for (int i = 0; i < 18; i++) {
                    if (expectedPattern.charAt(i) != actualPattern.charAt(i)) {
                        report.append("  Day ").append(i).append(": Expected '")
                                .append(expectedPattern.charAt(i)).append("', Got '")
                                .append(actualPattern.charAt(i)).append("'\n");
                    }
                }
            }

            // Test specific problem date analysis
            report.append("\nSPECIFIC PROBLEM ANALYSIS (Day 10):\n");
            LocalDate problemDate = baseDate.plusDays(10);
            int problemDayInCycle = repository.getDayInCycle(problemDate).join().getData();

            report.append("Problem date: ").append(problemDate).append("\n");
            report.append("Day in cycle: ").append(problemDayInCycle).append("\n");

            // Check QD_SCHEME for this specific day
            if (problemDayInCycle < scheme.length) {
                char[][] dayScheme = scheme[problemDayInCycle];
                boolean teamAInScheme = false;

                for (char[] teams : dayScheme) {
                    for (char team : teams) {
                        if (team == 'A') {
                            teamAInScheme = true;
                            break;
                        }
                    }
                    if (teamAInScheme) break;
                }

                boolean actualWorking = repository.isWorkingDayForTeam(problemDate, teamA).join().getData();

                report.append("QD_SCHEME says Team A working: ").append(teamAInScheme).append("\n");
                report.append("Repository says Team A working: ").append(actualWorking).append("\n");

                if (teamAInScheme != actualWorking) {
                    report.append("🎯 ROOT CAUSE: Repository logic doesn't match QD_SCHEME!\n");

                    // Show the scheme details for this day
                    report.append("\nQD_SCHEME details for day ").append(problemDayInCycle).append(":\n");
                    for (int shift = 0; shift < dayScheme.length; shift++) {
                        report.append("  Shift ").append(shift).append(": ");
                        for (char team : dayScheme[shift]) {
                            report.append(team).append(" ");
                        }
                        report.append("\n");
                    }
                } else {
                    report.append("✅ Repository logic matches QD_SCHEME for this day\n");
                }
            }

        } catch (Exception e) {
            report.append("❌ Pattern verification failed: ").append(e.getMessage()).append("\n");
        }

        report.append("\n=== END QUATTRODUE PATTERN VERIFICATION ===\n");

        String finalReport = report.toString();
        Log.i(TAG, finalReport);

        return finalReport;
    }

    /**
     * Check if Team A is working in the QD_SCHEME for a specific day
     */
    private static boolean isTeamAWorkingInScheme(int dayInCycle, char[][][] scheme) {
        try {
            if (dayInCycle < 0 || dayInCycle >= scheme.length) {
                return false;
            }

            char[][] dayScheme = scheme[dayInCycle];

            for (char[] teams : dayScheme) {
                for (char team : teams) {
                    if (team == 'A') {
                        return true;
                    }
                }
            }

            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error checking Team A in scheme for day " + dayInCycle, e);
            return false;
        }
    }

    /**
     * Quick verification of the problematic date only
     */
    public static String quickVerifyProblemDate(Context context) {
        StringBuilder report = new StringBuilder();

        report.append("=== QUICK PROBLEM DATE VERIFICATION ===\n");

        try {
            ServiceProvider serviceProvider = new ServiceProviderImpl(context);
            WorkScheduleRepository repository = serviceProvider.getWorkScheduleService();

            LocalDate problemDate = LocalDate.of(2025, 8, 18); // The date from our test
            Team teamA = repository.getTeamForUser(1L).join().getData();

            int dayInCycle = repository.getDayInCycle(problemDate).join().getData();
            boolean repositoryWorking = repository.isWorkingDayForTeam(problemDate, teamA).join().getData();

            char[][][] scheme = Costants.QD_SCHEME;
            boolean schemeWorking = isTeamAWorkingInScheme(dayInCycle, scheme);

            report.append("Date: ").append(problemDate).append("\n");
            report.append("Day in cycle: ").append(dayInCycle).append("\n");
            report.append("QD_SCHEME says working: ").append(schemeWorking).append("\n");
            report.append("Repository says working: ").append(repositoryWorking).append("\n");

            if (schemeWorking == repositoryWorking) {
                report.append("✅ Logic is consistent\n");
            } else {
                report.append("❌ INCONSISTENCY CONFIRMED!\n");
                report.append("🔧 Fix needed in isWorkingDayForTeam() logic\n");
            }

        } catch (Exception e) {
            report.append("❌ Quick verification failed: ").append(e.getMessage()).append("\n");
        }

        report.append("=== END QUICK VERIFICATION ===\n");

        return report.toString();
    }
}