package net.calvuz.qdue.diagnosis;

import static net.calvuz.qdue.core.common.Lib.isDebugBuild;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.core.di.impl.ServiceProviderImpl;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.quattrodue.Costants;
import net.calvuz.qdue.quattrodue.Preferences;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.utils.HalfTeamFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * QuattroDuePatternDiagnostics - Compare Old vs New Pattern Calculation
 *
 * <p>Investigates the day offset issue in QuattroDue pattern calculation
 * by comparing the old QuattroDue system with the new repository system.</p>
 */
public class QuattroDuePatternDiagnostics {

    private static final String TAG = "QuattroDuePatternDx";

    /**
     * Add debug menu option to run diagnostics (for debug builds)
     */
    public static void addDebugMenuOption(Activity activity, android.view.Menu menu) {
        // Only add in debug builds
        if (isDebugBuild(activity)) {
            menu.add(0, 88888, 0, "🔧 Pattern Diagnostics")
                    .setOnMenuItemClickListener(item -> {
                        // Test completo del pattern
                        new Thread(() -> {
                            String results = QuattroDuePatternDiagnostics.runCompletePatternDiagnosis(activity.getApplicationContext());
                            Log.v("PATTERN_DIAGNOSIS", results);
                        }).start();
                        return true;
                    });
        }
    }

    /**
     * Compare old QuattroDue system with new repository system
     */
    public static String compareOldVsNewPattern(Context context) {
        StringBuilder report = new StringBuilder();

        report.append( "=== QUATTRODUE PATTERN COMPARISON ===\n" );
        report.append( "Comparing Old QuattroDue vs New Repository System\n\n" );

        try {
            // Initialize both systems
            QuattroDue oldSystem = QuattroDue.getInstance( context );
            ServiceProvider serviceProvider = new ServiceProviderImpl( context );
            WorkScheduleRepository newRepository = serviceProvider.getWorkScheduleService();

            // Get scheme dates from both systems
            LocalDate oldSchemeDate = oldSystem.getSchemeDate();
            LocalDate newSchemeDate = newRepository.getSchemeStartDate().join().getData();

            report.append( "SCHEME DATES:\n" );
            report.append( "Old System: " ).append( oldSchemeDate ).append( "\n" );
            report.append( "New System: " ).append( newSchemeDate ).append( "\n" );

            if (!oldSchemeDate.equals( newSchemeDate )) {
                report.append( "❌ SCHEME DATE MISMATCH!\n" );
            } else {
                report.append( "✅ Scheme dates match\n" );
            }

            report.append( "\nPATTERN COMPARISON (Team A):\n" );
            report.append( "Date       | Old Cycle | New Cycle | Old Pattern | New Pattern | Match?\n" );
            report.append( "-----------|-----------|-----------|-------------|-------------|-------\n" );

            // Test dates around the problematic period
            LocalDate startDate = LocalDate.of( 2025, 6, 1 );

            for (int i = 0; i < 20; i++) {
                LocalDate testDate = startDate.plusDays( i );

                try {
                    // OLD SYSTEM: Get day in cycle using old QuattroDue logic
                    int oldDayInCycle = oldSystem.getDayInCycle( testDate );

                    // OLD SYSTEM: Check if Team A is working using old logic
                    HalfTeam teamA = HalfTeamFactory.getByName( "A" ); // .get("A");
                    Day oldDay = oldSystem.getDayByDate( testDate );
                    boolean oldIsWorking = false;
                    if (oldDay != null && teamA != null) {
                        oldIsWorking = oldDay.getInWichTeamIsHalfTeam( teamA ) >= 0; // .isTeamWorking(teamA);
                    }
                    String oldPattern = oldIsWorking ? "WORK" : "REST";

                    // NEW SYSTEM: Get day in cycle using new repository
                    int newDayInCycle = newRepository.getDayInCycle( testDate ).join().getData();

                    // NEW SYSTEM: Check if there are shifts generated
                    var newSchedule = newRepository.getWorkScheduleForDate( testDate, 1L ).join().getData();
                    boolean newIsWorking = newSchedule != null && !newSchedule.getShifts().isEmpty();
                    String newPattern = newIsWorking ? "WORK" : "REST";

                    // Compare
                    boolean cycleMatch = oldDayInCycle == newDayInCycle;
                    boolean patternMatch = oldPattern.equals( newPattern );
                    String matchStatus = (cycleMatch && patternMatch) ? "✅" : "❌";

                    report.append( String.format( "%-10s | %9d | %9d | %-11s | %-11s | %s\n",
                            testDate, oldDayInCycle, newDayInCycle, oldPattern, newPattern, matchStatus ) );

                    // Log discrepancies
                    if (!cycleMatch) {
                        report.append( "  → CYCLE MISMATCH: Old=" ).append( oldDayInCycle )
                                .append( ", New=" ).append( newDayInCycle ).append( "\n" );
                    }
                    if (!patternMatch) {
                        report.append( "  → PATTERN MISMATCH: Old=" ).append( oldPattern )
                                .append( ", New=" ).append( newPattern ).append( "\n" );
                    }
                } catch (Exception e) {
                    report.append( String.format( "%-10s | ERROR     | ERROR     | ERROR       | ERROR       | ❌\n", testDate ) );
                    report.append( "  → Exception: " ).append( e.getMessage() ).append( "\n" );
                }
            }
        } catch (Exception e) {
            report.append( "❌ Pattern comparison failed: " ).append( e.getMessage() ).append( "\n" );
        }

        report.append( "\n=== END PATTERN COMPARISON ===\n" );

        String finalReport = report.toString();
        Log.i( TAG, finalReport );

        return finalReport;
    }

    /**
     * Detailed analysis of day-in-cycle calculation
     */
    public static String analyzeDayInCycleCalculation(Context context) {
        StringBuilder report = new StringBuilder();

        report.append( "=== DAY-IN-CYCLE CALCULATION ANALYSIS ===\n" );

        try {
            // Get scheme start date
            LocalDate schemeStart = Preferences.getSchemeStartDate( context );
            report.append( "Scheme Start Date: " ).append( schemeStart ).append( "\n" );
            report.append( "Constants: DAY=" ).append( Costants.QD_SCHEME_START_DAY )
                    .append( ", MONTH=" ).append( Costants.QD_SCHEME_START_MONTH )
                    .append( ", YEAR=" ).append( Costants.QD_SCHEME_START_YEAR ).append( "\n\n" );

            // Test problematic dates
            LocalDate[] testDates = {
                    LocalDate.of( 2025, 6, 1 ),
                    LocalDate.of( 2025, 6, 2 ),
                    LocalDate.of( 2025, 6, 3 ),
                    LocalDate.of( 2025, 6, 4 ),
                    LocalDate.of( 2025, 6, 5 )
            };

            report.append( "CALCULATION BREAKDOWN:\n" );
            report.append( "Date       | Days Since | Day in Cycle | Manual Calc | Repository\n" );
            report.append( "-----------|------------|--------------|-------------|------------\n" );

            ServiceProvider serviceProvider = new ServiceProviderImpl( context );
            WorkScheduleRepository repository = serviceProvider.getWorkScheduleService();

            for (LocalDate testDate : testDates) {
                try {
                    // Manual calculation
                    long daysSinceStart = ChronoUnit.DAYS.between( schemeStart, testDate );
                    int manualDayInCycle = (int) (daysSinceStart % 18);

                    // Repository calculation
                    int repoDayInCycle = repository.getDayInCycle( testDate ).join().getData();

                    // Repository days since start
                    long repoDaysSince = repository.getDaysFromSchemeStart( testDate ).join().getData();

                    String match = (manualDayInCycle == repoDayInCycle) ? "✅" : "❌";

                    report.append( String.format( "%-10s | %10d | %12d | %11d | %10d %s\n",
                            testDate, daysSinceStart, repoDayInCycle, manualDayInCycle, repoDayInCycle, match ) );

                    if (manualDayInCycle != repoDayInCycle) {
                        report.append( "  → MISMATCH: Manual=" ).append( manualDayInCycle )
                                .append( ", Repository=" ).append( repoDayInCycle ).append( "\n" );
                    }

                    if (daysSinceStart != repoDaysSince) {
                        report.append( "  → DAYS SINCE MISMATCH: Manual=" ).append( daysSinceStart )
                                .append( ", Repository=" ).append( repoDaysSince ).append( "\n" );
                    }
                } catch (Exception e) {
                    report.append( String.format( "%-10s | ERROR      | ERROR        | ERROR       | ERROR\n", testDate ) );
                }
            }
        } catch (Exception e) {
            report.append( "❌ Day-in-cycle analysis failed: " ).append( e.getMessage() ).append( "\n" );
        }

        report.append( "\n=== END DAY-IN-CYCLE ANALYSIS ===\n" );

        String finalReport = report.toString();
        Log.i( TAG + "_CYCLE", finalReport );

        return finalReport;
    }

    /**
     * Test the QuattroDue 18-day pattern mapping
     */
    public static String testQuattroDuePattern() {
        StringBuilder report = new StringBuilder();

        report.append( "=== QUATTRODUE 18-DAY PATTERN TEST ===\n" );
        report.append( "Team A Pattern Analysis (18-day cycle)\n\n" );

        // The correct QuattroDue pattern for Team A
        // Based on the constants in Costants.java
        boolean[] teamAPattern = new boolean[18];

        // Analyze the QD_SCHEME constant to understand Team A pattern
        report.append( "QD_SCHEME Analysis for Team A:\n" );
        report.append( "Cycle | Shifts | Team A Working?\n" );
        report.append( "------|--------|----------------\n" );

        for (int day = 0; day < 18; day++) {
            try {
                // Get the scheme for this day
                char[][][] scheme = Costants.QD_SCHEME;
                char[][] dayScheme = scheme[day];

                boolean teamAWorking = false;

                // Check each shift for Team A
                for (int shift = 0; shift < dayScheme.length; shift++) {
                    char[] teams = dayScheme[shift];
                    for (char team : teams) {
                        if (team == 'A') {
                            teamAWorking = true;
                            break;
                        }
                    }
                    if (teamAWorking) break;
                }

                teamAPattern[day] = teamAWorking;

                report.append( String.format( "%5d | %6d | %s\n",
                        day, dayScheme.length, teamAWorking ? "WORK" : "REST" ) );
            } catch (Exception e) {
                report.append( String.format( "%5d | ERROR  | ERROR\n", day ) );
            }
        }

        // Analyze pattern
        report.append( "\nPattern Summary:\n" );
        StringBuilder patternStr = new StringBuilder();
        int workDays = 0;
        int restDays = 0;

        for (boolean working : teamAPattern) {
            patternStr.append( working ? "W" : "R" );
            if (working) workDays++;
            else restDays++;
        }

        report.append( "Pattern: " ).append( patternStr.toString() ).append( "\n" );
        report.append( "Work Days: " ).append( workDays ).append( "/18\n" );
        report.append( "Rest Days: " ).append( restDays ).append( "/18\n" );
        report.append( "Work Percentage: " ).append( String.format( "%.1f", (workDays * 100.0) / 18 ) ).append( "%\n" );

        // Expected 4-2 pattern validation
        if (Math.abs( workDays - 12 ) <= 1 && Math.abs( restDays - 6 ) <= 1) {
            report.append( "✅ Pattern consistent with 4-2 cycle (12 work, 6 rest)\n" );
        } else {
            report.append( "⚠️ Pattern may not be standard 4-2 cycle\n" );
        }

        report.append( "\n=== END PATTERN TEST ===\n" );

        String finalReport = report.toString();
        Log.i( TAG + "_PATTERN", finalReport );

        return finalReport;
    }

    /**
     * Comprehensive pattern diagnosis
     */
    public static String runCompletePatternDiagnosis(Context context) {
        StringBuilder fullReport = new StringBuilder();

        fullReport.append( "=== COMPREHENSIVE QUATTRODUE PATTERN DIAGNOSIS ===\n\n" );

        fullReport.append( testQuattroDuePattern() ).append( "\n" );
        fullReport.append( analyzeDayInCycleCalculation( context ) ).append( "\n" );
        fullReport.append( compareOldVsNewPattern( context ) ).append( "\n" );

        fullReport.append( "=== DIAGNOSIS COMPLETE ===\n" );

        String finalReport = fullReport.toString();
        Log.i( TAG + "_COMPLETE", finalReport );

        return finalReport;
    }
}