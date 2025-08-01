
package net.calvuz.qdue.core.infrastructure.db.seeders;

import static net.calvuz.qdue.core.Costants.DATETIME_FORMATTER;

import androidx.sqlite.db.SupportSQLiteDatabase;

import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Seeder for predefined shift types.
 *
 * <p>Populates the database with predefined shift types that users can use
 * as building blocks for their work schedules. These include standard shift
 * patterns commonly used in shift work environments.</p>
 *
 * <p>Predefined Shift Types:</p>
 * <ul>
 *   <li><strong>Morning</strong> - 05:00-13:00 (blue)</li>
 *   <li><strong>Afternoon</strong> - 13:00-21:00 (orange)</li>
 *   <li><strong>Night</strong> - 21:00-05:00 (purple, crosses midnight)</li>
 *   <li><strong>Rest</strong> - No timing (gray)</li>
 *   <li><strong>Daily</strong> - 08:00-17:00 with 12:00-13:00 break (green)</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 5
 */
public class ShiftTypeSeeder {

    private static final String TAG = "ShiftTypeSeeder";

    /**
     * Seeds predefined shift types into the database.
     *
     * @param database Database instance
     */
    public static void seedPredefinedShiftTypes(SupportSQLiteDatabase database) {
        Log.i(TAG, "Seeding predefined shift types");

        String currentDateTime = LocalDateTime.now().format(DATETIME_FORMATTER);

        try {
            // Check if shift types already exist to avoid duplicates
            if (shiftTypesAlreadySeeded(database)) {
                Log.d(TAG, "Predefined shift types already exist, skipping seeding");
                return;
            }

            // Insert predefined shift types
            insertMorningShift(database, currentDateTime);
            insertAfternoonShift(database, currentDateTime);
            insertNightShift(database, currentDateTime);
            insertRestPeriod(database, currentDateTime);
            insertDailyShift(database, currentDateTime);

            Log.i(TAG, "Successfully seeded " + getPredefinedShiftTypeCount() + " predefined shift types");

        } catch (Exception e) {
            Log.e(TAG, "Error seeding predefined shift types", e);
            throw new RuntimeException("Failed to seed predefined shift types", e);
        }
    }

    /**
     * Checks if predefined shift types have already been seeded.
     *
     * @param database Database instance
     * @return true if shift types exist, false otherwise
     */
    private static boolean shiftTypesAlreadySeeded(SupportSQLiteDatabase database) {
        String query = "SELECT COUNT(*) FROM shift_types WHERE is_user_defined = 0";
        try (android.database.Cursor cursor = database.query(query)) {
            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                return count > 0;
            }
        }
        return false;
    }

    /**
     * Inserts the Morning shift type (05:00-13:00).
     *
     * @param database Database instance
     * @param currentDateTime Current timestamp for audit fields
     */
    private static void insertMorningShift(SupportSQLiteDatabase database, String currentDateTime) {
        String insertSQL = "INSERT INTO shift_types " +
                "(name, description, start_time, end_time, color_hex, is_rest_period, has_break_time, " +
                "is_user_defined, is_active, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        database.execSQL(insertSQL, new Object[]{
                "Mattino",                              // name
                "Turno mattutino 05:00-13:00",        // description
                "05:00:00",                            // start_time
                "13:00:00",                            // end_time
                "#2196F3",                             // color_hex (Material Blue)
                0,                                     // is_rest_period (false)
                0,                                     // has_break_time (false)
                0,                                     // is_user_defined (false)
                1,                                     // is_active (true)
                currentDateTime,                       // created_at
                currentDateTime                        // updated_at
        });

        Log.d(TAG, "Inserted Morning shift type");
    }

    /**
     * Inserts the Afternoon shift type (13:00-21:00).
     *
     * @param database Database instance
     * @param currentDateTime Current timestamp for audit fields
     */
    private static void insertAfternoonShift(SupportSQLiteDatabase database, String currentDateTime) {
        String insertSQL = "INSERT INTO shift_types " +
                "(name, description, start_time, end_time, color_hex, is_rest_period, has_break_time, " +
                "is_user_defined, is_active, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        database.execSQL(insertSQL, new Object[]{
                "Pomeriggio",                          // name
                "Turno pomeridiano 13:00-21:00",      // description
                "13:00:00",                            // start_time
                "21:00:00",                            // end_time
                "#FF9800",                             // color_hex (Material Orange)
                0,                                     // is_rest_period (false)
                0,                                     // has_break_time (false)
                0,                                     // is_user_defined (false)
                1,                                     // is_active (true)
                currentDateTime,                       // created_at
                currentDateTime                        // updated_at
        });

        Log.d(TAG, "Inserted Afternoon shift type");
    }

    /**
     * Inserts the Night shift type (21:00-05:00, crosses midnight).
     *
     * @param database Database instance
     * @param currentDateTime Current timestamp for audit fields
     */
    private static void insertNightShift(SupportSQLiteDatabase database, String currentDateTime) {
        String insertSQL = "INSERT INTO shift_types " +
                "(name, description, start_time, end_time, color_hex, is_rest_period, has_break_time, " +
                "is_user_defined, is_active, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        database.execSQL(insertSQL, new Object[]{
                "Notte",                               // name
                "Turno notturno 21:00-05:00",         // description
                "21:00:00",                            // start_time
                "05:00:00",                            // end_time (crosses midnight)
                "#9C27B0",                             // color_hex (Material Purple)
                0,                                     // is_rest_period (false)
                0,                                     // has_break_time (false)
                0,                                     // is_user_defined (false)
                1,                                     // is_active (true)
                currentDateTime,                       // created_at
                currentDateTime                        // updated_at
        });

        Log.d(TAG, "Inserted Night shift type");
    }

    /**
     * Inserts the Rest period type (no timing).
     *
     * @param database Database instance
     * @param currentDateTime Current timestamp for audit fields
     */
    private static void insertRestPeriod(SupportSQLiteDatabase database, String currentDateTime) {
        String insertSQL = "INSERT INTO shift_types " +
                "(name, description, start_time, end_time, color_hex, is_rest_period, has_break_time, " +
                "is_user_defined, is_active, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        database.execSQL(insertSQL, new Object[]{
                "Riposo",                              // name
                "Giorno di riposo",                    // description
                null,                                  // start_time (null for rest)
                null,                                  // end_time (null for rest)
                "#9E9E9E",                             // color_hex (Material Gray)
                1,                                     // is_rest_period (true)
                0,                                     // has_break_time (false)
                0,                                     // is_user_defined (false)
                1,                                     // is_active (true)
                currentDateTime,                       // created_at
                currentDateTime                        // updated_at
        });

        Log.d(TAG, "Inserted Rest period type");
    }

    /**
     * Inserts the Daily shift type (08:00-17:00 with 12:00-13:00 break).
     *
     * @param database Database instance
     * @param currentDateTime Current timestamp for audit fields
     */
    private static void insertDailyShift(SupportSQLiteDatabase database, String currentDateTime) {
        String insertSQL = "INSERT INTO shift_types " +
                "(name, description, start_time, end_time, color_hex, is_rest_period, has_break_time, " +
                "break_start_time, break_end_time, is_user_defined, is_active, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        database.execSQL(insertSQL, new Object[]{
                "Giornaliero",                         // name
                "Turno giornaliero 08:00-17:00 con pausa pranzo", // description
                "08:00:00",                            // start_time
                "17:00:00",                            // end_time
                "#4CAF50",                             // color_hex (Material Green)
                0,                                     // is_rest_period (false)
                1,                                     // has_break_time (true)
                "12:00:00",                            // break_start_time
                "13:00:00",                            // break_end_time
                0,                                     // is_user_defined (false)
                1,                                     // is_active (true)
                currentDateTime,                       // created_at
                currentDateTime                        // updated_at
        });

        Log.d(TAG, "Inserted Daily shift type with break time");
    }

    /**
     * Gets the number of predefined shift types that should be seeded.
     *
     * @return number of predefined shift types
     */
    private static int getPredefinedShiftTypeCount() {
        return 5; // Morning, Afternoon, Night, Rest, Daily
    }
}