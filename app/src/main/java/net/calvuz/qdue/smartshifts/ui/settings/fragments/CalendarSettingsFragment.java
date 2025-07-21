package net.calvuz.qdue.smartshifts.ui.settings.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import net.calvuz.qdue.R;
import net.calvuz.qdue.smartshifts.ui.settings.SmartShiftsSettingsActivity;

/**
 * Calendar Settings Fragment for SmartShifts.
 * <p>
 * Manages all calendar-related preferences including:
 * - Display options (week start day, view type, density)
 * - Layout preferences (month view, week numbers, legend)
 * - Visual features (today highlight, shift times, weekend emphasis)
 * <p>
 * All preferences use smartshifts_ prefixed keys for consistency.
 * <p>
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features
 */
public class CalendarSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Create preference screen programmatically
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(requireContext());
        setPreferenceScreen(screen);

        // Add preference categories
        addDisplayCategory(screen);
        addLayoutCategory(screen);
        addFeaturesCategory(screen);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Update toolbar title
        if (getActivity() instanceof SmartShiftsSettingsActivity) {
            ((SmartShiftsSettingsActivity) getActivity()).updateToolbarTitle(
                    getString(R.string.smartshifts_settings_calendar)
            );
        }
    }

    /**
     * Add Display preferences category
     */
    private void addDisplayCategory(PreferenceScreen screen) {
        PreferenceCategory displayCategory = new PreferenceCategory(requireContext());
        displayCategory.setTitle(R.string.smartshifts_calendar_display);
        displayCategory.setOrder(1);
        screen.addPreference(displayCategory);

        // Week Start Day
        ListPreference weekStartPref = new ListPreference(requireContext());
        weekStartPref.setKey(getString(R.string.smartshifts_pref_week_start_day));
        weekStartPref.setTitle(R.string.smartshifts_week_start_day_title);
        weekStartPref.setSummary(R.string.smartshifts_week_start_day_summary);
        weekStartPref.setEntries(R.array.smartshifts_week_start_day_entries);
        weekStartPref.setEntryValues(R.array.smartshifts_week_start_day_values);
        weekStartPref.setDefaultValue(getString(R.string.smartshifts_default_week_start_day));
        weekStartPref.setIcon(R.drawable.ic_rounded_calendar_view_week_24);
        weekStartPref.setOrder(1);
        displayCategory.addPreference(weekStartPref);

        // Calendar View Type
        ListPreference viewTypePref = new ListPreference(requireContext());
        viewTypePref.setKey(getString(R.string.smartshifts_pref_calendar_view_type));
        viewTypePref.setTitle(R.string.smartshifts_calendar_view_type_title);
        viewTypePref.setSummary(R.string.smartshifts_calendar_view_type_summary);
        viewTypePref.setEntries(R.array.smartshifts_calendar_view_type_entries);
        viewTypePref.setEntryValues(R.array.smartshifts_calendar_view_type_values);
        viewTypePref.setDefaultValue(getString(R.string.smartshifts_default_calendar_view_type));
        viewTypePref.setIcon(R.drawable.ic_rounded_view_array_24);
        viewTypePref.setOrder(2);
        displayCategory.addPreference(viewTypePref);

        // Calendar Density
        ListPreference densityPref = new ListPreference(requireContext());
        densityPref.setKey(getString(R.string.smartshifts_pref_calendar_density));
        densityPref.setTitle(R.string.smartshifts_calendar_density_title);
        densityPref.setSummary(R.string.smartshifts_calendar_density_summary);
        densityPref.setEntries(R.array.smartshifts_calendar_density_entries);
        densityPref.setEntryValues(R.array.smartshifts_calendar_density_values);
        densityPref.setDefaultValue("normal");
        densityPref.setIcon(R.drawable.ic_rounded_density_large_24);
        densityPref.setOrder(3);
        displayCategory.addPreference(densityPref);
    }

    /**
     * Add Layout preferences category
     */
    private void addLayoutCategory(PreferenceScreen screen) {
        PreferenceCategory layoutCategory = new PreferenceCategory(requireContext());
        layoutCategory.setTitle(R.string.smartshifts_calendar_layout);
        layoutCategory.setOrder(2);
        screen.addPreference(layoutCategory);

        // Show Week Numbers
        SwitchPreferenceCompat weekNumbersPref = new SwitchPreferenceCompat(requireContext());
        weekNumbersPref.setKey(getString(R.string.smartshifts_pref_show_week_numbers));
        weekNumbersPref.setTitle(R.string.smartshifts_show_week_numbers_title);
        weekNumbersPref.setSummary(R.string.smartshifts_show_week_numbers_summary);
        weekNumbersPref.setDefaultValue(true);
        weekNumbersPref.setIcon(R.drawable.ic_rounded_numbers_24);
        weekNumbersPref.setOrder(1);
        layoutCategory.addPreference(weekNumbersPref);

        // Month View Layout
        ListPreference monthLayoutPref = new ListPreference(requireContext());
        monthLayoutPref.setKey(getString(R.string.smartshifts_pref_month_view_layout));
        monthLayoutPref.setTitle(R.string.smartshifts_month_view_layout_title);
        monthLayoutPref.setSummary(R.string.smartshifts_month_view_layout_summary);
        monthLayoutPref.setEntries(new CharSequence[]{"Griglia", "Lista"});
        monthLayoutPref.setEntryValues(new CharSequence[]{"grid", "list"});
        monthLayoutPref.setDefaultValue("grid");
        monthLayoutPref.setIcon(R.drawable.ic_rounded_event_list_24);
        monthLayoutPref.setOrder(2);
        layoutCategory.addPreference(monthLayoutPref);

        // Show Legend
        SwitchPreferenceCompat legendPref = new SwitchPreferenceCompat(requireContext());
        legendPref.setKey(getString(R.string.smartshifts_pref_show_legend));
        legendPref.setTitle(R.string.smartshifts_show_legend_title);
        legendPref.setSummary(R.string.smartshifts_show_legend_summary);
        legendPref.setDefaultValue(true);
        legendPref.setIcon(R.drawable.ic_rounded_legend_toggle_24);
        legendPref.setOrder(3);
        layoutCategory.addPreference(legendPref);
    }

    /**
     * Add Features preferences category
     */
    private void addFeaturesCategory(PreferenceScreen screen) {
        PreferenceCategory featuresCategory = new PreferenceCategory(requireContext());
        featuresCategory.setTitle(R.string.smartshifts_calendar_features);
        featuresCategory.setOrder(3);
        screen.addPreference(featuresCategory);

        // Highlight Today
        SwitchPreferenceCompat highlightTodayPref = new SwitchPreferenceCompat(requireContext());
        highlightTodayPref.setKey(getString(R.string.smartshifts_pref_highlight_today));
        highlightTodayPref.setTitle(R.string.smartshifts_highlight_today_title);
        highlightTodayPref.setSummary(R.string.smartshifts_highlight_today_summary);
        highlightTodayPref.setDefaultValue(true);
        highlightTodayPref.setIcon(R.drawable.ic_rounded_calendar_today_24);
        highlightTodayPref.setOrder(1);
        featuresCategory.addPreference(highlightTodayPref);

        // Show Shift Times
        SwitchPreferenceCompat shiftTimesPref = new SwitchPreferenceCompat(requireContext());
        shiftTimesPref.setKey(getString(R.string.smartshifts_pref_show_shift_times));
        shiftTimesPref.setTitle(R.string.smartshifts_show_shift_times_title);
        shiftTimesPref.setSummary(R.string.smartshifts_show_shift_times_summary);
        shiftTimesPref.setDefaultValue(false);
        shiftTimesPref.setIcon(R.drawable.ic_rounded_switch_access_3_24);
        shiftTimesPref.setOrder(2);
        featuresCategory.addPreference(shiftTimesPref);

        // Weekend Emphasis
        SwitchPreferenceCompat weekendPref = new SwitchPreferenceCompat(requireContext());
        weekendPref.setKey(getString(R.string.smartshifts_pref_weekend_emphasis));
        weekendPref.setTitle(R.string.smartshifts_weekend_emphasis_title);
        weekendPref.setSummary(R.string.smartshifts_weekend_emphasis_summary);
        weekendPref.setDefaultValue(true);
        weekendPref.setIcon(R.drawable.ic_rounded_weekend_24);
        weekendPref.setOrder(3);
        featuresCategory.addPreference(weekendPref);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update preference summaries with current values
        updatePreferenceSummaries();
    }

    /**
     * Update preference summaries to show current selected values
     */
    private void updatePreferenceSummaries() {
        // Update week start day summary
        ListPreference weekStartPref = findPreference(getString(R.string.smartshifts_pref_week_start_day));
        if (weekStartPref != null) {
            updateListPreferenceSummary(weekStartPref);
            weekStartPref.setOnPreferenceChangeListener((preference, newValue) -> {
                updateListPreferenceSummary((ListPreference) preference, newValue.toString());
                return true;
            });
        }

        // Update calendar view type summary
        ListPreference viewTypePref = findPreference(getString(R.string.smartshifts_pref_calendar_view_type));
        if (viewTypePref != null) {
            updateListPreferenceSummary(viewTypePref);
            viewTypePref.setOnPreferenceChangeListener((preference, newValue) -> {
                updateListPreferenceSummary((ListPreference) preference, newValue.toString());
                return true;
            });
        }

        // Update calendar density summary
        ListPreference densityPref = findPreference(getString(R.string.smartshifts_pref_calendar_density));
        if (densityPref != null) {
            updateListPreferenceSummary(densityPref);
            densityPref.setOnPreferenceChangeListener((preference, newValue) -> {
                updateListPreferenceSummary((ListPreference) preference, newValue.toString());
                return true;
            });
        }

        // Update month layout summary
        ListPreference monthLayoutPref = findPreference(getString(R.string.smartshifts_pref_month_view_layout));
        if (monthLayoutPref != null) {
            updateListPreferenceSummary(monthLayoutPref);
            monthLayoutPref.setOnPreferenceChangeListener((preference, newValue) -> {
                updateListPreferenceSummary((ListPreference) preference, newValue.toString());
                return true;
            });
        }
    }

    /**
     * Update ListPreference summary with current selection
     */
    private void updateListPreferenceSummary(ListPreference preference) {
        String value = preference.getValue();
        updateListPreferenceSummary(preference, value);
    }

    /**
     * Update ListPreference summary with specific value
     */
    private void updateListPreferenceSummary(ListPreference preference, String value) {
        if (value != null) {
            CharSequence[] entries = preference.getEntries();
            CharSequence[] entryValues = preference.getEntryValues();

            for (int i = 0; i < entryValues.length; i++) {
                if (entryValues[i].toString().equals(value)) {
                    preference.setSummary(entries[i]);
                    break;
                }
            }
        }
    }
}