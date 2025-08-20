package net.calvuz.qdue.diagnosis;

import static net.calvuz.qdue.core.common.Lib.isDebugBuild;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import net.calvuz.qdue.quattrodue.Costants;

/**
 * FixedQDSchemeAnalysis - Correct Analysis of QD_SCHEME Pattern
 *
 * <p>Fixed version that correctly reads the Costants.QD_SCHEME
 * to understand the real Team A pattern.</p>
 */
public class FixedQDSchemeAnalysis {

    /**
     * Add debug menu option to run diagnostics (for debug builds)
     */
    public static void addDebugMenuOption(Activity activity, android.view.Menu menu) {
        // Only add in debug builds
        if (isDebugBuild(activity)) {
            menu.add(0, 77777, 0, "🔧 QDScheme Analysis")
                    .setOnMenuItemClickListener(item -> {
                        // Test completo del pattern
                        new Thread(() -> {
                            String results = FixedQDSchemeAnalysis.analyzeCorrectQDScheme();
                            Log.v("QDSCHEME_ANALYSIS", results);
                        }).start();
                        return true;
                    });
        }
    }


    private static final String TAG = "FixedQDScheme";

    /**
     * Corrected analysis of QD_SCHEME for Team A
     */
    public static String analyzeCorrectQDScheme() {
        StringBuilder report = new StringBuilder();

        report.append("=== CORRECTED QD_SCHEME ANALYSIS ===\n");
        report.append("Team A Pattern Analysis (18-day cycle)\n\n");

        // Get the scheme
        char[][][] scheme = Costants.QD_SCHEME;

        report.append("QD_SCHEME Structure Analysis:\n");
        report.append("Total Days in Cycle: ").append(scheme.length).append("\n");

        if (scheme.length > 0) {
            report.append("Shifts per Day: ").append(scheme[0].length).append("\n");
            if (scheme[0].length > 0) {
                report.append("Sample - Day 0, Shift 0 Teams: ");
                for (char team : scheme[0][0]) {
                    report.append(team).append(" ");
                }
                report.append("\n");
            }
        }

        report.append("\nDetailed Team A Analysis:\n");
        report.append("Cycle | Shift 0 | Shift 1 | Shift 2 | Shift 3 | Team A Working?\n");
        report.append("------|---------|---------|---------|---------|----------------\n");

        // Analyze Team A pattern correctly
        boolean[] teamAPattern = new boolean[18];

        for (int day = 0; day < Math.min(18, scheme.length); day++) {
            boolean teamAWorking = false;
            StringBuilder shiftDetails = new StringBuilder();

            char[][] dayScheme = scheme[day];

            // Check each shift for Team A
            for (int shift = 0; shift < dayScheme.length; shift++) {
                char[] teams = dayScheme[shift];

                // Build teams string for this shift
                StringBuilder teamsStr = new StringBuilder();
                boolean teamAInThisShift = false;

                for (char team : teams) {
                    teamsStr.append(team);
                    if (team == 'A') {
                        teamAInThisShift = true;
                        teamAWorking = true;
                    }
                }

                // Pad to fixed width
                while (teamsStr.length() < 7) {
                    teamsStr.append(" ");
                }

                shiftDetails.append("| ").append(teamsStr.toString());
            }

            // Pad remaining shifts if less than 4
            while (dayScheme.length < 4) {
                shiftDetails.append("|        ");
            }

            teamAPattern[day] = teamAWorking;

            report.append(String.format("%5d %s | %s\n",
                    day, shiftDetails.toString(), teamAWorking ? "WORK" : "REST"));
        }

        // Analyze the pattern
        report.append("\nPattern Summary:\n");
        StringBuilder patternStr = new StringBuilder();
        int workDays = 0;
        int restDays = 0;

        for (boolean working : teamAPattern) {
            patternStr.append(working ? "W" : "R");
            if (working) workDays++; else restDays++;
        }

        report.append("Pattern: ").append(patternStr.toString()).append("\n");
        report.append("Work Days: ").append(workDays).append("/18\n");
        report.append("Rest Days: ").append(restDays).append("/18\n");
        report.append("Work Percentage: ").append(String.format("%.1f", (workDays * 100.0) / 18)).append("%\n");

        // Pattern validation
        if (workDays == 12 && restDays == 6) {
            report.append("✅ Perfect 4-2 pattern (12 work, 6 rest)\n");
        } else if (Math.abs(workDays - 12) <= 1) {
            report.append("✅ Approximately 4-2 pattern\n");
        } else {
            report.append("❌ Pattern does NOT match 4-2 cycle\n");
        }

        // Look for 4-2 pattern sequences
        report.append("\nPattern Sequence Analysis:\n");
        String pattern = patternStr.toString();
        if (pattern.contains("WWWWRR")) {
            report.append("✅ Found WWWWRR (4 work, 2 rest) sequence\n");
        } else {
            report.append("❌ Standard WWWWRR sequence not found\n");
        }

        // Find actual sequences
        report.append("Actual sequences found:\n");
        for (int i = 0; i < pattern.length() - 5; i++) {
            String seq = pattern.substring(i, Math.min(i + 6, pattern.length()));
            if (seq.matches("W{4}R{2}") || seq.matches("R{2}W{4}")) {
                report.append("  Position ").append(i).append(": ").append(seq).append(" ✅\n");
            }
        }

        report.append("\n=== END CORRECTED QD_SCHEME ANALYSIS ===\n");

        String finalReport = report.toString();
        Log.i(TAG, finalReport);

        return finalReport;
    }

    /**
     * Compare specific problematic dates with QD_SCHEME
     */
    public static String compareProblematicDates() {
        StringBuilder report = new StringBuilder();

        report.append("=== PROBLEMATIC DATES vs QD_SCHEME ===\n");

        // Problematic dates from the previous test
        int[] problematicCycles = {5, 6, 9, 11, 12, 15, 17, 3, 5}; // From the mismatch report
        String[] dates = {"2025-06-02", "2025-06-06", "2025-06-08", "2025-06-12",
                "2025-06-14", "2025-06-18", "2025-06-20"};

        char[][][] scheme = Costants.QD_SCHEME;

        report.append("Date       | Cycle | QD_SCHEME Team A | Expected\n");
        report.append("-----------|-------|------------------|----------\n");

        int dateIndex = 0;
        for (int cycle : problematicCycles) {
            if (dateIndex >= dates.length) break;

            boolean teamAWorking = false;

            if (cycle < scheme.length) {
                char[][] dayScheme = scheme[cycle];

                // Check all shifts for Team A
                for (char[] teams : dayScheme) {
                    for (char team : teams) {
                        if (team == 'A') {
                            teamAWorking = true;
                            break;
                        }
                    }
                    if (teamAWorking) break;
                }
            }

            report.append(String.format("%-10s | %5d | %-16s | %s\n",
                    dates[dateIndex], cycle, teamAWorking ? "WORK" : "REST",
                    teamAWorking ? "Generate Shifts" : "No Shifts"));

            dateIndex++;
        }

        report.append("\n=== END PROBLEMATIC DATES ANALYSIS ===\n");

        String finalReport = report.toString();
        Log.i(TAG + "_PROBLEM", finalReport);

        return finalReport;
    }

    /**
     * Full corrected analysis
     */
    public static String runCorrectedAnalysis() {
        StringBuilder fullReport = new StringBuilder();

        fullReport.append("=== FULL CORRECTED QD_SCHEME ANALYSIS ===\n\n");

        fullReport.append(analyzeCorrectQDScheme()).append("\n");
        fullReport.append(compareProblematicDates()).append("\n");

        fullReport.append("=== CORRECTED ANALYSIS COMPLETE ===\n");

        String finalReport = fullReport.toString();
        Log.i(TAG + "_FULL", finalReport);

        return finalReport;
    }
}