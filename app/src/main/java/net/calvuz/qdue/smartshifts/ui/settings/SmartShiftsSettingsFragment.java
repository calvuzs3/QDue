package net.calvuz.qdue.smartshifts.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.ListPreference;
import androidx.preference.SeekBarPreference;

import net.calvuz.qdue.R;
import net.calvuz.qdue.smartshifts.domain.common.SmartShiftsErrorHandler;
import net.calvuz.qdue.smartshifts.domain.common.UnifiedOperationResult;
import net.calvuz.qdue.smartshifts.ui.settings.viewmodel.SmartShiftsSettingsViewModel;
import net.calvuz.qdue.smartshifts.utils.UiHelper;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Settings Fragment per preferenze base SmartShifts.
 * <p>
 * Utilizza le preference keys esistenti con prefisso "smartshifts_" da:
 * - smartshifts_preference_strings.xml (keys)
 * - smartshifts_strings.xml (messages)
 * <p>
 * Carica il PreferenceScreen da: smartshifts_preferences.xml
 * <p>
 * Gestisce:
 * - Preferenze generali (tema, lingua, startup, auto_sync)
 * - Impostazioni calendario (week start, view type, display options)
 * - Notifiche (promemoria, suoni, vibrazione)
 * - Gestione dati (backup auto, frequenza, cache)
 * - Azioni rapide (export veloce, backup, clear cache)
 * <p>
 * Per funzioni avanzate delega a SmartShiftsSettingsActivity.
 */
@AndroidEntryPoint
public class SmartShiftsSettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    SmartShiftsErrorHandler errorHandler;

    private SmartShiftsSettingsViewModel viewModel;
    private SharedPreferences preferences;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        // CORREZIONE: Usa il file XML corretto
        setPreferencesFromResource(R.xml.smartshifts_preferences, rootKey);

        viewModel = new ViewModelProvider(this).get(SmartShiftsSettingsViewModel.class);
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        setupPreferences();
        observeViewModel();
    }

    private void setupPreferences() {
        setupGeneralPreferences();
        setupCalendarPreferences();
        setupNotificationPreferences();
        setupDataPreferences();
        setupQuickActions();
        setupAdvancedActions();
    }

    // ============================================
    // GENERAL PREFERENCES (matchando XML esistente)
    // ============================================

    private void setupGeneralPreferences() {
        // Theme preference
        ListPreference themePref = findPreference(getString(R.string.smartshifts_pref_theme));
        if (themePref != null) {
            themePref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateAppTheme((String) newValue);
                showRestartRequiredMessage();
                return true;
            });
        }

        // Language preference
        ListPreference languagePref = findPreference(getString(R.string.smartshifts_pref_language));
        if (languagePref != null) {
            languagePref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateAppLanguage((String) newValue);
                showRestartRequiredMessage();
                return true;
            });
        }

        // Startup screen preference
        ListPreference startupPref = findPreference(getString(R.string.smartshifts_pref_startup_screen));
        if (startupPref != null) {
            startupPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateStartupScreen((String) newValue);
                return true;
            });
        }

        // Auto sync preference
        SwitchPreferenceCompat autoSyncPref = findPreference(getString(R.string.smartshifts_pref_auto_sync));
        if (autoSyncPref != null) {
            autoSyncPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateAutoSync((Boolean) newValue);
                return true;
            });
        }

        // RIMOZIONE: debug_mode non presente nel XML smartshifts_preferences.xml
        // Debug mode preference - COMMENTATO perché non presente nel XML
        /*
        SwitchPreferenceCompat debugPref = findPreference(getString(R.string.smartshifts_pref_debug_mode));
        if (debugPref != null) {
            debugPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateDebugMode((Boolean) newValue);
                return true;
            });
        }
        */
    }

    // ============================================
    // CALENDAR PREFERENCES (matchando XML esistente)
    // ============================================

    private void setupCalendarPreferences() {
        // Week start day
        ListPreference weekStartPref = findPreference(getString(R.string.smartshifts_pref_week_start_day));
        if (weekStartPref != null) {
            weekStartPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateWeekStartDay((String) newValue);
                return true;
            });
        }

        // Calendar view type
        ListPreference viewTypePref = findPreference(getString(R.string.smartshifts_pref_calendar_view_type));
        if (viewTypePref != null) {
            viewTypePref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateCalendarViewType((String) newValue);
                return true;
            });
        }

        // Show week numbers
        SwitchPreferenceCompat weekNumbersPref = findPreference(getString(R.string.smartshifts_pref_show_week_numbers));
        if (weekNumbersPref != null) {
            weekNumbersPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateShowWeekNumbers((Boolean) newValue);
                return true;
            });
        }

        // Highlight today
        SwitchPreferenceCompat highlightTodayPref = findPreference(getString(R.string.smartshifts_pref_highlight_today));
        if (highlightTodayPref != null) {
            highlightTodayPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateHighlightToday((Boolean) newValue);
                return true;
            });
        }

        // Show shift times
        SwitchPreferenceCompat shiftTimesPref = findPreference(getString(R.string.smartshifts_pref_show_shift_times));
        if (shiftTimesPref != null) {
            shiftTimesPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateShowShiftTimes((Boolean) newValue);
                return true;
            });
        }

        // Show legend
        SwitchPreferenceCompat legendPref = findPreference(getString(R.string.smartshifts_pref_show_legend));
        if (legendPref != null) {
            legendPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateShowLegend((Boolean) newValue);
                return true;
            });
        }

        // Month view layout preference - AGGIUNTA
        ListPreference monthLayoutPref = findPreference(getString(R.string.smartshifts_pref_month_view_layout));
        if (monthLayoutPref != null) {
            monthLayoutPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateMonthViewLayout((String) newValue);
                return true;
            });
        }

        // RIMOZIONE: Altre preferenze non presenti nel XML smartshifts_preferences.xml
        // Calendar density, weekend emphasis - COMMENTATE
        /*
        ListPreference densityPref = findPreference(getString(R.string.smartshifts_pref_calendar_density));
        if (densityPref != null) {
            densityPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateCalendarDensity((String) newValue);
                return true;
            });
        }

        SwitchPreferenceCompat weekendPref = findPreference(getString(R.string.smartshifts_pref_weekend_emphasis));
        if (weekendPref != null) {
            weekendPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateWeekendEmphasis((Boolean) newValue);
                return true;
            });
        }
        */
    }

    // ============================================
    // NOTIFICATION PREFERENCES (matchando XML esistente)
    // ============================================

    private void setupNotificationPreferences() {
        // Notifications enabled
        SwitchPreferenceCompat notifEnabledPref = findPreference(getString(R.string.smartshifts_pref_notifications_enabled));
        if (notifEnabledPref != null) {
            notifEnabledPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateNotificationsEnabled((Boolean) newValue);
                return true;
            });
        }

        // Shift reminders
        SwitchPreferenceCompat remindersPref = findPreference(getString(R.string.smartshifts_pref_shift_reminders));
        if (remindersPref != null) {
            remindersPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateShiftReminders((Boolean) newValue);
                return true;
            });
        }

        // Reminder advance time
        SeekBarPreference advanceTimePref = findPreference(getString(R.string.smartshifts_pref_reminder_advance_time));
        if (advanceTimePref != null) {
            advanceTimePref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateReminderAdvanceTime((Integer) newValue);
                return true;
            });
        }

        // Notification sound
        SwitchPreferenceCompat soundPref = findPreference(getString(R.string.smartshifts_pref_notification_sound));
        if (soundPref != null) {
            soundPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateNotificationSound((Boolean) newValue);
                return true;
            });
        }

        // Notification vibration
        SwitchPreferenceCompat vibrationPref = findPreference(getString(R.string.smartshifts_pref_notification_vibration));
        if (vibrationPref != null) {
            vibrationPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateNotificationVibration((Boolean) newValue);
                return true;
            });
        }

        // RIMOZIONE: Preferenze non presenti nel XML smartshifts_preferences.xml
        // Pattern change alerts, notification priority, quiet hours - COMMENTATE
        /*
        SwitchPreferenceCompat patternAlertsPref = findPreference(getString(R.string.smartshifts_pref_pattern_change_alerts));
        if (patternAlertsPref != null) {
            patternAlertsPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updatePatternChangeAlerts((Boolean) newValue);
                return true;
            });
        }

        ListPreference priorityPref = findPreference(getString(R.string.smartshifts_pref_notification_priority));
        if (priorityPref != null) {
            priorityPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateNotificationPriority((String) newValue);
                return true;
            });
        }

        SwitchPreferenceCompat quietHoursPref = findPreference(getString(R.string.smartshifts_pref_quiet_hours_enabled));
        if (quietHoursPref != null) {
            quietHoursPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateQuietHoursEnabled((Boolean) newValue);
                return true;
            });
        }

        Preference quietStartPref = findPreference(getString(R.string.smartshifts_pref_quiet_hours_start));
        if (quietStartPref != null) {
            quietStartPref.setOnPreferenceClickListener(preference -> {
                showTimePickerDialog("start");
                return true;
            });
        }

        Preference quietEndPref = findPreference(getString(R.string.smartshifts_pref_quiet_hours_end));
        if (quietEndPref != null) {
            quietEndPref.setOnPreferenceClickListener(preference -> {
                showTimePickerDialog("end");
                return true;
            });
        }
        */
    }

    // ============================================
    // DATA MANAGEMENT PREFERENCES (matchando XML esistente)
    // ============================================

    private void setupDataPreferences() {
        // Auto backup
        SwitchPreferenceCompat autoBackupPref = findPreference(getString(R.string.smartshifts_pref_auto_backup));
        if (autoBackupPref != null) {
            autoBackupPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateAutoBackup((Boolean) newValue);
                return true;
            });
        }

        // Backup frequency
        ListPreference backupFreqPref = findPreference(getString(R.string.smartshifts_pref_backup_frequency));
        if (backupFreqPref != null) {
            backupFreqPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateBackupFrequency((String) newValue);
                return true;
            });
        }

        // Cache size limit
        SeekBarPreference cacheSizePref = findPreference(getString(R.string.smartshifts_pref_cache_size_limit));
        if (cacheSizePref != null) {
            cacheSizePref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateCacheSizeLimit((Integer) newValue);
                return true;
            });
        }

        // RIMOZIONE: Preferenze non presenti nel XML smartshifts_preferences.xml
        // Data retention, cloud sync, wifi only, performance mode - COMMENTATE
        /*
        SeekBarPreference retentionPref = findPreference(getString(R.string.smartshifts_pref_data_retention_days));
        if (retentionPref != null) {
            retentionPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateDataRetentionDays((Integer) newValue);
                return true;
            });
        }

        ListPreference cloudProviderPref = findPreference(getString(R.string.smartshifts_pref_cloud_sync_provider));
        if (cloudProviderPref != null) {
            cloudProviderPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateCloudSyncProvider((String) newValue);
                return true;
            });
        }

        SwitchPreferenceCompat wifiOnlyPref = findPreference(getString(R.string.smartshifts_pref_sync_wifi_only));
        if (wifiOnlyPref != null) {
            wifiOnlyPref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updateSyncWifiOnly((Boolean) newValue);
                return true;
            });
        }

        ListPreference performancePref = findPreference(getString(R.string.smartshifts_pref_performance_mode));
        if (performancePref != null) {
            performancePref.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.updatePerformanceMode((String) newValue);
                return true;
            });
        }
        */
    }

    // ============================================
    // QUICK ACTIONS (matchando XML esistente)
    // ============================================

    private void setupQuickActions() {
        // Quick Export
        Preference quickExportPref = findPreference("quick_export");
        if (quickExportPref != null) {
            quickExportPref.setTitle(R.string.smartshifts_export_data_title);
            quickExportPref.setSummary(R.string.smartshifts_export_data_summary);
            quickExportPref.setOnPreferenceClickListener(preference -> {
                viewModel.performQuickExport();
                return true;
            });
        }

        // Quick Backup
        Preference quickBackupPref = findPreference("quick_backup");
        if (quickBackupPref != null) {
            quickBackupPref.setTitle(R.string.smartshifts_backup_success_title);
            quickBackupPref.setSummary("Crea backup rapido dei dati correnti");
            quickBackupPref.setOnPreferenceClickListener(preference -> {
                viewModel.performQuickBackup();
                return true;
            });
        }

        // Clear Cache
        Preference clearCachePref = findPreference("clear_cache");
        if (clearCachePref != null) {
            clearCachePref.setTitle(R.string.smartshifts_clear_cache_title);
            clearCachePref.setSummary(R.string.smartshifts_clear_cache_summary);
            clearCachePref.setOnPreferenceClickListener(preference -> {
                showClearCacheConfirmation();
                return true;
            });
        }

        // Reset Settings
        Preference resetPref = findPreference("reset_settings");
        if (resetPref != null) {
            resetPref.setTitle(R.string.smartshifts_reset_settings_title);
            resetPref.setSummary(R.string.smartshifts_reset_settings_summary);
            resetPref.setOnPreferenceClickListener(preference -> {
                showResetConfirmationDialog();
                return true;
            });
        }
    }

    // ============================================
    // ADVANCED ACTIONS (delega a SmartShiftsSettingsActivity)
    // ============================================

    private void setupAdvancedActions() {
        // Advanced Settings - delega all'activity esistente
        Preference advancedPref = findPreference("advanced_settings");
        if (advancedPref != null) {
            advancedPref.setTitle(R.string.smartshifts_settings_data);
            advancedPref.setSummary(R.string.smartshifts_settings_data_summary);
            advancedPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), SmartShiftsSettingsActivity.class);
                intent.putExtra("hide_deprecation_warning", true); // Nasconde warning deprecazione
                startActivity(intent);
                return true;
            });
        }

        // About / App Info
        Preference aboutPref = findPreference("about_smartshifts");
        if (aboutPref != null) {
            aboutPref.setTitle(R.string.smartshifts_app_info_title);
            aboutPref.setSummary(R.string.smartshifts_app_info_summary);
            aboutPref.setOnPreferenceClickListener(preference -> {
                showAboutDialog();
                return true;
            });
        }
    }

    // ============================================
    // UI OBSERVERS & ERROR HANDLING (corretti)
    // ============================================

    private void observeViewModel() {
        // Observe operation results
        viewModel.getOperationResults().observe(this, this::handleOperationResult);

        // Observe loading state - CORRETTO: usa showLoadingMessage
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                UiHelper.showLoadingMessage(getActivity(), getString(R.string.smartshifts_progress_processing));
            }
            // Note: Snackbar loading messages dismiss automatically when operation completes
        });

        // Observe error state - CORRETTO: usa showErrorMessage
        errorHandler.getCurrentErrorState().observe(this, errorState -> {
            if (errorState.hasError()) {
                SmartShiftsErrorHandler.SmartShiftsError error = errorState.getCurrentError();
                UiHelper.showErrorMessage(getActivity(), error.getUserFriendlyMessage());
            }
        });
    }

    private void handleOperationResult(UnifiedOperationResult<Object> result) {
        if (result == null) return;

        if (result.isSuccess()) {
            String message = result.getMessage() != null ?
                    result.getMessage() : getString(R.string.smartshifts_operation_success_title);

            // CORRETTO: usa showSuccessMessage
            UiHelper.showSuccessMessage(getActivity(), message);
        } else {
            // Error handling tramite ErrorHandler
            errorHandler.handleOperationError(result);
        }
    }

    // ============================================
    // DIALOG HELPERS (corretti per UiHelper)
    // ============================================

    private void showTimePickerDialog(String type) {
        // TODO: Implementa time picker per quiet hours quando necessario
        // Usa stringhe da smartshifts_strings.xml per titoli
        UiHelper.showInfoDialog(
                getContext(),
                "Time Picker",
                "Funzionalità time picker per " + type + " in sviluppo",
                null
        );
    }

    private void showClearCacheConfirmation() {
        UiHelper.showConfirmationDialog(
                getContext(),
                getString(R.string.smartshifts_confirm_clear_cache_title),
                getString(R.string.smartshifts_confirm_clear_cache_message),
                getString(R.string.smartshifts_continue),
                getString(R.string.smartshifts_cancel),
                () -> viewModel.clearCache(),
                null
        );
    }

    private void showResetConfirmationDialog() {
        UiHelper.showConfirmationDialog(
                getContext(),
                getString(R.string.smartshifts_confirm_reset_title),
                getString(R.string.smartshifts_confirm_reset_message),
                getString(R.string.smartshifts_continue),
                getString(R.string.smartshifts_cancel),
                () -> viewModel.resetToDefaults(),
                null
        );
    }

    private void showRestartRequiredMessage() {
        UiHelper.showAlertDialog(
                getContext(),
                "Riavvio Richiesto",
                "Le modifiche saranno applicate al prossimo avvio dell'applicazione.",
                null
        );
    }

    private void showAboutDialog() {
        String versionName = "1.0.0"; // Da BuildConfig o ViewModel
        int versionCode = 1; // Da BuildConfig o ViewModel

        String aboutMessage = getString(R.string.smartshifts_app_version_summary, versionName, versionCode) +
                "\n\n" + getString(R.string.smartshifts_app_info_summary);

        UiHelper.showInfoDialog(
                getContext(),
                getString(R.string.smartshifts_app_info_title),
                aboutMessage,
                null
        );
    }

    // ============================================
    // PREFERENCE CHANGE LISTENERS
    // ============================================

    @Override
    public void onResume() {
        super.onResume();
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        preferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @NonNull String key) {
        // Handle global preference changes
        Object value = sharedPreferences.getAll().get(key);
        if (viewModel != null) {
            viewModel.onPreferenceChanged(key, value);
        }
    }
}

// ============================================
// RIEPILOGO MODIFICHE APPLICATE
// ============================================

/*
✅ CORREZIONI COMPLETATE:

1. **File XML corretto**: Cambiato da smartshifts_preferences_main a smartshifts_preferences

2. **Preferenze rimosse** (non presenti nel XML):
   - debug_mode
   - calendar_density
   - weekend_emphasis
   - pattern_change_alerts
   - notification_priority
   - quiet_hours_*
   - data_retention_days
   - cloud_sync_provider
   - sync_wifi_only
   - performance_mode

3. **Preferenze aggiunte**:
   - month_view_layout (singolo turno vs tutti i turni)

3. **Metodi UiHelper corretti**:
   - showLoadingMessage() ✅
   - showErrorMessage() ✅
   - showSuccessMessage() ✅
   - showInfoDialog() ✅
   - showAlertDialog() ✅

4. **Matchato con XML esistente**:
   - Tutte le preferenze nel Fragment ora corrispondono a quelle nel XML
   - Mantenute solo le preferenze effettivamente definite
   - Quick actions allineate con le chiavi nel XML

5. **Gestione errori migliorata**:
   - Null checks su tutte le preferenze
   - Error handling via SmartShiftsErrorHandler
   - Observer patterns corretti

🎯 **RISULTATO**:
Il Fragment ora è perfettamente allineato con smartshifts_preferences.xml
e usa correttamente tutti i metodi UiHelper disponibili.
*/