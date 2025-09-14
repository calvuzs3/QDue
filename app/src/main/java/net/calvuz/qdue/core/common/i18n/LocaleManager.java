package net.calvuz.qdue.core.common.i18n;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * LocaleManager - Dynamic Language Switching for QDue Application.
 *
 * <p>Provides comprehensive internationalization support with dynamic locale switching,
 * cultural adaptation, and seamless language changes without app restart. Supports
 * the gradual rollout strategy for QDue's international expansion.</p>
 *
 * <h3>Supported Languages:</h3>
 * <ul>
 *   <li><strong>Italian (IT)</strong> - Primary language (complete)</li>
 *   <li><strong>English (EN)</strong> - Secondary language (Phase 2)</li>
 *   <li><strong>German (DE)</strong> - Planned for Phase 3 (industrial focus)</li>
 *   <li><strong>French (FR)</strong> - Planned for Phase 3</li>
 * </ul>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Dynamic locale switching without app restart</li>
 *   <li>Cultural date/time formatting adaptation</li>
 *   <li>Automatic system locale detection</li>
 *   <li>Fallback to primary language for missing translations</li>
 *   <li>Work schedule terminology localization</li>
 * </ul>
 */
public class LocaleManager {

    private static final String TAG = "LocaleManager";

    // Preference keys
    private static final String PREF_SELECTED_LANGUAGE = "selected_language";
    private static final String PREF_USE_SYSTEM_LANGUAGE = "use_system_language";

    // Supported language codes
    public static final String LANGUAGE_ITALIAN = "it";
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_GERMAN = "de";    // Planned Phase 3
    public static final String LANGUAGE_FRENCH = "fr";    // Planned Phase 3
    public static final String LANGUAGE_SYSTEM = "system";

    // Default language (Italian - primary market)
    public static final String DEFAULT_LANGUAGE = LANGUAGE_ITALIAN;

    // Dependencies
    private final Context mContext;
    private final SharedPreferences mPreferences;

    // Current locale state
    private Locale mCurrentLocale;
    private boolean mUseSystemLanguage;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for LocaleManager with dependency injection.
     *
     * @param context Application context for accessing resources and preferences
     */
    public LocaleManager(@NonNull Context context) {
        this.mContext = context.getApplicationContext();
        this.mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        // Initialize current state
        initializeLocale();

        Log.d(TAG, "LocaleManager initialized. Current locale: " + mCurrentLocale);
    }

    // ==================== LOCALE INITIALIZATION ====================

    /**
     * Initialize locale settings from preferences or system default.
     */
    private void initializeLocale() {
        mUseSystemLanguage = mPreferences.getBoolean(PREF_USE_SYSTEM_LANGUAGE, true);

        if (mUseSystemLanguage) {
            mCurrentLocale = getSystemLocale();
        } else {
            String savedLanguage = mPreferences.getString(PREF_SELECTED_LANGUAGE, DEFAULT_LANGUAGE);
            mCurrentLocale = getLocaleFromLanguageCode(savedLanguage);
        }

        // Validate that we support this locale
        if (!isSupportedLocale(mCurrentLocale)) {
            Log.w(TAG, "Unsupported locale detected: " + mCurrentLocale + ". Falling back to default.");
            mCurrentLocale = getLocaleFromLanguageCode(DEFAULT_LANGUAGE);
        }
    }

    // ==================== LOCALE MANAGEMENT ====================

    /**
     * Get current application locale.
     *
     * @return Current Locale object
     */
    @NonNull
    public Locale getCurrentLocale() {
        return mCurrentLocale;
    }

    /**
     * Set application language with immediate effect.
     *
     * @param languageCode Language code (e.g., "it", "en", "de", "fr")
     * @return true if language was changed successfully
     */
    public boolean setLanguage(@NonNull String languageCode) {
        if (LANGUAGE_SYSTEM.equals(languageCode)) {
            return setUseSystemLanguage(true);
        }

        if (!isSupportedLanguage(languageCode)) {
            Log.w(TAG, "Attempted to set unsupported language: " + languageCode);
            return false;
        }

        Locale newLocale = getLocaleFromLanguageCode(languageCode);
        if (newLocale.equals(mCurrentLocale)) {
            return false; // No change needed
        }

        // Save preference
        mPreferences.edit()
                .putString(PREF_SELECTED_LANGUAGE, languageCode)
                .putBoolean(PREF_USE_SYSTEM_LANGUAGE, false)
                .apply();

        // Update current state
        mCurrentLocale = newLocale;
        mUseSystemLanguage = false;

        // Apply locale change
        applyLocaleChange();

        Log.d(TAG, "Language changed to: " + languageCode + " (" + newLocale + ")");
        return true;
    }

    /**
     * Set whether to use system language.
     *
     * @param useSystemLanguage true to follow system language
     * @return true if setting was changed
     */
    public boolean setUseSystemLanguage(boolean useSystemLanguage) {
        if (mUseSystemLanguage == useSystemLanguage) {
            return false; // No change needed
        }

        mUseSystemLanguage = useSystemLanguage;

        if (useSystemLanguage) {
            Locale systemLocale = getSystemLocale();
            if (!systemLocale.equals(mCurrentLocale)) {
                mCurrentLocale = isSupportedLocale(systemLocale) ?
                        systemLocale : getLocaleFromLanguageCode(DEFAULT_LANGUAGE);
                applyLocaleChange();
            }
        }

        // Save preference
        mPreferences.edit()
                .putBoolean(PREF_USE_SYSTEM_LANGUAGE, useSystemLanguage)
                .apply();

        Log.d(TAG, "Use system language: " + useSystemLanguage + ". Current locale: " + mCurrentLocale);
        return true;
    }

    /**
     * Check if currently using system language setting.
     *
     * @return true if following system language
     */
    public boolean isUsingSystemLanguage() {
        return mUseSystemLanguage;
    }

    // ==================== LOCALE UTILITIES ====================

    /**
     * Get system default locale.
     *
     * @return System default Locale
     */
    @NonNull
    private Locale getSystemLocale() {
        LocaleList localeList = mContext.getResources().getConfiguration().getLocales();
        if (!localeList.isEmpty()) {
            // Find first supported locale in system preferences
            for (int i = 0; i < localeList.size(); i++) {
                Locale locale = localeList.get(i);
                if (isSupportedLocale(locale)) {
                    return locale;
                }
            }
        }

        // Fallback for older Android versions or no supported locale found
        Locale systemLocale = Locale.getDefault();
        return isSupportedLocale(systemLocale) ? systemLocale :
                getLocaleFromLanguageCode(DEFAULT_LANGUAGE);
    }

    /**
     * Convert language code to Locale object.
     *
     * @param languageCode Language code (e.g., "it", "en")
     * @return Corresponding Locale object
     */
    @NonNull
    private Locale getLocaleFromLanguageCode(@NonNull String languageCode) {
        switch (languageCode.toLowerCase()) {
            case LANGUAGE_ITALIAN:
                return new Locale("it", "IT");
            case LANGUAGE_ENGLISH:
                return new Locale("en", "US"); // Default to US English
            case LANGUAGE_GERMAN:
                return new Locale("de", "DE");
            case LANGUAGE_FRENCH:
                return new Locale("fr", "FR");
            default:
                Log.w(TAG, "Unknown language code: " + languageCode + ". Using default.");
                return new Locale("it", "IT"); // Default to Italian
        }
    }

    /**
     * Check if a language code is supported.
     *
     * @param languageCode Language code to check
     * @return true if supported
     */
    public boolean isSupportedLanguage(@Nullable String languageCode) {
        if (languageCode == null) return false;

        return getSupportedLanguages().contains(languageCode.toLowerCase());
    }

    /**
     * Check if a locale is supported.
     *
     * @param locale Locale to check
     * @return true if supported
     */
    public boolean isSupportedLocale(@Nullable Locale locale) {
        if (locale == null) return false;

        return isSupportedLanguage(locale.getLanguage());
    }

    /**
     * Get list of supported language codes.
     *
     * @return List of supported language codes
     */
    @NonNull
    public List<String> getSupportedLanguages() {
        return Arrays.asList(
                LANGUAGE_ITALIAN        // Primary - complete
                //LANGUAGE_ENGLISH      // Phase 2 - planned
                // LANGUAGE_GERMAN,     // Phase 3 - planned
                // LANGUAGE_FRENCH      // Phase 3 - planned
        );
    }

    /**
     * Get list of all planned languages (including future phases).
     *
     * @return List of all planned language codes
     */
    @NonNull
    public List<String> getAllPlannedLanguages() {
        return Arrays.asList(
                LANGUAGE_ITALIAN,
                LANGUAGE_ENGLISH,   // Phase 2
                LANGUAGE_GERMAN,    // Phase 3
                LANGUAGE_FRENCH     // Phase 3
        );
    }

    // ==================== LOCALE APPLICATION ====================

    /**
     * Apply locale change to application context.
     * This method updates the app's resources to reflect the new locale.
     */
    private void applyLocaleChange() {
        Resources resources = mContext.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());

        configuration.setLocale( mCurrentLocale );

        // Update configuration
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        // Set as default for the JVM (affects date formatting, etc.)
        Locale.setDefault(mCurrentLocale);

        Log.d(TAG, "Applied locale change: " + mCurrentLocale);
    }

    /**
     * Update context with current locale. Use this method to ensure
     * Activities and Services use the correct locale.
     *
     * @param context Context to update
     * @return Context with updated locale
     */
    @NonNull
    public Context updateContextLocale(@NonNull Context context) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());

        configuration.setLocale( mCurrentLocale );

        return context.createConfigurationContext( configuration );
    }

    // ==================== LANGUAGE INFORMATION ====================

    /**
     * Get display name for a language code in current locale.
     *
     * @param languageCode Language code
     * @return Display name in current language
     */
    @NonNull
    public String getLanguageDisplayName(@NonNull String languageCode) {
        Locale locale = getLocaleFromLanguageCode(languageCode);
        return locale.getDisplayName(mCurrentLocale);
    }

    /**
     * Get native display name for a language code.
     *
     * @param languageCode Language code
     * @return Display name in the language itself
     */
    @NonNull
    public String getLanguageNativeName(@NonNull String languageCode) {
        Locale locale = getLocaleFromLanguageCode(languageCode);
        return locale.getDisplayName(locale);
    }

    /**
     * Get current language code.
     *
     * @return Current language code (e.g., "it", "en")
     */
    @NonNull
    public String getCurrentLanguageCode() {
        return mCurrentLocale.getLanguage();
    }

    // ==================== CULTURAL ADAPTATIONS ====================

    /**
     * Check if current locale uses 24-hour time format.
     *
     * @return true if 24-hour format should be used
     */
    public boolean is24HourFormat() {
        String language = getCurrentLanguageCode();

        // Most European languages prefer 24-hour format
        // US English typically uses 12-hour format
        switch (language) {
            case LANGUAGE_ENGLISH:
                // Check if it's US English (12-hour) or UK English (24-hour)
                return !"US".equals(mCurrentLocale.getCountry());
            case LANGUAGE_ITALIAN:
            case LANGUAGE_GERMAN:
            case LANGUAGE_FRENCH:
            default:
                return true; // Default to 24-hour for European languages
        }
    }

    /**
     * Get appropriate date format pattern for current locale.
     *
     * @return Date format pattern string
     */
    @NonNull
    public String getDateFormatPattern() {
        String language = getCurrentLanguageCode();

        switch (language) {
            case LANGUAGE_ENGLISH:
                // US: MM/dd/yyyy, UK: dd/MM/yyyy
                return "US".equals(mCurrentLocale.getCountry()) ? "MM/dd/yyyy" : "dd/MM/yyyy";
            case LANGUAGE_GERMAN:
            case LANGUAGE_FRENCH:
            case LANGUAGE_ITALIAN:
            default:
                return "dd/MM/yyyy";
        }
    }

    /**
     * Get quattrodue system terminology in current language.
     *
     * @return Localized name for the four-two system
     */
    @NonNull
    public String getQuattrodueTerminology() {
        String language = getCurrentLanguageCode();

        switch (language) {
            case LANGUAGE_ENGLISH:
                return "Four-Two System";
            case LANGUAGE_GERMAN:
                return "Vier-Zwei-System";
            case LANGUAGE_FRENCH:
                return "Système Quatre-Deux";
            case LANGUAGE_ITALIAN:
            default:
                return "Sistema Quattro-Due";
        }
    }

    // ==================== DEBUGGING AND UTILITIES ====================

    /**
     * Get debug information about current locale state.
     *
     * @return Debug information string
     */
    @NonNull
    public String getDebugInfo() {
        return "LocaleManager Debug Info:\n" +
                "Current Locale: " + mCurrentLocale + "\n" +
                "Language Code: " + getCurrentLanguageCode() + "\n" +
                "Use System Language: " + mUseSystemLanguage + "\n" +
                "System Locale: " + getSystemLocale() + "\n" +
                "Supported Languages: " + getSupportedLanguages() + "\n" +
                "24-Hour Format: " + is24HourFormat() + "\n" +
                "Date Pattern: " + getDateFormatPattern();
    }


    // ==================== CALENDAR LOCALIZATION METHODS ====================

    /**
     * Get localized shift name by shift type.
     *
     * @param shiftType Shift type (MORNING, AFTERNOON, NIGHT, CUSTOM)
     * @return Localized shift name
     */
    @NonNull
    public String getShiftName(@NonNull String shiftType) {
        String language = getCurrentLanguageCode();

        switch (shiftType.toUpperCase()) {
            case "MORNING":
                switch (language) {
                    case LANGUAGE_ENGLISH:
                        return "Morning";
                    case LANGUAGE_GERMAN:
                        return "Morgen";
                    case LANGUAGE_FRENCH:
                        return "Matin";
                    case LANGUAGE_ITALIAN:
                    default:
                        return "Mattino";
                }

            case "AFTERNOON":
                switch (language) {
                    case LANGUAGE_ENGLISH:
                        return "Afternoon";
                    case LANGUAGE_GERMAN:
                        return "Nachmittag";
                    case LANGUAGE_FRENCH:
                        return "Après-midi";
                    case LANGUAGE_ITALIAN:
                    default:
                        return "Pomeriggio";
                }

            case "NIGHT":
                switch (language) {
                    case LANGUAGE_ENGLISH:
                        return "Night";
                    case LANGUAGE_GERMAN:
                        return "Nacht";
                    case LANGUAGE_FRENCH:
                        return "Nuit";
                    case LANGUAGE_ITALIAN:
                    default:
                        return "Notte";
                }

            case "CUSTOM":
                switch (language) {
                    case LANGUAGE_ENGLISH:
                        return "Custom";
                    case LANGUAGE_GERMAN:
                        return "Benutzerdefiniert";
                    case LANGUAGE_FRENCH:
                        return "Personnalisé";
                    case LANGUAGE_ITALIAN:
                    default:
                        return "Personalizzato";
                }

            default:
                Log.w(TAG, "Unknown shift type: " + shiftType);
                return shiftType;
        }
    }

    /**
     * Get localized shift description by shift type.
     *
     * @param shiftType Shift type (MORNING, AFTERNOON, NIGHT, CUSTOM)
     * @return Localized shift description
     */
    @NonNull
    public String getShiftDescription(@NonNull String shiftType) {
        String language = getCurrentLanguageCode();

        switch (shiftType.toUpperCase()) {
            case "MORNING":
                switch (language) {
                    case LANGUAGE_ENGLISH:
                        return "Standard morning shift";
                    case LANGUAGE_GERMAN:
                        return "Standard-Morgenschicht";
                    case LANGUAGE_FRENCH:
                        return "Équipe de matin standard";
                    case LANGUAGE_ITALIAN:
                    default:
                        return "Turno standard del mattino";
                }

            case "AFTERNOON":
                switch (language) {
                    case LANGUAGE_ENGLISH:
                        return "Standard afternoon shift";
                    case LANGUAGE_GERMAN:
                        return "Standard-Nachmittagsschicht";
                    case LANGUAGE_FRENCH:
                        return "Équipe d'après-midi standard";
                    case LANGUAGE_ITALIAN:
                    default:
                        return "Turno standard del pomeriggio";
                }

            case "NIGHT":
                switch (language) {
                    case LANGUAGE_ENGLISH:
                        return "Standard night shift";
                    case LANGUAGE_GERMAN:
                        return "Standard-Nachtschicht";
                    case LANGUAGE_FRENCH:
                        return "Équipe de nuit standard";
                    case LANGUAGE_ITALIAN:
                    default:
                        return "Turno standard della notte";
                }

            case "CUSTOM":
                switch (language) {
                    case LANGUAGE_ENGLISH:
                        return "User-defined shift";
                    case LANGUAGE_GERMAN:
                        return "Benutzerdefinierte Schicht";
                    case LANGUAGE_FRENCH:
                        return "Équipe personnalisée";
                    case LANGUAGE_ITALIAN:
                    default:
                        return "Turno personalizzato";
                }

            default:
                Log.w(TAG, "Unknown shift type: " + shiftType);
                switch (language) {
                    case LANGUAGE_ENGLISH:
                        return "Unknown shift";
                    case LANGUAGE_GERMAN:
                        return "Unbekannte Schicht";
                    case LANGUAGE_FRENCH:
                        return "Équipe inconnue";
                    case LANGUAGE_ITALIAN:
                    default:
                        return "Turno sconosciuto";
                }
        }
    }

    /**
     * Get localized team description template.
     *
     * @return Localized team description template
     */
    @NonNull
    public String getTeamDescriptionTemplate() {
        String language = getCurrentLanguageCode();

        switch (language) {
            case LANGUAGE_ENGLISH:
                return "Standard QuattroDue team";
            case LANGUAGE_GERMAN:
                return "Standard Vier-Zwei Team";
            case LANGUAGE_FRENCH:
                return "Équipe Quatre-Deux standard";
            case LANGUAGE_ITALIAN:
            default:
                return "Team standard QuattroDue";
        }
    }

    /**
     * Get localized team display name.
     *
     * @param teamCode Team code (A, B, C, etc.)
     * @return Localized team display name
     */
    @NonNull
    public String getTeamDisplayName(@NonNull String teamCode) {
        String language = getCurrentLanguageCode();

        switch (language) {
            case LANGUAGE_ENGLISH:
                return "Team " + teamCode;
            case LANGUAGE_GERMAN:
                return "Team " + teamCode;
            case LANGUAGE_FRENCH:
                return "Équipe " + teamCode;
            case LANGUAGE_ITALIAN:
            default:
                return "Team " + teamCode;
        }
    }

    /**
     * Get localized break time text.
     *
     * @return Localized break time text
     */
    @NonNull
    public String getBreakTimeText() {
        String language = getCurrentLanguageCode();

        switch (language) {
            case LANGUAGE_ENGLISH:
                return "Break";
            case LANGUAGE_GERMAN:
                return "Pause";
            case LANGUAGE_FRENCH:
                return "Pause";
            case LANGUAGE_ITALIAN:
            default:
                return "Pausa";
        }
    }

    /**
     * Get localized work duration text.
     *
     * @return Localized work duration text
     */
    @NonNull
    public String getWorkDurationText() {
        String language = getCurrentLanguageCode();

        switch (language) {
            case LANGUAGE_ENGLISH:
                return "Work duration";
            case LANGUAGE_GERMAN:
                return "Arbeitszeit";
            case LANGUAGE_FRENCH:
                return "Durée de travail";
            case LANGUAGE_ITALIAN:
            default:
                return "Durata lavorativa";
        }
    }

    // ==================== STATIC UTILITY METHODS ====================

    /**
     * Get localized shift name using context (static utility).
     *
     * @param context Application context
     * @param shiftType Shift type
     * @return Localized shift name
     */
    @NonNull
    public static String getShiftName(@NonNull Context context, @NonNull String shiftType) {
        LocaleManager localeManager = new LocaleManager(context);
        return localeManager.getShiftName(shiftType);
    }

    /**
     * Get localized shift description using context (static utility).
     *
     * @param context Application context
     * @param shiftType Shift type
     * @return Localized shift description
     */
    @NonNull
    public static String getShiftDescription(@NonNull Context context, @NonNull String shiftType) {
        LocaleManager localeManager = new LocaleManager(context);
        return localeManager.getShiftDescription(shiftType);
    }

    /**
     * Get localized team description template using context (static utility).
     *
     * @param context Application context
     * @return Localized team description template
     */
    @NonNull
    public static String getTeamDescriptionTemplate(@NonNull Context context) {
        LocaleManager localeManager = new LocaleManager(context);
        return localeManager.getTeamDescriptionTemplate();
    }

    /**
     * Get localized team display name using context (static utility).
     *
     * @param context Application context
     * @param teamCode Team code
     * @return Localized team display name
     */
    @NonNull
    public static String getTeamDisplayName(@NonNull Context context, @NonNull String teamCode) {
        LocaleManager localeManager = new LocaleManager(context);
        return localeManager.getTeamDisplayName(teamCode);
    }





// ==================== RECURRENCE RULE I18N METHODS ====================

    /**
     * Get localized display name for recurrence frequency.
     *
     * @param context Application context (null safe)
     * @param frequencyKey Frequency key (DAILY, WEEKLY, MONTHLY, QUATTRODUE_CYCLE)
     * @return Localized frequency display name
     */
    @NonNull
    public static String getFrequencyDisplayName(@Nullable Context context, @NonNull String frequencyKey) {
        // Implementation would go here
        // Italian examples:
        // DAILY -> "Giornaliero"
        // WEEKLY -> "Settimanale"
        // MONTHLY -> "Mensile"
        // QUATTRODUE_CYCLE -> "Ciclo QuattroDue"
        return getLocalizedString(context, "frequency_" + frequencyKey.toLowerCase(), frequencyKey);
    }

    /**
     * Get localized description for recurrence frequency.
     */
    @NonNull
    public static String getFrequencyDescription(@Nullable Context context, @NonNull String frequencyKey) {
        // Italian examples:
        // DAILY -> "Ripetizione giornaliera"
        // WEEKLY -> "Ripetizione settimanale su giorni specifici"
        // QUATTRODUE_CYCLE -> "Pattern QuattroDue 4-2 (28 giorni lavoro, 14 riposo)"
        return getLocalizedString(context, "frequency_desc_" + frequencyKey.toLowerCase(), "Ripetizione " + frequencyKey);
    }

    /**
     * Get localized display name for end type.
     */
    @NonNull
    public static String getEndTypeDisplayName(@Nullable Context context, @NonNull String endTypeKey) {
        // Italian examples:
        // NEVER -> "Mai"
        // COUNT -> "Dopo N occorrenze"
        // UNTIL_DATE -> "Fino a data"
        return getLocalizedString(context, "end_type_" + endTypeKey.toLowerCase(), endTypeKey);
    }

    /**
     * Get localized description for end type.
     */
    @NonNull
    public static String getEndTypeDescription(@Nullable Context context, @NonNull String endTypeKey) {
        return getLocalizedString(context, "end_type_desc_" + endTypeKey.toLowerCase(), "Condizione di fine: " + endTypeKey);
    }

    @NonNull
    public static String getWeekStartDisplayName(@Nullable Context context, @NonNull String weekStartKey) {
        return getLocalizedString(context, "week_start_" + weekStartKey.toLowerCase(), weekStartKey);
    }

    /**
     * Get localized recurrence rule name.
     */
    @Deprecated
    @NonNull
    public static String getRecurrenceRuleName(@Nullable Context context, @NonNull String ruleTypeKey) {
        // Italian examples:
        // QUATTRODUE_CYCLE -> "Ciclo QuattroDue 4-2"
        // WEEKDAYS -> "Solo giorni feriali"
        // DAILY -> "Pattern giornaliero"
        return getLocalizedString(context, "recurrence_rule_" + ruleTypeKey.toLowerCase(), ruleTypeKey);
    }

    /**
     * Get localized recurrence rule description.
     */
    @Deprecated
    @NonNull
    public static String getRecurrenceRuleDescription(@Nullable Context context, @NonNull String ruleTypeKey) {
        return getLocalizedString(context, "recurrence_rule_desc_" + ruleTypeKey.toLowerCase(), "Regola di ricorrenza: " + ruleTypeKey);
    }

    /**
     * Get localized recurrence rule description with interval.
     */
    @NonNull
    public static String getRecurrenceRuleDescriptionWithInterval(@Nullable Context context, @NonNull String ruleTypeKey, int interval) {
        // Italian examples:
        // DAILY, 2 -> "Ogni 2 giorni"
        // WEEKLY, 3 -> "Ogni 3 settimane"
        String template = getLocalizedString(context, "recurrence_rule_interval_" + ruleTypeKey.toLowerCase(), "Ogni %d " + ruleTypeKey);
        return String.format(getCurrentLocale(context), template, interval);
    }

    /**
     * Get localized end condition description.
     */
    @NonNull
    public static String getRecurrenceEndCondition(@Nullable Context context, @NonNull String conditionKey) {
        // Italian examples:
        // NEVER -> "Nessuna fine"
        // COUNT -> "Numero occorrenze"
        // UNTIL_DATE -> "Fino a data specifica"
        return getLocalizedString(context, "recurrence_end_" + conditionKey.toLowerCase(), conditionKey);
    }

    /**
     * Get localized end condition with count.
     */
    @NonNull
    public static String getRecurrenceEndConditionWithCount(@Nullable Context context, int count) {
        // Italian: "Termina dopo 5 occorrenze"
        String template = getLocalizedString(context, "recurrence_end_count_template", "Termina dopo %d occorrenze");
        return String.format(getCurrentLocale(context), template, count);
    }

    /**
     * Get localized end condition with date.
     */
    @NonNull
    public static String getRecurrenceEndConditionWithDate(@Nullable Context context, @NonNull LocalDate endDate) {
        // Italian: "Termina il 31/12/2024"
        String template = getLocalizedString(context, "recurrence_end_date_template", "Termina il %s");
        String formattedDate = formatDate(context, endDate);
        return String.format(getCurrentLocale(context), template, formattedDate);
    }

// ==================== SHIFT EXCEPTION I18N METHODS ====================

    /**
     * Get localized display name for exception type.
     */
    @NonNull
    public static String getExceptionTypeDisplayName(@Nullable Context context, @NonNull String typeKey) {
        // Italian examples:
        // vacation -> "Ferie"
        // sick_leave -> "Malattia"
        // shift_swap -> "Scambio turno"
        // personal_reduction -> "Riduzione personale"
        // rol_reduction -> "ROL"
        return getLocalizedString(context, "exception_type_" + typeKey, typeKey);
    }

    /**
     * Get localized description for exception type.
     */
    @NonNull
    public static String getExceptionTypeDescription(@Nullable Context context, @NonNull String descriptionKey) {
        // Italian examples:
        // planned_vacation_holiday -> "Ferie pianificate o vacanza"
        // medical_absence -> "Assenza per motivi medici"
        // voluntary_swap_colleague -> "Scambio volontario con collega"
        return getLocalizedString(context, "exception_desc_" + descriptionKey, descriptionKey);
    }

    /**
     * Get localized display name for approval status.
     */
    @NonNull
    public static String getApprovalStatusDisplayName(@Nullable Context context, @NonNull String statusKey) {
        // Italian examples:
        // draft -> "Bozza"
        // pending -> "In attesa"
        // approved -> "Approvato"
        // rejected -> "Rifiutato"
        return getLocalizedString(context, "approval_status_" + statusKey, statusKey);
    }

    /**
     * Get localized description for approval status.
     */
    @NonNull
    public static String getApprovalStatusDescription(@Nullable Context context, @NonNull String descriptionKey) {
        // Italian examples:
        // being_created_not_submitted -> "In creazione, non ancora inviato"
        // submitted_awaiting_approval -> "Inviato, in attesa di approvazione"
        return getLocalizedString(context, "approval_desc_" + descriptionKey, descriptionKey);
    }

    /**
     * Get localized display name for priority level.
     */
    @NonNull
    public static String getPriorityDisplayName(@Nullable Context context, @NonNull String priorityKey) {
        // Italian examples:
        // low_priority -> "Priorità bassa"
        // normal_priority -> "Priorità normale"
        // high_priority -> "Priorità alta"
        // urgent_priority -> "Priorità urgente"
        return getLocalizedString(context, "enum_priority_" + priorityKey, priorityKey);
    }

    /**
     * Get localized default exception title.
     */
    @NonNull
    public static String getDefaultExceptionTitle(@Nullable Context context, @NonNull String exceptionTypeKey) {
        // Italian examples:
        // ABSENCE_VACATION -> "Ferie"
        // ABSENCE_SICK -> "Malattia"
        // CHANGE_SWAP -> "Scambio turno"
        return getLocalizedString(context, "default_exception_title_" + exceptionTypeKey.toLowerCase(), exceptionTypeKey);
    }

    /**
     * Get localized default shift swap title with user reference.
     */
    @NonNull
    public static String getDefaultShiftSwapTitle(@Nullable Context context, @NonNull Long swapWithUserId) {
        // Italian: "Scambio turno con utente 123"
        String template = getLocalizedString(context, "default_shift_swap_title", "Scambio turno con utente %d");
        return String.format(getCurrentLocale(context), template, swapWithUserId);
    }

    /**
     * Get localized default time reduction title.
     */
    @NonNull
    public static String getDefaultTimeReductionTitle(@Nullable Context context, @NonNull String reductionTypeKey) {
        // Italian examples:
        // personal_reduction -> "Riduzione orario personale"
        // rol_reduction -> "ROL"
        // union_time -> "Permesso sindacale"
        return getLocalizedString(context, "default_reduction_title_" + reductionTypeKey, reductionTypeKey);
    }

// ==================== USER SCHEDULE ASSIGNMENT I18N METHODS ====================

    /**
     * Get localized display name for assignment priority.
     */
    @NonNull
    public static String getAssignmentPriorityDisplayName(@Nullable Context context, @NonNull String priorityKey) {
        // Italian examples:
        // low_priority -> "Priorità bassa"
        // normal_priority -> "Priorità normale"
        // high_priority -> "Priorità alta"
        // override_priority -> "Priorità sostituzione"
        return getLocalizedString(context, "assignment_priority_" + priorityKey, priorityKey);
    }

    /**
     * Get localized display name for assignment status.
     */
    @NonNull
    public static String getAssignmentStatusDisplayName(@Nullable Context context, @NonNull String statusKey) {
        // Italian examples:
        // active -> "Attivo"
        // pending -> "In attesa"
        // expired -> "Scaduto"
        // suspended -> "Sospeso"
        // cancelled -> "Annullato"
        return getLocalizedString(context, "assignment_status_" + statusKey, statusKey);
    }

    /**
     * Get localized description for assignment status.
     */
    @NonNull
    public static String getAssignmentStatusDescription(@Nullable Context context, @NonNull String descriptionKey) {
        // Italian examples:
        // currently_active_assignment -> "Assegnazione attualmente attiva"
        // future_assignment_not_yet_active -> "Assegnazione futura non ancora attiva"
        return getLocalizedString(context, "assignment_status_desc_" + descriptionKey, descriptionKey);
    }

    /**
     * Get localized user display name template.
     */
    @NonNull
    public static String getUserDisplayNameTemplate(@Nullable Context context, @NonNull Long userId) {
        // Italian: "Utente 123"
        String template = getLocalizedString(context, "user_display_template", "Utente %d");
        return String.format(getCurrentLocale(context), template, userId);
    }

    /**
     * Get localized assignment display title.
     */
    @NonNull
    public static String getAssignmentDisplayTitle(@Nullable Context context, @NonNull String userDisplay, @NonNull String teamDisplay) {
        // Italian: "Marco Rossi → Team A"
        String template = getLocalizedString(context, "assignment_display_title", "%s → %s");
        return String.format(getCurrentLocale(context), template, userDisplay, teamDisplay);
    }

    /**
     * Get localized permanent assignment period description.
     */
    @NonNull
    public static String getAssignmentPermanentPeriod(@Nullable Context context, @NonNull LocalDate startDate) {
        // Italian: "Dal 01/01/2024 (Permanente)"
        String template = getLocalizedString(context, "assignment_permanent_period", "Dal %s (Permanente)");
        String formattedDate = formatDate(context, startDate);
        return String.format(getCurrentLocale(context), template, formattedDate);
    }

    /**
     * Get localized time period description.
     */
    @NonNull
    public static String getAssignmentTimePeriod(@Nullable Context context, @NonNull LocalDate startDate, @Nullable LocalDate endDate) {
        // Italian: "Dal 01/01/2024 al 31/12/2024"
        String template = getLocalizedString(context, "assignment_time_period", "Dal %s al %s");
        String formattedStartDate = formatDate(context, startDate);
        String formattedEndDate = endDate != null ? formatDate(context, endDate) : "";
        return String.format(getCurrentLocale(context), template, formattedStartDate, formattedEndDate);
    }

    /**
     * Get localized standard assignment title.
     */
    @NonNull
    public static String getStandardAssignmentTitle(@Nullable Context context) {
        // Italian: "Assegnazione standard team"
        return getLocalizedString(context, "standard_assignment_title", "Assegnazione standard team");
    }

    /**
     * Get localized temporary assignment title.
     */
    @NonNull
    public static String getTemporaryAssignmentTitle(@Nullable Context context) {
        // Italian: "Assegnazione temporanea"
        return getLocalizedString(context, "temporary_assignment_title", "Assegnazione temporanea");
    }

    /**
     * Get localized team transfer title.
     */
    @NonNull
    public static String getTeamTransferTitle(@Nullable Context context) {
        // Italian: "Trasferimento team"
        return getLocalizedString(context, "team_transfer_title", "Trasferimento team");
    }

    // ==================== PUBLIC LOCALIZED STRING ====================

    /**
     * Helper method to get localized string with fallback.
     * This integrates with the existing LocaleManager pattern.
     */
    @NonNull
    public static String getLocalizedString(@Nullable Context context, @NonNull String key, @NonNull String fallback) {
        if (context == null) {
            return fallback;
        }

        try {
            Resources resources = context.getResources();
            int resourceId = resources.getIdentifier(key, "string", context.getPackageName());

            if (resourceId != 0) {
                return resources.getString(resourceId);
            }
        } catch (Exception e) {
            // Resource not found or error accessing it
        }

        // Return fallback if resource not found
        return fallback;
    }

    /**
     * Helper method to get current locale.
     * This method should already exist in LocaleManager.
     */
    @NonNull
    public static Locale getCurrentLocale(@NonNull Context context) {
        LocaleManager localeManager = new LocaleManager(context);
        return localeManager.getCurrentLocale();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Helper method to format date according to current locale.
     * This method should already exist in LocaleManager or needs to be implemented.
     */
    @NonNull
    private static String formatDate(@Nullable Context context, @NonNull LocalDate date) {
        // Implementation would format date according to current locale preferences
        return date.toString(); // Placeholder implementation
    }

// ==================== STRING RESOURCES REQUIRED ====================

    /*
     * The following string resources need to be added to res/values/strings.xml (Italian)
     * and res/values-en/strings.xml (English) and other supported languages:
     *
     * <!-- Recurrence Rule Frequencies -->
     * <string name="frequency_daily">Giornaliero</string>
     * <string name="frequency_weekly">Settimanale</string>
     * <string name="frequency_monthly">Mensile</string>
     * <string name="frequency_quattrodue_cycle">Ciclo QuattroDue</string>
     *
     * <!-- Exception Types -->
     * <string name="exception_type_vacation">Ferie</string>
     * <string name="exception_type_sick_leave">Malattia</string>
     * <string name="exception_type_shift_swap">Scambio turno</string>
     * <string name="exception_type_personal_reduction">Riduzione personale</string>
     * <string name="exception_type_rol_reduction">ROL</string>
     * <string name="exception_type_union_time">Permesso sindacale</string>
     *
     * <!-- Approval Status -->
     * <string name="approval_status_draft">Bozza</string>
     * <string name="approval_status_pending">In attesa</string>
     * <string name="approval_status_approved">Approvato</string>
     * <string name="approval_status_rejected">Rifiutato</string>
     * <string name="approval_status_cancelled">Annullato</string>
     * <string name="approval_status_expired">Scaduto</string>
     *
     * <!-- Priority Levels -->
     * <string name="enum_priority_low_priority">Priorità bassa</string>
     * <string name="enum_priority_normal_priority">Priorità normale</string>
     * <string name="enum_priority_high_priority">Priorità alta</string>
     * <string name="enum_priority_urgent_priority">Priorità urgente</string>
     * <string name="enum_priority_override_priority">Priorità sostituzione</string>
     *
     * <!-- Assignment Status -->
     * <string name="assignment_status_active">Attivo</string>
     * <string name="assignment_status_pending">In attesa</string>
     * <string name="assignment_status_expired">Scaduto</string>
     * <string name="assignment_status_suspended">Sospeso</string>
     * <string name="assignment_status_cancelled">Annullato</string>
     *
     * <!-- Templates -->
     * <string name="user_display_template">Utente %d</string>
     * <string name="assignment_display_title">%s → %s</string>
     * <string name="assignment_permanent_period">Dal %s (Permanente)</string>
     * <string name="assignment_time_period">Dal %s al %s</string>
     * <string name="recurrence_end_count_template">Termina dopo %d occorrenze</string>
     * <string name="recurrence_end_date_template">Termina il %s</string>
     * <string name="default_shift_swap_title">Scambio turno con utente %d</string>
     *
     * <!-- Default Titles -->
     * <string name="standard_assignment_title">Assegnazione standard team</string>
     * <string name="temporary_assignment_title">Assegnazione temporanea</string>
     * <string name="team_transfer_title">Trasferimento team</string>
     * <string name="default_exception_title_absence_vacation">Ferie</string>
     * <string name="default_exception_title_absence_sick">Malattia</string>
     * <string name="default_exception_title_change_swap">Scambio turno</string>
     * <string name="default_reduction_title_personal_reduction">Riduzione orario personale</string>
     * <string name="default_reduction_title_rol_reduction">ROL</string>
     * <string name="default_reduction_title_union_time">Permesso sindacale</string>
     *
     * <!-- Recurrence Rules -->
     * <string name="recurrence_rule_quattrodue_cycle">Ciclo QuattroDue 4-2</string>
     * <string name="recurrence_rule_weekdays">Solo giorni feriali</string>
     * <string name="recurrence_rule_daily">Pattern giornaliero</string>
     * <string name="recurrence_rule_desc_quattrodue_cycle">Pattern QuattroDue standard: 4 periodi lavoro, 2 riposo</string>
     * <string name="recurrence_rule_desc_weekdays">Dal lunedì al venerdì</string>
     * <string name="recurrence_rule_desc_daily">Ogni %d giorni</string>
     *
     * <!-- End Conditions -->
     * <string name="recurrence_end_never">Nessuna fine</string>
     * <string name="recurrence_end_count">Numero occorrenze</string>
     * <string name="recurrence_end_until_date">Fino a data specifica</string>
     */
}