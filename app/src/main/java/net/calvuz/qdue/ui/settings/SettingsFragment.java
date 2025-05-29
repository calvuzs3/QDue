package net.calvuz.qdue.ui.settings;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import net.calvuz.qdue.R;
import net.calvuz.qdue.utils.ThemeManager;

public class SettingsFragment extends PreferenceFragmentCompat {

    private ThemeManager themeManager;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        themeManager = ThemeManager.getInstance(requireContext());

        setupThemePreferences();
    }

    private void setupThemePreferences() {
        // Gestione modalità tema
        ListPreference themePreference = findPreference("theme_mode");
        if (themePreference != null) {
            themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                int themeMode = Integer.parseInt((String) newValue);
                themeManager.setThemeMode(themeMode);

                // Riavvia l'activity per applicare il nuovo tema
                recreateActivity();
                return true;
            });
        }

        // Gestione colori dinamici
        SwitchPreferenceCompat dynamicColorsPreference = findPreference("dynamic_colors");
        if (dynamicColorsPreference != null) {
            // Mostra solo su Android 12+
            dynamicColorsPreference.setVisible(themeManager.isDynamicColorsSupported());

            dynamicColorsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                themeManager.setDynamicColorsEnabled(enabled);

                // Riavvia l'activity per applicare i nuovi colori
                recreateActivity();
                return true;
            });
        }
    }

    private void recreateActivity() {
        if (getActivity() != null) {
            getActivity().recreate();
        }
    }
}