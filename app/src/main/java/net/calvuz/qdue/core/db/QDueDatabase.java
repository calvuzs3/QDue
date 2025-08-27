package net.calvuz.qdue.core.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import net.calvuz.qdue.events.dao.EventDao;
import net.calvuz.qdue.events.dao.TurnExceptionDao;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.models.TurnException;
import net.calvuz.qdue.user.data.dao.EstablishmentDao;
import net.calvuz.qdue.user.data.dao.MacroDepartmentDao;
import net.calvuz.qdue.user.data.dao.SubDepartmentDao;
import net.calvuz.qdue.user.data.dao.UserDao;
import net.calvuz.qdue.user.data.entities.Establishment;
import net.calvuz.qdue.user.data.entities.MacroDepartment;
import net.calvuz.qdue.user.data.entities.SubDepartment;
import net.calvuz.qdue.user.data.entities.User;
import net.calvuz.qdue.core.db.converters.QDueTypeConverters;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.text.MessageFormat;

/**
 * QDueDatabase - Unified main database for Q-DUE application.
 * <p>
 * Database Evolution:
 * Version 1: Events only (LocalEvent)
 * Version 2: Events + TurnException
 * Version 3: Events + TurnException + User management (UNIFIED)
 * <p>
 * Features:
 * - Google Calendar-like event storage
 * - Turn pattern exception management with real Foreign Keys
 * - Complete user and organizational management
 * - Virtual scrolling optimization
 * - Cross-entity transactions and joins
 * - Unified backup and migration system
 */
@Database (
        entities = {
                // Events and Calendar
                LocalEvent.class,
                TurnException.class,

                // User Management (migrated from UserDatabase)
                User.class,
                Establishment.class,
                MacroDepartment.class,
                SubDepartment.class,

        },
        version = QDueDatabase.DATABASE_VERSION,
        exportSchema = false
)
@TypeConverters ({
        QDueTypeConverters.class
})
public abstract class QDueDatabase extends RoomDatabase {

    public  static final int DATABASE_VERSION = 4;

    private static final String TAG = "QDueDatabase";
    private static final String DATABASE_NAME = "qd_database";
    private static volatile QDueDatabase INSTANCE;

    // ==================== ABSTRACT DAO METHODS ====================

    // Events and Calendar DAOs
    public abstract EventDao eventDao();
    public abstract TurnExceptionDao turnExceptionDao();

    // User Management DAOs
    public abstract UserDao userDao();
    public abstract EstablishmentDao establishmentDao();
    public abstract MacroDepartmentDao macroDepartmentDao();
    public abstract SubDepartmentDao subDepartmentDao();


    // ==================== SINGLETON INSTANCE ====================

    /**
     * Get singleton instance of QDueDatabase.
     * Thread-safe implementation with double-checked locking.
     */
    public static QDueDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (QDueDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    QDueDatabase.class,
                                    DATABASE_NAME
                            )
                            .fallbackToDestructiveMigration()
                            .addCallback( DATABASE_CALLBACK )
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // ==================== DATABASE CALLBACK ====================

    /**
     * Database callback for initialization and optimization.
     * Optimizes database for virtual scrolling and cross-entity performance.
     */
    private static final RoomDatabase.Callback DATABASE_CALLBACK = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate( db );
            Log.i( TAG, MessageFormat.format( "QDueDatabase created successfully (Version {0})",
                    DATABASE_VERSION));

            // Enable foreign keys
            db.execSQL( "PRAGMA foreign_keys = ON" );

            // Optimize for read-heavy workload (virtual scrolling)
//            db.execSQL("PRAGMA journal_mode = WAL");
//            db.execSQL("PRAGMA synchronous = NORMAL");
//            db.execSQL("PRAGMA cache_size = 10000");

            // Additional performance indexes
            createPerformanceIndexes( db );

            // Initialize default data if needed
            initializeDefaultData( db );
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen( db );

            Log.d(TAG, "QDueDatabase opened (version " + db.getVersion() + ")");

            // Ensure foreign keys are enabled
            db.execSQL( "PRAGMA foreign_keys = ON" );

            // Analyze tables for query optimization
            db.execSQL( "ANALYZE" );
        }

        /**
         * Create additional performance indexes for virtual scrolling and cross-entity queries
         */
        private void createPerformanceIndexes(SupportSQLiteDatabase db) {
            try {
                // Cross-entity indexes for user-exception queries
                db.execSQL( "CREATE INDEX IF NOT EXISTS idx_turn_exceptions_user_date_active " +
                        "ON turn_exceptions(user_id, date, status) WHERE status = 'ACTIVE'" );

                // Virtual scrolling month queries
                db.execSQL( "CREATE INDEX IF NOT EXISTS idx_turn_exceptions_month_range " +
                        "ON turn_exceptions(user_id, date) WHERE status = 'ACTIVE'" );

                // User-events correlation
                db.execSQL( "CREATE INDEX IF NOT EXISTS idx_events_date_user " +
                        "ON events(start_time, end_time) WHERE package_id IS NULL" );

                // Organizational hierarchy queries
                db.execSQL( "CREATE INDEX IF NOT EXISTS idx_users_organization " +
                        "ON users(establishment_id, macro_department_id, sub_department_id, is_active)" );

                Log.d( TAG, "Performance indexes created successfully" );
            } catch (Exception e) {
                Log.e( TAG, "Error creating performance indexes: " + e.getMessage() );
            }
        }

        /**
         * Initialize default data for new installations
         */
        private void initializeDefaultData(SupportSQLiteDatabase db) {
            try {
                // Create default establishment if none exists
                db.execSQL( "INSERT OR IGNORE INTO establishments (id, name, code, created_at, updated_at) " +
                        "VALUES (1, 'Default Organization', 'DEFAULT', date('now'), date('now'))" );

                Log.d( TAG, "Default data initialized" );
            } catch (Exception e) {
                Log.e( TAG, "Error initializing default data: " + e.getMessage() );
            }
        }
    };

    // ==================== UTILITY METHODS ====================

    /**
     * Clear all data from database (for testing/reset purposes).
     */
    public void clearAllData() {
        runInTransaction( () -> {
            // Clear in correct order due to FK constraints
            turnExceptionDao().deleteAllExceptionsForUser( 1L ); // This deletes all
            eventDao().deleteAllEvents();
            userDao().deleteAllUsers();
            subDepartmentDao().deleteAllSubDepartments();
            macroDepartmentDao().deleteAllMacroDepartments();
            establishmentDao().deleteAllEstablishments();

            Log.i( TAG, "All database data cleared" );
        } );
    }

    /**
     * Get comprehensive database statistics
     */
    public DatabaseStatistics getDatabaseStatistics() {
        return runInTransaction( () -> {
            DatabaseStatistics stats = new DatabaseStatistics();

            // Event statistics
            stats.totalEvents = eventDao().getTotalEventCount();
            stats.upcomingEvents = eventDao().getUpcomingEventsCount( java.time.LocalDateTime.now() );

            // Exception statistics
            TurnExceptionDao.ExceptionStatistics exceptionStats =
                    turnExceptionDao().getExceptionStatistics( java.time.LocalDate.now() );
            stats.totalExceptions = exceptionStats.total;
            stats.activeExceptions = exceptionStats.active;
            stats.futureExceptions = exceptionStats.future;

            // User statistics
            stats.totalUsers = userDao().getUserCount();
            stats.activeUsers = userDao().getActiveUserCount();

            // Organizational statistics
            stats.totalEstablishments = establishmentDao().getEstablishmentCount();

            // Database size
            stats.databaseSizeKB = getDatabaseSizeKB();

            return stats;
        } );
    }

    /**
     * Cleanup old data and maintain database health
     */
    public int performMaintenance() {
        return runInTransaction( () -> {
            int itemsCleanedUp = 0;

            try {
                // Clean up old cancelled exceptions (older than 30 days)
                java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusDays( 30 );
                java.util.List<TurnException> expiredExceptions =
                        turnExceptionDao().getExpiredCancelledExceptions( cutoff );

                if (!expiredExceptions.isEmpty()) {
                    turnExceptionDao().deleteExceptions( expiredExceptions );
                    itemsCleanedUp += expiredExceptions.size();
                    Log.i( TAG, "Cleaned up " + expiredExceptions.size() + " old exceptions" );
                }

                // Clean up orphaned data
                java.util.List<TurnException> orphanedExceptions =
                        turnExceptionDao().getOrphanedExceptions();

                if (!orphanedExceptions.isEmpty()) {
                    turnExceptionDao().deleteExceptions( orphanedExceptions );
                    itemsCleanedUp += orphanedExceptions.size();
                    Log.i( TAG, "Cleaned up " + orphanedExceptions.size() + " orphaned exceptions" );
                }

                return itemsCleanedUp;
            } catch (Exception e) {
                Log.e( TAG, "Error during database maintenance: " + e.getMessage() );
                return 0;
            }
        } );
    }

    /**
     * Get current active user
     */
    public User getCurrentUser() {
        return userDao().getActiveUser();
    }

    /**
     * Get approximate database size in KB
     */
    private long getDatabaseSizeKB() {
        try {
            android.database.Cursor cursor = query( "PRAGMA page_count", null );
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

    // ==================== DATABASE STATISTICS ====================

    /**
     * Comprehensive database statistics container
     */
    public static class DatabaseStatistics {
        // Events
        public int totalEvents;
        public int upcomingEvents;

        // Exceptions
        public int totalExceptions;
        public int activeExceptions;
        public int futureExceptions;

        // Users
        public int totalUsers;
        public int activeUsers;

        // Organization
        public int totalEstablishments;

        // System
        public long databaseSizeKB;

        @NonNull
        @Override
        public String toString() {
            return "DatabaseStatistics{" +
                    "totalEvents=" + totalEvents +
                    ", upcomingEvents=" + upcomingEvents +
                    ", totalExceptions=" + totalExceptions +
                    ", activeExceptions=" + activeExceptions +
                    ", futureExceptions=" + futureExceptions +
                    ", totalUsers=" + totalUsers +
                    ", activeUsers=" + activeUsers +
                    ", totalEstablishments=" + totalEstablishments +
                    ", databaseSizeKB=" + databaseSizeKB +
                    '}';
        }
    }
}