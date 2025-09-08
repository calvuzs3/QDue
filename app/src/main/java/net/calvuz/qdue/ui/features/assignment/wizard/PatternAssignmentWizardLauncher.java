package net.calvuz.qdue.ui.features.assignment.wizard;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * PatternAssignmentWizardLauncher - Enhanced launcher with result callback support
 *
 * <p>Provides consistent launching mechanism for PatternAssignmentWizardActivity
 * with support for result handling and data refresh communication.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li><strong>Request Code Management</strong>: Consistent request codes for result handling</li>
 *   <li><strong>Intent Factory</strong>: Type-safe intent creation</li>
 *   <li><strong>Result Communication</strong>: Standardized result codes</li>
 *   <li><strong>Error Handling</strong>: Graceful fallback and validation</li>
 * </ul>
 */
public class PatternAssignmentWizardLauncher
{

    private static final String TAG = "PatternWizardLauncher";

    // ==================== REQUEST CODES ====================

    /**
     * Request code for first assignment creation
     */
    public static final int REQUEST_FIRST_ASSIGNMENT = 1001;

    /**
     * Request code for assignment changes/updates
     */
    public static final int REQUEST_CHANGE_ASSIGNMENT = 1002;

    // ==================== RESULT CODES ====================

    /**
     * Result code indicating assignment was successfully created/updated
     */
    public static final int RESULT_ASSIGNMENT_UPDATED = AppCompatActivity.RESULT_OK;

    /**
     * Result code indicating assignment creation was cancelled
     */
    public static final int RESULT_ASSIGNMENT_CANCELLED = AppCompatActivity.RESULT_CANCELED;

    // ==================== LAUNCHER METHODS ====================

    /**
     * Launch wizard for first-time assignment creation with result callback.
     * Maintains compatibility with existing launch() calls.
     *
     * @param fromActivity Activity that will receive the result
     */
    public static void launch(AppCompatActivity fromActivity) {
        launchForResult( fromActivity, false, REQUEST_CHANGE_ASSIGNMENT );
    }

    /**
     * Launch wizard for assignment modification with result callback.
     *
     * @param fromActivity Activity that will receive the result
     */
    public static void launchForChange(AppCompatActivity fromActivity) {
        launchForResult( fromActivity, false, REQUEST_CHANGE_ASSIGNMENT );
    }

    /**
     * Internal method to launch wizard with result handling.
     *
     * @param fromActivity      Activity launching the wizard
     * @param isFirstAssignment Whether this is the first assignment
     * @param requestCode       Request code for result identification
     */
    private static void launchForResult(
            AppCompatActivity fromActivity,
            boolean isFirstAssignment,
            int requestCode
    ) {
        try {
            Intent intent = PatternAssignmentWizardActivity.createIntent(
                    fromActivity, isFirstAssignment );

            Log.d( TAG, "Launching PatternAssignmentWizard with requestCode: " + requestCode );
            fromActivity.startActivityForResult( intent, requestCode );
        } catch (Exception e) {
            Log.e( TAG, "Error launching PatternAssignmentWizard", e );
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if assignment wizard is available and properly configured.
     *
     * @return true if wizard can be launched
     */
    public static boolean isAvailable() {
        // Add any availability checks here
        // For example, check if required services are initialized
        return true;
    }

    /**
     * Helper method to check if result indicates successful assignment update.
     *
     * @param requestCode The request code from onActivityResult
     * @param resultCode  The result code from onActivityResult
     * @return true if assignment was successfully updated
     */
    public static boolean isAssignmentUpdated(
            int requestCode,
            int resultCode
    ) {
        return (requestCode == REQUEST_FIRST_ASSIGNMENT || requestCode == REQUEST_CHANGE_ASSIGNMENT)
                && resultCode == RESULT_ASSIGNMENT_UPDATED;
    }

    private PatternAssignmentWizardLauncher() {
        throw new UnsupportedOperationException( "This class cannot be instantiated" );
    }
}