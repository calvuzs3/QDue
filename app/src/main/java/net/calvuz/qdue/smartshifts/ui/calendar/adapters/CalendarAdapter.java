package net.calvuz.qdue.smartshifts.ui.calendar.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.databinding.ItemSmartShiftCalendarDayBinding;
import net.calvuz.qdue.smartshifts.ui.calendar.models.CalendarDay;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for calendar days RecyclerView
 * Handles 42-day calendar grid (6 weeks × 7 days)
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarDayViewHolder> {

    private List<CalendarDay> days = new ArrayList<>();
    private LocalDate selectedDate;
    private OnDayClickListener onDayClickListener;

    public interface OnDayClickListener {
        void onDayClick(CalendarDay day);
    }

    public CalendarAdapter(OnDayClickListener listener) {
        this.onDayClickListener = listener;
    }

    @NonNull
    @Override
    public CalendarDayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSmartShiftCalendarDayBinding binding = ItemSmartShiftCalendarDayBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CalendarDayViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarDayViewHolder holder, int position) {
        CalendarDay day = days.get(position);
        holder.bind(day, selectedDate, onDayClickListener);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    /**
     * Update calendar days
     */
    public void updateDays(List<CalendarDay> newDays) {
        this.days = new ArrayList<>(newDays);
        notifyDataSetChanged();
    }

    /**
     * Update selected date
     */
    public void updateSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder for calendar day
     */
    static class CalendarDayViewHolder extends RecyclerView.ViewHolder {

        private final ItemSmartShiftCalendarDayBinding binding;

        public CalendarDayViewHolder(ItemSmartShiftCalendarDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CalendarDay day, LocalDate selectedDate, OnDayClickListener listener) {
            // Set day number
            binding.dayNumber.setText(String.valueOf(day.getDayOfMonth()));

            // Handle current month styling
            float alpha = day.isCurrentMonth() ? 1.0f : 0.3f;
            binding.dayNumber.setAlpha(alpha);

            // Handle today styling
            if (day.isToday()) {
                binding.dayNumber.setTextColor(
                        binding.getRoot().getContext().getColor(android.R.color.white)
                );
                binding.getRoot().setCardBackgroundColor(
                        binding.getRoot().getContext().getColor(R.color.calendar_today)
                );
            } else if (selectedDate != null && day.getDate().equals(selectedDate)) {
                // Selected date styling
                binding.getRoot().setCardBackgroundColor(
                        binding.getRoot().getContext().getColor(R.color.calendar_selected)
                );
                binding.dayNumber.setTextColor(
                        binding.getRoot().getContext().getColor(R.color.calendar_today)
                );
            } else {
                // Default styling
                binding.getRoot().setCardBackgroundColor(
                        binding.getRoot().getContext().getColor(android.R.color.transparent)
                );
                int textColor = day.isCurrentMonth() ?
                        android.R.color.black : R.color.calendar_other_month;
                binding.dayNumber.setTextColor(
                        binding.getRoot().getContext().getColor(textColor)
                );
            }

            // Handle shift indicator
            if (day.hasShift()) {
                binding.shiftIndicator.setVisibility(android.view.View.VISIBLE);
                binding.shiftLabel.setVisibility(android.view.View.VISIBLE);

                // Set shift color and label based on shift type
                String shiftTypeId = day.getShiftTypeId();
                int color = getShiftColor(shiftTypeId);
                String label = getShiftLabel(shiftTypeId);

                binding.shiftIndicator.setBackgroundColor(color);
                binding.shiftLabel.setText(label);
                binding.shiftLabel.setTextColor(color);
            } else {
                binding.shiftIndicator.setVisibility(android.view.View.GONE);
                binding.shiftLabel.setVisibility(android.view.View.GONE);
            }

            // Handle click
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null && day.isCurrentMonth()) {
                    listener.onDayClick(day);
                }
            });

            // Set clickable state
            binding.getRoot().setClickable(day.isCurrentMonth());
            binding.getRoot().setFocusable(day.isCurrentMonth());
        }

        /**
         * Get color for shift type
         */
        private int getShiftColor(String shiftTypeId) {
            switch (shiftTypeId) {
                case "morning":
                    return 0xFF4CAF50; // Green
                case "afternoon":
                    return 0xFFFF9800; // Orange
                case "night":
                    return 0xFF3F51B5; // Indigo
                case "rest":
                    return 0xFF9E9E9E; // Grey
                default:
                    return 0xFF9E9E9E;
            }
        }

        /**
         * Get label for shift type
         */
        private String getShiftLabel(String shiftTypeId) {
            switch (shiftTypeId) {
                case "morning":
                    return "M";
                case "afternoon":
                    return "P";
                case "night":
                    return "N";
                case "rest":
                    return "R";
                default:
                    return "?";
            }
        }
    }
}
