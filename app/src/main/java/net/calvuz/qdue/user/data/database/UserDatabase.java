package net.calvuz.qdue.user.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import net.calvuz.qdue.user.data.dao.EstablishmentDao;
import net.calvuz.qdue.user.data.dao.MacroDepartmentDao;
import net.calvuz.qdue.user.data.dao.SubDepartmentDao;
import net.calvuz.qdue.user.data.dao.UserDao;
import net.calvuz.qdue.user.data.entities.Establishment;
import net.calvuz.qdue.user.data.entities.MacroDepartment;
import net.calvuz.qdue.user.data.entities.SubDepartment;
import net.calvuz.qdue.user.data.entities.User;
import net.calvuz.qdue.user.data.converters.LocalDateConverter;

/**
 * Room database for user management system.
 * Handles all user, establishment, and organizational data.
 */
@Database(
        entities = {
                User.class,
                Establishment.class,
                MacroDepartment.class,
                SubDepartment.class
        },
        version = 1,
        exportSchema = false
)
@TypeConverters({LocalDateConverter.class})
public abstract class UserDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "qdue_user_database";
    private static volatile UserDatabase INSTANCE;

    // Abstract DAO methods
    public abstract UserDao userDao();
    public abstract EstablishmentDao establishmentDao();
    public abstract MacroDepartmentDao macroDepartmentDao();
    public abstract SubDepartmentDao subDepartmentDao();

    /**
     * Get singleton instance of UserDatabase.
     */
    public static UserDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (UserDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    UserDatabase.class,
                                    DATABASE_NAME
                            )
                            .addCallback(DATABASE_CALLBACK)
                            .addMigrations(MIGRATION_1_2) // For future migrations
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Database callback for initialization.
     */
    private static final RoomDatabase.Callback DATABASE_CALLBACK = new RoomDatabase.Callback() {
        @Override
        public void onCreate(SupportSQLiteDatabase db) {
            super.onCreate(db);
            // Database created - can add initial data here if needed
        }

        @Override
        public void onOpen(SupportSQLiteDatabase db) {
            super.onOpen(db);
            // Database opened - can perform maintenance here if needed
        }
    };

    /**
     * Example migration for future database updates.
     */
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Example: Add new column to users table
            // database.execSQL("ALTER TABLE users ADD COLUMN new_field TEXT");
        }
    };

    /**
     * Clear all data from database (for testing/reset purposes).
     */
    public void clearAllData() {
        runInTransaction(() -> {
//            userDao().deleteAllUsers();
//            subDepartmentDao().deleteAllSubDepartments();
//            macroDepartmentDao().deleteAllMacroDepartments();
//            establishmentDao().deleteAllEstablishments();
        });
    }
}
