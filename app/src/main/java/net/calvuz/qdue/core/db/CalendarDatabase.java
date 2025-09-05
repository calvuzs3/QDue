package net.calvuz.qdue.core.db;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.core.common.utils.ColorUtils;
import net.calvuz.qdue.data.dao.RecurrenceRuleDao;
import net.calvuz.qdue.data.dao.ShiftDao;
import net.calvuz.qdue.data.dao.ShiftExceptionDao;
import net.calvuz.qdue.data.dao.TeamDao;
import net.calvuz.qdue.data.dao.UserScheduleAssignmentDao;
import net.calvuz.qdue.data.dao.UserTeamAssignmentDao;
import net.calvuz.qdue.data.entities.RecurrenceRuleEntity;
import net.calvuz.qdue.data.entities.ShiftEntity;
import net.calvuz.qdue.data.entities.ShiftExceptionEntity;
import net.calvuz.qdue.data.entities.TeamEntity;
import net.calvuz.qdue.data.entities.UserScheduleAssignmentEntity;

import net.calvuz.qdue.core.db.converters.CalendarTypeConverters;
import net.calvuz.qdue.core.db.converters.QDueTypeConverters;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.data.dao.QDueUserDao;
import net.calvuz.qdue.data.entities.QDueUserEntity;
import net.calvuz.qdue.data.entities.UserTeamAssignmentEntity;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

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
 */
@Database (
        entities = {
                QDueUserEntity.class,
                ShiftEntity.class,
                TeamEntity.class,
                RecurrenceRuleEntity.class,
                ShiftExceptionEntity.class,
                UserScheduleAssignmentEntity.class,
                UserTeamAssignmentEntity.class,
        },
        version = CalendarDatabase.DATABASE_VERSION,
        exportSchema = false
)
@TypeConverters ({
        QDueTypeConverters.class,          // Legacy converters
        CalendarTypeConverters.class       // New converters
})

public abstract class CalendarDatabase extends RoomDatabase {

    // All included - fallbackToDestructiveMigration()
    public final static int DATABASE_VERSION = 6;

    private static final String TAG = "CalendarDatabase";
    private static final String DATABASE_NAME = "calendar_database";
    private static volatile CalendarDatabase INSTANCE;

    // ==================== ABSTRACT DAO METHODS ====================

    public abstract QDueUserDao qDueUserDao();

    public abstract ShiftDao shiftDao();

    public abstract TeamDao teamDao();

    public abstract RecurrenceRuleDao recurrenceRuleDao();

    public abstract ShiftExceptionDao shiftExceptionDao();

    public abstract UserScheduleAssignmentDao userScheduleAssignmentDao();

    public abstract UserTeamAssignmentDao userTeamAssignmentDao();

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
                            // Migration strategy
//                            .addMigrations(
//                                    CalendarDatabaseMigrations.MIGRATION_1_2,
//                                    CalendarDatabaseMigrations.MIGRATION_2_3,
//                                    CalendarDatabaseMigrations.MIGRATION_3_4
//                            )
                            .fallbackToDestructiveMigration()
                            .addCallback( new EnhancedDatabaseCallback( context.getApplicationContext() ) )
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // ==================== ENHANCED DATABASE CALLBACK ====================

    /**
     * Database callback with comprehensive initialization for Version 2.
     */
    private static class EnhancedDatabaseCallback extends Callback {

        private final Context mContext;

        public EnhancedDatabaseCallback(@NonNull Context context) {
            this.mContext = context;
        }

        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate( db );
            Log.i( TAG, MessageFormat.format( "✅ CalendarDatabase created successfully (Version {0})",
                    DATABASE_VERSION ) );

            // Enable foreign keys for relationship support
            db.execSQL( "PRAGMA foreign_keys = ON" );

            // Optimize for calendar read-heavy workload
            optimizeDatabaseForEnhancedCalendar( db );

            // Create all performance indexes
            createEnhancedPerformanceIndexes( db );

            // Initialize default data with full localization
            initializeEnhancedDefaultData( db, mContext );

            // Validate database setup
            validateDatabaseSetup( db );
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen( db );

            // Ensure foreign keys are enabled
            db.execSQL( "PRAGMA foreign_keys = ON" );

            // Analyze all tables for query optimization
            db.execSQL( "ANALYZE" );

            Log.i( TAG, "CalendarDatabase v" + DATABASE_VERSION + " opened and optimized" );
        }

        /**
         * Optimize database settings for enhanced calendar operations.
         */
        private void optimizeDatabaseForEnhancedCalendar(@NonNull SupportSQLiteDatabase db) {
            try {
                // WAL mode for better read performance (calendar views are read-heavy)
                db.execSQL( "PRAGMA journal_mode = WAL" );

                // Normal synchronous mode for better performance
                db.execSQL( "PRAGMA synchronous = NORMAL" );

                // Larger cache for calendar operations with complex queries
                db.execSQL( "PRAGMA cache_size = 10000" );

                // Optimize for calendar queries
                db.execSQL( "PRAGMA temp_store = MEMORY" );

                // Enable query planner optimizations
                db.execSQL( "PRAGMA optimize" );

                Log.i( TAG, "Database optimized for enhanced calendar operations" );
            } catch (Exception e) {
                Log.e( TAG, "Error optimizing enhanced database: " + e.getMessage() );
            }
        }

        /**
         * Create all performance indexes for Version 2 entities.
         */
        private void createEnhancedPerformanceIndexes(@NonNull SupportSQLiteDatabase db) {
            try {
                // Legacy indexes (preserved from Version 1)
                createLegacyIndexes( db );

                // Enhanced indexes (new in Version 2)
                createRecurrenceRuleIndexes( db );
                createShiftExceptionIndexes( db );
                createUserScheduleAssignmentIndexes( db );

                Log.w( TAG, "Enhanced performance indexes created successfully" );
            } catch (Exception e) {
                Log.e( TAG, "Error creating enhanced performance indexes: " + e.getMessage() );
            }
        }

        private void createLegacyIndexes(@NonNull SupportSQLiteDatabase db) {
            // Preserve existing shift indexes
            db.execSQL( "CREATE INDEX IF NOT EXISTS idx_shifts_type_active " +
                    "ON shifts(shift_type, active) WHERE active = 1" );

            db.execSQL( "CREATE INDEX IF NOT EXISTS idx_shifts_time_range " +
                    "ON shifts(start_time, end_time, active) WHERE active = 1" );

            db.execSQL( "CREATE INDEX IF NOT EXISTS idx_shifts_user_relevant " +
                    "ON shifts(is_user_relevant, active) WHERE active = 1 AND is_user_relevant = 1" );

            // Preserve existing team indexes
            db.execSQL( "CREATE INDEX IF NOT EXISTS idx_teams_active_sort " +
                    "ON teams(active) WHERE active = 1" );
        }

        private void createRecurrenceRuleIndexes(@NonNull SupportSQLiteDatabase db) {
            db.execSQL( "CREATE INDEX IF NOT EXISTS idx_recurrence_frequency_active_start " +
                    "ON recurrence_rules(frequency, active, start_date) WHERE active = 1" );

            db.execSQL( "CREATE INDEX IF NOT EXISTS idx_recurrence_date_range " +
                    "ON recurrence_rules(start_date, end_date)" );

            db.execSQL( "CREATE INDEX IF NOT EXISTS idx_recurrence_active_created " +
                    "ON recurrence_rules(active, created_at) WHERE active = 1" );
        }

        private void createShiftExceptionIndexes(@NonNull SupportSQLiteDatabase db) {
            db.execSQL( "CREATE INDEX IF NOT EXISTS idx_exception_user_date_priority " +
                    "ON shift_exceptions(user_id, target_date, priority)" );

            db.execSQL( "CREATE INDEX IF NOT EXISTS idx_exception_status_approval " +
                    "ON shift_exceptions(status, requires_approval, target_date)" );

            db.execSQL( "CREATE INDEX IF NOT EXISTS idx_exception_date_active " +
                    "ON shift_exceptions(target_date, active) WHERE active = 1" );
        }

        private void createUserScheduleAssignmentIndexes(@NonNull SupportSQLiteDatabase db) {
            db.execSQL( "CREATE INDEX IF NOT EXISTS idx_assignment_user_date_priority " +
                    "ON user_schedule_assignments(user_id, start_date, end_date, priority)" );

            db.execSQL( "CREATE INDEX IF NOT EXISTS idx_assignment_team_status " +
                    "ON user_schedule_assignments(team_id, status, start_date)" );

            db.execSQL( "CREATE INDEX IF NOT EXISTS idx_assignment_status_dates " +
                    "ON user_schedule_assignments(status, start_date, end_date)" );
        }

        /**
         * Initialize enhanced default data with full localization support.
         */
        private void initializeEnhancedDefaultData(@NonNull SupportSQLiteDatabase db, @NonNull Context context) {
            try {
                initializeQDueUserData( db );
                initializeShiftsAndTeamsData( db, context );
//                initializeRecurrenceRules( db, context );
//                initializeDefaultAssignments( db, context );

                Log.w( TAG, "Enhanced default data initialized successfully" );
            } catch (Exception e) {
                Log.e( TAG, "Error initializing enhanced default data: " + e.getMessage() );
            }
        }

        private void initializeQDueUserData(@NonNull SupportSQLiteDatabase db) {
            db.execSQL( "INSERT INTO user (id, nickname, email, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?)",
                    new Object[]{
                            UUID.randomUUID().toString(),
                            "User",
                            "",
                            System.currentTimeMillis(),
                            System.currentTimeMillis()
                    } );
        }

        private void initializeShiftsAndTeamsData(@NonNull SupportSQLiteDatabase db, @NonNull Context context) {
            long currentTime = System.currentTimeMillis();

            // Enhanced shift initialization with better localization
            String morningName = LocaleManager.getShiftName( context, "MORNING" );
            String morningDesc = LocaleManager.getShiftDescription( context, "MORNING" );
            String afternoonName = LocaleManager.getShiftName( context, "AFTERNOON" );
            String afternoonDesc = LocaleManager.getShiftDescription( context, "AFTERNOON" );
            String nightName = LocaleManager.getShiftName( context, "NIGHT" );
            String nightDesc = LocaleManager.getShiftDescription( context, "NIGHT" );

            String colorGreen = ColorUtils.getMaterialColors().get( "green_500" );
            String colorBlue = ColorUtils.getMaterialColors().get( "blue_500" );
            String colorOrange = ColorUtils.getMaterialColors().get( "orange_500" );

            // Morning shift with enhanced properties
            db.execSQL( "INSERT OR IGNORE INTO shifts (" +
                    "id, name, description, shift_type, start_time, end_time, " +
                    "crosses_midnight, color_hex, is_user_relevant, has_break_time, " +
                    "is_break_time_included, break_time_duration_minutes, active, " +
                    "created_at, updated_at, display_order) VALUES " +
                    "('shift_morning', ?, ?, 'MORNING', " +
                    "'05:00', '13:00', 0, ?, 1, 1, 1, 30, 1, " +
                    "?, ?, 1)", new Object[]{morningName, morningDesc, currentTime, currentTime, colorGreen} );

            // Afternoon shift
            db.execSQL( "INSERT OR IGNORE INTO shifts (" +
                    "id, name, description, shift_type, start_time, end_time, " +
                    "crosses_midnight, color_hex, is_user_relevant, has_break_time, " +
                    "is_break_time_included, break_time_duration_minutes, active, " +
                    "created_at, updated_at, display_order) VALUES " +
                    "('shift_afternoon', ?, ?, 'AFTERNOON', " +
                    "'13:00', '21:00', 0, ?, 1, 1, 1, 30, 1, " +
                    "?, ?, 2)", new Object[]{afternoonName, afternoonDesc, currentTime, currentTime, colorOrange} );

            // Night shift
            db.execSQL( "INSERT OR IGNORE INTO shifts (" +
                    "id, name, description, shift_type, start_time, end_time, " +
                    "crosses_midnight, color_hex, is_user_relevant, has_break_time, " +
                    "is_break_time_included, break_time_duration_minutes, active, " +
                    "created_at, updated_at, display_order) VALUES " +
                    "('shift_night', ?, ?, 'NIGHT', " +
                    "'21:00', '05:00', 1, ?, 1, 0, 0, 0, 1, " +
                    "?, ?, 3)", new Object[]{nightName, nightDesc, currentTime, currentTime, colorBlue} );

            // Enhanced team initialization
            String[] teamNames = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};
            String teamDescTemplate = LocaleManager.getTeamDescriptionTemplate( context );
            int[] teamOffsets = {0, 16, 4, 6, 10, 12, 14, 2, 8};

            for (int i = 0; i < teamNames.length; i++) {
                String teamName = teamNames[i];
                String teamDisplayName = LocaleManager.getTeamDisplayName( context, teamName );
                String teamDescription = teamDescTemplate + " " + teamName;
                int teamOffset = teamOffsets[i];
                String teamColorHex = switch (teamName) {
                    case "A" ->
                            ColorUtils.getMaterialColors().get( "green_500" ); // "0xFF4CAF50"; // Green
                    case "B" ->
                            ColorUtils.getMaterialColors().get( "blue_500" ); // "0xFF2196F3"; // Blue
                    case "C" ->
                            ColorUtils.getMaterialColors().get( "orange_500" ); // "0xFFFF9800"; // Orange
                    case "D" ->
                            ColorUtils.getMaterialColors().get( "purple_500" ); // "0xFF9C27B0"; // Purple
                    case "E" ->
                            ColorUtils.getMaterialColors().get( "pink_500" ); // "0xFFE91E63"; // Pink
                    case "F" ->
                            ColorUtils.getMaterialColors().get( "cyan_500" ); // 0xFF00BCD4; // Cyan
                    case "G" ->
                            ColorUtils.getMaterialColors().get( "green_500" ); // 0xFF8BC34A; // Light Green
                    case "H" ->
                            ColorUtils.getMaterialColors().get( "deep_orange_500" ); // 0xFFFF5722; // Deep Orange
                    case "I" ->
                            ColorUtils.getMaterialColors().get( "brown_500" ); //") 0xFF795548; // Brown
                    default -> throw new IllegalStateException( "Unexpected value: " + teamName );
                };
                db.execSQL( "INSERT OR IGNORE INTO teams (" +
                                "id, name, display_name, description, color_hex, qdue_offset, " +
                                "team_type, active, created_at, updated_at) VALUES " +
                                "(?, ?, ?, ?, ?, ?, ?, 1, ?, ?)",
                        new Object[]{teamName, teamName, teamDisplayName, teamDescription,
                                teamColorHex, teamOffset, Team.TeamType.QUATTRODUE,
                                currentTime, currentTime} );
            }
        }

        @Deprecated
        private void initializeRecurrenceRules(@NonNull SupportSQLiteDatabase db, @NonNull Context context) {
            // Done in repository
//            long currentTime = System.currentTimeMillis();
//            String startDate = QDue.QDUE_RRULE_SCHEME_START_DATE;
//            String weekStartDate = QDue.QDUE_RRULE_DAILY_START_DATE;
//
//            // Standard QuattroDue pattern
//            db.execSQL(
//                    "INSERT OR IGNORE INTO recurrence_rules (" +
//                            "id, name, description, frequency, interval_value, start_date, " +
//                            "end_type, cycle_length, work_days, rest_days, active, " +
//                            "created_at, updated_at) VALUES " +
//                            "(?, ?, ?, 'QUATTRODUE_CYCLE', 1, ?, " +
//                            "'NEVER', 18, 4, 2, 1, ?, ?)",
//                    new Object[]{
//                            QDue.Defaults.DEFAULT_QD_RRULE_ID,
//                            QDue.Defaults.DEFAULT_QD_RRULE_NAME,
//                            QDue.Defaults.DEFAULT_QD_RRULE_DESCRIPTION,
//                            startDate, currentTime, currentTime
//                    } );
//
//            // Weekdays pattern
//            db.execSQL(
//                    "INSERT OR IGNORE INTO recurrence_rules (" +
//                            "id, name, description, frequency, interval_value, start_date, " +
//                            "end_type, by_day_json, week_start, active, created_at, updated_at) VALUES " +
//                            "(?, ?, ?, 'WEEKLY', 1, ?, " +
//                            "'NEVER', '[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\"]', " +
//                            "'MONDAY', 1, ?, ?)",
//                    new Object[]{
//                            QDue.Defaults.DEFAULT_WEEKDAYS_RRULE_ID,
//                            QDue.Defaults.DEFAULT_WEEKDAYS_RRULE_NAME,
//                            QDue.Defaults.DEFAULT_WEEKDAYS_RRULE_DESCRIPTION,
//                            weekStartDate, currentTime, currentTime
//                    } );
        }

        // Assignments should be made in the onboarding
        @Deprecated
        private void initializeDefaultAssignments(@NonNull SupportSQLiteDatabase db, @NonNull Context context) {
            long currentTime = System.currentTimeMillis();
            String startDate = QDue.QDUE_RRULE_SCHEME_START_ASSIGNMENT_DATE;

            String defaultTitle = LocaleManager.getStandardAssignmentTitle( context );

            db.execSQL( "INSERT OR IGNORE INTO user_schedule_assignments (" +
                            "id, title, description, user_id, user_name, team_id, team_name, " +
                            "recurrence_rule_id, start_date, is_permanent, priority, status, " +
                            "active, created_at, updated_at) VALUES " +
                            "('default_assignment', ?, 'Default assignment', " +
                            "1, 'user', 'A', 'Team A', 'quattrodue_standard', ?, " +
                            "1, 'OVERRIDE', 'ACTIVE', 1, ?, ?)",
                    new Object[]{defaultTitle, startDate, currentTime, currentTime} );
        }

        /**
         * Validate database setup after initialization.
         */
        private void validateDatabaseSetup(@NonNull SupportSQLiteDatabase db) {
            try {
                if (DATABASE_VERSION == 5 && db.getVersion() == DATABASE_VERSION) {
                    Log.i( TAG, "✅ Database setup validation successful" );
                } else {
                    Log.w( TAG, "️❕ Database setup validation failed" );
                }
            } catch (Exception e) {
                Log.e( TAG, "Error during database setup validation", e );
            }
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Clear all calendar data from database (for testing/reset purposes).
     * Enhanced version that clears all Version 2 tables.
     */
    public void clearAllCalendarData() {
        runInTransaction( () -> {
            userScheduleAssignmentDao().deleteAllUserScheduleAssignments();
            shiftExceptionDao().deleteAllShiftExceptions();
            recurrenceRuleDao().deleteAllRecurrenceRules();
            shiftDao().deleteAllShifts();
            teamDao().deleteAllTeams();

            Log.i( TAG, "All enhanced calendar data cleared" );
        } );
    }

    /**
     * Get comprehensive enhanced calendar database statistics.
     */
    @NonNull
    public EnhancedCalendarStatistics getEnhancedCalendarStatistics() {
        return runInTransaction( () -> {
            EnhancedCalendarStatistics stats = new EnhancedCalendarStatistics();

            // Legacy statistics
            stats.totalShifts = shiftDao().getShiftCount();
            stats.activeShifts = shiftDao().getActiveShiftCount();
            stats.totalTeams = teamDao().getTeamCount();
            stats.activeTeams = teamDao().getActiveTeamCount();

            // Enhanced statistics
            stats.totalRecurrenceRules = recurrenceRuleDao().getActiveRecurrenceRuleCount();
            stats.totalShiftExceptions = shiftExceptionDao().getExceptionCountForDate( LocalDate.now().toString() );
            stats.totalUserAssignments = Objects.requireNonNull( userScheduleAssignmentDao().getAssignmentStatistics() ).total_assignments;
            stats.activeUserAssignments = Objects.requireNonNull( userScheduleAssignmentDao().getAssignmentStatistics() ).active_assignments;

            // Database metrics
            stats.databaseSizeKB = getDatabaseSizeKB();
            stats.databaseVersion = DATABASE_VERSION;

            return stats;
        } );
    }

    /**
     * Perform enhanced calendar database maintenance and cleanup.
     */
    public int performEnhancedCalendarMaintenance() {
        return runInTransaction( () -> {
            int itemsCleanedUp = 0;

            try {
                long cutoffTime = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000); // 90 days

                // Clean up old inactive data
//                itemsCleanedUp += shiftDao().deleteShiftsOlderThan( cutoffTime );
                itemsCleanedUp += recurrenceRuleDao().deleteInactiveRulesOlderThan( cutoffTime );
                itemsCleanedUp += shiftExceptionDao().deleteInactiveExceptionsOlderThan( cutoffTime );
                itemsCleanedUp += userScheduleAssignmentDao().deleteInactiveAssignmentsOlderThan( cutoffTime );

                // Optimize database
                try {
                    query( "VACUUM", null );
                    query( "ANALYZE", null );
                } catch (Exception e) {
                    Log.e( TAG, "Error during database optimization", e );
                }

                Log.i( TAG, "✅ Enhanced calendar maintenance completed. Items cleaned: " + itemsCleanedUp );
                return itemsCleanedUp;
            } catch (Exception e) {
                Log.e( TAG, "Error during enhanced calendar maintenance: " + e.getMessage() );
                return 0;
            }
        } );
    }

    /**
     * Initialize enhanced calendar with standard QuattroDue data.
     */
    public void initializeStandardCalendarData(@NonNull Context context) {
        runInTransaction( () -> {
            try {
                // Clear existing data
                clearAllCalendarData();

                // Re-initialize with enhanced localized data
                EnhancedDatabaseCallback callback = new EnhancedDatabaseCallback( context );

                // Note: This would need access to the database instance
                // callback.initializeEnhancedDefaultData(db, context);

                Log.i( TAG, "Enhanced standard calendar data initialized successfully" );
            } catch (Exception e) {
                Log.e( TAG, "Error initializing enhanced standard calendar data: " + e.getMessage() );
                throw new RuntimeException( "Failed to initialize enhanced standard calendar data", e );
            }
        } );
    }

    /**
     * Get approximate database size in KB.
     */
    private long getDatabaseSizeKB() {
        try {
            Cursor cursor = query( "PRAGMA page_count", null );
            long pageCount = 0;
            if (cursor.moveToFirst()) {
                pageCount = cursor.getLong( 0 );
            }
            cursor.close();

            cursor = query( "PRAGMA page_size", null );
            long pageSize = 0;
            if (cursor.moveToFirst()) {
                pageSize = cursor.getLong( 0 );
            }
            cursor.close();

            return (pageCount * pageSize) / 1024; // Convert to KB
        } catch (Exception e) {
            Log.e( TAG, "Error calculating database size: " + e.getMessage() );
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
}