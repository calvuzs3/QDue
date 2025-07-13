package net.calvuz.qdue.smartshifts.ui.setup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import net.calvuz.qdue.databinding.FragmentSetupConfirmationBinding;
import net.calvuz.qdue.smartshifts.ui.setup.SetupWizardViewModel;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Confirmation step fragment
 * Shows summary of selected configuration before completing setup
 */
public class ConfirmationStepFragment extends Fragment {

    private FragmentSetupConfirmationBinding binding;
    private SetupWizardViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSetupConfirmationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SetupWizardViewModel.class);

        setupConfirmationDisplay();
    }

    /**
     * Setup confirmation display with selected options
     */
    private void setupConfirmationDisplay() {
        // Display selected pattern
        String patternId = viewModel.getSelectedPatternId();
        if (patternId != null) {
            String patternName = getPatternDisplayName(patternId);
            binding.selectedPatternText.setText(patternName);
        }

        // Display selected start date
        if (viewModel.getSelectedStartDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ITALIAN);
            String formattedDate = viewModel.getSelectedStartDate().format(formatter);
            binding.selectedDateText.setText(capitalize(formattedDate));
        }

        // Display additional info based on pattern type
        if (viewModel.isCustomPattern()) {
            binding.additionalInfoText.setText("Pattern personalizzato - Potrai modificarlo successivamente");
        } else {
            binding.additionalInfoText.setText("Verranno generati i turni per i prossimi 12 mesi");
        }
    }

    /**
     * Get display name for pattern ID
     */
    private String getPatternDisplayName(String patternId) {
        switch (patternId) {
            case "continuous_4_2":
                return "Ciclo Continuo 4-2";
            case "continuous_3_2":
                return "Ciclo Continuo 3-2";
            case "weekly_5_2":
                return "Settimana Standard 5-2";
            case "weekly_6_1":
                return "Settimana Standard 6-1";
            case "custom":
                return "Pattern Personalizzato";
            default:
                return "Pattern Sconosciuto";
        }
    }

    /**
     * Capitalize first letter
     */
    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
