package net.calvuz.qdue.smartshifts.ui.calendar.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.databinding.ItemShiftLegendBinding;
import net.calvuz.qdue.smartshifts.data.entities.ShiftType;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for shift legend RecyclerView
 * Shows color indicators and shift type information
 */
public class ShiftLegendAdapter extends RecyclerView.Adapter<ShiftLegendAdapter.LegendViewHolder> {

    private List<ShiftType> shiftTypes = new ArrayList<>();

    @NonNull
    @Override
    public LegendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemShiftLegendBinding binding = ItemShiftLegendBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new LegendViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LegendViewHolder holder, int position) {
        ShiftType shiftType = shiftTypes.get(position);
        holder.bind(shiftType);
    }

    @Override
    public int getItemCount() {
        return shiftTypes.size();
    }

    /**
     * Update shift types
     */
    public void updateShiftTypes(List<ShiftType> newShiftTypes) {
        this.shiftTypes = new ArrayList<>(newShiftTypes);
        notifyDataSetChanged();

        // If no data provided, create default legend
        if (this.shiftTypes.isEmpty()) {
            createDefaultLegend();
        }
    }

    /**
     * Create default legend when no shift types available
     */
    private void createDefaultLegend() {
        List<ShiftType> defaultTypes = new ArrayList<>();

        // Morning
        ShiftType morning = new ShiftType();
        morning.id = "morning";
        morning.name = "Mattina";
        morning.startTime = "06:00";
        morning.endTime = "14:00";
        morning.colorHex = "#4CAF50";
        morning.isWorkingShift = true;
        defaultTypes.add(morning);

        // Afternoon
        ShiftType afternoon = new ShiftType();
        afternoon.id = "afternoon";
        afternoon.name = "Pomeriggio";
        afternoon.startTime = "14:00";
        afternoon.endTime = "22:00";
        afternoon.colorHex = "#FF9800";
        afternoon.isWorkingShift = true;
        defaultTypes.add(afternoon);

        // Night
        ShiftType night = new ShiftType();
        night.id = "night";
        night.name = "Notte";
        night.startTime = "22:00";
        night.endTime = "06:00";
        night.colorHex = "#3F51B5";
        night.isWorkingShift = true;
        defaultTypes.add(night);

        // Rest
        ShiftType rest = new ShiftType();
        rest.id = "rest";
        rest.name = "Riposo";
        rest.startTime = "00:00";
        rest.endTime = "23:59";
        rest.colorHex = "#9E9E9E";
        rest.isWorkingShift = false;
        defaultTypes.add(rest);

        this.shiftTypes = defaultTypes;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder for legend item
     */
    static class LegendViewHolder extends RecyclerView.ViewHolder {

        private final ItemShiftLegendBinding binding;

        public LegendViewHolder(ItemShiftLegendBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ShiftType shiftType) {
            // Set shift name
            binding.shiftName.setText(shiftType.name);

            // Set shift time
            if (shiftType.isWorkingShift) {
                String timeText = shiftType.startTime + " - " + shiftType.endTime;
                binding.shiftTime.setText(timeText);
                binding.shiftTime.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.shiftTime.setText("Riposo");
                binding.shiftTime.setVisibility(android.view.View.VISIBLE);
            }

            // Set color indicator
            try {
                int color = Color.parseColor(shiftType.colorHex);
                binding.colorIndicator.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(color)
                );
            } catch (IllegalArgumentException e) {
                // Fallback to grey if color parsing fails
                binding.colorIndicator.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF9E9E9E)
                );
            }

            // Set color indicator shape
            binding.colorIndicator.setBackground(
                    binding.getRoot().getContext().getDrawable(
                            R.drawable.circle_background
                    )
            );
        }
    }
}