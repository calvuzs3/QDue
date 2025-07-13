package net.calvuz.qdue.smartshifts.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.calvuz.qdue.databinding.FragmentSmartShiftsSettingsBinding;
/**
 * Fragment for SmartShifts settings
 * Placeholder for future implementation
 */
public class SmartShiftsSettingsFragment extends Fragment {

    private FragmentSmartShiftsSettingsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSmartShiftsSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Placeholder content
        setupPlaceholder();
    }

    private void setupPlaceholder() {
        // Add placeholder content for settings
        // This will be implemented in Fase 4
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}