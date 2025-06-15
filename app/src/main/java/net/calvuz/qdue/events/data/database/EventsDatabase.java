package net.calvuz.qdue.events.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import net.calvuz.qdue.events.EventDao;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.data.converters.EventsTypeConverters;

/**
 * Room database for events management system.
 * Central database that will eventually replace UserDatabase as main app database.
 *
 * Features:
 * - Google Calendar-like event storage
 * - Support for external event packages
 * - Advanced querying and filtering
 * - Batch operations for performance
 */
@Database(
        entities = {
                LocalEvent.class
        },
        version = 1,
        exportSchema = false
)
@TypeConverters({
        EventsTypeConverters.class
})
public abstract class EventsDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "qdue_events_database";
    private static volatile EventsDatabase INSTANCE;

    // Abstract DAO methods
    public abstract EventDao eventDao();

    /**
     * Get singleton instance of EventsDatabase.
     * Thread-safe implementation with double-checked locking.
     */
    public static EventsDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (EventsDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    EventsDatabase.class,
                                    DATABASE_NAME
                            )
                            .addCallback(DATABASE_CALLBACK)
                            .addMigrations(MIGRATION_1_2) // For future schema changes
                            .fallbackToDestructiveMigration() // For development only
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Database callback for initialization and maintenance.
     */
    private static final RoomDatabase.Callback DATABASE_CALLBACK = new RoomDatabase.Callback() {
        @Override
        public void onCreate(SupportSQLiteDatabase db) {
            super.onCreate(db);
            // Create indexes for better query performance
            db.execSQL("CREATE INDEX IF NOT EXISTS index_events_start_time ON events(start_time)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_events_package_id ON events(package_id)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_events_event_type ON events(event_type)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_events_priority ON events(priority)");
        }

        @Override
        public void onOpen(SupportSQLiteDatabase db) {
            super.onOpen(db);
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON");
        }
    };

    /**
     * Example migration for future database updates.
     * This shows how to add new columns while preserving existing data.
     */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Example: Add new column for recurring events (future feature)
            // database.execSQL("ALTER TABLE events ADD COLUMN recurrence_rule TEXT");
            // database.execSQL("ALTER TABLE events ADD COLUMN recurrence_end_date INTEGER");
        }
    };

    /**
     * Close database instance (for testing or cleanup).
     * Should not be called in production unless app is shutting down.
     */
    public static void closeDatabase() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }

    /**
     * Get database path for debugging purposes.
     */
    public static String getDatabasePath(Context context) {
        return context.getDatabasePath(DATABASE_NAME).getAbsolutePath();
    }

    /**
     * Check if database exists.
     */
    public static boolean databaseExists(Context context) {
        return context.getDatabasePath(DATABASE_NAME).exists();
    }

    /**
     * Get database size in bytes for monitoring.
     */
    public long getDatabaseSize(Context context) {
        if (databaseExists(context)) {
            return context.getDatabasePath(DATABASE_NAME).length();
        }
        return 0;
    }
}