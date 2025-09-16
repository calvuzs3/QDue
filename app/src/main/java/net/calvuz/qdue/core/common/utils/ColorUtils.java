package net.calvuz.qdue.core.common.utils;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalField;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * ColorUtils - Comprehensive Color Management Utilities
 *
 * <p>Utility class for color conversion, validation, and manipulation integrated with
 * QDue's database storage and UI theming system.</p>
 *
 * <h3>Integration Features:</h3>
 * <ul>
 *   <li><strong>Database Compatible</strong>: Works with QDueTypeConverters hex storage</li>
 *   <li><strong>Material Design</strong>: Supports Material color palettes and themes</li>
 *   <li><strong>Validation</strong>: Comprehensive hex color format validation</li>
 *   <li><strong>Conversion</strong>: Multiple color format conversions</li>
 * </ul>
 *
 * <h3>Supported Formats:</h3>
 * <ul>
 *   <li><strong>Hex Strings</strong>: "#RGB", "#ARGB", "#RRGGBB", "#AARRGGBB"</li>
 *   <li><strong>Android Color Int</strong>: Standard Android Color.* values</li>
 *   <li><strong>RGB Components</strong>: Separate red, green, blue values</li>
 *   <li><strong>HSV Components</strong>: Hue, saturation, value color space</li>
 * </ul>
 */
public class ColorUtils
{

    private static final String TAG = "ColorUtils";

    // ==================== HEX VALIDATION PATTERNS ====================

    private static final Pattern HEX_PATTERN = Pattern.compile( "^#?([0-9A-Fa-f]{3,8})$" );
    private static final Pattern RGB_PATTERN = Pattern.compile( "^#?[0-9A-Fa-f]{3}$" );
    private static final Pattern ARGB_PATTERN = Pattern.compile( "^#?[0-9A-Fa-f]{4}$" );
    private static final Pattern RRGGBB_PATTERN = Pattern.compile( "^#?[0-9A-Fa-f]{6}$" );
    private static final Pattern AARRGGBB_PATTERN = Pattern.compile( "^#?[0-9A-Fa-f]{8}$" );

    // ==================== DEFAULT COLORS ====================

    public static final String DEFAULT_PRIMARY_COLOR = "#FF6200EE";
    public static final String DEFAULT_SECONDARY_COLOR = "#FF03DAC5";
    public static final String DEFAULT_ERROR_COLOR = "#FFB00020";
    public static final String DEFAULT_BACKGROUND_COLOR = "#FFFFFFFF";
    public static final String DEFAULT_SURFACE_COLOR = "#FFFFFFFF";

    // ==================== CONVERSION: HEX STRING TO COLOR INT ====================

    /**
     * Convert hex string to Android Color int.
     * Supports all standard hex formats with comprehensive error handling.
     *
     * @param hexString Hex color string (e.g., "#FF5733", "#RGB", etc.)
     * @return Android Color int or Color.BLACK if conversion fails
     */
    @ColorInt
    public static int hexStringToColorInt(@Nullable String hexString) {
        return hexStringToColorInt( hexString, Color.BLACK );
    }

    /**
     * Convert hex string to Android Color int with custom fallback.
     *
     * @param hexString     Hex color string
     * @param fallbackColor Color to return if conversion fails
     * @return Android Color int or fallbackColor
     */
    @ColorInt
    public static int hexStringToColorInt(@Nullable String hexString, @ColorInt int fallbackColor) {
        if (!isValidHexColor( hexString )) {
            Log.w( TAG, "Invalid hex color, using fallback: " + hexString );
            return fallbackColor;
        }

        try {
            String cleanHex = normalizeHexString( hexString );
            long colorLong = Long.parseLong( cleanHex, 16 );
            return (int) colorLong;
        } catch (NumberFormatException e) {
            Log.e( TAG, "Error parsing hex color: " + hexString, e );
            return fallbackColor;
        }
    }

    // ==================== CONVERSION: COLOR INT TO HEX STRING ====================

    /**
     * Convert Android Color int to hex string with alpha.
     *
     * @param colorInt Android Color int value
     * @return Hex color string in #AARRGGBB format
     */
    @NonNull
    public static String colorIntToHexString(@ColorInt int colorInt) {
        return String.format( "#%08X", colorInt );
    }

    /**
     * Convert Android Color int to RGB hex string (no alpha).
     *
     * @param colorInt Android Color int value
     * @return Hex color string in #RRGGBB format
     */
    @NonNull
    public static String colorIntToRgbHexString(@ColorInt int colorInt) {
        int rgb = colorInt & 0x00FFFFFF;
        return String.format( "#%06X", rgb );
    }

    // ==================== VALIDATION ====================

    /**
     * Validate if string is a valid hex color format.
     *
     * @param hexString String to validate
     * @return true if valid hex color format
     */
    public static boolean isValidHexColor(@Nullable String hexString) {
        if (hexString == null || hexString.trim().isEmpty()) {
            return false;
        }

        return HEX_PATTERN.matcher( hexString.trim() ).matches();
    }

    /**
     * Get detailed validation info for hex color string.
     *
     * @param hexString String to validate
     * @return ColorValidationResult with details
     */
    @NonNull
    public static ColorValidationResult validateHexColor(@Nullable String hexString) {
        if (hexString == null || hexString.trim().isEmpty()) {
            return new ColorValidationResult( false, "Color string is null or empty" );
        }

        String trimmed = hexString.trim();

        if (!HEX_PATTERN.matcher( trimmed ).matches()) {
            return new ColorValidationResult( false, "Invalid hex color format: " + trimmed );
        }

        String cleanHex = trimmed.startsWith( "#" ) ? trimmed.substring( 1 ) : trimmed;

        switch (cleanHex.length()) {
            case 3:
                return new ColorValidationResult( true, "Valid RGB format" );
            case 4:
                return new ColorValidationResult( true, "Valid ARGB format" );
            case 6:
                return new ColorValidationResult( true, "Valid RRGGBB format" );
            case 8:
                return new ColorValidationResult( true, "Valid AARRGGBB format" );
            default:
                return new ColorValidationResult( false,
                                                  "Unsupported hex length: " + cleanHex.length() );
        }
    }

    // ==================== HEX STRING NORMALIZATION ====================

    /**
     * Normalize hex string to AARRGGBB format for consistent parsing.
     *
     * @param hexString Input hex string in any supported format
     * @return Normalized hex string in AARRGGBB format (8 characters)
     */
    @NonNull
    private static String normalizeHexString(@NonNull String hexString) {
        String cleanHex = hexString.trim();

        // Remove # if present
        if (cleanHex.startsWith( "#" )) {
            cleanHex = cleanHex.substring( 1 );
        }

        // Convert to uppercase for consistency
        cleanHex = cleanHex.toUpperCase();

        // Normalize to 8-digit AARRGGBB format
        switch (cleanHex.length()) {
            case 3: // RGB -> AARRGGBB
                return "FF" + cleanHex.charAt( 0 ) + cleanHex.charAt( 0 ) +
                        cleanHex.charAt( 1 ) + cleanHex.charAt( 1 ) +
                        cleanHex.charAt( 2 ) + cleanHex.charAt( 2 );

            case 4: // ARGB -> AARRGGBB
                return "" + cleanHex.charAt( 0 ) + cleanHex.charAt( 0 ) +
                        cleanHex.charAt( 1 ) + cleanHex.charAt( 1 ) +
                        cleanHex.charAt( 2 ) + cleanHex.charAt( 2 ) +
                        cleanHex.charAt( 3 ) + cleanHex.charAt( 3 );

            case 6: // RRGGBB -> AARRGGBB
                return "FF" + cleanHex;

            case 8: // AARRGGBB - already correct
                return cleanHex;

            default:
                throw new IllegalArgumentException( "Unsupported hex format: " + hexString );
        }
    }

    // ==================== RGB COMPONENT EXTRACTION ====================

    /**
     * Extract alpha component from color int.
     *
     * @param colorInt Android Color int
     * @return Alpha value (0-255)
     */
    public static int getAlpha(@ColorInt int colorInt) {
        return Color.alpha( colorInt );
    }

    /**
     * Extract red component from color int.
     *
     * @param colorInt Android Color int
     * @return Red value (0-255)
     */
    public static int getRed(@ColorInt int colorInt) {
        return Color.red( colorInt );
    }

    /**
     * Extract green component from color int.
     *
     * @param colorInt Android Color int
     * @return Green value (0-255)
     */
    public static int getGreen(@ColorInt int colorInt) {
        return Color.green( colorInt );
    }

    /**
     * Extract blue component from color int.
     *
     * @param colorInt Android Color int
     * @return Blue value (0-255)
     */
    public static int getBlue(@ColorInt int colorInt) {
        return Color.blue( colorInt );
    }

    /**
     * Create color from RGB components (full alpha).
     *
     * @param red   Red component (0-255)
     * @param green Green component (0-255)
     * @param blue  Blue component (0-255)
     * @return Android Color int
     */
    @ColorInt
    public static int createColor(int red, int green, int blue) {
        return Color.rgb( red, green, blue );
    }

    /**
     * Create color from ARGB components.
     *
     * @param alpha Alpha component (0-255)
     * @param red   Red component (0-255)
     * @param green Green component (0-255)
     * @param blue  Blue component (0-255)
     * @return Android Color int
     */
    @ColorInt
    public static int createColor(int alpha, int red, int green, int blue) {
        return Color.argb( alpha, red, green, blue );
    }

    // ==================== COLOR MANIPULATION ====================

    /**
     * Lighten color by specified percentage.
     *
     * @param colorInt   Original color
     * @param percentage Percentage to lighten (0.0 to 1.0)
     * @return Lightened color
     */
    @ColorInt
    public static int lightenColor(@ColorInt int colorInt, float percentage) {
        float[] hsv = new float[3];
        Color.colorToHSV( colorInt, hsv );
        hsv[2] = Math.min( 1.0f, hsv[2] + (hsv[2] * percentage) );
        return Color.HSVToColor( Color.alpha( colorInt ), hsv );
    }

    /**
     * Darken color by specified percentage.
     *
     * @param colorInt   Original color
     * @param percentage Percentage to darken (0.0 to 1.0)
     * @return Darkened color
     */
    @ColorInt
    public static int darkenColor(@ColorInt int colorInt, float percentage) {
        float[] hsv = new float[3];
        Color.colorToHSV( colorInt, hsv );
        hsv[2] = Math.max( 0.0f, hsv[2] - (hsv[2] * percentage) );
        return Color.HSVToColor( Color.alpha( colorInt ), hsv );
    }

    /**
     * Adjust color alpha/transparency.
     *
     * @param colorInt Original color
     * @param alpha    New alpha value (0-255)
     * @return Color with adjusted alpha
     */
    @ColorInt
    public static int adjustAlpha(@ColorInt int colorInt, int alpha) {
        return Color.argb( alpha, Color.red( colorInt ), Color.green( colorInt ),
                           Color.blue( colorInt ) );
    }

    // ==================== MATERIAL DESIGN HELPERS ====================

    /**
     * Get predefined Material Design colors.
     *
     * @return Map of Material color names to hex strings
     */
    @NonNull
    public static Map<String, String> getMaterialColors() {
        Map<String, String> colors = new HashMap<>();
        colors.put( "primary", DEFAULT_PRIMARY_COLOR );
        colors.put( "secondary", DEFAULT_SECONDARY_COLOR );
        colors.put( "error", DEFAULT_ERROR_COLOR );
        colors.put( "background", DEFAULT_BACKGROUND_COLOR );
        colors.put( "surface", DEFAULT_SURFACE_COLOR );

        // Palette for shifts
        colors.put( "shift_morning", "#FFE8F5E8" );    // Verde molto tenue
        colors.put( "shift_afternoon", "#FFFFF3E0" ); // Arancione molto tenue
        colors.put( "shift_night", "#FFF3E5F5" );     // Viola molto tenue

        // Palette for Priority
        colors.put( "low_priority", "#FF03A9F4");
        colors.put("normal_priority", "#FF00BCD4");
        colors.put("high_priority", "#FFFFC107");
        colors.put("override_priority", "#FFFF5722");

        // Material color palette
        colors.put( "red_500", "#FFF44336" );
        colors.put( "pink_500", "#FFE91E63" );
        colors.put( "purple_500", "#FF9C27B0" );
        colors.put( "deep_purple_500", "#FF673AB7" );
        colors.put( "indigo_500", "#FF3F51B5" );
        colors.put( "blue_500", "#FF2196F3" );
        colors.put( "light_blue_500", "#FF03A9F4" );
        colors.put( "cyan_500", "#FF00BCD4" );
        colors.put( "teal_500", "#FF009688" );
        colors.put( "green_500", "#FF4CAF50" );
        colors.put( "light_green_500", "#FF8BC34A" );
        colors.put( "lime_500", "#FFCDDC39" );
        colors.put( "yellow_500", "#FFFFEB3B" );
        colors.put( "amber_500", "#FFFFC107" );
        colors.put( "orange_500", "#FFFF9800" );
        colors.put( "deep_orange_500", "#FFFF5722" );

        // NEW: Softer _300 variants for calendar/schedule display
        colors.put( "red_300", "#FFE57373" );
        colors.put( "pink_300", "#FFF06292" );
        colors.put( "purple_300", "#FFBA68C8" );
        colors.put( "deep_purple_300", "#FF9575CD" );
        colors.put( "indigo_300", "#FF7986CB" );
        colors.put( "blue_300", "#FF64B5F6" );
        colors.put( "light_blue_300", "#FF4FC3F7" );
        colors.put( "cyan_300", "#FF4DD0E1" );
        colors.put( "teal_300", "#FF4DB6AC" );
        colors.put( "green_300", "#FF81C784" );
        colors.put( "light_green_300", "#FFAED581" );
        colors.put( "lime_300", "#FFDCE775" );
        colors.put( "yellow_300", "#FFFFF176" );
        colors.put( "amber_300", "#FFFFDB65" );
        colors.put( "orange_300", "#FFFFB74D" );
        colors.put( "deep_orange_300", "#FFFF8A65" );

        return colors;
    }

    /**
     * Check if color is considered "light" (useful for text contrast).
     *
     * @param colorInt Color to check
     * @return true if color is light
     */
    public static boolean isLightColor(@ColorInt int colorInt) {
        // Calculate luminance using standard formula
        double red = Color.red( colorInt ) / 255.0;
        double green = Color.green( colorInt ) / 255.0;
        double blue = Color.blue( colorInt ) / 255.0;

        // Apply gamma correction
        red = red < 0.03928 ? red / 12.92 : Math.pow( (red + 0.055) / 1.055, 2.4 );
        green = green < 0.03928 ? green / 12.92 : Math.pow( (green + 0.055) / 1.055, 2.4 );
        blue = blue < 0.03928 ? blue / 12.92 : Math.pow( (blue + 0.055) / 1.055, 2.4 );

        double luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue;
        return luminance > 0.5;
    }

    private static String getMaterialColor(
            @NonNull String materialColor
    ) {
        String _color;
        try {

            _color = getMaterialColors().get( materialColor );
            if (_color == null) {
                throw new IllegalArgumentException( "Invalid material color: " + materialColor );
            }
        } catch (IllegalArgumentException e) {
            Log.w( TAG, "Invalid material color: " + materialColor, e );
            _color = "#FF03A9F4"; // LightBlue
        }
        return _color;
    }

    /**
     * Get time-based color for calendar events based on start date hour.
     *
     * <p>Maps different time periods throughout the day to distinct Material Design colors
     * for improved visual organization and time-based color coding in calendar interfaces.</p>
     *
     * @param startTime Start time for color selection
     * @return Hex color string (e.g., "#FF4CAF50") corresponding to the time period
     */
    @NonNull
    public static String getTimeBasedColor(
            @NonNull LocalTime startTime
    ) {
        // Extract hour from start date (0-23)
        int hour = startTime.getHour();

        // Get Material colors map for consistent color palette
        Map<String, String> materialColors = getMaterialColors();

        // Map time periods to specific Material colors
        // Each period covers 3 hours for balanced distribution
        return switch (hour / 3) {
            case 0 -> // 00:00-03:00 - Late Night
//                    getMaterialColor( "deep_purple_500" ); // Deep Purple
                    getMaterialColor( "deep_purple_300" ); // Deep Purple

            case 1 -> // 03:00-06:00 - Early Morning
//                    getMaterialColor( "green_500" ); // Green (as requested)
                    getMaterialColor( "green_300" ); // Green (as requested)
//                    getMaterialColor( "shift_morning" );

            case 2 -> // 06:00-09:00 - Morning
//                    getMaterialColor( "light_blue_500" ); // Light Blue
                    getMaterialColor( "light_blue_300" ); // Light Blue

            case 3 -> // 09:00-12:00 - Late Morning
//                    getMaterialColor( "orange_500" ); // Orange
                    getMaterialColor( "orange_300" ); // Orange

            case 4 -> // 12:00-15:00 - Afternoon
//                    getMaterialColor( "amber_500" ); // Amber
                    getMaterialColor( "amber_300" ); // Amber
//                    getMaterialColor( "shift_afternoon" );

            case 5 -> // 15:00-18:00 - Late Afternoon
//                    getMaterialColor( "red_500" ); // Red
                    getMaterialColor( "red_300" ); // Red

            case 6 -> // 18:00-21:00 - Evening
//                    getMaterialColor( "indigo_500" ); // Indigo
                    getMaterialColor( "indigo_300" ); // Indigo

            case 7 -> // 21:00-24:00 - Night
//                    getMaterialColor( "purple_500" ); // Purple
                    getMaterialColor( "purple_300" ); // Purple
//                    getMaterialColor( "shift_night" ); // Night

            default -> {
                // Fallback for any edge cases
                Log.w( TAG, "Unexpected hour value: " + hour + ", using default blue" );
                yield getMaterialColor( "unknown" );
            }
        };
    }

    /**
     * Apply alpha transparency to existing hex color.
     *
     * @param hexColor        Original hex color (with or without alpha)
     * @param alphaPercentage Alpha value (0.0 to 1.0)
     * @return Hex color with alpha applied
     */
    @ColorInt
    public static int applyAlphaToColor(@NonNull String hexColor, float alphaPercentage) {
        // Clamp alpha percentage
        alphaPercentage = Math.max( 0.0f, Math.min( 1.0f, alphaPercentage ) );

        try {
            // Convert to color int
            int colorInt = hexStringToColorInt( hexColor );

            // Calculate alpha value (0-255)
            int alpha = Math.round( 255 * alphaPercentage );

            // Apply alpha to color

            return adjustAlpha( colorInt, alpha );
        } catch (Exception e) {
            Log.e( TAG, "Error applying alpha to color: " + hexColor, e );
            return Color.parseColor( hexColor ); // Return original on error
        }
    }

    // ==================== INNER CLASSES ====================

    /**
     * Color validation result container.
     */
    public record ColorValidationResult(boolean isValid, String message)
    {

        @NonNull
        @Override
        public String toString() {
            return "ColorValidation{valid=" + isValid + ", message='" + message + "'}";
        }
    }

    // ==================== DEBUGGING ====================

    /**
     * Get detailed color information for debugging.
     *
     * @param colorInt Color to analyze
     * @return Formatted string with color details
     */
    @NonNull
    public static String debugColorInfo(@ColorInt int colorInt) {
        return String.format( QDue.getLocale(),
                              "Color Debug Info:\n" +
                                      "  Hex: %s\n" +
                                      "  RGB Hex: %s\n" +
                                      "  ARGB: A=%d, R=%d, G=%d, B=%d\n" +
                                      "  Is Light: %s",
                              colorIntToHexString( colorInt ),
                              colorIntToRgbHexString( colorInt ),
                              getAlpha( colorInt ), getRed( colorInt ), getGreen( colorInt ),
                              getBlue( colorInt ),
                              isLightColor( colorInt )
        );
    }
}