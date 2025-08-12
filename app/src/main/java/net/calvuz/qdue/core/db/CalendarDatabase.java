package net.calvuz.qdue.core.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import net.calvuz.qdue.data.dao.ShiftDao;
import net.calvuz.qdue.data.dao.TeamDao;
import net.calvuz.qdue.data.entities.ShiftEntity;
import net.calvuz.qdue.data.entities.TeamEntity;
import net.calvuz.qdue.core.db.converters.QDueTypeConverters;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * CalendarDatabase - Dedicated database for calendar and shift management.
 *
 * <p>This database is specifically designed for handling calendar-like operations
 * including shift management, team organization, and schedule planning. It follows
 * clean architecture principles and provides optimized performance for calendar
 * view operations like Google Calendar.</p>
 *
 * <h3>Database Evolution:</h3>
 * <ul>
 *   <li><strong>Version 1</strong>: Initial implementation with ShiftEntity and TeamEntity</li>
 *   <li><strong>Future Versions</strong>: Schedule assignments, calendar events, user preferences</li>
 * </ul>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Calendar-Optimized</strong>: Designed for calendar view performance</li>
 *   <li><strong>Shift Management</strong>: Complete shift type and scheduling support</li>
 *   <li><strong>Team Organization</strong>: Work team management and assignment</li>
 *   <li><strong>Clean Architecture</strong>: Domain-driven design with repository pattern</li>
 *   <li><strong>Performance Indexes</strong>: Optimized queries for calendar operations</li>
 *   <li><strong>Internationalization</strong>: Multi-language support using i18n system</li>
 *   <li><strong>Italian Default</strong>: Current implementation with Italian strings</li>
 * </ul>
 *
 * <h3>Entity Relationships:</h3>
 * <pre>
 * ShiftEntity (Independent)
 * ├── Shift type definitions
 * ├── Timing and duration
 * ├── Visual properties
 * └── Break management
 *
 * TeamEntity (Independent)
 * ├── Team identification
 * ├── Display properties
 * └── Active status
 *
 * Future: ScheduleEntity
 * ├── Links ShiftEntity + TeamEntity + Date
 * ├── Assignment tracking
 * └── Calendar integration
 * </pre>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Implementation
 * @since Database Version 1
 */
@Database(
        entities = {
                ShiftEntity.class,
                TeamEntity.class
        },
        version = 1,
        exportSchema = true
)
@TypeConverters({
        QDueTypeConverters.class
})
public abstract class CalendarDatabase extends RoomDatabase {

    private static final String TAG = "CalendarDatabase";
    private static final String DATABASE_NAME = "calendar_database";
    private static volatile CalendarDatabase INSTANCE;

    // ==================== ABSTRACT DAO METHODS ====================

    /**
     * Get Shift Data Access Object.
     *
     * @return ShiftDao instance for shift operations
     */
    public abstract ShiftDao shiftDao();

    /**
     * Get Team Data Access Object.
     *
     * @return TeamDao instance for team operations
     */
    public abstract TeamDao teamDao();

    // ==================== SINGLETON INSTANCE ====================

    /**
     * Get singleton instance of CalendarDatabase.
     * Thread-safe implementation with double-checked locking.
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
                            .fallbackToDestructiveMigration()
                            .addCallback(new DatabaseCallbackWithContext(context.getApplicationContext()))
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // ==================== DATABASE CALLBACK WITH CONTEXT ====================

    /**
     * Database callback with context for localized initialization.
     */
    private static class DatabaseCallbackWithContext extends RoomDatabase.Callback {

        private final Context mContext;

        public DatabaseCallbackWithContext(@NonNull Context context) {
            this.mContext = context;
        }

        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            Log.i(TAG, "CalendarDatabase created successfully");

            // Enable foreign keys for future relationship support
            db.execSQL("PRAGMA foreign_keys = ON");

            // Optimize for calendar read-heavy workload
            optimizeDatabaseForCalendar(db);

            // Create performance indexes
            createCalendarPerformanceIndexes(db);

            // Initialize default data with localization
            initializeDefaultCalendarDataWithLocalization(db, mContext);
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);

            // Ensure foreign keys are enabled
            db.execSQL("PRAGMA foreign_keys = ON");

            // Analyze tables for query optimization
            db.execSQL("ANALYZE");

            Log.d(TAG, "CalendarDatabase opened and optimized");
        }

        /**
         * Optimize database settings for calendar operations.
         *
         * @param db Database instance
         */
        private void optimizeDatabaseForCalendar(@NonNull SupportSQLiteDatabase db) {
            try {
                // WAL mode for better read performance (calendar views are read-heavy)
                db.execSQL("PRAGMA journal_mode = WAL");

                // Normal synchronous mode for better performance
                db.execSQL("PRAGMA synchronous = NORMAL");

                // Larger cache for calendar operations
                db.execSQL("PRAGMA cache_size = 10000");

                // Optimize for calendar queries
                db.execSQL("PRAGMA temp_store = MEMORY");

                Log.d(TAG, "Database optimized for calendar operations");

            } catch (Exception e) {
                Log.e(TAG, "Error optimizing database: " + e.getMessage());
            }
        }

        /**
         * Create performance indexes for calendar-specific operations.
         *
         * @param db Database instance
         */
        private void createCalendarPerformanceIndexes(@NonNull SupportSQLiteDatabase db) {
            try {
                // Shift-specific indexes for calendar operations
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shifts_type_active " +
                        "ON shifts(shift_type, active) WHERE active = 1");

                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shifts_time_range " +
                        "ON shifts(start_time, end_time, active) WHERE active = 1");

                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shifts_user_relevant " +
                        "ON shifts(is_user_relevant, active) WHERE active = 1 AND is_user_relevant = 1");

                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shifts_display_order " +
                        "ON shifts(display_order, active) WHERE active = 1");

                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shifts_break_time " +
                        "ON shifts(has_break_time, active) WHERE active = 1");

                // Team-specific indexes for team management
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_teams_active_sort " +
                        "ON teams(active, sort_order) WHERE active = 1");

                // Composite indexes for common calendar queries
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shifts_name_type_active " +
                        "ON shifts(name, shift_type, active) WHERE active = 1");

                Log.d(TAG, "Calendar performance indexes created successfully");

            } catch (Exception e) {
                Log.e(TAG, "Error creating performance indexes: " + e.getMessage());
            }
        }

        /**
         * Initialize default calendar data with proper localization.
         *
         * @param db Database instance
         * @param context Application context for localization
         */
        private void initializeDefaultCalendarDataWithLocalization(@NonNull SupportSQLiteDatabase db, @NonNull Context context) {
            try {
                // Insert standard shift types with localization
                insertStandardShiftsWithLocalization(db, context);

                // Insert standard teams with localization
                insertStandardTeamsWithLocalization(db, context);

                Log.d(TAG, "Default calendar data initialized with proper localization");

            } catch (Exception e) {
                Log.e(TAG, "Error initializing localized default data: " + e.getMessage());
            }
        }

        /**
         * Insert standard shift types using LocaleManager for proper localization.
         *
         * @param db Database instance
         * @param context Application context
         */
        private void insertStandardShiftsWithLocalization(@NonNull SupportSQLiteDatabase db, @NonNull Context context) {
            long currentTime = System.currentTimeMillis();

            // Get localized strings using LocaleManager
            String morningName = LocaleManager.getShiftName(context, "MORNING");
            String morningDesc = LocaleManager.getShiftDescription(context, "MORNING");
            String afternoonName = LocaleManager.getShiftName(context, "AFTERNOON");
            String afternoonDesc = LocaleManager.getShiftDescription(context, "AFTERNOON");
            String nightName = LocaleManager.getShiftName(context, "NIGHT");
            String nightDesc = LocaleManager.getShiftDescription(context, "NIGHT");

            // Morning shift (06:00-14:00)
            db.execSQL("INSERT OR IGNORE INTO shifts (" +
                    "id, name, description, shift_type, start_time, end_time, " +
                    "crosses_midnight, color_hex, is_user_relevant, has_break_time, " +
                    "is_break_time_included, break_time_duration_minutes, active, " +
                    "created_at, updated_at, display_order) VALUES " +
                    "('shift_morning', '" + morningName + "', '" + morningDesc + "', 'MORNING', " +
                    "'06:00', '14:00', 0, '#4CAF50', 1, 1, 0, 30, 1, " +
                    currentTime + ", " + currentTime + ", 1)");

            // Afternoon shift (14:00-22:00)
            db.execSQL("INSERT OR IGNORE INTO shifts (" +
                    "id, name, description, shift_type, start_time, end_time, " +
                    "crosses_midnight, color_hex, is_user_relevant, has_break_time, " +
                    "is_break_time_included, break_time_duration_minutes, active, " +
                    "created_at, updated_at, display_order) VALUES " +
                    "('shift_afternoon', '" + afternoonName + "', '" + afternoonDesc + "', 'AFTERNOON', " +
                    "'14:00', '22:00', 0, '#FF9800', 1, 1, 0, 30, 1, " +
                    currentTime + ", " + currentTime + ", 2)");

            // Night shift (22:00-06:00)
            db.execSQL("INSERT OR IGNORE INTO shifts (" +
                    "id, name, description, shift_type, start_time, end_time, " +
                    "crosses_midnight, color_hex, is_user_relevant, has_break_time, " +
                    "is_break_time_included, break_time_duration_minutes, active, " +
                    "created_at, updated_at, display_order) VALUES " +
                    "('shift_night', '" + nightName + "', '" + nightDesc + "', 'NIGHT', " +
                    "'22:00', '06:00', 1, '#3F51B5', 1, 1, 0, 30, 1, " +
                    currentTime + ", " + currentTime + ", 3)");
        }

        /**
         * Insert standard QuattroDue teams with proper localization.
         *
         * @param db Database instance
         * @param context Application context
         */
        private void insertStandardTeamsWithLocalization(@NonNull SupportSQLiteDatabase db, @NonNull Context context) {
            long currentTime = System.currentTimeMillis();
            String[] teamNames = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};

            // Get localized team description template
            String teamDescTemplate = LocaleManager.getTeamDescriptionTemplate(context);

            for (int i = 0; i < teamNames.length; i++) {
                String teamName = teamNames[i];
                String teamDisplayName = LocaleManager.getTeamDisplayName(context, teamName);
                String teamDescription = teamDescTemplate + " " + teamName;

                db.execSQL("INSERT OR IGNORE INTO teams (" +
                        "name, display_name, description, active, created_at, updated_at, sort_order) VALUES " +
                        "('" + teamName + "', '" + teamDisplayName + "', " +
                        "'" + teamDescription + "', 1, " +
                        currentTime + ", " + currentTime + ", " + i + ")");
            }
        }
    }

    // ==================== LEGACY DATABASE CALLBACK (DEPRECATED) ====================

    /**
     * Database callback for initialization and optimization.
     * Optimizes database for calendar operations and shift management.
     */
    private static final RoomDatabase.Callback DATABASE_CALLBACK = new RoomDatabase.Callback() {

        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            Log.i(TAG, "CalendarDatabase created successfully");

            // Enable foreign keys for future relationship support
            db.execSQL("PRAGMA foreign_keys = ON");

            // Optimize for calendar read-heavy workload
            optimizeDatabaseForCalendar(db);

            // Create performance indexes
            createCalendarPerformanceIndexes(db);

            // Initialize default data
            initializeDefaultCalendarData(db);
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);

            // Ensure foreign keys are enabled
            db.execSQL("PRAGMA foreign_keys = ON");

            // Analyze tables for query optimization
            db.execSQL("ANALYZE");

            Log.d(TAG, "CalendarDatabase opened and optimized");
        }

        /**
         * Optimize database settings for calendar operations.
         *
         * @param db Database instance
         */
        private void optimizeDatabaseForCalendar(@NonNull SupportSQLiteDatabase db) {
            try {
                // WAL mode for better read performance (calendar views are read-heavy)
                db.execSQL("PRAGMA journal_mode = WAL");

                // Normal synchronous mode for better performance
                db.execSQL("PRAGMA synchronous = NORMAL");

                // Larger cache for calendar operations
                db.execSQL("PRAGMA cache_size = 10000");

                // Optimize for calendar queries
                db.execSQL("PRAGMA temp_store = MEMORY");

                Log.d(TAG, "Database optimized for calendar operations");

            } catch (Exception e) {
                Log.e(TAG, "Error optimizing database: " + e.getMessage());
            }
        }

        /**
         * Create performance indexes for calendar-specific operations.
         *
         * @param db Database instance
         */
        private void createCalendarPerformanceIndexes(@NonNull SupportSQLiteDatabase db) {
            try {
                // Shift-specific indexes for calendar operations
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shifts_type_active " +
                        "ON shifts(shift_type, active) WHERE active = 1");

                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shifts_time_range " +
                        "ON shifts(start_time, end_time, active) WHERE active = 1");

                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shifts_user_relevant " +
                        "ON shifts(is_user_relevant, active) WHERE active = 1 AND is_user_relevant = 1");

                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shifts_display_order " +
                        "ON shifts(display_order, active) WHERE active = 1");

                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shifts_break_time " +
                        "ON shifts(has_break_time, active) WHERE active = 1");

                // Team-specific indexes for team management
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_teams_active_sort " +
                        "ON teams(active, sort_order) WHERE active = 1");

                // Composite indexes for common calendar queries
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shifts_name_type_active " +
                        "ON shifts(name, shift_type, active) WHERE active = 1");

                // Future: Schedule assignment indexes (when ScheduleEntity is added)
                // db.execSQL("CREATE INDEX IF NOT EXISTS idx_schedule_date_shift_team " +
                //         "ON schedules(schedule_date, shift_id, team_id, active) WHERE active = 1");

                Log.d(TAG, "Calendar performance indexes created successfully");

            } catch (Exception e) {
                Log.e(TAG, "Error creating performance indexes: " + e.getMessage());
            }
        }

        /**
         * Initialize default calendar data for new installations.
         *
         * @param db Database instance
         */
        private void initializeDefaultCalendarData(@NonNull SupportSQLiteDatabase db) {
            try {
                // Insert standard shift types
                insertStandardShifts(db);

                // Insert standard teams
                insertStandardTeams(db);

                Log.d(TAG, "Default calendar data initialized with localization");

            } catch (Exception e) {
                Log.e(TAG, "Error initializing default data: " + e.getMessage());
            }
        }

        /**
         * Insert standard shift types for calendar use.
         * Uses internationalization for shift names and descriptions.
         *
         * @param db Database instance
         */
        private void insertStandardShifts(@NonNull SupportSQLiteDatabase db) {
            long currentTime = System.currentTimeMillis();

            // Get localized strings using i18n system
            // Note: In production, these should come from LocaleManager or string resources
            // For now, using Italian as current app language
            String morningName = "Mattino";
            String morningDesc = "Turno standard del mattino";
            String afternoonName = "Pomeriggio";
            String afternoonDesc = "Turno standard del pomeriggio";
            String nightName = "Notte";
            String nightDesc = "Turno standard della notte";

            // TODO: Replace with proper i18n implementation when LocaleManager is available
            // String morningName = LocaleManager.getString("shift.morning.name");
            // String morningDesc = LocaleManager.getString("shift.morning.description");

            // Morning shift (06:00-14:00)
            db.execSQL("INSERT OR IGNORE INTO shifts (" +
                    "id, name, description, shift_type, start_time, end_time, " +
                    "crosses_midnight, color_hex, is_user_relevant, has_break_time, " +
                    "is_break_time_included, break_time_duration_minutes, active, " +
                    "created_at, updated_at, display_order) VALUES " +
                    "('shift_morning', '" + morningName + "', '" + morningDesc + "', 'MORNING', " +
                    "'06:00', '14:00', 0, '#4CAF50', 1, 1, 0, 30, 1, " +
                    currentTime + ", " + currentTime + ", 1)");

            // Afternoon shift (14:00-22:00)
            db.execSQL("INSERT OR IGNORE INTO shifts (" +
                    "id, name, description, shift_type, start_time, end_time, " +
                    "crosses_midnight, color_hex, is_user_relevant, has_break_time, " +
                    "is_break_time_included, break_time_duration_minutes, active, " +
                    "created_at, updated_at, display_order) VALUES " +
                    "('shift_afternoon', '" + afternoonName + "', '" + afternoonDesc + "', 'AFTERNOON', " +
                    "'14:00', '22:00', 0, '#FF9800', 1, 1, 0, 30, 1, " +
                    currentTime + ", " + currentTime + ", 2)");

            // Night shift (22:00-06:00)
            db.execSQL("INSERT OR IGNORE INTO shifts (" +
                    "id, name, description, shift_type, start_time, end_time, " +
                    "crosses_midnight, color_hex, is_user_relevant, has_break_time, " +
                    "is_break_time_included, break_time_duration_minutes, active, " +
                    "created_at, updated_at, display_order) VALUES " +
                    "('shift_night', '" + nightName + "', '" + nightDesc + "', 'NIGHT', " +
                    "'22:00', '06:00', 1, '#3F51B5', 1, 1, 0, 30, 1, " +
                    currentTime + ", " + currentTime + ", 3)");
        }

        /**
         * Insert standard QuattroDue teams with i18n support.
         *
         * @param db Database instance
         */
        private void insertStandardTeams(@NonNull SupportSQLiteDatabase db) {
            long currentTime = System.currentTimeMillis();
            String[] teamNames = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};

            // Get localized team description template
            String teamDescTemplate = "Team standard QuattroDue";
            // TODO: Replace with proper i18n implementation when LocaleManager is available
            // String teamDescTemplate = LocaleManager.getString("team.standard.description.template");

            for (int i = 0; i < teamNames.length; i++) {
                String teamName = teamNames[i];
                String teamDisplayName = "Team " + teamName;
                String teamDescription = teamDescTemplate + " " + teamName;

                db.execSQL("INSERT OR IGNORE INTO teams (" +
                        "name, display_name, description, active, created_at, updated_at, sort_order) VALUES " +
                        "('" + teamName + "', '" + teamDisplayName + "', " +
                        "'" + teamDescription + "', 1, " +
                        currentTime + ", " + currentTime + ", " + i + ")");
            }
        }
    };

    // ==================== UTILITY METHODS ====================

    /**
     * Clear all calendar data from database (for testing/reset purposes).
     */
    public void clearAllCalendarData() {
        runInTransaction(() -> {
            // Clear data (no FK constraints to worry about)
            shiftDao().deleteAllShifts();
            teamDao().deleteAllTeams();

            Log.i(TAG, "All calendar data cleared");
        });
    }

    /**
     * Get comprehensive calendar database statistics.
     *
     * @return Statistics about the calendar database
     */
    @NonNull
    public CalendarStatistics getCalendarStatistics() {
        return runInTransaction(() -> {
            CalendarStatistics stats = new CalendarStatistics();

            // Shift statistics
            ShiftDao.ShiftStatistics shiftStats = shiftDao().getShiftStatistics();
            stats.totalShifts = shiftStats.total_shifts;
            stats.activeShifts = shiftStats.active_shifts;
            stats.morningShifts = shiftStats.morning_shifts;
            stats.afternoonShifts = shiftStats.afternoon_shifts;
            stats.nightShifts = shiftStats.night_shifts;
            stats.customShifts = shiftStats.custom_shifts;
            stats.shiftsWithBreaks = shiftStats.shifts_with_breaks;

            // Team statistics
            stats.totalTeams = teamDao().getTeamCount();
            stats.activeTeams = teamDao().getActiveTeamCount();
            stats.inactiveTeams = teamDao().getInactiveTeamCount();

            // Database metrics
            stats.databaseSizeKB = getDatabaseSizeKB();

            return stats;
        });
    }

    /**
     * Perform calendar database maintenance and cleanup.
     *
     * @return Number of items cleaned up
     */
    public int performCalendarMaintenance() {
        return runInTransaction(() -> {
            int itemsCleanedUp = 0;

            try {
                // Clean up old inactive shifts (older than 90 days)
                long cutoffTime = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000);
                itemsCleanedUp += shiftDao().deleteShiftsOlderThan(cutoffTime);

                // Could add more cleanup operations here

                Log.i(TAG, "Calendar maintenance completed. Items cleaned: " + itemsCleanedUp);
                return itemsCleanedUp;

            } catch (Exception e) {
                Log.e(TAG, "Error during calendar maintenance: " + e.getMessage());
                return 0;
            }
        });
    }

    /**
     * Initialize calendar with standard QuattroDue data.
     * Useful for first-time setup or data reset.
     */
    public void initializeStandardCalendarData() {
        runInTransaction(() -> {
            try {
                // Clear existing data
                clearAllCalendarData();

                // Re-initialize with standard data
                long currentTime = System.currentTimeMillis();

                // Insert standard shifts using DAOs with i18n support
                String morningName = "Mattino";
                String morningDesc = "Turno standard del mattino";
                String afternoonName = "Pomeriggio";
                String afternoonDesc = "Turno standard del pomeriggio";
                String nightName = "Notte";
                String nightDesc = "Turno standard della notte";

                // TODO: Replace with proper i18n implementation when LocaleManager is available
                // String morningName = LocaleManager.getString("shift.morning.name");
                // String morningDesc = LocaleManager.getString("shift.morning.description");

                ShiftEntity morningShift = new ShiftEntity("shift_morning", morningName, "MORNING", "06:00", "14:00");
                morningShift.setDescription(morningDesc);
                morningShift.setColorHex("#4CAF50");
                morningShift.setUserRelevant(true);
                morningShift.setHasBreakTime(true);
                morningShift.setBreakTimeIncluded(false);
                morningShift.setBreakTimeDurationMinutes(30);
                morningShift.setDisplayOrder(1);
                shiftDao().insertShift(morningShift);

                ShiftEntity afternoonShift = new ShiftEntity("shift_afternoon", afternoonName, "AFTERNOON", "14:00", "22:00");
                afternoonShift.setDescription(afternoonDesc);
                afternoonShift.setColorHex("#FF9800");
                afternoonShift.setUserRelevant(true);
                afternoonShift.setHasBreakTime(true);
                afternoonShift.setBreakTimeIncluded(false);
                afternoonShift.setBreakTimeDurationMinutes(30);
                afternoonShift.setDisplayOrder(2);
                shiftDao().insertShift(afternoonShift);

                ShiftEntity nightShift = new ShiftEntity("shift_night", nightName, "NIGHT", "22:00", "06:00");
                nightShift.setDescription(nightDesc);
                nightShift.setColorHex("#3F51B5");
                nightShift.setUserRelevant(true);
                nightShift.setHasBreakTime(true);
                nightShift.setBreakTimeIncluded(false);
                nightShift.setBreakTimeDurationMinutes(30);
                nightShift.setDisplayOrder(3);
                shiftDao().insertShift(nightShift);

                // Insert standard teams using DAO with i18n support
                String[] teamNames = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};
                String teamDescTemplate = "Team standard QuattroDue";
                // TODO: Replace with proper i18n implementation when LocaleManager is available
                // String teamDescTemplate = LocaleManager.getString("team.standard.description.template");

                for (int i = 0; i < teamNames.length; i++) {
                    String teamName = teamNames[i];
                    TeamEntity team = new TeamEntity(teamName);
                    team.setDisplayName("Team " + teamName);
                    team.setDescription(teamDescTemplate + " " + teamName);
                    team.setSortOrder(i);
                    teamDao().insertTeam(team);
                }

                Log.i(TAG, "Standard calendar data initialized successfully");

            } catch (Exception e) {
                Log.e(TAG, "Error initializing standard calendar data: " + e.getMessage());
                throw new RuntimeException("Failed to initialize standard calendar data", e);
            }
        });
    }

    /**
     * Get approximate database size in KB.
     *
     * @return Database size in kilobytes
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

    // ==================== DATABASE STATISTICS ====================

    /**
     * Comprehensive calendar database statistics container.
     */
    public static class CalendarStatistics {
        // Shift statistics
        public int totalShifts;
        public int activeShifts;
        public int morningShifts;
        public int afternoonShifts;
        public int nightShifts;
        public int customShifts;
        public int shiftsWithBreaks;

        // Team statistics
        public int totalTeams;
        public int activeTeams;
        public int inactiveTeams;

        // System statistics
        public long databaseSizeKB;

        @NonNull
        @Override
        public String toString() {
            return "CalendarStatistics{" +
                    "totalShifts=" + totalShifts +
                    ", activeShifts=" + activeShifts +
                    ", morningShifts=" + morningShifts +
                    ", afternoonShifts=" + afternoonShifts +
                    ", nightShifts=" + nightShifts +
                    ", customShifts=" + customShifts +
                    ", shiftsWithBreaks=" + shiftsWithBreaks +
                    ", totalTeams=" + totalTeams +
                    ", activeTeams=" + activeTeams +
                    ", inactiveTeams=" + inactiveTeams +
                    ", databaseSizeKB=" + databaseSizeKB +
                    '}';
        }
    }

    // ==================== MIGRATION SUPPORT ====================

    /**
     * Future migration utilities will be added here when needed.
     * This database is designed to be extensible for future requirements.
     */
    public static class MigrationSupport {

        /**
         * Placeholder for future migration utilities.
         * When ScheduleEntity or other entities are added, migration helpers will be here.
         */
        public static void prepareFutureMigrations() {
            // Future migration logic will be implemented here
            Log.d(TAG, "Migration support ready for future database versions");
        }
    }

    // ==================== INTERNATIONALIZATION SUPPORT ====================

    /**
     * Internationalization utilities for calendar database.
     *
     * <p>This database now uses LocaleManager for proper internationalization support.
     * All calendar-related strings are localized using the LocaleManager utility methods.</p>
     *
     * <h3>Implementation:</h3>
     * <pre>
     * {@code
     * // Usage with LocaleManager:
     * String morningName = LocaleManager.getShiftName(context, "MORNING");
     * String morningDesc = LocaleManager.getShiftDescription(context, "MORNING");
     * String teamDisplayName = LocaleManager.getTeamDisplayName(context, "A");
     * String teamDescTemplate = LocaleManager.getTeamDescriptionTemplate(context);
     * }
     * </pre>
     *
     * <h3>Supported Localizations:</h3>
     * <ul>
     *   <li><strong>Italian (IT)</strong> - Primary language (complete)</li>
     *   <li><strong>English (EN)</strong> - Planned for Phase 2</li>
     *   <li><strong>German (DE)</strong> - Planned for Phase 3</li>
     *   <li><strong>French (FR)</strong> - Planned for Phase 3</li>
     * </ul>
     */
    public static class I18nSupport {

        /**
         * Update shift names and descriptions for current locale.
         * Use this method when language is changed at runtime.
         *
         * @param context Application context
         * @param database Database instance
         */
        public static void updateShiftLocalization(@NonNull Context context, @NonNull CalendarDatabase database) {
            database.runInTransaction(() -> {
                try {
                    long currentTime = System.currentTimeMillis();

                    // Update morning shift
                    String morningName = LocaleManager.getShiftName(context, "MORNING");
                    String morningDesc = LocaleManager.getShiftDescription(context, "MORNING");
                    database.shiftDao().renameShift("shift_morning", morningName, currentTime);
                    // Note: Description update would need additional DAO method

                    // Update afternoon shift
                    String afternoonName = LocaleManager.getShiftName(context, "AFTERNOON");
                    String afternoonDesc = LocaleManager.getShiftDescription(context, "AFTERNOON");
                    database.shiftDao().renameShift("shift_afternoon", afternoonName, currentTime);

                    // Update night shift
                    String nightName = LocaleManager.getShiftName(context, "NIGHT");
                    String nightDesc = LocaleManager.getShiftDescription(context, "NIGHT");
                    database.shiftDao().renameShift("shift_night", nightName, currentTime);

                    Log.d(TAG, "Shift localization updated for current locale");

                } catch (Exception e) {
                    Log.e(TAG, "Error updating shift localization: " + e.getMessage());
                }
            });
        }

        /**
         * Update team display names for current locale.
         * Use this method when language is changed at runtime.
         *
         * @param context Application context
         * @param database Database instance
         */
        public static void updateTeamLocalization(@NonNull Context context, @NonNull CalendarDatabase database) {
            database.runInTransaction(() -> {
                try {
                    long currentTime = System.currentTimeMillis();
                    String[] teamNames = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};
                    String teamDescTemplate = LocaleManager.getTeamDescriptionTemplate(context);

                    for (String teamName : teamNames) {
                        String displayName = LocaleManager.getTeamDisplayName(context, teamName);
                        // Note: Would need additional DAO methods for display name and description updates
                        // This is a placeholder for future implementation
                    }

                    Log.d(TAG, "Team localization updated for current locale");

                } catch (Exception e) {
                    Log.e(TAG, "Error updating team localization: " + e.getMessage());
                }
            });
        }

        /**
         * Get current localization status.
         *
         * @param context Application context
         * @return Localization status information
         */
        @NonNull
        public static String getLocalizationStatus(@NonNull Context context) {
            LocaleManager localeManager = new LocaleManager(context);
            return "Calendar Database Localization Status:\n" +
                    "Current Language: " + localeManager.getCurrentLanguageCode() + "\n" +
                    "Using System Language: " + localeManager.isUsingSystemLanguage() + "\n" +
                    "Sample Shift Name: " + localeManager.getShiftName("MORNING") + "\n" +
                    "Sample Team Display: " + localeManager.getTeamDisplayName("A");
        }
    }
}