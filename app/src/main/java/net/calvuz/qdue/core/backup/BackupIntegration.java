package net.calvuz.qdue.core.backup;

import android.content.Context;

import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.List;

/**
 * STEP 3: Backup Integration Helper
 * <p>
 * Provides seamless integration of automatic backup functionality
 * with existing EventsActivity and EventPackageManager systems.
 * <p>
 * This class acts as a bridge between the events management system
 * and the backup/restore functionality, ensuring backups are triggered
 * automatically when events are modified.
 */
public class BackupIntegration {

    private static final String TAG = "EV_BACKUP_INT";

    // Singleton instance for app-wide access
    private static BackupIntegration sInstance;

    // Components
    private final Context mContext;
    private final BackupManager mBackupManager;
    private final RestoreManager mRestoreManager;
    private final ExportManager mExportManager;

    // Backup triggers configuration
    private boolean mAutoBackupOnImport = true;
    private boolean mAutoBackupOnCreate = true;
    private boolean mAutoBackupOnUpdate = true;
    private boolean mAutoBackupOnDelete = true;

    /**
     * Private constructor for singleton
     */
    private BackupIntegration(Context context) {
        mContext = context.getApplicationContext();
        mBackupManager = new BackupManager(mContext);
        mRestoreManager = new RestoreManager(mContext);
        mExportManager = new ExportManager(mContext);

        Log.d(TAG, "BackupIntegration initialized");
    }

    /**
     * Get singleton instance
     */
    public static synchronized BackupIntegration getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BackupIntegration(context);
        }
        return sInstance;
    }

    /**
     * Get BackupManager instance
     */
    public BackupManager getBackupManager() {
        return mBackupManager;
    }

    /**
     * Get RestoreManager instance
     */
    public RestoreManager getRestoreManager() {
        return mRestoreManager;
    }

    /**
     * Get ExportManager instance
     */
    public ExportManager getExportManager() {
        return mExportManager;
    }

    // ==================== AUTO BACKUP TRIGGERS ====================

    /**
     * Trigger backup after events import
     * Call this from EventsActivity after successful import
     */
    public void onEventsImported(List<LocalEvent> allEvents, int importedCount) {
        if (!mAutoBackupOnImport) {
            return;
        }

        Log.d(TAG, String.format("Auto backup triggered: %d events imported, %d total events",
                importedCount, allEvents.size()));

        mBackupManager.performAutoBackup(allEvents);
    }

    /**
     * Trigger backup after event creation
     * Call this after creating new events
     */
    public void onEventCreated(List<LocalEvent> allEvents, LocalEvent newEvent) {
        if (!mAutoBackupOnCreate) {
            return;
        }

        Log.d(TAG, "Auto backup triggered: event created - " + newEvent.getTitle());
        mBackupManager.performAutoBackup(allEvents);
    }

    /**
     * Trigger backup after event update
     * Call this after updating existing events
     */
    public void onEventUpdated(List<LocalEvent> allEvents, LocalEvent updatedEvent) {
        if (!mAutoBackupOnUpdate) {
            return;
        }

        Log.d(TAG, "Auto backup triggered: event updated - " + updatedEvent.getTitle());
        mBackupManager.performAutoBackup(allEvents);
    }

    /**
     * Trigger backup after event deletion
     * Call this after deleting events
     */
    public void onEventDeleted(List<LocalEvent> allEvents, String deletedEventTitle) {
        if (!mAutoBackupOnDelete) {
            return;
        }

        Log.d(TAG, "Auto backup triggered: event deleted - " + deletedEventTitle);
        mBackupManager.performAutoBackup(allEvents);
    }

    /**
     * Trigger backup after bulk operations
     * Call this after clearing all events or bulk operations
     */
    public void onBulkEventsChanged(List<LocalEvent> allEvents, String operation) {
        Log.d(TAG, "Auto backup triggered: bulk operation - " + operation +
                " (" + allEvents.size() + " events)");
        mBackupManager.performAutoBackup(allEvents);
    }

    // ==================== BACKUP CONFIGURATION ====================

    /**
     * Enable/disable auto backup for different operations
     */
    public void setAutoBackupEnabled(boolean onImport, boolean onCreate,
                                     boolean onUpdate, boolean onDelete) {
        mAutoBackupOnImport = onImport;
        mAutoBackupOnCreate = onCreate;
        mAutoBackupOnUpdate = onUpdate;
        mAutoBackupOnDelete = onDelete;

        Log.d(TAG, String.format("Auto backup configuration: import=%b, create=%b, update=%b, delete=%b",
                onImport, onCreate, onUpdate, onDelete));
    }

    /**
     * Check if auto backup is enabled for any operation
     */
    public boolean isAnyAutoBackupEnabled() {
        return mAutoBackupOnImport || mAutoBackupOnCreate ||
                mAutoBackupOnUpdate || mAutoBackupOnDelete;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get backup summary for UI display
     */
    public BackupSummary getBackupSummary() {
        BackupManager.BackupStats stats = mBackupManager.getBackupStats();
        List<BackupManager.BackupInfo> backups = mBackupManager.getAvailableBackups();

        return new BackupSummary(
                stats.autoBackupEnabled,
                stats.lastBackupTime,
                stats.totalBackups,
                backups.size(),
                isAnyAutoBackupEnabled()
        );
    }

    /**
     * Perform manual backup with UI callback
     */
    public void performManualBackup(List<LocalEvent> events,
                                    BackupManager.BackupCallback callback) {
        Log.d(TAG, "Manual backup requested for " + events.size() + " events");
        mBackupManager.performManualBackup(events, callback);
    }

    /**
     * Show backup/restore options for EventsActivity
     */
    public BackupRestoreOptions getBackupRestoreOptions() {
        return new BackupRestoreOptions(
                mBackupManager.getAvailableBackups(),
                mBackupManager.isAutoBackupEnabled(),
                getBackupSummary()
        );
    }

    // ==================== INTEGRATION HELPER CLASSES ====================

    /**
     * Backup summary for UI display
     */
    public static class BackupSummary {
        public final boolean autoBackupEnabled;
        public final String lastBackupTime;
        public final int totalBackupsCreated;
        public final int availableBackups;
        public final boolean anyAutoBackupEnabled;

        public BackupSummary(boolean autoBackupEnabled, String lastBackupTime,
                             int totalBackupsCreated, int availableBackups,
                             boolean anyAutoBackupEnabled) {
            this.autoBackupEnabled = autoBackupEnabled;
            this.lastBackupTime = lastBackupTime;
            this.totalBackupsCreated = totalBackupsCreated;
            this.availableBackups = availableBackups;
            this.anyAutoBackupEnabled = anyAutoBackupEnabled;
        }

        public String getStatusText() {
            if (!autoBackupEnabled) {
                return "Auto backup disabled";
            } else if (lastBackupTime != null) {
                return "Last backup: " + lastBackupTime;
            } else {
                return "No backups yet";
            }
        }
    }

    /**
     * Backup and restore options for UI
     */
    public static class BackupRestoreOptions {
        public final List<BackupManager.BackupInfo> availableBackups;
        public final boolean autoBackupEnabled;
        public final BackupSummary summary;

        public BackupRestoreOptions(List<BackupManager.BackupInfo> availableBackups,
                                    boolean autoBackupEnabled, BackupSummary summary) {
            this.availableBackups = availableBackups;
            this.autoBackupEnabled = autoBackupEnabled;
            this.summary = summary;
        }

        public boolean hasBackups() {
            return !availableBackups.isEmpty();
        }

        public BackupManager.BackupInfo getLatestBackup() {
            return hasBackups() ? availableBackups.get(0) : null;
        }
    }

    // ==================== EVENTSACTIVITY INTEGRATION METHODS ====================

    /**
     * Integration method for EventsActivity.importEventsFromFile()
     * Add this call after successful import in EventsActivity
     */
    public static void integrateWithImport(Context context, List<LocalEvent> allEvents,
                                           int importedCount) {
        BackupIntegration integration = getInstance(context);
        integration.onEventsImported(allEvents, importedCount);
    }

    /**
     * Integration method for EventsActivity.clearAllEvents()
     * Add this call after clearing events in EventsActivity
     */
    public static void integrateWithClearAll(Context context, List<LocalEvent> remainingEvents) {
        BackupIntegration integration = getInstance(context);
        integration.onBulkEventsChanged(remainingEvents, "clear_all");
    }

    /**
     * Integration method for future event creation
     * Call this when implementing event creation in EventsActivity
     */
    public static void integrateWithEventCreation(Context context, List<LocalEvent> allEvents,
                                                  LocalEvent newEvent) {
        BackupIntegration integration = getInstance(context);
        integration.onEventCreated(allEvents, newEvent);
    }

    /**
     * Integration method for future event updates
     * Call this when implementing event editing in EventsActivity
     */
    public static void integrateWithEventUpdate(Context context, List<LocalEvent> allEvents,
                                                LocalEvent updatedEvent) {
        BackupIntegration integration = getInstance(context);
        integration.onEventUpdated(allEvents, updatedEvent);
    }

    /**
     * Integration method for event deletion
     * Call this when implementing event deletion in EventsActivity
     */
    public static void integrateWithEventDeletion(Context context, List<LocalEvent> allEvents,
                                                  String deletedEventTitle) {
        BackupIntegration integration = getInstance(context);
        integration.onEventDeleted(allEvents, deletedEventTitle);
    }

    // ==================== MENU INTEGRATION FOR EVENTSACTIVITY ====================

    /**
     * Get menu items for backup/restore functionality
     * Use this to add backup/restore options to EventsActivity menu
     */
    public MenuItems getMenuItems() {
        return new MenuItems(
                mBackupManager.getAvailableBackups().size() > 0, // Has backups to restore
                mBackupManager.isAutoBackupEnabled(),
                getBackupSummary()
        );
    }

    /**
     * Menu items configuration for EventsActivity
     */
    public static class MenuItems {
        public final boolean showRestoreOption;
        public final boolean autoBackupEnabled;
        public final BackupSummary summary;

        public MenuItems(boolean showRestoreOption, boolean autoBackupEnabled,
                         BackupSummary summary) {
            this.showRestoreOption = showRestoreOption;
            this.autoBackupEnabled = autoBackupEnabled;
            this.summary = summary;
        }

        public String getBackupStatusMenuTitle() {
            if (autoBackupEnabled) {
                return "Auto Backup: ON";
            } else {
                return "Auto Backup: OFF";
            }
        }

        public String getRestoreMenuTitle() {
            if (showRestoreOption) {
                return "Restore from Backup";
            } else {
                return "No Backups Available";
            }
        }
    }

    // ==================== PREFERENCE INTEGRATION ====================

    /**
     * Sync with preferences for auto backup settings
     * Call this from EventsPreferenceFragment or settings
     */
    public void syncWithPreferences() {
        boolean autoBackupEnabled = mBackupManager.isAutoBackupEnabled();

        // Update local configuration based on global preference
        setAutoBackupEnabled(
                autoBackupEnabled, // Import
                autoBackupEnabled, // Create
                autoBackupEnabled, // Update
                autoBackupEnabled  // Delete
        );

        Log.d(TAG, "Synced with preferences: auto backup " +
                (autoBackupEnabled ? "enabled" : "disabled"));
    }

    /**
     * Update preferences from integration settings
     */
    public void updatePreferences(boolean enabled) {
        mBackupManager.setAutoBackupEnabled(enabled);
        syncWithPreferences();

        Log.d(TAG, "Updated preferences: auto backup " + (enabled ? "enabled" : "disabled"));
    }
}