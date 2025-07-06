package net.calvuz.qdue.core.handlers;

import android.os.Build;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

import net.calvuz.qdue.utils.Log;

/**
 * Back button callback handler with API level compatibility.
 * <p>
 * Provides consistent back button handling across different Android versions
 * while maintaining compatibility with API 31+ and the new predictive back gesture
 * introduced in API 33+.
 * <p>
 * Features:
 * - API 31+ compatible (no manifest attribute needed)
 * - Automatic registration for API 33+ back callback
 * - Fallback to traditional onBackPressed() for older versions
 * - Lifecycle-aware cleanup
 * - Customizable back handling logic
 * <p>
 * Usage in Activity:
 * BackCallbackHandler backHandler = new BackCallbackHandler(this);
 * backHandler.setBackPressedListener(() -> {
 * // Your custom back logic
 * return true; // true if handled, false for default behavior
 * });
 */
public class BackCallbackHandler {
    private static final String TAG = "BackCallbackHandler";

    private final AppCompatActivity mActivity;
    private OnBackPressedCallback mBackCallback;
    private BackPressedListener mListener;

    /**
     * Interface for custom back press handling
     */
    public interface BackPressedListener {
        /**
         * Called when back button is pressed
         *
         * @return true if the event was handled, false for default behavior
         */
        boolean onBackPressed();
    }

    /**
     * Constructor
     *
     * @param activity The activity to handle back presses for
     */
    public BackCallbackHandler(@NonNull AppCompatActivity activity) {
        this.mActivity = activity;
        setupBackHandling();
    }

    /**
     * Set up back handling based on API level
     */
    private void setupBackHandling() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            Log.d(TAG, "Setting up new back callback for API 33+");
            setupNewBackCallback();
        } else {
            Log.d(TAG, "Using traditional back handling for API < 33");
            // For API 31-32, we rely on the traditional onBackPressed() override
            // The Activity should override onBackPressed() and call our listener
        }
    }

    /**
     * Set up the new OnBackPressedCallback for API 33+
     */
    private void setupNewBackCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mBackCallback = new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    Log.d(TAG, "New back callback triggered");

                    if (mListener != null) {
                        boolean handled = mListener.onBackPressed();
                        if (!handled) {
                            // If not handled by custom logic, use default behavior
                            setEnabled(false);
                            mActivity.onBackPressed();
                            setEnabled(true);
                        }
                    } else {
                        // No custom listener, use default behavior
                        setEnabled(false);
                        mActivity.onBackPressed();
                        setEnabled(true);
                    }
                }
            };

            // Register the callback
            mActivity.getOnBackPressedDispatcher().addCallback(mActivity, mBackCallback);
        }
    }

    /**
     * Set a custom back press listener
     *
     * @param listener The listener to handle back presses
     */
    public void setBackPressedListener(@NonNull BackPressedListener listener) {
        this.mListener = listener;
        Log.d(TAG, "Back press listener set");
    }

    /**
     * Remove the back press listener
     */
    public void removeBackPressedListener() {
        this.mListener = null;
        Log.d(TAG, "Back press listener removed");
    }

    /**
     * Enable or disable back handling
     *
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        if (mBackCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mBackCallback.setEnabled(enabled);
            Log.d(TAG, "Back callback enabled: " + enabled);
        }
    }

    /**
     * Check if back handling is enabled
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        if (mBackCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return mBackCallback.isEnabled();
        }
        return true; // Traditional handling is always "enabled"
    }

    /**
     * Call this method from Activity.onBackPressed() for API < 33
     * <p>
     * Example in your Activity:
     *
     * @Override public void onBackPressed() {
     * if (!backHandler.handleTraditionalBackPress()) {
     * super.onBackPressed();
     * }
     * }
     */
    public boolean handleTraditionalBackPress() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "Traditional back press handling");

            if (mListener != null) {
                return mListener.onBackPressed();
            }
        }
        return false; // Not handled
    }

    /**
     * Clean up resources
     * Call this in Activity.onDestroy()
     */
    public void cleanup() {
        if (mBackCallback != null) {
            mBackCallback.remove();
            mBackCallback = null;
        }
        mListener = null;
        Log.d(TAG, "Back callback handler cleaned up");
    }

    /**
     * Utility method to check if predictive back is available
     */
    public static boolean isPredictiveBackAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }

    /**
     * Create a simple back handler that just finishes the activity
     */
    public static BackCallbackHandler createSimpleFinishHandler(@NonNull AppCompatActivity activity) {
        BackCallbackHandler handler = new BackCallbackHandler(activity);
        handler.setBackPressedListener(() -> {
            activity.finish();
            return true;
        });
        return handler;
    }


    /*

    //
    // Example: Integration in your Activity
    //

    public class EventsActivity extends AppCompatActivity {
        private BackCallbackHandler mBackHandler;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_events);

            // Initialize back handler
            mBackHandler = new BackCallbackHandler(this);
            mBackHandler.setBackPressedListener(this::handleCustomBackPress);
        }

        // Custom back press logic
        private boolean handleCustomBackPress() {
            // Your custom logic here

            // Example: Check if there are unsaved changes
            if (hasUnsavedChanges()) {
                showUnsavedChangesDialog();
                return true; // Handled
            }

            // Example: Close drawer if open
            if (isDrawerOpen()) {
                closeDrawer();
                return true; // Handled
            }

            // Default behavior - finish activity
            finish();
            return true; // Handled
        }

         * For API < 33 compatibility
        @Override
        public void onBackPressed() {
            // Try custom handling first
            if (!mBackHandler.handleTraditionalBackPress()) {
                // If not handled, use default behavior
                super.onBackPressed();
            }
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            // Clean up back handler
            if (mBackHandler != null) {
                mBackHandler.cleanup();
            }
        }

        // Helper methods for the example
        private boolean hasUnsavedChanges() {
            // Your logic to check unsaved changes
            return false;
        }

        private boolean isDrawerOpen() {
            // Your logic to check if drawer is open
            return false;
        }

        private void closeDrawer() {
            // Your logic to close drawer
        }

        private void showUnsavedChangesDialog() {
            // Show dialog asking user about unsaved changes
        }
    }
     */
}