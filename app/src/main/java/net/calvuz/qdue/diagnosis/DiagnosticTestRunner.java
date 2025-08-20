package net.calvuz.qdue.diagnosis;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.ScrollView;
import android.widget.TextView;

import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * DiagnosticTestRunner - Easy Integration for WorkSchedule Diagnostics
 *
 * <p>Simple wrapper to run diagnostic tests from anywhere in the app.
 * Provides both programmatic access and UI dialogs for easy debugging.</p>
 *
 * <h3>Usage Examples:</h3>
 * <pre>
 * // Run from Activity with UI dialog
 * DiagnosticTestRunner.runWithDialog(this);
 *
 * // Run programmatically and get results
 * String results = DiagnosticTestRunner.runSilent(context);
 *
 * // Add debug menu item
 * DiagnosticTestRunner.addDebugMenuOption(activity, menu);
 * </pre>
 *
 * @author QDue Development Team
 * @version 1.0.0
 */
public class DiagnosticTestRunner {

    private static final String TAG = "DiagnosticTestRunner";

    /**
     * Run diagnostics with UI dialog showing results
     */
    public static void runWithDialog(Activity activity) {
        // Show progress dialog while running tests
        AlertDialog progressDialog = new AlertDialog.Builder(activity)
                .setTitle("Running Diagnostics...")
                .setMessage("Analyzing WorkSchedule components...\nPlease wait.")
                .setCancelable(false)
                .show();

        // Run tests in background
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    return WorkScheduleDiagnosticTestSuite.runDiagnostics(activity);
                } catch (Exception e) {
                    Log.e(TAG, "Diagnostic test execution failed", e);
                    return "ERROR: Diagnostic test execution failed: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String results) {
                progressDialog.dismiss();
                showResultsDialog(activity, results);
            }
        }.execute();
    }

    /**
     * Run diagnostics silently and return results
     */
    public static String runSilent(Context context) {
        try {
            Log.d(TAG, "Running silent diagnostics...");
            return WorkScheduleDiagnosticTestSuite.runDiagnostics(context);
        } catch (Exception e) {
            Log.e(TAG, "Silent diagnostic execution failed", e);
            return "ERROR: Silent diagnostic execution failed: " + e.getMessage();
        }
    }

    /**
     * Show diagnostic results in a scrollable dialog
     */
    private static void showResultsDialog(Activity activity, String results) {
        // Create scrollable text view
        TextView textView = new TextView(activity);
        textView.setText(results);
        textView.setTextSize(12);
        textView.setPadding(16, 16, 16, 16);
        textView.setTextIsSelectable(true);
        textView.setTypeface(android.graphics.Typeface.MONOSPACE);

        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(textView);

        // Determine dialog title based on results
        String title = results.contains("❌ FAIL") ?
                "❌ Diagnostic Results (ISSUES FOUND)" :
                "✅ Diagnostic Results (ALL PASSED)";

        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .setNeutralButton("Copy to Log", (dialog, which) -> {
                    Log.i(TAG + "_FULL_REPORT", results);
                    // Show toast that results were logged
                    android.widget.Toast.makeText(activity,
                            "Full results copied to logcat",
                            android.widget.Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    /**
     * Add debug menu option to run diagnostics (for debug builds)
     */
    public static void addDebugMenuOption(Activity activity, android.view.Menu menu) {
        // Only add in debug builds
        if (isDebugBuild(activity)) {
            menu.add(0, 99999, 0, "🔧 Run Diagnostics")
                    .setOnMenuItemClickListener(item -> {
                        runWithDialog(activity);
                        return true;
                    });
        }
    }

    /**
     * Check if this is a debug build
     */
    private static boolean isDebugBuild(Context context) {
        try {
            return (context.getApplicationInfo().flags &
                    android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== QUICK ACCESS METHODS ====================

    /**
     * Quick diagnostic check - returns summary status
     */
    public static String quickCheck(Context context) {
        try {
            String fullResults = runSilent(context);

            // Extract key metrics
            boolean hasFails = fullResults.contains("❌");
            boolean hasWarnings = fullResults.contains("⚠️");

            if (!hasFails && !hasWarnings) {
                return "✅ All diagnostics passed";
            } else if (!hasFails) {
                return "⚠️ Minor issues detected";
            } else {
                return "❌ Critical issues found";
            }

        } catch (Exception e) {
            return "💥 Diagnostic error: " + e.getMessage();
        }
    }

    /**
     * Log diagnostics to console (for automated testing)
     */
    public static void logDiagnostics(Context context) {
        Log.i(TAG, "=== STARTING AUTOMATED DIAGNOSTICS ===");

        String results = runSilent(context);

        // Log summary
        String summary = quickCheck(context);
        Log.i(TAG, "Diagnostic Summary: " + summary);

        // Log full results
        Log.i(TAG + "_FULL", results);

        Log.i(TAG, "=== DIAGNOSTICS COMPLETED ===");
    }

    // ==================== CONVENIENCE STATIC LAUNCHERS ====================

    /**
     * Launch diagnostics from SwipeCalendarFragment (when calendar is empty)
     */
    public static void launchFromSwipeCalendar(android.app.Activity activity) {
        Log.w(TAG, "Launching diagnostics from SwipeCalendar due to empty calendar");

        new AlertDialog.Builder(activity)
                .setTitle("Empty Calendar Detected")
                .setMessage("The calendar appears to be empty. Would you like to run diagnostics to identify the issue?")
                .setPositiveButton("Run Diagnostics", (dialog, which) -> runWithDialog(activity))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Launch diagnostics from MainActivity debug menu
     */
    public static void launchFromMainActivity(android.app.Activity activity) {
        runWithDialog(activity);
    }

    /**
     * Launch diagnostics automatically on app startup (debug builds only)
     */
    public static void autoLaunchIfDebug(Context context) {
        if (isDebugBuild(context)) {
            // Log diagnostics automatically in debug builds
            new Thread(() -> {
                try {
                    Thread.sleep(3000); // Wait 3 seconds after app startup
                    logDiagnostics(context);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Auto-launch diagnostics interrupted");
                }
            }).start();
        }
    }
}

// ==================== INTEGRATION HELPER ====================

/**
 * Integration examples for different parts of the app
 */
class DiagnosticIntegrationExamples {

    /**
     * Add to SwipeCalendarFragment when calendar is empty
     */
    public void integrateInSwipeCalendarFragment() {
        /*
        // Add this in SwipeCalendarFragment when calendar is empty:

        if (scheduleMap.isEmpty()) {
            Log.w(TAG, "Calendar is empty - launching diagnostics");

            // Show diagnostic option in debug builds
            if (BuildConfig.DEBUG) {
                DiagnosticTestRunner.launchFromSwipeCalendar(getActivity());
            } else {
                // Silent diagnostics in release builds
                String status = DiagnosticTestRunner.quickCheck(getContext());
                Log.w(TAG, "Diagnostic status: " + status);
            }
        }
        */
    }

    /**
     * Add to MainActivity options menu
     */
    public void integrateInMainActivity() {
        /*
        // Add this in MainActivity.onCreateOptionsMenu():

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.main_menu, menu);

            // Add diagnostic option in debug builds
            DiagnosticTestRunner.addDebugMenuOption(this, menu);

            return true;
        }
        */
    }

    /**
     * Add to Application class for auto-launch
     */
    public void integrateInApplication() {
        /*
        // Add this in Application.onCreate():

        public class QDueApplication extends Application {
            @Override
            public void onCreate() {
                super.onCreate();

                // Auto-launch diagnostics in debug builds
                DiagnosticTestRunner.autoLaunchIfDebug(this);
            }
        }
        */
    }
}