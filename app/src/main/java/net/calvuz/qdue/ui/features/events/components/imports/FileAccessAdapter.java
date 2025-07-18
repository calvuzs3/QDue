package net.calvuz.qdue.ui.features.events.components.imports;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * File Access Adapter - SAF bridge for EventsImportManager
 * <p>
 * This adapter provides a clean interface between the existing EventsImportManager
 * and the new SAF-based file access system, ensuring zero breaking changes
 * while solving storage permission issues.
 * <p>
 * Features:
 * - Drop-in replacement for direct file access
 * - SAF-based file selection (no permissions required)
 * - Compatible with existing EventsImportManager interface
 * - Support for .json and .qdue files
 * - Maintains all existing error handling patterns
 * <p>
 * Usage in EventsActivity:
 * mFileAccessAdapter = new FileAccessAdapter(this);
 * mFileAccessAdapter.selectFile(uri -> importEventsFromFile(uri));
 */
public class FileAccessAdapter {
    private static final String TAG = "FileAccessAdapter";

    // Request code for file selection
    private static final int REQUEST_CODE_SELECT_FILE = 3001;

    // Supported file types for import
    private static final String[] SUPPORTED_MIME_TYPES = {
            "application/json",
            "application/octet-stream",  // .qdue files
            "text/plain",
            "*/*"
    };

    // Context references
    private final Activity mActivity;
    private Fragment mFragment;

    // Pending callback for file selection result
    private FileSelectionCallback mPendingCallback;

    /**
     * Callback interface for file selection
     */
    public interface FileSelectionCallback {
        /**
         * Called when file is successfully selected
         * @param fileUri URI of the selected file
         */
        void onFileSelected(@NonNull Uri fileUri);

        /**
         * Called when file selection fails
         * @param error Error message
         */
        void onSelectionError(@NonNull String error);

        /**
         * Called when user cancels file selection
         */
        default void onSelectionCancelled() {
            Log.d(TAG, "File selection cancelled by user");
        }
    }

    /**
     * Constructor for Activity context
     */
    public FileAccessAdapter(@NonNull Activity activity) {
        this.mActivity = activity;
    }

    /**
     * Constructor for Fragment context
     */
    public FileAccessAdapter(@NonNull Fragment fragment) {
        this.mActivity = fragment.requireActivity();
        this.mFragment = fragment;
    }

    /**
     * Open file selection dialog using SAF
     * This is the main method that replaces direct file access in EventsActivity
     */
    public void selectFile(@NonNull FileSelectionCallback callback) {
        mPendingCallback = callback;

        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            // Set supported MIME types
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, SUPPORTED_MIME_TYPES);

            // Add title for better UX
            intent.putExtra(Intent.EXTRA_TITLE, "Select Events File (JSON/QDue)");

            // Start file picker
            if (mFragment != null) {
                mFragment.startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
            } else {
                mActivity.startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
            }

            Log.d(TAG, "SAF file picker opened for events import");

        } catch (Exception e) {
            Log.e(TAG, "Error opening file picker", e);
            callback.onSelectionError("Failed to open file selector: " + e.getMessage());
        }
    }

    /**
     * Handle activity result from file selection
     * IMPORTANT: Call this from EventsActivity.onActivityResult()
     */
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != REQUEST_CODE_SELECT_FILE) {
            return; // Not our request
        }

        if (mPendingCallback == null) {
            Log.w(TAG, "No pending callback for file selection result");
            return;
        }

        if (resultCode != Activity.RESULT_OK || data == null) {
            // User cancelled or error occurred
            mPendingCallback.onSelectionCancelled();
            mPendingCallback = null;
            return;
        }

        Uri uri = data.getData();
        if (uri == null) {
            mPendingCallback.onSelectionError("No file URI received from file picker");
            mPendingCallback = null;
            return;
        }

        // Validate file accessibility
        if (!isFileAccessible(uri)) {
            mPendingCallback.onSelectionError("Selected file is not accessible");
            mPendingCallback = null;
            return;
        }

        // Success - pass URI to callback
        Log.d(TAG, "File selected successfully: " + uri);
        mPendingCallback.onFileSelected(uri);
        mPendingCallback = null;
    }

    /**
     * Check if file URI is accessible
     */
    private boolean isFileAccessible(@NonNull Uri uri) {
        try {
            mActivity.getContentResolver().openInputStream(uri).close();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "File not accessible: " + uri, e);
            return false;
        }
    }

    /**
     * Get display name of file from URI (utility method for UI)
     */
    @Nullable
    public String getFileName(@NonNull Uri uri) {
        String filename = null;

        try (android.database.Cursor cursor = mActivity.getContentResolver().query(
                uri, null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    filename = cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error reading filename from URI", e);
        }

        return filename;
    }

    /**
     * Get file size from URI (utility method for UI)
     */
    public long getFileSize(@NonNull Uri uri) {
        long size = 0;

        try (android.database.Cursor cursor = mActivity.getContentResolver().query(
                uri, null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (sizeIndex >= 0) {
                    size = cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error reading file size from URI", e);
        }

        return size;
    }

    /**
     * Check if the selected file has a supported extension
     */
    public boolean isSupportedFile(@NonNull Uri uri) {
        String filename = getFileName(uri);
        if (filename == null) return false;

        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".json") || lowerFilename.endsWith(".qdue");
    }

    /**
     * Get human-readable file info string for UI display
     */
    @NonNull
    public String getFileDisplayInfo(@NonNull Uri uri) {
        String filename = getFileName(uri);
        long size = getFileSize(uri);

        StringBuilder info = new StringBuilder();
        info.append(filename != null ? filename : "Unknown file");

        if (size > 0) {
            if (size < 1024) {
                info.append(" (").append(size).append(" B)");
            } else if (size < 1024 * 1024) {
                info.append(" (").append(String.format(QDue.getLocale(),"%.1f KB", size / 1024.0)).append(")");
            } else {
                info.append(" (").append(String.format(QDue.getLocale(), "%.1f MB", size / (1024.0 * 1024.0))).append(")");
            }
        }

        return info.toString();
    }

    /**
     * Clear pending callback (call in onDestroy)
     */
    public void clearPendingCallback() {
        mPendingCallback = null;
        Log.d(TAG, "Cleared pending file selection callback");
    }

    /**
     * Static utility method to check if SAF is available
     */
    @SuppressLint("ObsoleteSdkInt")
    public static boolean isSAFAvailable() {
        // SAF is available on all supported Android versions (API 19+)
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT;
    }

    /**
     * Get supported file types description for user display
     */
    @NonNull
    public static String getSupportedFileTypesDescription() {
        return "Supported: JSON files (.json) and QDue event files (.qdue)";
    }
}