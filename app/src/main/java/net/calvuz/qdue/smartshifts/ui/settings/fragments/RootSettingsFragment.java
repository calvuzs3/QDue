package net.calvuz.qdue.smartshifts.ui.settings.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import net.calvuz.qdue.R;
import net.calvuz.qdue.smartshifts.ui.settings.SmartShiftsSettingsActivity;

/**
 * Root Settings Fragment for SmartShifts.
 * <p>
 * Displays the main settings categories as clickable preferences that navigate
 * to specific settings sections:
 * - General Settings (theme, language, behavior)
 * - Calendar Settings (display, layout, features)
 * - Notification Settings (reminders, alerts, quiet hours)
 * - Data Management (backup, export, import, reset)
 * - About (version, credits, help, legal)
 * <p>
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features
 */
public class RootSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Create preference screen programmatically
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(requireContext());
        setPreferenceScreen(screen);

        // Add main settings categories
        addGeneralSettings(screen);
        addCalendarSettings(screen);
        addNotificationSettings(screen);
        addDataManagementSettings(screen);
        addAboutSettings(screen);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Update toolbar title
        if (getActivity() instanceof SmartShiftsSettingsActivity) {
            ((SmartShiftsSettingsActivity) getActivity()).updateToolbarTitle(
                    getString(R.string.smartshifts_settings_title)
            );
        }
    }

    /**
     * Add General Settings preference
     */
    private void addGeneralSettings(PreferenceScreen screen) {
        Preference generalPref = new Preference(requireContext());
        generalPref.setKey("pref_general");
        generalPref.setTitle(R.string.smartshifts_settings_general);
        generalPref.setSummary(R.string.smartshifts_settings_general_summary);
        generalPref.setIcon(R.drawable.ic_rounded_settings_24);
        generalPref.setOrder(1);

        generalPref.setOnPreferenceClickListener(preference -> {
            navigateToSection("general");
            return true;
        });

        screen.addPreference(generalPref);
    }

    /**
     * Add Calendar Settings preference
     */
    private void addCalendarSettings(PreferenceScreen screen) {
        Preference calendarPref = new Preference(requireContext());
        calendarPref.setKey("pref_calendar");
        calendarPref.setTitle(R.string.smartshifts_settings_calendar);
        calendarPref.setSummary(R.string.smartshifts_settings_calendar_summary);
        calendarPref.setIcon(R.drawable.ic_rounded_calendar_month_24);
        calendarPref.setOrder(2);

        calendarPref.setOnPreferenceClickListener(preference -> {
            navigateToSection("calendar");
            return true;
        });

        screen.addPreference(calendarPref);
    }

    /**
     * Add Notification Settings preference
     */
    private void addNotificationSettings(PreferenceScreen screen) {
        Preference notificationPref = new Preference(requireContext());
        notificationPref.setKey("pref_notifications");
        notificationPref.setTitle(R.string.smartshifts_settings_notifications);
        notificationPref.setSummary(R.string.smartshifts_settings_notifications_summary);
        notificationPref.setIcon(R.drawable.ic_rounded_notifications_24);
        notificationPref.setOrder(3);

        notificationPref.setOnPreferenceClickListener(preference -> {
            navigateToSection("notifications");
            return true;
        });

        screen.addPreference(notificationPref);
    }

    /**
     * Add Data Management Settings preference
     */
    private void addDataManagementSettings(PreferenceScreen screen) {
        Preference dataPref = new Preference(requireContext());
        dataPref.setKey("pref_data");
        dataPref.setTitle(R.string.smartshifts_settings_data);
        dataPref.setSummary(R.string.smartshifts_settings_data_summary);
        dataPref.setIcon(R.drawable.ic_rounded_storage_24);
        dataPref.setOrder(4);

        dataPref.setOnPreferenceClickListener(preference -> {
            navigateToSection("data");
            return true;
        });

        screen.addPreference(dataPref);
    }

    /**
     * Add About Settings preference
     */
    private void addAboutSettings(PreferenceScreen screen) {
        Preference aboutPref = new Preference(requireContext());
        aboutPref.setKey("pref_about");
        aboutPref.setTitle(R.string.smartshifts_settings_about);
        aboutPref.setSummary(R.string.smartshifts_settings_about_summary);
        aboutPref.setIcon(R.drawable.ic_rounded_info_24);
        aboutPref.setOrder(5);

        aboutPref.setOnPreferenceClickListener(preference -> {
            navigateToSection("about");
            return true;
        });

        screen.addPreference(aboutPref);
    }

    /**
     * Navigate to specific settings section
     */
    private void navigateToSection(String section) {
        if (getActivity() instanceof SmartShiftsSettingsActivity) {
            ((SmartShiftsSettingsActivity) getActivity()).navigateToSection(section);
        }
    }
}