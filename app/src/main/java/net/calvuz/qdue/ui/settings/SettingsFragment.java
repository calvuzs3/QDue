package net.calvuz.qdue.ui.settings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import net.calvuz.qdue.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }
}