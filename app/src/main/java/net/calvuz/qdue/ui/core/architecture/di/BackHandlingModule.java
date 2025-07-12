package net.calvuz.qdue.ui.core.architecture.di;

import android.content.Context;
import androidx.annotation.NonNull;

import net.calvuz.qdue.ui.core.common.interfaces.BackHandlingService;
import net.calvuz.qdue.ui.core.architecture.services.BackHandlingServiceImpl;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * Dependency injection module for back handling services using the project's
 * custom ServiceLocator pattern (no external libraries required).
 *
 * This module follows the same singleton pattern used throughout the project:
 * - QuattroDue.getInstance(context)
 * - QDueDatabase.getInstance(context)
 * - CalendarDataManagerEnhanced.getEnhancedInstance()
 *
 * Features:
 * - Thread-safe singleton implementation
 * - Lazy initialization
 * - Context-aware service creation
 * - Manual dependency resolution
 * - Consistent with project architecture
 *
 * Usage:
 * BackHandlingService service = BackHandlingModule.getBackHandlingService(context);
 * BackHandlerFactory factory = BackHandlingModule.getBackHandlerFactory(context);
 */
public class BackHandlingModule {
    private static final String TAG = "BackHandlingModule";

    // Singleton instances
    private static volatile BackHandlingService sBackHandlingService;
    private static volatile BackHandlerFactory sBackHandlerFactory;

    // Synchronization objects
    private static final Object SERVICE_LOCK = new Object();
    private static final Object FACTORY_LOCK = new Object();

    /**
     * Private constructor to prevent instantiation
     */
    private BackHandlingModule() {
        throw new UnsupportedOperationException("BackHandlingModule is a utility class");
    }

    /**
     * Get the singleton BackHandlingService instance.
     *
     * Thread-safe lazy initialization following the project's singleton pattern.
     *
     * @param context Application context for service initialization
     * @return BackHandlingService singleton instance
     */
    @NonNull
    public static BackHandlingService getBackHandlingService(@NonNull Context context) {
        if (sBackHandlingService == null) {
            synchronized (SERVICE_LOCK) {
                if (sBackHandlingService == null) {
                    Log.d(TAG, "Initializing BackHandlingService singleton");
                    sBackHandlingService = new BackHandlingServiceImpl(context.getApplicationContext());
                    Log.d(TAG, "BackHandlingService singleton created");
                }
            }
        }
        return sBackHandlingService;
    }

    /**
     * Get the singleton BackHandlerFactory instance.
     *
     * Depends on BackHandlingService, so it will automatically initialize it if needed.
     *
     * @param context Application context for factory initialization
     * @return BackHandlerFactory singleton instance
     */
    @NonNull
    public static BackHandlerFactory getBackHandlerFactory(@NonNull Context context) {
        if (sBackHandlerFactory == null) {
            synchronized (FACTORY_LOCK) {
                if (sBackHandlerFactory == null) {
                    Log.d(TAG, "Initializing BackHandlerFactory singleton");
                    BackHandlingService service = getBackHandlingService(context);
                    sBackHandlerFactory = new BackHandlerFactory(service);
                    Log.d(TAG, "BackHandlerFactory singleton created");
                }
            }
        }
        return sBackHandlerFactory;
    }

    /**
     * Initialize all services early (optional optimization).
     *
     * Call this from Application.onCreate() to pre-initialize services
     * and avoid potential delays during first access.
     *
     * @param context Application context
     */
    public static void initialize(@NonNull Context context) {
        Log.d(TAG, "Pre-initializing all back handling services");
        getBackHandlingService(context);
        getBackHandlerFactory(context);
        Log.d(TAG, "All back handling services pre-initialized");
    }

    /**
     * Clear all singleton instances (for testing or memory cleanup).
     *
     * Warning: This will invalidate all existing references to services.
     * Only use this for testing or when the application is shutting down.
     */
    public static void clearInstances() {
        synchronized (SERVICE_LOCK) {
            synchronized (FACTORY_LOCK) {
                Log.d(TAG, "Clearing all singleton instances");

                // Cleanup existing service if it supports it
                if (sBackHandlingService instanceof BackHandlingServiceImpl) {
                    ((BackHandlingServiceImpl) sBackHandlingService).forceCleanup();
                }

                sBackHandlingService = null;
                sBackHandlerFactory = null;

                Log.d(TAG, "All singleton instances cleared");
            }
        }
    }

    /**
     * Get debug information about the module state (for testing/debugging).
     *
     * @return Debug information string
     */
    @NonNull
    public static String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== BackHandlingModule Debug Info ===\n");
        info.append("BackHandlingService initialized: ").append(sBackHandlingService != null).append("\n");
        info.append("BackHandlerFactory initialized: ").append(sBackHandlerFactory != null).append("\n");

        if (sBackHandlingService instanceof BackHandlingServiceImpl) {
            BackHandlingServiceImpl impl = (BackHandlingServiceImpl) sBackHandlingService;
            info.append("Registered handlers: ").append(impl.getRegisteredHandlerCount()).append("\n");
        }

        return info.toString();
    }
}

// ==================== ESEMPI DI IMPLEMENTAZIONE ====================

/**
 * Example Activity implementation with custom DI back handling
 */
/*
public class ExampleActivity extends AppCompatActivity {

    private BackHandlerFactory mBackHandlerFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get factory using custom DI module
        mBackHandlerFactory = BackHandlingModule.getBackHandlerFactory(this);

        // Register activity-level back handler
        mBackHandlerFactory.forComponent(this)
            .withPriority(10)
            .withDescription("Activity navigation handler")
            .register(() -> {
                // Handle activity-level back logic
                if (canNavigateBack()) {
                    navigateBack();
                    return true;
                } else {
                    finish();
                    return true;
                }
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Automatic cleanup via weak references in service
        BackHandlingModule.getBackHandlingService(this).unregisterComponent(this);
    }
}
*/

/**
 * Example Fragment implementation with custom DI back handling
 */
/*
public class ExampleFragment extends Fragment implements UnsavedChangesHandler {

    private BackHandlerFactory mBackHandlerFactory;
    private boolean mHasUnsavedChanges = false;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get factory using custom DI module
        mBackHandlerFactory = BackHandlingModule.getBackHandlerFactory(requireContext());

        // Register fragment back handlers with different priorities

        // High priority: Unsaved changes
        mBackHandlerFactory.forComponent(this)
            .withPriority(100)
            .registerUnsavedChanges(this);

        // Medium priority: Selection mode (if applicable)
        mBackHandlerFactory.forComponent(this)
            .withPriority(50)
            .withDescription("Selection mode handler")
            .register(() -> {
                if (isInSelectionMode()) {
                    exitSelectionMode();
                    return true;
                }
                return false;
            });
    }

    @Override
    public boolean hasUnsavedChanges() {
        return mHasUnsavedChanges;
    }

    @Override
    public void handleUnsavedChanges(@NonNull Runnable onProceed, @NonNull Runnable onCancel) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. What would you like to do?")
            .setPositiveButton("Discard", (dialog, which) -> {
                mHasUnsavedChanges = false;
                onProceed.run();
            })
            .setNegativeButton("Cancel", (dialog, which) -> onCancel.run())
            .setNeutralButton("Save", (dialog, which) -> {
                saveChanges();
                onProceed.run();
            })
            .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Automatic cleanup via service
        BackHandlingModule.getBackHandlingService(requireContext()).unregisterComponent(this);
    }

    private boolean isInSelectionMode() { return false; }
    private void exitSelectionMode() { }
    private void saveChanges() { mHasUnsavedChanges = false; }
}
*/