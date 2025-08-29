package net.calvuz.qdue.ui.features.assignment.wizard.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.databinding.ItemCustomPatternSelectionBinding;
import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

/**
 * CustomPatternSelectionAdapter - Custom pattern selection from user-defined patterns
 *
 * <p>RecyclerView adapter for custom pattern selection that displays user-created patterns
 * with their cycle information and creation details. Supports single selection.</p>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since Clean Architecture Phase 2
 */
public class CustomPatternSelectionAdapter extends RecyclerView.Adapter<CustomPatternSelectionAdapter.CustomPatternViewHolder> {

    private static final String TAG = "CustomPatternSelectionAdapter";

    // ==================== DATA & CALLBACKS ====================

    private List<RecurrenceRule> mCustomPatterns;
    private RecurrenceRule mSelectedPattern;
    private final CustomPatternSelectionCallback mCallback;

    // ==================== CALLBACK INTERFACE ====================

    public interface CustomPatternSelectionCallback {
        void onCustomPatternSelected(@NonNull RecurrenceRule customPattern);
    }

    // ==================== CONSTRUCTOR ====================

    public CustomPatternSelectionAdapter(@NonNull CustomPatternSelectionCallback callback) {
        this.mCallback = callback;
        this.mCustomPatterns = new ArrayList<>();
        this.mSelectedPattern = null;
    }

    // ==================== DATA MANAGEMENT ====================

    public void setCustomPatterns(@NonNull List<RecurrenceRule> patterns) {
        this.mCustomPatterns = new ArrayList<>(patterns);
        notifyDataSetChanged();
        Log.d(TAG, "Custom patterns updated: " + patterns.size() + " patterns available");
    }

    public void setSelectedPattern(@Nullable RecurrenceRule selectedPattern) {
        RecurrenceRule previousSelection = this.mSelectedPattern;
        this.mSelectedPattern = selectedPattern;

        // Update UI efficiently
        if (previousSelection != null) {
            int previousIndex = findPatternIndex(previousSelection);
            if (previousIndex >= 0) {
                notifyItemChanged(previousIndex);
            }
        }

        if (selectedPattern != null) {
            int currentIndex = findPatternIndex(selectedPattern);
            if (currentIndex >= 0) {
                notifyItemChanged(currentIndex);
            }
        }

        Log.d(TAG, "Selected pattern: " + (selectedPattern != null ? selectedPattern.getName() : "none"));
    }

    @Nullable
    public RecurrenceRule getSelectedPattern() {
        return mSelectedPattern;
    }

    private int findPatternIndex(@NonNull RecurrenceRule pattern) {
        for (int i = 0; i < mCustomPatterns.size(); i++) {
            if (mCustomPatterns.get(i).getId().equals(pattern.getId())) {
                return i;
            }
        }
        return -1;
    }

    // ==================== RECYCLERVIEW ADAPTER METHODS ====================

    @NonNull
    @Override
    public CustomPatternViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCustomPatternSelectionBinding binding = ItemCustomPatternSelectionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CustomPatternViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomPatternViewHolder holder, int position) {
        RecurrenceRule pattern = mCustomPatterns.get(position);
        holder.bind(pattern);
    }

    @Override
    public int getItemCount() {
        return mCustomPatterns.size();
    }

    // ==================== VIEW HOLDER ====================

    public class CustomPatternViewHolder extends RecyclerView.ViewHolder {

        private final ItemCustomPatternSelectionBinding mBinding;

        public CustomPatternViewHolder(@NonNull ItemCustomPatternSelectionBinding binding) {
            super(binding.getRoot());
            this.mBinding = binding;

            // Set click listener
            mBinding.getRoot().setOnClickListener(this::onItemClick);
        }

        public void bind(@NonNull RecurrenceRule pattern) {
            // Pattern basic information
            mBinding.txtPatternName.setText(pattern.getName());

            String description = pattern.getDescription();
            if (description == null || description.isEmpty()) {
                description = "Custom pattern with " + pattern.getCycleLength() + " day cycle";
            }
            mBinding.txtPatternDescription.setText(description);

            // Pattern statistics
            mBinding.txtCycleLength.setText(pattern.getCycleLength() + " days");

            // Work/Rest days information (if available)
            if (pattern.getWorkDays() > 0 && pattern.getRestDays() > 0) {
                String workRestInfo = pattern.getWorkDays() + " work, " + pattern.getRestDays() + " rest";
                mBinding.txtWorkRestInfo.setText(workRestInfo);
                mBinding.txtWorkRestInfo.setVisibility(View.VISIBLE);
            } else {
                mBinding.txtWorkRestInfo.setVisibility(View.GONE);
            }

            // Pattern dates
            if (pattern.getStartDate() != null) {
                String startDateText = "From " + pattern.getStartDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
                mBinding.txtPatternStartDate.setText(startDateText);
                mBinding.txtPatternStartDate.setVisibility(View.VISIBLE);
            } else {
                mBinding.txtPatternStartDate.setVisibility(View.GONE);
            }

            // Selection state
            boolean isSelected = mSelectedPattern != null && mSelectedPattern.getId().equals(pattern.getId());
            mBinding.radioPatternSelection.setChecked(isSelected);

            // Visual selection feedback
            mBinding.cardPattern.setSelected(isSelected);
            mBinding.cardPattern.setCardElevation(isSelected ? 8f : 2f);

            // Pattern status indicator
            if (pattern.isActive()) {
                mBinding.imgPatternStatus.setImageResource(R.drawable.ic_rounded_check_circle_24);
                mBinding.imgPatternStatus.setImageTintList(
                        android.content.res.ColorStateList.valueOf(0xFF4CAF50)); // Green
            } else {
                mBinding.imgPatternStatus.setImageResource(R.drawable.ic_rounded_warning_24);
                mBinding.imgPatternStatus.setImageTintList(
                        android.content.res.ColorStateList.valueOf(0xFFFF9800)); // Orange
            }

            // Pattern type badge
            setupPatternTypeBadge(pattern);

            // Accessibility
            String contentDesc = pattern.getName() + ", " + pattern.getCycleLength() + " day cycle";
            if (isSelected) {
                contentDesc += ", selected";
            }
            mBinding.getRoot().setContentDescription(contentDesc);
        }

        private void setupPatternTypeBadge(@NonNull RecurrenceRule pattern) {
            String frequency = pattern.getFrequency().toString();

            switch (frequency) {
                case "DAILY":
                    mBinding.txtPatternType.setText("Daily");
                    mBinding.txtPatternType.setBackgroundResource(R.drawable.bg_badge_blue);
                    break;
                case "WEEKLY":
                    mBinding.txtPatternType.setText("Weekly");
                    mBinding.txtPatternType.setBackgroundResource(R.drawable.bg_badge_green);
                    break;
                case "CUSTOM_PATTERN":
                    mBinding.txtPatternType.setText("Custom");
                    mBinding.txtPatternType.setBackgroundResource(R.drawable.bg_badge_purple);
                    break;
                default:
                    mBinding.txtPatternType.setText("Pattern");
                    mBinding.txtPatternType.setBackgroundResource(R.drawable.bg_badge_grey);
                    break;
            }

            mBinding.txtPatternType.setVisibility(View.VISIBLE);
        }

        private void onItemClick(View view) {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && position < mCustomPatterns.size()) {
                RecurrenceRule clickedPattern = mCustomPatterns.get(position);

                // Update selection
                setSelectedPattern(clickedPattern);

                // Notify callback
                mCallback.onCustomPatternSelected(clickedPattern);

                Log.d(TAG, "Custom pattern selected: " + clickedPattern.getName() +
                        " (cycle: " + clickedPattern.getCycleLength() + " days)");
            }
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Clear selection (for reset scenarios)
     */
    public void clearSelection() {
        setSelectedPattern(null);
    }

    /**
     * Get pattern by ID for validation
     */
    @Nullable
    public RecurrenceRule getPatternById(@NonNull String patternId) {
        for (RecurrenceRule pattern : mCustomPatterns) {
            if (pattern.getId().equals(patternId)) {
                return pattern;
            }
        }
        return null;
    }

    /**
     * Check if adapter has patterns
     */
    public boolean hasPatterns() {
        return !mCustomPatterns.isEmpty();
    }

    /**
     * Get pattern count
     */
    public int getPatternCount() {
        return mCustomPatterns.size();
    }

    /**
     * Filter patterns by active status
     */
    public void filterActivePatterns(boolean showActiveOnly) {
        if (showActiveOnly) {
            List<RecurrenceRule> activePatterns = new ArrayList<>();
            for (RecurrenceRule pattern : mCustomPatterns) {
                if (pattern.isActive()) {
                    activePatterns.add(pattern);
                }
            }
            setCustomPatterns(activePatterns);
        }
    }
}