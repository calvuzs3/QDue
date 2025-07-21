package net.calvuz.qdue.smartshifts.utils;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import net.calvuz.qdue.R;
import net.calvuz.qdue.smartshifts.domain.common.UnifiedOperationResult;

import java.io.File;
import java.util.Locale;

/**
 * SmartShifts UI Library - Shared UI utilities and helper methods.
 * <p>
 * Provides consistent UI patterns and components across all SmartShifts activities and fragments.
 * All methods are static for easy access without instantiation.
 * <p>
 * Key features:
 * - Unified Snackbar messaging with consistent styling
 * - Material Design 3 compliant dialog creation
 * - Progress and loading state management
 * - File sharing and confirmation dialogs
 * - Color and theming utilities
 * - Layout and view utilities
 * <p>
 * Usage examples:
 * ```java
 * // Show success message
 * UiHelper.showSuccessMessage(activity, "Operation completed");
 * <p>
 * // Show error with retry action
 * UiHelper.showErrorMessage(activity, "Error occurred", () -> retryOperation());
 * <p>
 * // Show confirmation dialog
 * UiHelper.showConfirmationDialog(activity, "Delete data?", () -> deleteData());
 * ```
 *
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features
 */
public final class UiHelper {

    private static final String TAG = "UiHelper";

    // Private constructor to prevent instantiation
    private UiHelper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ============================================
    // SNACKBAR MESSAGING METHODS
    // ============================================

    /**
     * Show success message with default styling
     */
    public static void showSuccessMessage(@NonNull Activity activity, @NonNull String message) {
        showSuccessMessage(activity, message, null, null);
    }

    /**
     * Show success message with custom action
     */
    public static void showSuccessMessage(
            @NonNull Activity activity,
            @NonNull String message,
            @Nullable String actionText,
            @Nullable Runnable actionCallback
    ) {
        View coordinatorLayout = findCoordinatorLayout(activity);
        if (coordinatorLayout == null) return;

        Snackbar snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(activity, R.color.smartshifts_success))
                .setTextColor(ContextCompat.getColor(activity, R.color.smartshifts_success));

        if (actionText != null && actionCallback != null) {
            snackbar.setAction(actionText, v -> actionCallback.run())
                    .setActionTextColor(ContextCompat.getColor(activity, R.color.smartshifts_success));
        }

        // Adjust for progress container if visible
        adjustSnackbarForProgressContainer(activity, snackbar);
        snackbar.show();
    }

    /**
     * Show error message with default styling
     */
    public static void showErrorMessage(@NonNull Activity activity, @NonNull String message) {
        showErrorMessage(activity, message, null, null);
    }

    /**
     * Show error message with retry action
     */
    public static void showErrorMessage(
            @NonNull Activity activity,
            @NonNull String message,
            @Nullable Runnable retryCallback
    ) {
        String actionText = retryCallback != null ? "RIPROVA" : "DETTAGLI";
        Runnable actionCallback = retryCallback != null ? retryCallback :
                () -> showErrorDialog(activity, "Dettagli Errore", message, null);

        showErrorMessage(activity, message, actionText, actionCallback);
    }

    /**
     * Show error message with custom action
     */
    public static void showErrorMessage(
            @NonNull Activity activity,
            @NonNull String message,
            @Nullable String actionText,
            @Nullable Runnable actionCallback
    ) {
        View coordinatorLayout = findCoordinatorLayout(activity);
        if (coordinatorLayout == null) return;

        Snackbar snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(activity, R.color.smartshifts_error))
                .setTextColor(ContextCompat.getColor(activity, R.color.smartshifts_error));

        if (actionText != null && actionCallback != null) {
            snackbar.setAction(actionText, v -> actionCallback.run())
                    .setActionTextColor(ContextCompat.getColor(activity, R.color.smartshifts_error));
        }

        // Adjust for progress container if visible
        adjustSnackbarForProgressContainer(activity, snackbar);
        snackbar.show();
    }

    /**
     * Show warning message with default styling
     */
    public static void showWarningMessage(@NonNull Activity activity, @NonNull String message) {
        showWarningMessage(activity, message, null, null);
    }

    /**
     * Show warning message with custom action
     */
    public static void showWarningMessage(
            @NonNull Activity activity,
            @NonNull String message,
            @Nullable String actionText,
            @Nullable Runnable actionCallback
    ) {
        View coordinatorLayout = findCoordinatorLayout(activity);
        if (coordinatorLayout == null) return;

        Snackbar snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(activity, R.color.smartshifts_warning))
                .setTextColor(ContextCompat.getColor(activity, R.color.smartshifts_warning));

        if (actionText != null && actionCallback != null) {
            snackbar.setAction(actionText, v -> actionCallback.run())
                    .setActionTextColor(ContextCompat.getColor(activity, R.color.smartshifts_warning));
        }

        // Adjust for progress container if visible
        adjustSnackbarForProgressContainer(activity, snackbar);
        snackbar.show();
    }

    /**
     * Show info message with default styling
     */
    public static void showInfoMessage(@NonNull Activity activity, @NonNull String message) {
        showInfoMessage(activity, message, null, null);
    }

    /**
     * Show info message with custom action
     */
    public static void showInfoMessage(
            @NonNull Activity activity,
            @NonNull String message,
            @Nullable String actionText,
            @Nullable Runnable actionCallback
    ) {
        View coordinatorLayout = findCoordinatorLayout(activity);
        if (coordinatorLayout == null) return;

        Snackbar snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(ContextCompat.getColor(activity, R.color.smartshifts_info))
                .setTextColor(ContextCompat.getColor(activity, R.color.smartshifts_info));

        if (actionText != null && actionCallback != null) {
            snackbar.setAction(actionText, v -> actionCallback.run())
                    .setActionTextColor(ContextCompat.getColor(activity, R.color.smartshifts_info));
        }

        // Adjust for progress container if visible
        adjustSnackbarForProgressContainer(activity, snackbar);
        snackbar.show();
    }

    /**
     * Show progress message (short duration)
     */
    public static void showProgressMessage(@NonNull Activity activity, @NonNull String message) {
        showInfoMessage(activity, message, null, null);
    }

    // ============================================
    // DIALOG METHODS
    // ============================================

    /**
     * Show success dialog with default icon
     */
    public static void showSuccessDialog(
            @NonNull Context context,
            @NonNull String message,
            @Nullable Runnable onDismiss
    ) {
        showSuccessDialog(context, "Successo", message, onDismiss);
    }

    /**
     * Show success dialog with custom title
     */
    public static void showSuccessDialog(
            @NonNull Context context,
            @NonNull String title,
            @NonNull String message,
            @Nullable Runnable onDismiss
    ) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setIcon(R.drawable.ic_rounded_check_circle_24)
                .setPositiveButton(R.string.smartshifts_ok, (dialog, which) -> {
                    if (onDismiss != null) {
                        onDismiss.run();
                    }
                })
                .show();
    }

    /**
     * Show error dialog with default icon
     */
    public static void showErrorDialog(
            @NonNull Context context,
            @NonNull String message,
            @Nullable Runnable onDismiss
    ) {
        showErrorDialog(context, "Errore", message, onDismiss);
    }

    /**
     * Show error dialog with custom title
     */
    public static void showErrorDialog(
            @NonNull Context context,
            @NonNull String title,
            @NonNull String message,
            @Nullable Runnable onDismiss
    ) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setIcon(R.drawable.ic_rounded_error_24)
                .setPositiveButton(R.string.smartshifts_ok, (dialog, which) -> {
                    if (onDismiss != null) {
                        onDismiss.run();
                    }
                })
                .show();
    }

    /**
     * Show warning dialog with default icon
     */
    public static void showWarningDialog(
            @NonNull Context context,
            @NonNull String message,
            @Nullable Runnable onDismiss
    ) {
        showWarningDialog(context, "Attenzione", message, onDismiss);
    }

    /**
     * Show warning dialog with custom title
     */
    public static void showWarningDialog(
            @NonNull Context context,
            @NonNull String title,
            @NonNull String message,
            @Nullable Runnable onDismiss
    ) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setIcon(R.drawable.ic_rounded_warning_24)
                .setPositiveButton(R.string.smartshifts_ok, (dialog, which) -> {
                    if (onDismiss != null) {
                        onDismiss.run();
                    }
                })
                .show();
    }

    /**
     * Show confirmation dialog with Yes/No buttons
     */
    public static void showConfirmationDialog(
            @NonNull Context context,
            @NonNull String message,
            @NonNull Runnable onConfirm
    ) {
        showConfirmationDialog(context, "Conferma", message, onConfirm);
    }

    /**
     * Show confirmation dialog with custom title
     */
    public static void showConfirmationDialog(
            @NonNull Context context,
            @NonNull String title,
            @NonNull String message,
            @NonNull Runnable onConfirm
    ) {
        showConfirmationDialog(context, title, message, "Conferma", "Annulla", onConfirm, null);
    }

    /**
     * Show confirmation dialog with custom buttons
     */
    public static void showConfirmationDialog(
            @NonNull Context context,
            @NonNull String title,
            @NonNull String message,
            @NonNull String positiveButtonText,
            @NonNull String negativeButtonText,
            @NonNull Runnable onConfirm,
            @Nullable Runnable onCancel
    ) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setIcon(R.drawable.ic_rounded_help_24)
                .setPositiveButton(positiveButtonText, (dialog, which) -> onConfirm.run())
                .setNegativeButton(negativeButtonText, (dialog, which) -> {
                    if (onCancel != null) {
                        onCancel.run();
                    }
                })
                .show();
    }

    /**
     * Show destructive confirmation dialog (with warning styling)
     */
    public static void showDestructiveConfirmationDialog(
            @NonNull Context context,
            @NonNull String title,
            @NonNull String message,
            @NonNull Runnable onConfirm
    ) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setIcon(R.drawable.ic_rounded_warning_24)
                .setPositiveButton("Conferma", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Annulla", null)
                .show();
    }

    // ============================================
    // UNIFIED OPERATION RESULT METHODS
    // ============================================

    /**
     * Handle UnifiedOperationResult with appropriate UI feedback
     */
    public static void handleOperationResult(
            @NonNull Activity activity,
            @NonNull UnifiedOperationResult<?> result
    ) {
        handleOperationResult(activity, result, null);
    }

    /**
     * Handle UnifiedOperationResult with custom success callback
     */
    public static void handleOperationResult(
            @NonNull Activity activity,
            @NonNull UnifiedOperationResult<?> result,
            @Nullable Runnable onSuccess
    ) {
        if (result.isSuccess()) {
            handleSuccessResult(activity, result, onSuccess);
        } else {
            handleErrorResult(activity, result);
        }
    }

    /**
     * Handle success result with appropriate UI feedback
     */
    private static void handleSuccessResult(
            @NonNull Activity activity,
            @NonNull UnifiedOperationResult<?> result,
            @Nullable Runnable onSuccess
    ) {
        String message = result.getSuccessMessage();

        // Add file details if available
        if (result.hasFileOutput()) {
            message += "\nFile: " + result.getResultFile().getName();
        }

        // Add record count if available
        if (result.hasRecordCount()) {
            message += "\nRecord: " + result.getRecordCount();
        }

        // Add file size if available
        if (result.getFileSize() > 0) {
            message += "\nDimensione: " + formatBytes(result.getFileSize());
        }

        // Show appropriate feedback based on operation type
        if (result.getOperationType().isDestructive() || result.hasFileOutput()) {
            // Show dialog for important operations
            showSuccessDialog(activity, message, onSuccess);
        } else {
            // Show snackbar for simple operations
            showSuccessMessage(activity, message);
            if (onSuccess != null) {
                onSuccess.run();
            }
        }
    }

    /**
     * Handle error result with appropriate UI feedback
     */
    private static void handleErrorResult(
            @NonNull Activity activity,
            @NonNull UnifiedOperationResult<?> result
    ) {
        String errorMessage = result.getFormattedErrorMessage();
        String title = "Errore " + result.getOperationType().getDisplayName();

        // Show detailed error dialog for critical operations
        if (result.getOperationType().isDestructive()) {
            showErrorDialog(activity, title, errorMessage, null);
        } else {
            // Show simple error message for non-critical operations
            showErrorMessage(activity, errorMessage);
        }
    }

    // ============================================
    // FILE SHARING METHODS
    // ============================================

    /**
     * Offer file sharing dialog
     */
    public static void offerFileSharing(
            @NonNull Context context,
            @NonNull File file,
            @NonNull String title
    ) {
        if (!file.exists()) {
            showErrorDialog(context, "File non trovato: " + file.getName(), null);
            return;
        }

        String message = "Vuoi condividere il file " + file.getName() + "?";
        if (file.length() > 0) {
            message += "\nDimensione: " + formatBytes(file.length());
        }

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Condividi", (dialog, which) -> {
                    // TODO: Implement file sharing using FileProvider
                    // For now, show placeholder message
                    if (context instanceof Activity) {
                        showInfoMessage((Activity) context, "Funzionalità di condivisione in sviluppo");
                    }
                })
                .setNegativeButton("Annulla", null)
                .show();
    }

    // ============================================
    // VALIDATION ERROR METHODS
    // ============================================

    /**
     * Show validation errors in a formatted dialog
     */
    public static void showValidationErrors(
            @NonNull Context context,
            @NonNull java.util.List<String> errors
    ) {
        if (errors.isEmpty()) return;

        StringBuilder message = new StringBuilder();
        message.append("Errori di validazione rilevati:\n\n");
        for (String error : errors) {
            message.append("• ").append(error).append("\n");
        }

        showErrorDialog(context, "Errore Validazione", message.toString(), null);
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Find CoordinatorLayout in activity (for Snackbar)
     */
    @Nullable
    private static View findCoordinatorLayout(@NonNull Activity activity) {
        // Try common CoordinatorLayout IDs
        View coordinatorLayout = activity.findViewById(R.id.coordinator_layout_settings);
        if (coordinatorLayout == null) {
            coordinatorLayout = activity.findViewById(R.id.coordinator_layout_settings);
        }
//        if (coordinatorLayout == null) {
//            coordinatorLayout = activity.findViewById(R.id.coordinator_layout_main);
//        }
        if (coordinatorLayout == null) {
            // Fallback to content view
            coordinatorLayout = activity.findViewById(android.R.id.content);
        }
        return coordinatorLayout;
    }

    /**
     * Adjust Snackbar position for progress container
     */
    private static void adjustSnackbarForProgressContainer(
            @NonNull Activity activity,
            @NonNull Snackbar snackbar
    ) {
        // Try common progress container IDs
        View progressContainer = activity.findViewById(R.id.progress_container_settings);

        if (progressContainer != null && progressContainer.getVisibility() == View.VISIBLE) {
            snackbar.setAnchorView(progressContainer);
        }
    }

    /**
     * Format bytes to human-readable format
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * Get color from resources safely
     */
    public static int getColor(@NonNull Context context, @ColorRes int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }

    /**
     * Get string from resources safely
     */
    public static String getString(@NonNull Context context, @StringRes int stringRes) {
        return context.getString(stringRes);
    }

    /**
     * Get string from resources with arguments
     */
    public static String getString(@NonNull Context context, @StringRes int stringRes, Object... args) {
        return context.getString(stringRes, args);
    }

    /**
     * Set text safely on TextView (null check)
     */
    public static void setTextSafely(@Nullable TextView textView, @Nullable String text) {
        if (textView != null) {
            textView.setText(text != null ? text : "");
        }
    }

    /**
     * Set visibility safely on View (null check)
     */
    public static void setVisibilitySafely(@Nullable View view, int visibility) {
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    /**
     * Set enabled state safely on View (null check)
     */
    public static void setEnabledSafely(@Nullable View view, boolean enabled) {
        if (view != null) {
            view.setEnabled(enabled);
        }
    }

    /**
     * Set click listener safely on View (null checks)
     */
    public static void setOnClickListenerSafely(@Nullable View view, @Nullable View.OnClickListener listener) {
        if (view != null) {
            view.setOnClickListener(listener);
        }
    }

    // ============================================
    // PROGRESS AND LOADING UTILITIES
    // ============================================

    /**
     * Update progress safely (null checks)
     */
    public static void updateProgressSafely(@Nullable android.widget.ProgressBar progressBar, int progress) {
        if (progressBar != null) {
            progressBar.setProgress(progress);
        }
    }

    /**
     * Update progress with animation
     */
    public static void updateProgressWithAnimation(
            @Nullable android.widget.ProgressBar progressBar,
            int progress
    ) {
        if (progressBar != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            progressBar.setProgress(progress, true);
        } else if (progressBar != null) {
            progressBar.setProgress(progress);
        }
    }

    /**
     * Show loading state with fade animation
     */
    public static void showLoadingWithAnimation(@Nullable View loadingView) {
        if (loadingView != null) {
            loadingView.setVisibility(View.VISIBLE);
            loadingView.setAlpha(0f);
            loadingView.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        }
    }

    /**
     * Hide loading state with fade animation
     */
    public static void hideLoadingWithAnimation(@Nullable View loadingView) {
        if (loadingView != null) {
            loadingView.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> loadingView.setVisibility(View.GONE))
                    .start();
        }
    }

    // ============================================
    // THEME AND STYLING UTILITIES
    // ============================================

    /**
     * Apply Material Design 3 button styling
     */
    public static void applyPrimaryButtonStyle(@Nullable MaterialButton button) {
        if (button != null) {
            // Button styling will be handled by theme
            // This method can be extended for custom styling
        }
    }

    /**
     * Apply Material Design 3 secondary button styling
     */
    public static void applySecondaryButtonStyle(@Nullable MaterialButton button) {
        if (button != null) {
            // Button styling will be handled by theme
            // This method can be extended for custom styling
        }
    }

    /**
     * Apply Material Design 3 text button styling
     */
    public static void applyTextButtonStyle(@Nullable MaterialButton button) {
        if (button != null) {
            // Button styling will be handled by theme
            // This method can be extended for custom styling
        }
    }
}