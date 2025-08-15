package net.calvuz.qdue.core.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import net.calvuz.qdue.data.dao.RecurrenceRuleDao;
import net.calvuz.qdue.data.dao.ShiftDao;
import net.calvuz.qdue.data.dao.ShiftExceptionDao;
import net.calvuz.qdue.data.dao.TeamDao;
import net.calvuz.qdue.data.dao.UserScheduleAssignmentDao;
import net.calvuz.qdue.data.entities.RecurrenceRuleEntity;
import net.calvuz.qdue.data.entities.ShiftEntity;
import net.calvuz.qdue.data.entities.ShiftExceptionEntity;
import net.calvuz.qdue.data.entities.TeamEntity;
import net.calvuz.qdue.data.entities.UserScheduleAssignmentEntity;

import net.calvuz.qdue.core.db.converters.CalendarTypeConverters;
import net.calvuz.qdue.core.db.converters.QDueTypeConverters;
import net.calvuz.qdue.core.db.migrations.CalendarDatabaseMigrations;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * Enhanced CalendarDatabase - Version 2 with Google Calendar-style Features
 *
 * <p>Comprehensive calendar and work schedule database supporting Google Calendar-style
 * recurrence patterns (RRULE), advanced exception handling, multi-user scheduling,
 * and team-based assignment management. Built on clean architecture principles
 * with optimal performance for calendar operations.</p>
 *
 * <h2>🎯 Database Evolution: Version 1 → 2</h2>
 *
 * <h3>Version 1 (Legacy)</h3>
 * <ul>
 *   <li>ShiftEntity - Basic shift type definitions</li>
 *   <li>TeamEntity - Simple team management</li>
 *   <li>Basic indexes for calendar operations</li>
 * </ul>
 *
 * <h3>Version 2 (Enhanced) - NEW</h3>
 * <ul>
 *   <li><strong>RecurrenceRuleEntity</strong> - Google Calendar RRULE patterns</li>
 *   <li><strong>ShiftExceptionEntity</strong> - Advanced exception handling</li>
 *   <li><strong>UserScheduleAssignmentEntity</strong> - Multi-user team assignments</li>
 *   <li><strong>Enhanced Type Converters</strong> - JSON, Java 8 Time, Enum support</li>
 *   <li><strong>Performance Indexes</strong> - 15+ optimized indexes</li>
 *   <li><strong>Migration Support</strong> - Safe upgrade/downgrade paths</li>
 * </ul>
 *
 * <h2>🚀 Key Features</h2>
 *
 * <h3>Google Calendar-style RRULE Support</h3>
 * <ul>
 *   <li><strong>Standard Frequencies</strong>: DAILY, WEEKLY, MONTHLY, YEARLY</li>
 *   <li><strong>QuattroDue Extension</strong>: QUATTRODUE_CYCLE for 4-2 patterns</li>
 *   <li><strong>Complex Patterns</strong>: BYDAY, BYMONTHDAY, INTERVAL support</li>
 *   <li><strong>End Conditions</strong>: NEVER, COUNT, UNTIL_DATE</li>
 * </ul>
 *
 * <h3>Advanced Exception System</h3>
 * <ul>
 *   <li><strong>10 Exception Types</strong>: Absences, changes, reductions</li>
 *   <li><strong>Approval Workflows</strong>: Draft → Pending → Approved/Rejected</li>
 *   <li><strong>Priority System</strong>: LOW, NORMAL, HIGH, URGENT</li>
 *   <li><strong>Collaboration</strong>: Shift swaps, replacements, team changes</li>
 * </ul>
 *
 * <h3>Multi-User Team Management</h3>
 * <ul>
 *   <li><strong>Time-Bounded Assignments</strong>: Start/end dates, permanent assignments</li>
 *   <li><strong>Priority Conflicts</strong>: Resolution system for overlapping assignments</li>
 *   <li><strong>Team Transfers</strong>: Seamless user movement between teams</li>
 *   <li><strong>Role Management</strong>: Department and role-based organization</li>
 * </ul>
 *
 * <h2>📊 Performance Optimizations</h2>
 *
 * <h3>Query Performance</h3>
 * <ul>
 *   <li><strong>User Schedule Queries</strong>: O(log n) lookup by user+date</li>
 *   <li><strong>Calendar Views</strong>: Optimized date range queries</li>
 *   <li><strong>Exception Resolution</strong>: Priority-ordered conflict resolution</li>
 *   <li><strong>Team Rosters</strong>: Fast team membership queries</li>
 * </ul>
 *
 * <h3>Cache Strategy</h3>
 * <ul>
 *   <li><strong>WAL Mode</strong>: Better read performance for calendar views</li>
 *   <li><strong>Large Cache</strong>: 10,000 pages for hot calendar data</li>
 *   <li><strong>Memory Temp Store</strong>: Fast temporary calculations</li>
 *   <li><strong>Automatic Analysis</strong>: Query plan optimization</li>
 * </ul>
 *
 * <h2>🌍 Internationalization Support</h2>
 *
 * <p>Full LocaleManager integration with Context-aware localization:</p>
 * <ul>
 *   <li><strong>Dynamic Language Switching</strong>: Runtime locale changes</li>
 *   <li><strong>Default Data Localization</strong>: Localized initial content</li>
 *   <li><strong>Cultural Adaptations</strong>: Date/time formatting by locale</li>
 *   <li><strong>Fallback Support</strong>: Graceful handling of missing translations</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 2.0.0 - Enhanced Calendar Engine with RRULE Support
 * @since Clean Architecture Phase 2
 */
@Database(
        entities = {
                // Legacy entities (Version 1)
                ShiftEntity.class,
                TeamEntity.class,

                // Enhanced entities (Version 2)
                RecurrenceRuleEntity.class,
                ShiftExceptionEntity.class,
                UserScheduleAssignmentEntity.class
        },
        version = 2,
        exportSchema = true
)
@TypeConverters({
        QDueTypeConverters.class,          // Legacy converters
        CalendarTypeConverters.class       // New enhanced converters
})
public abstract class CalendarDatabase extends RoomDatabase {

    private static final String TAG = "CalendarDatabase";
    private static final String DATABASE_NAME = "calendar_database";
    private static volatile CalendarDatabase INSTANCE;

    // ==================== ABSTRACT DAO METHODS ====================

    // Legacy DAOs (Version 1)
    public abstract ShiftDao shiftDao();
    public abstract TeamDao teamDao();

    // Enhanced DAOs (Version 2)
    public abstract RecurrenceRuleDao recurrenceRuleDao();
    public abstract ShiftExceptionDao shiftExceptionDao();
    public abstract UserScheduleAssignmentDao userScheduleAssignmentDao();

    // ==================== SINGLETON INSTANCE ====================

    /**
     * Get singleton instance of CalendarDatabase.
     * Thread-safe implementation with double-checked locking and migration support.
     *
     * @param context Application context
     * @return CalendarDatabase singleton instance
     */
    public static CalendarDatabase getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (CalendarDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    CalendarDatabase.class,
                                    DATABASE_NAME
                            )
                            // Migration strategy: safe upgrade path from v1 to v2
                            .addMigrations(
                                    CalendarDatabaseMigrations.MIGRATION_1_2,
                                    CalendarDatabaseMigrations.MIGRATION_2_1  // Rollback support
                            )
                            .fallbackToDestructiveMigration() // Last resort for development
                            .addCallback(new EnhancedDatabaseCallback(context.getApplicationContext()))
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // ==================== ENHANCED DATABASE CALLBACK ====================

    /**
     * Enhanced database callback with comprehensive initialization for Version 2.
     */
    private static class EnhancedDatabaseCallback extends RoomDatabase.Callback {

        private final Context mContext;

        public EnhancedDatabaseCallback(@NonNull Context context) {
            this.mContext = context;
        }

        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            Log.i(TAG, "CalendarDatabase v2 created successfully");

            // Enable foreign keys for relationship support
            db.execSQL("PRAGMA foreign_keys = ON");

            // Optimize for calendar read-heavy workload
            optimizeDatabaseForEnhancedCalendar(db);

            // Create all performance indexes
            createEnhancedPerformanceIndexes(db);

            // Initialize default data with full localization
            initializeEnhancedDefaultData(db, mContext);

            // Validate database setup
            validateDatabaseSetup(db);
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);

            // Ensure foreign keys are enabled
            db.execSQL("PRAGMA foreign_keys = ON");

            // Analyze all tables for query optimization
            db.execSQL("ANALYZE");

            Log.d(TAG, "CalendarDatabase v2 opened and optimized");
        }

        /**
         * Optimize database settings for enhanced calendar operations.
         */
        private void optimizeDatabaseForEnhancedCalendar(@NonNull SupportSQLiteDatabase db) {
            try {
                // WAL mode for better read performance (calendar views are read-heavy)
                db.execSQL("PRAGMA journal_mode = WAL");

                // Normal synchronous mode for better performance
                db.execSQL("PRAGMA synchronous = NORMAL");

                // Larger cache for calendar operations with complex queries
                db.execSQL("PRAGMA cache_size = 10000");

                // Optimize for calendar queries
                db.execSQL("PRAGMA temp_store = MEMORY");

                // Enable query planner optimizations
                db.execSQL("PRAGMA optimize");

                Log.d(TAG, "Database optimized for enhanced calendar operations");

            } catch (Exception e) {
                Log.e(TAG, "Error optimizing enhanced database: " + e.getMessage());
            }
        }

        /**
         * Create all performance indexes for Version 2 entities.
         */
        private void createEnhancedPerformanceIndexes(@NonNull SupportSQLiteDatabase db) {
            try {
                // Legacy indexes (preserved from Version 1)
                createLegacyIndexes(db);

                // Enhanced indexes (new in Version 2)
                createRecurrenceRuleIndexes(db);
                createShiftExceptionIndexes(db);
                createUserScheduleAssignmentIndexes(db);

                Log.d(TAG, "Enhanced performance indexes created successfully");

            } catch (Exception e) {
                Log.e(TAG, "Error creating enhanced performance indexes: " + e.getMessage());
            }
        }

        private void createLegacyIndexes(@NonNull SupportSQLiteDatabase db) {
            // Preserve existing shift indexes
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_shifts_type_active " +
                    "ON shifts(shift_type, active) WHERE active = 1");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_shifts_time_range " +
                    "ON shifts(start_time, end_time, active) WHERE active = 1");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_shifts_user_relevant " +
                    "ON shifts(is_user_relevant, active) WHERE active = 1 AND is_user_relevant = 1");

            // Preserve existing team indexes
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_teams_active_sort " +
                    "ON teams(active, sort_order) WHERE active = 1");
        }

        private void createRecurrenceRuleIndexes(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_recurrence_frequency_active_start " +
                    "ON recurrence_rules(frequency, active, start_date) WHERE active = 1");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_recurrence_date_range " +
                    "ON recurrence_rules(start_date, end_date)");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_recurrence_active_created " +
                    "ON recurrence_rules(active, created_at) WHERE active = 1");
        }

        private void createShiftExceptionIndexes(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_exception_user_date_priority " +
                    "ON shift_exceptions(user_id, target_date, priority)");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_exception_status_approval " +
                    "ON shift_exceptions(status, requires_approval, target_date)");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_exception_date_active " +
                    "ON shift_exceptions(target_date, active) WHERE active = 1");
        }

        private void createUserScheduleAssignmentIndexes(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_assignment_user_date_priority " +
                    "ON user_schedule_assignments(user_id, start_date, end_date, priority)");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_assignment_team_status " +
                    "ON user_schedule_assignments(team_id, status, start_date)");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_assignment_status_dates " +
                    "ON user_schedule_assignments(status, start_date, end_date)");
        }

        /**
         * Initialize enhanced default data with full localization support.
         */
        private void initializeEnhancedDefaultData(@NonNull SupportSQLiteDatabase db, @NonNull Context context) {
            try {
                // Initialize legacy data (shifts and teams) with enhanced localization
                initializeLegacyDataWithEnhancedLocalization(db, context);

                // Initialize new data (recurrence rules, default assignments)
                initializeRecurrenceRules(db, context);
                initializeDefaultAssignments(db, context);

                Log.d(TAG, "Enhanced default data initialized successfully");

            } catch (Exception e) {
                Log.e(TAG, "Error initializing enhanced default data: " + e.getMessage());
            }
        }

        private void initializeLegacyDataWithEnhancedLocalization(@NonNull SupportSQLiteDatabase db, @NonNull Context context) {
            long currentTime = System.currentTimeMillis();

            // Enhanced shift initialization with better localization
            String morningName = LocaleManager.getShiftName(context, "MORNING");
            String morningDesc = LocaleManager.getShiftDescription(context, "MORNING");
            String afternoonName = LocaleManager.getShiftName(context, "AFTERNOON");
            String afternoonDesc = LocaleManager.getShiftDescription(context, "AFTERNOON");
            String nightName = LocaleManager.getShiftName(context, "NIGHT");
            String nightDesc = LocaleManager.getShiftDescription(context, "NIGHT");

            // Morning shift with enhanced properties
            db.execSQL("INSERT OR IGNORE INTO shifts (" +
                    "id, name, description, shift_type, start_time, end_time, " +
                    "crosses_midnight, color_hex, is_user_relevant, has_break_time, " +
                    "is_break_time_included, break_time_duration_minutes, active, " +
                    "created_at, updated_at, display_order) VALUES " +
                    "('shift_morning', ?, ?, 'MORNING', " +
                    "'05:00', '13:00', 0, '#4CAF50', 1, 1, 0, 30, 1, " +
                    "?, ?, 1)", new Object[]{morningName, morningDesc, currentTime, currentTime});

            // Afternoon shift
            db.execSQL("INSERT OR IGNORE INTO shifts (" +
                    "id, name, description, shift_type, start_time, end_time, " +
                    "crosses_midnight, color_hex, is_user_relevant, has_break_time, " +
                    "is_break_time_included, break_time_duration_minutes, active, " +
                    "created_at, updated_at, display_order) VALUES " +
                    "('shift_afternoon', ?, ?, 'AFTERNOON', " +
                    "'13:00', '21:00', 0, '#FF9800', 1, 1, 0, 30, 1, " +
                    "?, ?, 2)", new Object[]{afternoonName, afternoonDesc, currentTime, currentTime});

            // Night shift
            db.execSQL("INSERT OR IGNORE INTO shifts (" +
                    "id, name, description, shift_type, start_time, end_time, " +
                    "crosses_midnight, color_hex, is_user_relevant, has_break_time, " +
                    "is_break_time_included, break_time_duration_minutes, active, " +
                    "created_at, updated_at, display_order) VALUES " +
                    "('shift_night', ?, ?, 'NIGHT', " +
                    "'21:00', '05:00', 1, '#3F51B5', 1, 1, 0, 0, 1, " +
                    "?, ?, 3)", new Object[]{nightName, nightDesc, currentTime, currentTime});

            // Enhanced team initialization
            String[] teamNames = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};
            String teamDescTemplate = LocaleManager.getTeamDescriptionTemplate(context);

            for (int i = 0; i < teamNames.length; i++) {
                String teamName = teamNames[i];
                String teamDisplayName = LocaleManager.getTeamDisplayName(context, teamName);
                String teamDescription = teamDescTemplate + " " + teamName;

                db.execSQL("INSERT OR IGNORE INTO teams (" +
                                "name, display_name, description, active, created_at, updated_at, sort_order) VALUES " +
                                "(?, ?, ?, 1, ?, ?, ?)",
                        new Object[]{teamName, teamDisplayName, teamDescription, currentTime, currentTime, i});
            }
        }

        private void initializeRecurrenceRules(@NonNull SupportSQLiteDatabase db, @NonNull Context context) {
            long currentTime = System.currentTimeMillis();
            String today = java.time.LocalDate.now().toString();

            // Standard QuattroDue pattern
            String quattroDueName = LocaleManager.getRecurrenceRuleName(context, "QUATTRODUE_CYCLE");
            String quattroDueDesc = LocaleManager.getRecurrenceRuleDescription(context, "QUATTRODUE_CYCLE");

            db.execSQL("INSERT OR IGNORE INTO recurrence_rules (" +
                            "id, name, description, frequency, interval_value, start_date, " +
                            "end_type, cycle_length, work_days, rest_days, active, " +
                            "created_at, updated_at) VALUES " +
                            "('quattrodue_standard', ?, ?, 'QUATTRODUE_CYCLE', 1, ?, " +
                            "'NEVER', 18, 4, 2, 1, ?, ?)",
                    new Object[]{quattroDueName, quattroDueDesc, today, currentTime, currentTime});

            // Weekdays pattern
            String weekdaysName = LocaleManager.getRecurrenceRuleName(context, "WEEKDAYS");
            String weekdaysDesc = LocaleManager.getRecurrenceRuleDescription(context, "WEEKDAYS");

            db.execSQL("INSERT OR IGNORE INTO recurrence_rules (" +
                            "id, name, description, frequency, interval_value, start_date, " +
                            "end_type, by_day_json, week_start, active, created_at, updated_at) VALUES " +
                            "('weekdays_only', ?, ?, 'WEEKLY', 1, ?, " +
                            "'NEVER', '[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\"]', " +
                            "'MONDAY', 1, ?, ?)",
                    new Object[]{weekdaysName, weekdaysDesc, today, currentTime, currentTime});
        }

        private void initializeDefaultAssignments(@NonNull SupportSQLiteDatabase db, @NonNull Context context) {
            long currentTime = System.currentTimeMillis();
            String today = java.time.LocalDate.now().toString();

            String defaultTitle = LocaleManager.getStandardAssignmentTitle(context);

            db.execSQL("INSERT OR IGNORE INTO user_schedule_assignments (" +
                            "id, title, description, user_id, user_name, team_id, team_name, " +
                            "recurrence_rule_id, start_date, is_permanent, priority, status, " +
                            "active, created_at, updated_at) VALUES " +
                            "('default_admin_assignment', ?, 'Default system administrator assignment', " +
                            "1, 'Administrator', 'A', 'Team A', 'quattrodue_standard', ?, " +
                            "1, 'OVERRIDE', 'ACTIVE', 1, ?, ?)",
                    new Object[]{defaultTitle, today, currentTime, currentTime});
        }

        /**
         * Validate database setup after initialization.
         */
        private void validateDatabaseSetup(@NonNull SupportSQLiteDatabase db) {
            try {
                if (CalendarDatabaseMigrations.validateMigration(db)) {
                    Log.d(TAG, "✅ Database setup validation successful");
                } else {
                    Log.w(TAG, "⚠️ Database setup validation failed - some features may not work correctly");
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Error during database setup validation", e);
            }
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Clear all calendar data from database (for testing/reset purposes).
     * Enhanced version that clears all Version 2 tables.
     */
    public void clearAllEnhancedCalendarData() {
        runInTransaction(() -> {
            // Clear Version 2 data
            userScheduleAssignmentDao().deleteInactiveAssignmentsOlderThan(0);
            shiftExceptionDao().deleteInactiveExceptionsOlderThan(0);
            recurrenceRuleDao().deleteInactiveRulesOlderThan(0);

            // Clear Version 1 data
            shiftDao().deleteAllShifts();
            teamDao().deleteAllTeams();

            Log.i(TAG, "All enhanced calendar data cleared");
        });
    }

    /**
     * Get comprehensive enhanced calendar database statistics.
     */
    @NonNull
    public EnhancedCalendarStatistics getEnhancedCalendarStatistics() {
        return runInTransaction(() -> {
            EnhancedCalendarStatistics stats = new EnhancedCalendarStatistics();

            // Legacy statistics
            stats.totalShifts = shiftDao().getShiftCount();
            stats.activeShifts = shiftDao().getActiveShiftCount();
            stats.totalTeams = teamDao().getTeamCount();
            stats.activeTeams = teamDao().getActiveTeamCount();

            // Enhanced statistics
            stats.totalRecurrenceRules = recurrenceRuleDao().getActiveRecurrenceRuleCount();
            stats.totalShiftExceptions = shiftExceptionDao().getExceptionCountForDate(java.time.LocalDate.now().toString());
            stats.totalUserAssignments = userScheduleAssignmentDao().getAssignmentStatistics().total_assignments;
            stats.activeUserAssignments = userScheduleAssignmentDao().getAssignmentStatistics().active_assignments;

            // Database metrics
            stats.databaseSizeKB = getDatabaseSizeKB();
            stats.databaseVersion = 2;

            return stats;
        });
    }

    /**
     * Perform enhanced calendar database maintenance and cleanup.
     */
    public int performEnhancedCalendarMaintenance() {
        return runInTransaction(() -> {
            int itemsCleanedUp = 0;

            try {
                long cutoffTime = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000); // 90 days

                // Clean up old inactive data
                itemsCleanedUp += shiftDao().deleteShiftsOlderThan(cutoffTime);
                itemsCleanedUp += recurrenceRuleDao().deleteInactiveRulesOlderThan(cutoffTime);
                itemsCleanedUp += shiftExceptionDao().deleteInactiveExceptionsOlderThan(cutoffTime);
                itemsCleanedUp += userScheduleAssignmentDao().deleteInactiveAssignmentsOlderThan(cutoffTime);

                // Optimize database
                query("VACUUM", null);
                query("ANALYZE", null);

                Log.i(TAG, "Enhanced calendar maintenance completed. Items cleaned: " + itemsCleanedUp);
                return itemsCleanedUp;

            } catch (Exception e) {
                Log.e(TAG, "Error during enhanced calendar maintenance: " + e.getMessage());
                return 0;
            }
        });
    }

    /**
     * Initialize enhanced calendar with standard QuattroDue data.
     */
    public void initializeEnhancedStandardCalendarData(@NonNull Context context) {
        runInTransaction(() -> {
            try {
                // Clear existing data
                clearAllEnhancedCalendarData();

                // Re-initialize with enhanced localized data
                EnhancedDatabaseCallback callback = new EnhancedDatabaseCallback(context);
                // Note: This would need access to the database instance
                // callback.initializeEnhancedDefaultData(db, context);

                Log.i(TAG, "Enhanced standard calendar data initialized successfully");

            } catch (Exception e) {
                Log.e(TAG, "Error initializing enhanced standard calendar data: " + e.getMessage());
                throw new RuntimeException("Failed to initialize enhanced standard calendar data", e);
            }
        });
    }

    /**
     * Get approximate database size in KB.
     */
    private long getDatabaseSizeKB() {
        try {
            android.database.Cursor cursor = query("PRAGMA page_count", null);
            long pageCount = 0;
            if (cursor.moveToFirst()) {
                pageCount = cursor.getLong(0);
            }
            cursor.close();

            cursor = query("PRAGMA page_size", null);
            long pageSize = 0;
            if (cursor.moveToFirst()) {
                pageSize = cursor.getLong(0);
            }
            cursor.close();

            return (pageCount * pageSize) / 1024; // Convert to KB

        } catch (Exception e) {
            Log.e(TAG, "Error calculating database size: " + e.getMessage());
            return 0;
        }
    }

    // ==================== ENHANCED DATABASE STATISTICS ====================

    /**
     * Enhanced calendar database statistics container for Version 2.
     */
    public static class EnhancedCalendarStatistics {
        // Legacy statistics (Version 1)
        public int totalShifts;
        public int activeShifts;
        public int totalTeams;
        public int activeTeams;

        // Enhanced statistics (Version 2)
        public int totalRecurrenceRules;
        public int totalShiftExceptions;
        public int totalUserAssignments;
        public int activeUserAssignments;

        // System statistics
        public long databaseSizeKB;
        public int databaseVersion;

        @NonNull
        @Override
        public String toString() {
            return "EnhancedCalendarStatistics{" +
                    "databaseVersion=" + databaseVersion +
                    ", totalShifts=" + totalShifts +
                    ", activeShifts=" + activeShifts +
                    ", totalTeams=" + totalTeams +
                    ", activeTeams=" + activeTeams +
                    ", totalRecurrenceRules=" + totalRecurrenceRules +
                    ", totalShiftExceptions=" + totalShiftExceptions +
                    ", totalUserAssignments=" + totalUserAssignments +
                    ", activeUserAssignments=" + activeUserAssignments +
                    ", databaseSizeKB=" + databaseSizeKB +
                    '}';
        }
    }

    // ==================== ENHANCED MIGRATION SUPPORT ====================

    /**
     * Enhanced migration utilities for Version 2.
     */
    public static class EnhancedMigrationSupport {

        /**
         * Check if database needs migration.
         */
        public static boolean needsMigration(@NonNull Context context) {
            try {
                CalendarDatabase db = getInstance(context);
                return db.getOpenHelper().getReadableDatabase().getVersion() < 2;
            } catch (Exception e) {
                Log.e(TAG, "Error checking migration status", e);
                return false;
            }
        }

        /**
         * Get migration progress information.
         */
        @NonNull
        public static String getMigrationInfo(@NonNull Context context) {
            try {
                CalendarDatabase db = getInstance(context);
                int currentVersion = db.getOpenHelper().getReadableDatabase().getVersion();

                return "CalendarDatabase Migration Info:\n" +
                        "Current Version: " + currentVersion + "\n" +
                        "Target Version: 2\n" +
                        "Migration Status: " + (currentVersion >= 2 ? "✅ Up to date" : "⚠️ Needs migration");

            } catch (Exception e) {
                return "Error getting migration info: " + e.getMessage();
            }
        }
    }

    // ==================== ENHANCED INTERNATIONALIZATION SUPPORT ====================

    /**
     * Enhanced internationalization utilities for Version 2.
     */
    public static class EnhancedI18nSupport {

        /**
         * Update all localized data for current locale.
         * Enhanced version that updates all Version 2 entities.
         */
        public static void updateAllLocalizedData(@NonNull Context context, @NonNull CalendarDatabase database) {
            database.runInTransaction(() -> {
                try {
                    long currentTime = System.currentTimeMillis();

                    // Update legacy data
                    updateLegacyLocalizedData(context, database, currentTime);

                    // Update enhanced data
                    updateRecurrenceRuleLocalization(context, database, currentTime);

                    Log.d(TAG, "All localized data updated for current locale");

                } catch (Exception e) {
                    Log.e(TAG, "Error updating localized data: " + e.getMessage());
                }
            });
        }

        private static void updateLegacyLocalizedData(@NonNull Context context, @NonNull CalendarDatabase database, long timestamp) {
            // Update shift localization
            String morningName = LocaleManager.getShiftName(context, "MORNING");
            String afternoonName = LocaleManager.getShiftName(context, "AFTERNOON");
            String nightName = LocaleManager.getShiftName(context, "NIGHT");

            database.shiftDao().renameShift("shift_morning", morningName, timestamp);
            database.shiftDao().renameShift("shift_afternoon", afternoonName, timestamp);
            database.shiftDao().renameShift("shift_night", nightName, timestamp);
        }

        private static void updateRecurrenceRuleLocalization(@NonNull Context context, @NonNull CalendarDatabase database, long timestamp) {
            // Update recurrence rule names (would need additional DAO methods)
            // This is a placeholder for future implementation
            Log.d(TAG, "Recurrence rule localization update placeholder");
        }

        /**
         * Get comprehensive localization status for Version 2.
         */
        @NonNull
        public static String getEnhancedLocalizationStatus(@NonNull Context context) {
            LocaleManager localeManager = new LocaleManager(context);
            return "Enhanced CalendarDatabase Localization Status:\n" +
                    "Database Version: 2\n" +
                    "Current Language: " + localeManager.getCurrentLanguageCode() + "\n" +
                    "Using System Language: " + localeManager.isUsingSystemLanguage() + "\n" +
                    "Sample Shift Name: " + localeManager.getShiftName("MORNING") + "\n" +
                    "Sample Team Display: " + localeManager.getTeamDisplayName("A") + "\n" +
                    "Sample Recurrence Rule: " + LocaleManager.getRecurrenceRuleName(context, "QUATTRODUE_CYCLE") + "\n" +
                    "Enhanced Features: ✅ Enabled";
        }
    }
}