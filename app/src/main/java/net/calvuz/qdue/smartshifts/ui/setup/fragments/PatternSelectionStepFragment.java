package net.calvuz.qdue.smartshifts.ui.setup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import net.calvuz.qdue.databinding.FragmentPatternSelectionBinding;
import net.calvuz.qdue.smartshifts.ui.setup.SetupWizardViewModel;
import net.calvuz.qdue.smartshifts.ui.setup.adapters.PredefinedPatternAdapter;

/**
 * Pattern selection step fragment
 * Allows user to choose between predefined and custom patterns
 */
public class PatternSelectionStepFragment extends Fragment {

    private FragmentPatternSelectionBinding binding;
    private SetupWizardViewModel viewModel;
    private PredefinedPatternAdapter patternsAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPatternSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SetupWizardViewModel.class);

        setupPatternOptions();
        setupPredefinedPatterns();
        setupObservers();
    }

    /**
     * Setup pattern option radio buttons
     */
    private void setupPatternOptions() {
        // ✅ CORRETTO: Stato iniziale
        binding.radioPredefined.setChecked(true);
        binding.radioCustom.setChecked(false);
        binding.predefinedPatternsCard.setVisibility(View.VISIBLE);

        // ✅ CORRETTO: Listener con esclusione mutua
        binding.radioPredefined.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Disattiva l'altro radiobutton
                binding.radioCustom.setChecked(false);

                // Mostra patterns predefiniti
                binding.predefinedPatternsCard.setVisibility(View.VISIBLE);

                // Reset pattern selection nel ViewModel
                viewModel.setSelectedPattern(null, false);

                android.util.Log.d("PatternSelection", "Predefined selected");
            }
        });

        binding.radioCustom.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Disattiva l'altro radiobutton
                binding.radioPredefined.setChecked(false);

                // Nascondi patterns predefiniti
                binding.predefinedPatternsCard.setVisibility(View.GONE);

                // Imposta custom pattern nel ViewModel
                viewModel.setSelectedPattern("custom", true);

                android.util.Log.d("PatternSelection", "Custom selected");
            }
        });


//
//        binding.radioPredefined.setChecked(true);
//
//        binding.radioPredefined.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            if (isChecked) {
//                binding.predefinedPatternsCard.setVisibility(View.VISIBLE);
//                viewModel.setSelectedPattern(null, false);
//            }
//        });
//
//        binding.radioCustom.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            if (isChecked) {
//                binding.predefinedPatternsCard.setVisibility(View.GONE);
//                viewModel.setSelectedPattern("custom", true);
//            }
//        });
    }

    /**
     * Setup predefined patterns RecyclerView
     */
    private void setupPredefinedPatterns() {
        patternsAdapter = new PredefinedPatternAdapter(pattern -> {
            // Handle pattern selection
            viewModel.setSelectedPattern(pattern.id, false);
        });

        binding.predefinedPatternsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.predefinedPatternsRecycler.setAdapter(patternsAdapter);

        // Load predefined patterns
        loadPredefinedPatterns();
    }

    /**
     * Setup ViewModel observers
     */
    private void setupObservers() {
        // Observe pattern loading if needed
    }

    /**
     * Load predefined patterns
     */
    private void loadPredefinedPatterns() {
        // Create mock patterns for now
        java.util.List<PatternInfo> patterns = java.util.Arrays.asList(
                new PatternInfo("continuous_4_2", "Ciclo Continuo 4-2",
                        "4 mattine, 2 riposi, 4 notti, 2 riposi, 4 pomeriggi, 2 riposi",
                        "18 giorni • Ciclo continuo • 9 squadre"),
                new PatternInfo("continuous_3_2", "Ciclo Continuo 3-2",
                        "3 mattine, 2 riposi, 3 notti, 2 riposi, 3 pomeriggi, 2 riposi",
                        "15 giorni • Ciclo continuo • 5 squadre"),
                new PatternInfo("weekly_5_2", "Settimana Standard 5-2",
                        "5 giorni lavorativi, 2 giorni riposo (lunedì-venerdì)",
                        "7 giorni • Settimanale • 1 squadra"),
                new PatternInfo("weekly_6_1", "Settimana Standard 6-1",
                        "6 giorni lavorativi, 1 giorno riposo",
                        "7 giorni • Settimanale • 1 squadra")
        );

        patternsAdapter.updatePatterns(patterns);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ===== HELPER CLASSES =====

    /**
     * Simple pattern info class for display
     */
    public static class PatternInfo {
        public final String id;
        public final String name;
        public final String description;
        public final String details;

        public PatternInfo(String id, String name, String description, String details) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.details = details;
        }
    }
}
