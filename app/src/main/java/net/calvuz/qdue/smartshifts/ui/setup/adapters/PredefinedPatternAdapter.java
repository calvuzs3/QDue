package net.calvuz.qdue.smartshifts.ui.setup.adapters;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.databinding.ItemPatternSelectionBinding;
import net.calvuz.qdue.smartshifts.ui.setup.fragments.PatternSelectionStepFragment.PatternInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for predefined patterns in setup wizard
 */
public class PredefinedPatternAdapter extends RecyclerView.Adapter<PredefinedPatternAdapter.PatternViewHolder> {

    private List<PatternInfo> patterns = new ArrayList<>();
    private OnPatternSelectedListener onPatternSelectedListener;
    private int selectedPosition = -1;

    public interface OnPatternSelectedListener {
        void onPatternSelected(PatternInfo pattern);
    }

    public PredefinedPatternAdapter(OnPatternSelectedListener listener) {
        this.onPatternSelectedListener = listener;
    }

    @NonNull
    @Override
    public PatternViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPatternSelectionBinding binding = ItemPatternSelectionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PatternViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PatternViewHolder holder, int position) {
        PatternInfo pattern = patterns.get(position);
        holder.bind(pattern, position == selectedPosition, onPatternSelectedListener);
    }

    @Override
    public int getItemCount() {
        return patterns.size();
    }

    /**
     * Update patterns list
     */
    public void updatePatterns(List<PatternInfo> newPatterns) {
        this.patterns = new ArrayList<>(newPatterns);
        notifyDataSetChanged();
    }

    /**
     * Set selected pattern
     */
    public void setSelectedPattern(int position) {
        int previousSelected = selectedPosition;
        selectedPosition = position;

        if (previousSelected != -1) {
            notifyItemChanged(previousSelected);
        }
        if (selectedPosition != -1) {
            notifyItemChanged(selectedPosition);
        }
    }

    /**
     * ViewHolder for pattern item
     */
    static class PatternViewHolder extends RecyclerView.ViewHolder {

        private final ItemPatternSelectionBinding binding;

        public PatternViewHolder(ItemPatternSelectionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(PatternInfo pattern, boolean isSelected, OnPatternSelectedListener listener) {
            // Set pattern data
            binding.patternName.setText(pattern.name);
            binding.patternDescription.setText(pattern.description);
            binding.patternDetails.setText(pattern.details);

            // Set selection state
            binding.patternRadio.setChecked(isSelected);

            // Handle click
            binding.getRoot().setOnClickListener(v -> {
                binding.patternRadio.setChecked(true);
                if (listener != null) {
                    listener.onPatternSelected(pattern);
                }
            });

            binding.patternRadio.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPatternSelected(pattern);
                }
            });
        }
    }
}
