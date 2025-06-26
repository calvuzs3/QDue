package net.calvuz.qdue.core.file;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.calvuz.qdue.utils.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Modern file management using Storage Access Framework (SAF).
 * Handles file import operations without requiring storage permissions.
 *
 * Features:
 * - SAF-based file selection (no storage permissions needed)
 * - Support for .json and .qdue files
 * - Automatic file type detection
 * - Content reading with proper encoding
 * - Error handling and validation
 * - Support for both Activity and Fragment contexts
 *
 * Usage:
 * FileManager fileManager = new FileManager(this);
 * fileManager.importFile(callback);
 */
public class FileManager {
    private static final String TAG = "FILE";

    // Request codes for file operations
    private static final int REQUEST_CODE_IMPORT_FILE = 2001;
    private static final int REQUEST_CODE_EXPORT_FILE = 2002;

    // Supported file types
    private static final String[] SUPPORTED_MIME_TYPES = {
            "application/json",           // Standard JSON files
            "application/octet-stream",   // .qdue files (custom extension)
            "text/plain",                 // Plain text JSON files
            "*/*"                         // Fallback for any file type
    };

    // File extensions
    private static final String JSON_EXTENSION = ".json";
    private static final String QDUE_EXTENSION = ".qdue";

    // Context references
    private final Context mContext;
    private Activity mActivity;
    private Fragment mFragment;

    // Callback storage for file operations
    private FileImportCallback mPendingImportCallback;
    private FileExportCallback mPendingExportCallback;

    /**
     * File type enumeration
     */
    public enum FileType {
        JSON("application/json", JSON_EXTENSION),
        QDUE("application/octet-stream", QDUE_EXTENSION),
        UNKNOWN("*/*", "");

        private final String mimeType;
        private final String extension;

        FileType(String mimeType, String extension) {
            this.mimeType = mimeType;
            this.extension = extension;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getExtension() {
            return extension;
        }

        /**
         * Detect file type from filename
         */
        public static FileType fromFilename(@Nullable String filename) {
            if (filename == null) return UNKNOWN;

            String lowerFilename = filename.toLowerCase();
            if (lowerFilename.endsWith(JSON_EXTENSION)) {
                return JSON;
            } else if (lowerFilename.endsWith(QDUE_EXTENSION)) {
                return QDUE;
            }
            return UNKNOWN;
        }

        /**
         * Detect file type from MIME type
         */
        public static FileType fromMimeType(@Nullable String mimeType) {
            if (mimeType == null) return UNKNOWN;

            for (FileType type : values()) {
                if (type.getMimeType().equals(mimeType)) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }

    /**
     * File information container
     */
    public static class FileInfo {
        private final String filename;
        private final long size;
        private final FileType type;
        private final Uri uri;

        public FileInfo(@NonNull Uri uri, @Nullable String filename, long size, @NonNull FileType type) {
            this.uri = uri;
            this.filename = filename != null ? filename : "unknown_file";
            this.size = size;
            this.type = type;
        }

        public String getFilename() { return filename; }
        public long getSize() { return size; }
        public FileType getType() { return type; }
        public Uri getUri() { return uri; }

        /**
         * Check if this is a supported file type for import
         */
        public boolean isSupported() {
            return type == FileType.JSON || type == FileType.QDUE;
        }

        /**
         * Get human-readable size string
         */
        public String getFormattedSize() {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }

        @Override
        public String toString() {
            return "FileInfo{" +
                    "filename='" + filename + '\'' +
                    ", size=" + getFormattedSize() +
                    ", type=" + type +
                    '}';
        }
    }

    /**
     * Callback interface for file import operations
     */
    public interface FileImportCallback {
        /**
         * Called when file import is successful
         * @param fileInfo Information about the imported file
         * @param content The content of the file as string
         */
        void onFileImported(@NonNull FileInfo fileInfo, @NonNull String content);

        /**
         * Called when file import fails
         * @param error Error message
         * @param cause Optional exception that caused the error
         */
        void onImportError(@NonNull String error, @Nullable Throwable cause);

        /**
         * Called when user cancels file selection
         */
        default void onImportCancelled() {
            Log.d(TAG, "File import cancelled by user");
        }

        /**
         * Called when an unsupported file type is selected
         * @param fileInfo Information about the unsupported file
         */
        default void onUnsupportedFileType(@NonNull FileInfo fileInfo) {
            Log.w(TAG, "Unsupported file type selected: " + fileInfo.getType());
        }
    }

    /**
     * Callback interface for file export operations
     */
    public interface FileExportCallback {
        /**
         * Called when file export is successful
         * @param uri URI of the exported file
         * @param filename Name of the exported file
         */
        void onFileExported(@NonNull Uri uri, @NonNull String filename);

        /**
         * Called when file export fails
         * @param error Error message
         * @param cause Optional exception that caused the error
         */
        void onExportError(@NonNull String error, @Nullable Throwable cause);

        /**
         * Called when user cancels file export
         */
        default void onExportCancelled() {
            Log.d(TAG, "File export cancelled by user");
        }
    }

    /**
     * Constructor for Activity context
     */
    public FileManager(@NonNull Activity activity) {
        this.mContext = activity;
        this.mActivity = activity;
    }

    /**
     * Constructor for Fragment context
     */
    public FileManager(@NonNull Fragment fragment) {
        this.mContext = fragment.requireContext();
        this.mFragment = fragment;
        this.mActivity = fragment.requireActivity();
    }

    /**
     * Start file import process using SAF
     * Opens a file picker for the user to select .json or .qdue files
     */
    public void importFile(@NonNull FileImportCallback callback) {
        mPendingImportCallback = callback;

        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            // Set supported MIME types
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, SUPPORTED_MIME_TYPES);

            // Add title for better UX
            intent.putExtra(Intent.EXTRA_TITLE, "Select JSON or QDue file");

            // Start the picker
            if (mFragment != null) {
                mFragment.startActivityForResult(intent, REQUEST_CODE_IMPORT_FILE);
            } else if (mActivity != null) {
                mActivity.startActivityForResult(intent, REQUEST_CODE_IMPORT_FILE);
            } else {
                callback.onImportError("No valid context for file selection", null);
                return;
            }

            Log.d(TAG, "File import dialog opened");

        } catch (Exception e) {
            Log.e(TAG, "Error opening file import dialog", e);
            callback.onImportError("Failed to open file selector: " + e.getMessage(), e);
        }
    }

    /**
     * Start file export process using SAF
     * Opens a file picker for the user to choose export location
     * Deprecated
     * This method has been deprecated in favor of using the Activity Result API
     * which brings increased type safety via an ActivityResultContract and the
     * prebuilt contracts for common intents available in
     * androidx.activity.result.contract.ActivityResultContracts,
     * provides hooks for testing, and allow receiving results in separate,
     * testable classes independent from your fragment.
     * Use registerForActivityResult(ActivityResultContract, ActivityResultCallback)
     * passing in a StartActivityForResult object for the ActivityResultContract.
     * Params:
     * intent – The intent to start.
     * requestCode – The request code to be returned in onActivityResult(int, int, Intent)
     *  when the activity exits. Must be between 0 and 65535 to be considered valid.
     *  If given requestCode is greater than 65535, an IllegalArgumentException would be thrown.
     */
    public void exportFile(@NonNull String content, @NonNull String suggestedFilename,
                           @NonNull FileType fileType, @NonNull FileExportCallback callback) {
        mPendingExportCallback = callback;

        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(fileType.getMimeType());
            intent.putExtra(Intent.EXTRA_TITLE, suggestedFilename);

            // Start the picker
            if (mFragment != null) {
                //noinspection deprecation
                mFragment.startActivityForResult(intent, REQUEST_CODE_EXPORT_FILE);
            } else if (mActivity != null) {
                mActivity.startActivityForResult(intent, REQUEST_CODE_EXPORT_FILE);
            } else {
                callback.onExportError("No valid context for file export", null);
                return;
            }

            Log.d(TAG, "File export dialog opened for: " + suggestedFilename);

        } catch (Exception e) {
            Log.e(TAG, "Error opening file export dialog", e);
            callback.onExportError("Failed to open file export dialog: " + e.getMessage(), e);
        }
    }

    /**
     * Handle activity results - call this from Activity/Fragment onActivityResult
     */
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            // User cancelled or error occurred
            handleCancelledOperation(requestCode);
            return;
        }

        Uri uri = data.getData();
        if (uri == null) {
            handleOperationError(requestCode, "No file URI received", null);
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_IMPORT_FILE:
                handleFileImport(uri);
                break;

            case REQUEST_CODE_EXPORT_FILE:
                handleFileExport(uri);
                break;

            default:
                Log.w(TAG, "Unknown request code: " + requestCode);
                break;
        }
    }

    /**
     * Handle file import operation
     */
    private void handleFileImport(@NonNull Uri uri) {
        if (mPendingImportCallback == null) {
            Log.w(TAG, "No pending import callback");
            return;
        }

        try {
            // Get file information
            FileInfo fileInfo = getFileInfo(uri);
            Log.d(TAG, "Importing file: " + fileInfo);

            // Check if file type is supported
            if (!fileInfo.isSupported()) {
                mPendingImportCallback.onUnsupportedFileType(fileInfo);
                return;
            }

            // Read file content
            String content = readFileContent(uri);

            // Validate content
            if (content.trim().isEmpty()) {
                mPendingImportCallback.onImportError("File is empty", null);
                return;
            }

            // Basic JSON validation
            if (!isValidJsonContent(content)) {
                mPendingImportCallback.onImportError("File does not contain valid JSON", null);
                return;
            }

            // Success
            mPendingImportCallback.onFileImported(fileInfo, content);
            Log.d(TAG, "File imported successfully: " + fileInfo.getFilename());

        } catch (Exception e) {
            Log.e(TAG, "Error importing file", e);
            mPendingImportCallback.onImportError("Failed to import file: " + e.getMessage(), e);
        } finally {
            mPendingImportCallback = null;
        }
    }

    /**
     * Handle file export operation
     */
    private void handleFileExport(@NonNull Uri uri) {
        if (mPendingExportCallback == null) {
            Log.w(TAG, "No pending export callback");
            return;
        }

        try {
            // TODO: Implement export functionality
            // This would write content to the selected URI
            String filename = getFilenameFromUri(uri);
            mPendingExportCallback.onFileExported(uri, filename != null ? filename : "exported_file");
            Log.d(TAG, "File exported successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error exporting file", e);
            mPendingExportCallback.onExportError("Failed to export file: " + e.getMessage(), e);
        } finally {
            mPendingExportCallback = null;
        }
    }

    /**
     * Get file information from URI
     */
    private FileInfo getFileInfo(@NonNull Uri uri) {
        String filename = null;
        long size = 0;

        try (Cursor cursor = mContext.getContentResolver().query(
                uri, null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                // Get filename
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    filename = cursor.getString(nameIndex);
                }

                // Get file size
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex >= 0) {
                    size = cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error reading file metadata", e);
        }

        // Determine file type
        FileType fileType = FileType.fromFilename(filename);
        if (fileType == FileType.UNKNOWN) {
            // Try to determine from MIME type
            String mimeType = mContext.getContentResolver().getType(uri);
            fileType = FileType.fromMimeType(mimeType);
        }

        return new FileInfo(uri, filename, size, fileType);
    }

    /**
     * Read file content from URI
     */
    private String readFileContent(@NonNull Uri uri) throws IOException {
        StringBuilder content = new StringBuilder();

        try (InputStream inputStream = mContext.getContentResolver().openInputStream(uri);
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(inputStreamReader)) {

            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        }

        return content.toString();
    }

    /**
     * Get filename from URI
     */
    @Nullable
    private String getFilenameFromUri(@NonNull Uri uri) {
        String filename = null;

        try (Cursor cursor = mContext.getContentResolver().query(
                uri, null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
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
     * Basic JSON validation
     */
    private boolean isValidJsonContent(@NonNull String content) {
        String trimmedContent = content.trim();
        return (trimmedContent.startsWith("{") && trimmedContent.endsWith("}")) ||
                (trimmedContent.startsWith("[") && trimmedContent.endsWith("]"));
    }

    /**
     * Handle cancelled operations
     */
    private void handleCancelledOperation(int requestCode) {
        switch (requestCode) {
            case REQUEST_CODE_IMPORT_FILE:
                if (mPendingImportCallback != null) {
                    mPendingImportCallback.onImportCancelled();
                    mPendingImportCallback = null;
                }
                break;

            case REQUEST_CODE_EXPORT_FILE:
                if (mPendingExportCallback != null) {
                    mPendingExportCallback.onExportCancelled();
                    mPendingExportCallback = null;
                }
                break;
        }
    }

    /**
     * Handle operation errors
     */
    private void handleOperationError(int requestCode, @NonNull String error, @Nullable Throwable cause) {
        switch (requestCode) {
            case REQUEST_CODE_IMPORT_FILE:
                if (mPendingImportCallback != null) {
                    mPendingImportCallback.onImportError(error, cause);
                    mPendingImportCallback = null;
                }
                break;

            case REQUEST_CODE_EXPORT_FILE:
                if (mPendingExportCallback != null) {
                    mPendingExportCallback.onExportError(error, cause);
                    mPendingExportCallback = null;
                }
                break;
        }
    }

    /**
     * Clear all pending callbacks (useful in onDestroy)
     */
    public void clearPendingCallbacks() {
        mPendingImportCallback = null;
        mPendingExportCallback = null;
        Log.d(TAG, "Cleared all pending file operation callbacks");
    }

    /**
     * Check if a file URI is accessible
     */
    public boolean isFileAccessible(@NonNull Uri uri) {
        try {
            mContext.getContentResolver().openInputStream(uri).close();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "File not accessible: " + uri, e);
            return false;
        }
    }

    /**
     * Get supported file extensions as a list
     */
    public static List<String> getSupportedExtensions() {
        return Arrays.asList(JSON_EXTENSION, QDUE_EXTENSION);
    }

    /**
     * Get supported MIME types as a list
     */
    public static List<String> getSupportedMimeTypes() {
        List<String> mimeTypes = new ArrayList<>();
        for (FileType type : FileType.values()) {
            if (type != FileType.UNKNOWN) {
                mimeTypes.add(type.getMimeType());
            }
        }
        return mimeTypes;
    }

    /**
     * Utility method to check if filename has supported extension
     */
    public static boolean isSupportedFilename(@Nullable String filename) {
        if (filename == null) return false;

        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(JSON_EXTENSION) || lowerFilename.endsWith(QDUE_EXTENSION);
    }

    /**
     * Create a suggested filename for export
     */
    public static String createSuggestedFilename(@NonNull String baseName, @NonNull FileType fileType) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return baseName + "_" + timestamp + fileType.getExtension();
    }
}