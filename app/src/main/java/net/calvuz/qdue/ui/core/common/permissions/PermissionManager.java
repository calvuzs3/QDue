package net.calvuz.qdue.ui.core.common.permissions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized permission management for the application.
 * Handles runtime permissions, permission requests, and status checks.
 *
 * Features:
 * - Runtime permission handling for Android 6.0+
 * - Bulk permission requests
 * - Permission status checking
 * - Callback-based permission results
 * - Support for both Activity and Fragment contexts
 *
 * Usage:
 * PermissionManager permissionManager = new PermissionManager(this);
 * permissionManager.requestPermission(Permission.CALENDAR_READ, callback);
 */
public class PermissionManager {
    private static final String TAG = "PERMISSIONS";

    // Request codes for different permission types
    private static final int REQUEST_CODE_CALENDAR = 1001;
    private static final int REQUEST_CODE_STORAGE = 1002;
    private static final int REQUEST_CODE_CAMERA = 1003;
    private static final int REQUEST_CODE_LOCATION = 1004;
    private static final int REQUEST_CODE_CONTACTS = 1005;
    private static final int REQUEST_CODE_PHONE = 1006;
    private static final int REQUEST_CODE_MICROPHONE = 1007;
    private static final int REQUEST_CODE_NOTIFICATIONS = 1008;
    private static final int REQUEST_CODE_MULTIPLE = 1100;

    // Context references
    private final Context mContext;
    private Activity mActivity;
    private Fragment mFragment;

    // Callback storage for permission results
    private final Map<Integer, PermissionCallback> mPendingCallbacks = new HashMap<>();

    /**
     * Permission definitions with their Android manifest strings and request codes
     */
    public enum Permission {
        // Calendar permissions
        CALENDAR_READ(Manifest.permission.READ_CALENDAR, REQUEST_CODE_CALENDAR),
        CALENDAR_WRITE(Manifest.permission.WRITE_CALENDAR, REQUEST_CODE_CALENDAR),

        // Storage permissions (legacy - for Android < 10)
        STORAGE_READ(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_CODE_STORAGE),
        STORAGE_WRITE(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_CODE_STORAGE),

        // Camera permission
        CAMERA(Manifest.permission.CAMERA, REQUEST_CODE_CAMERA),

        // Location permissions
        LOCATION_FINE(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_CODE_LOCATION),
        LOCATION_COARSE(Manifest.permission.ACCESS_COARSE_LOCATION, REQUEST_CODE_LOCATION),

        // Contacts permissions
        CONTACTS_READ(Manifest.permission.READ_CONTACTS, REQUEST_CODE_CONTACTS),
        CONTACTS_WRITE(Manifest.permission.WRITE_CONTACTS, REQUEST_CODE_CONTACTS),

        // Phone permissions
        PHONE_CALL(Manifest.permission.CALL_PHONE, REQUEST_CODE_PHONE),
        PHONE_STATE(Manifest.permission.READ_PHONE_STATE, REQUEST_CODE_PHONE),

        // Microphone permission
        MICROPHONE(Manifest.permission.RECORD_AUDIO, REQUEST_CODE_MICROPHONE),

        // Notification permission (Android 13+)
        NOTIFICATIONS(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.POST_NOTIFICATIONS : "", REQUEST_CODE_NOTIFICATIONS);

        private final String manifestPermission;
        private final int requestCode;

        Permission(String manifestPermission, int requestCode) {
            this.manifestPermission = manifestPermission;
            this.requestCode = requestCode;
        }

        public String getManifestPermission() {
            return manifestPermission;
        }

        public int getRequestCode() {
            return requestCode;
        }

        /**
         * Check if this permission is available on current Android version
         */
        public boolean isAvailableOnCurrentVersion() {
            // Handle version-specific permissions
            if (this == NOTIFICATIONS) {
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
            }
            return !manifestPermission.isEmpty();
        }
    }

    /**
     * Permission status enumeration
     */
    public enum PermissionStatus {
        GRANTED,           // Permission is granted
        DENIED,            // Permission is denied
        DENIED_FOREVER,    // Permission is denied with "Don't ask again"
        NOT_APPLICABLE     // Permission not applicable for current Android version
    }

    /**
     * Callback interface for permission requests
     */
    public interface PermissionCallback {
        /**
         * Called when permission is granted
         */
        void onPermissionGranted();

        /**
         * Called when permission is denied
         * @param deniedForever true if user selected "Don't ask again"
         */
        void onPermissionDenied(boolean deniedForever);

        /**
         * Called when permission request encounters an error
         * @param error Error message
         */
        default void onPermissionError(String error) {
            Log.e(TAG, "Permission error: " + error);
        }
    }

    /**
     * Callback interface for multiple permission requests
     */
    public interface MultiplePermissionCallback {
        /**
         * Called when all permissions are processed
         * @param results Map of permission to its grant status
         */
        void onPermissionsResult(Map<Permission, Boolean> results);

        /**
         * Called when some permissions are granted and some are denied
         * @param granted List of granted permissions
         * @param denied List of denied permissions
         */
        default void onPartialPermissionsGranted(List<Permission> granted, List<Permission> denied) {
            // Default implementation - can be overridden
        }
    }

    /**
     * Constructor for Activity context
     */
    public PermissionManager(@NonNull Activity activity) {
        this.mContext = activity;
        this.mActivity = activity;
    }

    /**
     * Constructor for Fragment context
     */
    public PermissionManager(@NonNull Fragment fragment) {
        this.mContext = fragment.requireContext();
        this.mFragment = fragment;
        this.mActivity = fragment.requireActivity();
    }

    /**
     * Check if a specific permission is granted
     */
    public boolean isPermissionGranted(@NonNull Permission permission) {
        if (!permission.isAvailableOnCurrentVersion()) {
            return true; // Consider unavailable permissions as granted
        }

        return ContextCompat.checkSelfPermission(mContext, permission.getManifestPermission())
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Get detailed status of a permission
     */
    public PermissionStatus getPermissionStatus(@NonNull Permission permission) {
        if (!permission.isAvailableOnCurrentVersion()) {
            return PermissionStatus.NOT_APPLICABLE;
        }

        if (isPermissionGranted(permission)) {
            return PermissionStatus.GRANTED;
        }

        // Check if we should show rationale (permission was denied but not permanently)
        boolean shouldShowRationale = false;
        if (mActivity != null) {
            shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    mActivity, permission.getManifestPermission());
        }

        return shouldShowRationale ? PermissionStatus.DENIED : PermissionStatus.DENIED_FOREVER;
    }

    /**
     * Request a single permission
     */
    public void requestPermission(@NonNull Permission permission, @NonNull PermissionCallback callback) {
        if (!permission.isAvailableOnCurrentVersion()) {
            callback.onPermissionGranted(); // Treat as granted for unavailable permissions
            return;
        }

        if (isPermissionGranted(permission)) {
            callback.onPermissionGranted();
            return;
        }

        // Store callback for later use
        mPendingCallbacks.put(permission.getRequestCode(), callback);

        // Request permission
        String[] permissions = {permission.getManifestPermission()};

        if (mFragment != null) {
            mFragment.requestPermissions(permissions, permission.getRequestCode());
        } else if (mActivity != null) {
            ActivityCompat.requestPermissions(mActivity, permissions, permission.getRequestCode());
        } else {
            callback.onPermissionError("No valid context for permission request");
        }

        Log.d(TAG, "Requesting permission: " + permission.getManifestPermission());
    }

    /**
     * Request multiple permissions at once
     */
    public void requestPermissions(@NonNull List<Permission> permissions,
                                   @NonNull MultiplePermissionCallback callback) {
        // Filter permissions available on current version
        List<Permission> availablePermissions = new ArrayList<>();
        List<Permission> alreadyGranted = new ArrayList<>();

        for (Permission permission : permissions) {
            if (!permission.isAvailableOnCurrentVersion()) {
                alreadyGranted.add(permission); // Treat as granted
            } else if (isPermissionGranted(permission)) {
                alreadyGranted.add(permission);
            } else {
                availablePermissions.add(permission);
            }
        }

        // If all permissions are already granted
        if (availablePermissions.isEmpty()) {
            Map<Permission, Boolean> results = new HashMap<>();
            for (Permission permission : permissions) {
                results.put(permission, true);
            }
            callback.onPermissionsResult(results);
            return;
        }

        // Create a special callback that handles multiple permissions
        PermissionCallback multiCallback = new PermissionCallback() {
            @Override
            public void onPermissionGranted() {
                // This will be handled in onRequestPermissionsResult
            }

            @Override
            public void onPermissionDenied(boolean deniedForever) {
                // This will be handled in onRequestPermissionsResult
            }
        };

        // Store the original callback
        mPendingCallbacks.put(REQUEST_CODE_MULTIPLE, new PermissionCallback() {
            @Override
            public void onPermissionGranted() {
                // Not used for multiple permissions
            }

            @Override
            public void onPermissionDenied(boolean deniedForever) {
                // Not used for multiple permissions
            }

            @Override
            public void onPermissionError(String error) {
                callback.onPermissionsResult(new HashMap<>());
            }
        });

        // Convert to string array
        String[] permissionArray = new String[availablePermissions.size()];
        for (int i = 0; i < availablePermissions.size(); i++) {
            permissionArray[i] = availablePermissions.get(i).getManifestPermission();
        }

        // Request permissions
        if (mFragment != null) {
            mFragment.requestPermissions(permissionArray, REQUEST_CODE_MULTIPLE);
        } else if (mActivity != null) {
            ActivityCompat.requestPermissions(mActivity, permissionArray, REQUEST_CODE_MULTIPLE);
        } else {
            callback.onPermissionsResult(new HashMap<>());
        }

        Log.d(TAG, "Requesting multiple permissions: " + Arrays.toString(permissionArray));
    }

    /**
     * Handle permission request results - call this from Activity/Fragment onRequestPermissionsResult
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        PermissionCallback callback = mPendingCallbacks.remove(requestCode);
        if (callback == null) {
            Log.w(TAG, "No callback found for request code: " + requestCode);
            return;
        }

        if (requestCode == REQUEST_CODE_MULTIPLE) {
            handleMultiplePermissionsResult(permissions, grantResults);
            return;
        }

        // Handle single permission result
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission granted: " + permissions[0]);
            callback.onPermissionGranted();
        } else {
            // Check if denied forever
            boolean deniedForever = mActivity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permissions[0]);

            Log.d(TAG, "Permission denied: " + permissions[0] + " (forever: " + deniedForever + ")");
            callback.onPermissionDenied(deniedForever);
        }
    }

    /**
     * Handle multiple permissions result
     */
    private void handleMultiplePermissionsResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
        Map<Permission, Boolean> results = new HashMap<>();
        List<Permission> granted = new ArrayList<>();
        List<Permission> denied = new ArrayList<>();

        for (int i = 0; i < permissions.length; i++) {
            Permission permission = findPermissionByManifest(permissions[i]);
            if (permission != null) {
                boolean isGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                results.put(permission, isGranted);

                if (isGranted) {
                    granted.add(permission);
                } else {
                    denied.add(permission);
                }
            }
        }

        // Notify about partial results if applicable
        if (!granted.isEmpty() && !denied.isEmpty()) {
            // Implementation depends on actual callback interface
            Log.d(TAG, "Partial permissions granted - Granted: " + granted.size() + ", Denied: " + denied.size());
        }

        Log.d(TAG, "Multiple permissions result: " + results.size() + " permissions processed");
    }

    /**
     * Find Permission enum by manifest string
     */
    private Permission findPermissionByManifest(String manifestPermission) {
        for (Permission permission : Permission.values()) {
            if (permission.getManifestPermission().equals(manifestPermission)) {
                return permission;
            }
        }
        return null;
    }

    /**
     * Check if any of the provided permissions are granted
     */
    public boolean isAnyPermissionGranted(@NonNull List<Permission> permissions) {
        for (Permission permission : permissions) {
            if (isPermissionGranted(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if all of the provided permissions are granted
     */
    public boolean areAllPermissionsGranted(@NonNull List<Permission> permissions) {
        for (Permission permission : permissions) {
            if (!isPermissionGranted(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get list of permissions that are not granted from the provided list
     */
    public List<Permission> getUngranted(@NonNull List<Permission> permissions) {
        List<Permission> ungranted = new ArrayList<>();
        for (Permission permission : permissions) {
            if (!isPermissionGranted(permission)) {
                ungranted.add(permission);
            }
        }
        return ungranted;
    }

    /**
     * Clear all pending callbacks (useful in onDestroy)
     */
    public void clearPendingCallbacks() {
        mPendingCallbacks.clear();
        Log.d(TAG, "Cleared all pending permission callbacks");
    }

    /**
     * Utility method to check if storage permissions are needed for current Android version
     */
    public static boolean isStoragePermissionNeeded() {
        // For Android 10+ (API 29+), scoped storage is used by default
        // Storage permissions are only needed for legacy external storage access
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q;
    }

    /**
     * Check if notification permission is needed for current Android version
     */
    public static boolean isNotificationPermissionNeeded() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }
}