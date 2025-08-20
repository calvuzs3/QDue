package net.calvuz.qdue.diagnosis;

import static net.calvuz.qdue.core.common.Lib.isDebugBuild;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.core.di.impl.ServiceProviderImpl;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.domain.calendar.usecases.GenerateUserScheduleUseCase;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

/**
 * PatternAnalysisTest - Deep Dive into QuattroDue Pattern Logic
 *
 * <p>Analyzes the specific QuattroDue 4-2 pattern to understand why some dates
 * generate shifts while others don't.</p>
 */
public class PatternAnalysisTest {

    private static final String TAG = "PatternAnalysis";

    /**
     * Add debug menu option to run diagnostics (for debug builds)
     */
    public static void addDebugMenuOption(Activity activity, android.view.Menu menu) {
        // Only add in debug builds
        if (isDebugBuild(activity)) {
            menu.add(0, 55555, 0, "🔧 Pattern Analysis")
                    .setOnMenuItemClickListener(item -> {
                        // Test completo del pattern
                        new Thread(() -> {
                            String results = PatternAnalysisTest.runAllPatternTests(activity.getApplicationContext());
                            android.util.Log.v("PATTERN", results);
                        }).start();
                        return true;
                    });
        }
    }


    /**
     * Analyze QuattroDue pattern for User 1 (Team A) over next 30 days
     */
    public static String analyzeQuattroDuePattern(Context context) {
        StringBuilder report = new StringBuilder();

        report.append("=== QUATTRODUE PATTERN ANALYSIS ===\n");
        report.append("User: 1 (Team A)\n");
        report.append("Analysis Period: Next 30 days\n\n");

        try {
            ServiceProvider serviceProvider = new ServiceProviderImpl(context);
            WorkScheduleRepository repository = serviceProvider.getWorkScheduleService();

            LocalDate startDate = LocalDate.now();

            // Analyze each day for the next 30 days
            report.append("Day-by-Day Analysis:\n");
            report.append("Date       | Cycle | Shifts | Teams | Pattern\n");
            report.append("-----------|-------|--------|-------|--------\n");

            int workDays = 0;
            int restDays = 0;

            for (int i = 0; i < 30; i++) {
                LocalDate testDate = startDate.plusDays(i);

                try {
                    // Get day in cycle
                    int dayInCycle = repository.getDayInCycle(testDate).join().getData();

                    // Get schedule for this date
                    WorkScheduleDay schedule = repository.getWorkScheduleForDate(testDate, 1L).join().getData();

                    int shiftCount = schedule != null ? schedule.getShifts().size() : 0;
                    String pattern = shiftCount > 0 ? "WORK" : "REST";

                    if (shiftCount > 0) {
                        workDays++;

                        // Get team info
                        StringBuilder teamInfo = new StringBuilder();
                        if (schedule != null) {
                            for (var shift : schedule.getShifts()) {
                                if (teamInfo.length() > 0) teamInfo.append(",");
                                teamInfo.append(shift.getTeams().size()).append("t");
                            }
                        }

                        report.append(String.format("%-10s | %2d    | %2d     | %-5s | %s\n",
                                testDate, dayInCycle, shiftCount, teamInfo.toString(), pattern));
                    } else {
                        restDays++;
                        report.append(String.format("%-10s | %2d    | %2d     | %-5s | %s\n",
                                testDate, dayInCycle, shiftCount, "-", pattern));
                    }

                } catch (Exception e) {
                    report.append(String.format("%-10s | ERROR | %s\n", testDate, e.getMessage()));
                }
            }

            report.append("\nPattern Summary:\n");
            report.append("Work Days: ").append(workDays).append("\n");
            report.append("Rest Days: ").append(restDays).append("\n");
            report.append("Work/Rest Ratio: ").append(workDays).append("/").append(restDays).append("\n");

            // Analyze QuattroDue 4-2 pattern
            if (workDays > 0 && restDays > 0) {
                double workRatio = (double) workDays / (workDays + restDays);
                report.append("Work Percentage: ").append(String.format("%.1f", workRatio * 100)).append("%\n");

                if (workRatio >= 0.6 && workRatio <= 0.7) {
                    report.append("✅ Ratio consistent with QuattroDue 4-2 pattern (≈66.7%)\n");
                } else {
                    report.append("⚠️ Ratio NOT consistent with QuattroDue 4-2 pattern\n");
                }
            } else if (workDays == 0) {
                report.append("❌ CRITICAL: No work days found in 30-day period!\n");
            } else if (restDays == 0) {
                report.append("⚠️ WARNING: No rest days found in 30-day period!\n");
            }

        } catch (Exception e) {
            report.append("❌ Pattern analysis failed: ").append(e.getMessage()).append("\n");
        }

        report.append("=== END PATTERN ANALYSIS ===\n");

        String finalReport = report.toString();
        Log.i(TAG, finalReport);

        return finalReport;
    }

    /**
     * Compare Repository vs UseCase for the same data
     */
    public static String compareRepositoryVsUseCase(Context context) {
        StringBuilder report = new StringBuilder();

        report.append("=== REPOSITORY vs USECASE COMPARISON ===\n");

        try {
            ServiceProvider serviceProvider = new ServiceProviderImpl(context);
            WorkScheduleRepository repository = serviceProvider.getWorkScheduleService();

            // Get UseCase if available
            GenerateUserScheduleUseCase useCase = null;
            try {
                useCase = serviceProvider.getCalendarServiceProvider().getUseCaseFactory().getUserScheduleUseCase();
            } catch (Exception e) {
                report.append("⚠️ UseCase not available: ").append(e.getMessage()).append("\n");
            }

            LocalDate testDate = LocalDate.now();
            YearMonth testMonth = YearMonth.from(testDate);

            // Test single date
            report.append("SINGLE DATE TEST (").append(testDate).append("):\n");

            try {
                WorkScheduleDay repoSchedule = repository.getWorkScheduleForDate(testDate, 1L).join().getData();
                int repoShifts = repoSchedule != null ? repoSchedule.getShifts().size() : 0;
                report.append("Repository: ").append(repoShifts).append(" shifts\n");

                if (useCase != null) {
                    WorkScheduleDay useCaseSchedule = useCase.executeForDate(1L, testDate).join().getData();
                    int useCaseShifts = useCaseSchedule != null ? useCaseSchedule.getShifts().size() : 0;
                    report.append("UseCase: ").append(useCaseShifts).append(" shifts\n");

                    if (repoShifts == useCaseShifts) {
                        report.append("✅ Results match\n");
                    } else {
                        report.append("❌ Results differ! Repository: ").append(repoShifts)
                                .append(", UseCase: ").append(useCaseShifts).append("\n");
                    }
                }
            } catch (Exception e) {
                report.append("❌ Single date test failed: ").append(e.getMessage()).append("\n");
            }

            report.append("\nMONTH TEST (").append(testMonth).append("):\n");

            try {
                Map<LocalDate, WorkScheduleDay> repoMonth = repository.getWorkScheduleForMonth(testMonth, 1L).join().getData();
                int repoDaysWithShifts = 0;
                int repoTotalShifts = 0;

                if (repoMonth != null) {
                    for (WorkScheduleDay day : repoMonth.values()) {
                        if (!day.getShifts().isEmpty()) {
                            repoDaysWithShifts++;
                            repoTotalShifts += day.getShifts().size();
                        }
                    }
                }

                report.append("Repository: ").append(repoDaysWithShifts).append(" days with shifts, ")
                        .append(repoTotalShifts).append(" total shifts\n");

                if (useCase != null) {
                    Map<LocalDate, WorkScheduleDay> useCaseMonth = useCase.executeForMonth(1L, testMonth).join().getData();
                    int useCaseDaysWithShifts = 0;
                    int useCaseTotalShifts = 0;

                    if (useCaseMonth != null) {
                        for (WorkScheduleDay day : useCaseMonth.values()) {
                            if (!day.getShifts().isEmpty()) {
                                useCaseDaysWithShifts++;
                                useCaseTotalShifts += day.getShifts().size();
                            }
                        }
                    }

                    report.append("UseCase: ").append(useCaseDaysWithShifts).append(" days with shifts, ")
                            .append(useCaseTotalShifts).append(" total shifts\n");

                    if (repoDaysWithShifts == useCaseDaysWithShifts && repoTotalShifts == useCaseTotalShifts) {
                        report.append("✅ Month results match\n");
                    } else {
                        report.append("❌ Month results differ!\n");
                    }
                }

                // Check if specific test date is in month results
                if (repoMonth != null && repoMonth.containsKey(testDate)) {
                    WorkScheduleDay monthDaySchedule = repoMonth.get(testDate);
                    int monthDayShifts = monthDaySchedule != null ? monthDaySchedule.getShifts().size() : 0;
                    report.append("\nSpecific date in month data: ").append(monthDayShifts).append(" shifts\n");

                    // Compare with single date call
                    WorkScheduleDay singleDaySchedule = repository.getWorkScheduleForDate(testDate, 1L).join().getData();
                    int singleDayShifts = singleDaySchedule != null ? singleDaySchedule.getShifts().size() : 0;

                    if (monthDayShifts == singleDayShifts) {
                        report.append("✅ Single date call matches month data\n");
                    } else {
                        report.append("❌ INCONSISTENCY: Single date (").append(singleDayShifts)
                                .append(") vs Month data (").append(monthDayShifts).append(")\n");
                    }
                }

            } catch (Exception e) {
                report.append("❌ Month test failed: ").append(e.getMessage()).append("\n");
            }

        } catch (Exception e) {
            report.append("❌ Comparison failed: ").append(e.getMessage()).append("\n");
        }

        report.append("=== END COMPARISON ===\n");

        String finalReport = report.toString();
        Log.i(TAG + "_COMPARE", finalReport);

        return finalReport;
    }

    /**
     * Test multiple consecutive dates to find the pattern
     */
    public static String findWorkRestPattern(Context context) {
        StringBuilder report = new StringBuilder();

        report.append("=== WORK/REST PATTERN FINDER ===\n");

        try {
            ServiceProvider serviceProvider = new ServiceProviderImpl(context);
            WorkScheduleRepository repository = serviceProvider.getWorkScheduleService();

            LocalDate startDate = LocalDate.now().minusDays(5); // Start 5 days ago

            report.append("Looking for QuattroDue 4-2 pattern (4 work, 2 rest):\n");
            report.append("Date       | Cycle | Pattern | Note\n");
            report.append("-----------|-------|---------|-----\n");

            String currentPattern = "";
            int consecutiveWork = 0;
            int consecutiveRest = 0;

            for (int i = 0; i < 20; i++) { // Check 20 days
                LocalDate testDate = startDate.plusDays(i);

                try {
                    int dayInCycle = repository.getDayInCycle(testDate).join().getData();
                    WorkScheduleDay schedule = repository.getWorkScheduleForDate(testDate, 1L).join().getData();

                    boolean isWork = schedule != null && !schedule.getShifts().isEmpty();
                    String dayPattern = isWork ? "W" : "R";

                    // Track consecutive patterns
                    if (isWork) {
                        if (consecutiveRest > 0) {
                            // Transition from rest to work
                            consecutiveWork = 1;
                            consecutiveRest = 0;
                        } else {
                            consecutiveWork++;
                        }
                    } else {
                        if (consecutiveWork > 0) {
                            // Transition from work to rest
                            consecutiveRest = 1;
                            consecutiveWork = 0;
                        } else {
                            consecutiveRest++;
                        }
                    }

                    currentPattern += dayPattern;
                    if (currentPattern.length() > 10) {
                        currentPattern = currentPattern.substring(1); // Keep last 10 chars
                    }

                    String note = "";
                    if (isWork && consecutiveWork == 1) note = "Work starts";
                    if (!isWork && consecutiveRest == 1) note = "Rest starts";
                    if (isWork && consecutiveWork == 4) note = "4th work day";
                    if (!isWork && consecutiveRest == 2) note = "2nd rest day";

                    report.append(String.format("%-10s | %2d    | %-7s | %s\n",
                            testDate, dayInCycle, dayPattern + "(" +
                                    (isWork ? consecutiveWork : consecutiveRest) + ")", note));

                } catch (Exception e) {
                    report.append(String.format("%-10s | ERROR | %s\n", testDate, e.getMessage()));
                }
            }

            report.append("\nPattern Sequence: ").append(currentPattern).append("\n");

            // Analyze pattern
            if (currentPattern.contains("WWWWRR") || currentPattern.contains("RRWWWW")) {
                report.append("✅ QuattroDue 4-2 pattern detected!\n");
            } else if (currentPattern.contains("WWWW") && currentPattern.contains("RR")) {
                report.append("⚠️ Partial QuattroDue pattern detected\n");
            } else {
                report.append("❌ QuattroDue 4-2 pattern NOT detected\n");
            }

        } catch (Exception e) {
            report.append("❌ Pattern finder failed: ").append(e.getMessage()).append("\n");
        }

        report.append("=== END PATTERN FINDER ===\n");

        String finalReport = report.toString();
        Log.i(TAG + "_PATTERN", finalReport);

        return finalReport;
    }

    /**
     * Run all pattern analysis tests
     */
    public static String runAllPatternTests(Context context) {
        StringBuilder fullReport = new StringBuilder();

        fullReport.append("=== COMPREHENSIVE PATTERN ANALYSIS ===\n\n");

        fullReport.append(analyzeQuattroDuePattern(context)).append("\n");
        fullReport.append(compareRepositoryVsUseCase(context)).append("\n");
        fullReport.append(findWorkRestPattern(context)).append("\n");

        fullReport.append("=== ALL PATTERN TESTS COMPLETE ===\n");

        String finalReport = fullReport.toString();
        Log.i(TAG + "_ALL", finalReport);

        return finalReport;
    }
}