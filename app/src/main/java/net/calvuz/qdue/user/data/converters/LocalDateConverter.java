package net.calvuz.qdue.user.data.converters;

import androidx.room.TypeConverter;
import java.time.LocalDate;

/**
 * Room type converter for LocalDate.
 */
public class LocalDateConverter {

    @TypeConverter
    public static LocalDate fromString(String value) {
        return value == null ? null : LocalDate.parse(value);
    }

    @TypeConverter
    public static String fromLocalDate(LocalDate date) {
        return date == null ? null : date.toString();
    }
}
