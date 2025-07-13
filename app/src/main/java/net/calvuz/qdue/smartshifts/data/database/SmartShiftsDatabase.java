package net.calvuz.qdue.smartshifts.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import net.calvuz.qdue.smartshifts.data.dao.ShiftTypeDao;
import net.calvuz.qdue.smartshifts.data.dao.ShiftPatternDao;
import net.calvuz.qdue.smartshifts.data.dao.UserShiftAssignmentDao;
import net.calvuz.qdue.smartshifts.data.dao.SmartShiftEventDao;
import net.calvuz.qdue.smartshifts.data.dao.TeamContactDao;
import net.calvuz.qdue.smartshifts.data.entities.ShiftType;
import net.calvuz.qdue.smartshifts.data.entities.ShiftPattern;
import net.calvuz.qdue.smartshifts.data.entities.UserShiftAssignment;
import net.calvuz.qdue.smartshifts.data.entities.SmartShiftEvent;
import net.calvuz.qdue.smartshifts.data.entities.TeamContact;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main Room Database for SmartShifts system
 * Contains all entities and provides access to DAOs
 */
@Database(
        entities = {
                ShiftType.class,
                ShiftPattern.class,
                UserShiftAssignment.class,
                SmartShiftEvent.class,
                TeamContact.class
        },
        version = 1,
        exportSchema = false
)
@TypeConverters({SmartShiftsConverters.class})
public abstract class SmartShiftsDatabase extends RoomDatabase {

    // ===== ABSTRACT DAO GETTERS =====

    public abstract ShiftTypeDao shiftTypeDao();
    public abstract ShiftPatternDao shiftPatternDao();
    public abstract UserShiftAssignmentDao userAssignmentDao();
    public abstract SmartShiftEventDao smartEventDao();
    public abstract TeamContactDao teamContactDao();

    // ===== SINGLETON PATTERN =====

    private static volatile SmartShiftsDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;

    /**
     * Executor service for background database operations
     */
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    /**
     * Get database instance (singleton pattern)
     */
    public static SmartShiftsDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (SmartShiftsDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    SmartShiftsDatabase.class,
                                    "smartshifts_database"
                            )
                            .addCallback(new DatabaseCallback())
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Database callback for initialization
     */
    private static class DatabaseCallback extends RoomDatabase.Callback {

        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            // Populate database with initial data in background thread
            databaseWriteExecutor.execute(() -> {
                if (INSTANCE != null) {
                    DatabaseInitializer.populateInitialData(INSTANCE);
                }
            });
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);

            // Verify data integrity on each open
            databaseWriteExecutor.execute(() -> {
                if (INSTANCE != null) {
                    DatabaseInitializer.verifyDataIntegrity(INSTANCE);
                }
            });
        }
    }

    /**
     * Close database and cleanup resources
     */
    public static void closeDatabase() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
        databaseWriteExecutor.shutdown();
    }
}

// =====================================================================

