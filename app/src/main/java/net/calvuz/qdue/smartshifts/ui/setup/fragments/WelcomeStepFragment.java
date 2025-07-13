package net.calvuz.qdue.smartshifts.ui.setup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.calvuz.qdue.databinding.FragmentSetupWelcomeBinding;

import java.time.LocalDate;

/**
 * Welcome step fragment for setup wizard
 * Introduces SmartShifts features to new users
 */
public class WelcomeStepFragment extends Fragment {

    private FragmentSetupWelcomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSetupWelcomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup welcome content
        setupWelcomeContent();
    }

    /**
     * Setup welcome content and animations
     */
    private void setupWelcomeContent() {
        // Could add entrance animations here
        // For now, content is static from layout
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
