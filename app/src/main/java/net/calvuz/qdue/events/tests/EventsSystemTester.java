package net.calvuz.qdue.events.tests;

import android.content.Context;

import net.calvuz.qdue.events.EventDao;
import net.calvuz.qdue.events.EventPackageManagerExtension;
import net.calvuz.qdue.events.backup.BackupManager;
import net.calvuz.qdue.events.backup.ExportManager;
import net.calvuz.qdue.events.backup.RestoreManager;
import net.calvuz.qdue.events.data.database.EventsDatabase;
import net.calvuz.qdue.events.models.EventPriority;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.utils.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ==================== 3. TESTING E DEBUGGING UTILITY ====================

/**
 * Test and debug utilities for the events system
 */
public class EventsSystemTester {

    private static final String TAG = "EV_TEST";

    /**
     * Create test events for debugging
     */
    public static List<LocalEvent> createTestEvents() {
        List<LocalEvent> testEvents = new ArrayList<>();

        LocalDate today = LocalDate.now();

        // Test event 1: All day event
        LocalEvent event1 = new LocalEvent("Test All Day Event", today);
        event1.setDescription("This is a test all-day event");
        event1.setEventType(EventType.GENERAL);
        event1.setPriority(EventPriority.NORMAL);
        testEvents.add(event1);

        // Test event 2: Timed event
        LocalEvent event2 = new LocalEvent();
        event2.setId("test_timed_event");
        event2.setTitle("Test Timed Event");
        event2.setDescription("This is a test timed event");
        event2.setStartTime(today.atTime(9, 0));
        event2.setEndTime(today.atTime(17, 0));
        event2.setEventType(EventType.STOP_PLANNED);
        event2.setPriority(EventPriority.HIGH);
        event2.setLocation("Test Location");
        event2.setAllDay(false);
        testEvents.add(event2);

        // Test event 3: Multi-day event
        LocalEvent event3 = new LocalEvent();
        event3.setId("test_multiday_event");
        event3.setTitle("Test Multi-Day Event");
        event3.setDescription("This spans multiple days");
        event3.setStartTime(today.atTime(8, 0));
        event3.setEndTime(today.plusDays(2).atTime(18, 0));
        event3.setEventType(EventType.MAINTENANCE);
        event3.setPriority(EventPriority.HIGH);
        event3.setAllDay(false);
        testEvents.add(event3);

        // Test event 4: Custom properties
        LocalEvent event4 = new LocalEvent();
        event4.setId("test_custom_props");
        event4.setTitle("Test Custom Properties");
        event4.setDescription("Event with custom properties");
        event4.setStartTime(today.plusDays(1).atTime(14, 30));
        event4.setEndTime(today.plusDays(1).atTime(16, 30));
        event4.setEventType(EventType.MEETING);
        event4.setPriority(EventPriority.NORMAL);

        Map<String, String> customProps = new HashMap<>();
        customProps.put("department", "Engineering");
        customProps.put("room", "Conference Room A");
        customProps.put("attendees", "10");
        event4.setCustomProperties(customProps);
        testEvents.add(event4);

        Log.d(TAG, "Created " + testEvents.size() + " test events");
        return testEvents;
    }

    /**
     * Test JSON export/import cycle
     */
    public static void testJsonCycle(Context context, List<LocalEvent> events) {
        Log.d(TAG, "Testing JSON export/import cycle with " + events.size() + " events");

        try {
            // Test export
            ExportManager exportManager = new ExportManager(context);
            ExportManager.ExportOptions exportOptions = ExportManager.ExportOptions.createDefault();
            exportOptions.packageName = "Test Export";
            exportOptions.packageDescription = "Testing JSON export/import cycle";

            // Create temporary file for testing
            File tempFile = new File(context.getCacheDir(), "test_export.json");

            exportManager.exportToFile(events, tempFile.getAbsolutePath(), exportOptions,
                    new ExportManager.ExportCallback() {
                        @Override
                        public void onExportComplete(ExportManager.ExportResult result) {
                            if (result.success) {
                                Log.d(TAG, "Export test successful: " + result.getSummary());
                                // Test import
                                testImportFromFile(context, tempFile);
                            } else {
                                Log.e(TAG, "Export test failed");
                            }
                        }

                        @Override
                        public void onExportProgress(int processed, int total, String currentEvent) {
                            Log.d(TAG, "Export progress: " + processed + "/" + total + " - " + currentEvent);
                        }

                        @Override
                        public void onExportError(String error, Exception exception) {
                            Log.e(TAG, "Export test error: " + error, exception);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "JSON cycle test failed", e);
        }
    }

    /**
     * Test import from file
     */
    private static void testImportFromFile(Context context, File testFile) {
        try {
            String jsonContent = readFileContent(testFile);

            EventPackageManagerExtension.importFromJsonString(
                    null, // Package manager
                    jsonContent,
                    "Test Import",
                    new EventPackageManagerExtension.ImportCallback() {
                        @Override
                        public void onSuccess(int importedCount, String message) {
                            Log.d(TAG, "Import test successful: " + message);

                            // Verify imported events
                            EventDao eventDao = EventsDatabase.getInstance(context).eventDao();
                            List<LocalEvent> importedEvents = eventDao.getAllEvents();
                            Log.d(TAG, "Verification: found " + importedEvents.size() + " events after import");

                            // Cleanup
                            testFile.delete();
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Import test failed: " + error);
                        }
                    }
            );

        } catch (Exception e) {
            Log.e(TAG, "Import test failed", e);
        }
    }

    /**
     * Test backup/restore cycle
     */
    public static void testBackupRestore(Context context, List<LocalEvent> events) {
        Log.d(TAG, "Testing backup/restore cycle");

        BackupManager backupManager = new BackupManager(context);

        // Test backup creation
        backupManager.performManualBackup(events, new BackupManager.BackupCallback() {
            @Override
            public void onBackupComplete(BackupManager.BackupResult result) {
                if (result.success) {
                    Log.d(TAG, "Backup test successful: " + result.eventsCount + " events backed up");

                    // Test restore
                    testRestore(context, result.backupFilePath);
                } else {
                    Log.e(TAG, "Backup test failed");
                }
            }

            @Override
            public void onBackupError(String error, Exception exception) {
                Log.e(TAG, "Backup test error: " + error, exception);
            }
        });
    }

    /**
     * Test restore operation
     */
    private static void testRestore(Context context, String backupFilePath) {
        RestoreManager restoreManager = new RestoreManager(context);
        RestoreManager.RestoreOptions options = RestoreManager.RestoreOptions.createDefault();

        restoreManager.restoreFromBackup(backupFilePath, options,
                new RestoreManager.RestoreCallback() {
                    @Override
                    public void onRestoreComplete(RestoreManager.RestoreResult result) {
                        if (result.success) {
                            Log.d(TAG, "Restore test successful: " + result.getSummary());
                        } else {
                            Log.e(TAG, "Restore test failed: " + result.getSummary());
                        }
                    }

                    @Override
                    public void onRestoreProgress(int processed, int total, String currentEvent) {
                        Log.d(TAG, "Restore progress: " + processed + "/" + total + " - " + currentEvent);
                    }

                    @Override
                    public void onRestoreError(String error, Exception exception) {
                        Log.e(TAG, "Restore test error: " + error, exception);
                    }
                });
    }

    /**
     * Run all tests
     */
    public static void runAllTests(Context context) {
        Log.d(TAG, "=== STARTING EVENTS SYSTEM TESTS ===");

        List<LocalEvent> testEvents = createTestEvents();

        // Test 1: JSON cycle
        testJsonCycle(context, testEvents);

        // Test 2: Backup/restore cycle
        testBackupRestore(context, testEvents);

        // Test 3: Event DAO operations
        testRoomDao(context, testEvents);

        Log.d(TAG, "=== EVENTS SYSTEM TESTS COMPLETED ===");
    }

    /**
     * Test Room DAO operations
     */
    public static void testRoomDao(Context context, List<LocalEvent> testEvents) {
        Log.d(TAG, "Testing Room DAO operations");

        EventDao dao = EventsDatabase.getInstance(context).eventDao();

        // Run tests in background thread (Room requirement)
        new Thread(() -> {
            try {
                // Test insertions
                for (LocalEvent event : testEvents) {
                    dao.insertEvent(event);
                }

                // Test retrieval
                List<LocalEvent> allEvents = dao.getAllEvents();
                Log.d(TAG, "DAO test: inserted " + testEvents.size() + ", retrieved " + allEvents.size());

                // Test date filtering
                LocalDate today = LocalDate.now();
                List<LocalEvent> todayEvents = dao.getEventsForDate(today.atStartOfDay(), today.atTime(23, 59));
                Log.d(TAG, "DAO test: found " + todayEvents.size() + " events for today");

                // Test cleanup
                dao.deleteAllLocalEvents();
                List<LocalEvent> finalEvents = dao.getAllEvents();
                Log.d(TAG, "DAO test: " + finalEvents.size() + " events after cleanup (should be 0)");

            } catch (Exception e) {
                Log.e(TAG, "Room DAO test failed", e);
            }
        }).start();
    }

    /**
     * Helper method to read file content
     */
    private static String readFileContent(File file) throws Exception {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        }
        return content.toString();
    }
}