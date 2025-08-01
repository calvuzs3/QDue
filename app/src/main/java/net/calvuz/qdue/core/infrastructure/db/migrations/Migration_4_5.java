
package net.calvuz.qdue.core.infrastructure.db.migrations;

import androidx.sqlite.db.SupportSQLiteDatabase;

import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * Database migration from version 4 to 5.
 *
 * <p>This migration adds the shift_types table to support work schedule templates
 * and customization features. The table includes predefined shift types that will
 * be populated by the seeder.</p>
 *
 * <p>Changes in Version 5:</p>
 * <ul>
 *   <li>Added shift_types table with full schema</li>
 *   <li>Added indices for performance optimization</li>
 *   <li>Support for both predefined and user-defined shift types</li>
 *   <li>Break time support for shifts like daily work</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 5
 */
public class Migration_4_5 {

    private static final String TAG = "Migration_4_5";

    /**
     * Executes the migration from version 4 to 5.
     *
     * @param database Database instance to migrate
     */
    public static void migrate(SupportSQLiteDatabase database) {
        Log.i(TAG, "Starting migration from version 4 to 5");

        // Create shift_types table
        createShiftTypesTable(database);

        // Create indices for performance
        createShiftTypesIndices(database);

        Log.i(TAG, "Migration from version 4 to 5 completed successfully");
    }

    /**
     * Creates the shift_types table with all required columns.
     *
     * @param database Database instance
     */
    private static void createShiftTypesTable(SupportSQLiteDatabase database) {
        Log.d(TAG, "Creating shift_types table");

        String createTableSQL = "CREATE TABLE IF NOT EXISTS `shift_types` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL COLLATE NOCASE, " +
                "`description` TEXT, " +
                "`start_time` TEXT, " +
                "`end_time` TEXT, " +
                "`color_hex` TEXT NOT NULL, " +
                "`is_rest_period` INTEGER NOT NULL DEFAULT 0, " +
                "`has_break_time` INTEGER NOT NULL DEFAULT 0, " +
                "`break_start_time` TEXT, " +
                "`break_end_time` TEXT, " +
                "`is_user_defined` INTEGER NOT NULL DEFAULT 0, " +
                "`is_active` INTEGER NOT NULL DEFAULT 1, " +
                "`created_at` TEXT NOT NULL, " +
                "`updated_at` TEXT NOT NULL" +
                ")";

        database.execSQL(createTableSQL);
        Log.d(TAG, "shift_types table created successfully");
    }

    /**
     * Creates indices for the shift_types table to optimize queries.
     *
     * @param database Database instance
     */
    private static void createShiftTypesIndices(SupportSQLiteDatabase database) {
        Log.d(TAG, "Creating indices for shift_types table");

        // Unique index on name for constraint enforcement
        String uniqueNameIndex = "CREATE UNIQUE INDEX IF NOT EXISTS `index_shift_types_name` " +
                "ON `shift_types` (`name`)";
        database.execSQL(uniqueNameIndex);

        // Index on is_active for filtering active shift types
        String activeIndex = "CREATE INDEX IF NOT EXISTS `index_shift_types_is_active` " +
                "ON `shift_types` (`is_active`)";
        database.execSQL(activeIndex);

        // Index on is_user_defined for filtering predefined vs user-defined
        String userDefinedIndex = "CREATE INDEX IF NOT EXISTS `index_shift_types_is_user_defined` " +
                "ON `shift_types` (`is_user_defined`)";
        database.execSQL(userDefinedIndex);

        Log.d(TAG, "Indices created successfully");
    }
}
