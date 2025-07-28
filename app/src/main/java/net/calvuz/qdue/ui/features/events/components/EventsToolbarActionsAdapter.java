/**
 * 🎨 Enhanced Actions Adapter for Events Bottom Toolbar
 * Material 3 implementation with custom styling support
 */
package net.calvuz.qdue.ui.features.events.components;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.core.common.utils.Library;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for Event Action Buttons
 * Supports different action types with custom styling
 */
public class EventsToolbarActionsAdapter extends RecyclerView.Adapter<EventsToolbarActionsAdapter.ActionViewHolder> {

    private static final String TAG = "EventsActionsAdapter";

    private final Context mContext;
    private List<EventsBottomSelectionToolbar.EventAction> mActions = new ArrayList<>();
    private OnActionClickListener mClickListener;

    public EventsToolbarActionsAdapter(@NonNull Context context) {
        mContext = context;
    }

    // ==================== VIEW HOLDER ====================

    static class ActionViewHolder extends RecyclerView.ViewHolder {
        MaterialButton actionButton;

        ActionViewHolder(@NonNull MaterialButton button) {
            super(button);
            actionButton = button;
        }

        /**
         * 🎨 Apply styling based on action type
         */
        void applyActionStyling(Context context, EventsBottomSelectionToolbar.EventAction action) {
            if (actionButton == null) return;

            // Base styling
            int iconSize = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 24,
                    context.getResources().getDisplayMetrics()
            );
            actionButton.setIconSize(iconSize);

            // Minimum touch target
            int minTouchTarget = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 48,
                    context.getResources().getDisplayMetrics()
            );
//            actionButton.setMinWidth(minTouchTarget);
            actionButton.setMinHeight(minTouchTarget);

            // Apply colors based on action type
            applyActionColors(context, action);
        }

        /**
         * 🎨 Apply colors based on action type using floatingMenu attributes
         */
        private void applyActionColors(Context context, EventsBottomSelectionToolbar.EventAction action) {
            ColorStateList backgroundTint;
            ColorStateList iconTint;
            ColorStateList textColor;
            ColorStateList rippleColor;

            switch (action.getType()) {
                case DESTRUCTIVE:
                    // Red colors for destructive actions (Delete)
                    backgroundTint = ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.red_500)
                    );
                    iconTint = ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.grey_50)
                    );
                    textColor = ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.grey_50)
                    );
                    rippleColor = ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.red_300)
                    );
                    break;

                case PRIMARY:
                    // Primary blue colors for main actions (Edit)
                    backgroundTint = ColorStateList.valueOf(
                            Library.getColorByThemeAttr(context, R.attr.floatingMenuPrimary)
                    );
                    iconTint = ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.grey_50)
                    );
                    textColor = ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.grey_50)
                    );
                    rippleColor = ColorStateList.valueOf(
                            Library.getColorByThemeAttr(context, R.attr.floatingMenuSelected)
                    );
                    break;

                case SECONDARY:
                default:
                    // Surface colors for secondary actions (Share, Export, etc.)
                    backgroundTint = ColorStateList.valueOf(
                            Library.getColorByThemeAttr(context, R.attr.floatingMenuSurface)
                    );
                    iconTint = ColorStateList.valueOf(
                            Library.getColorByThemeAttr(context, R.attr.floatingMenuOnSurface)
                    );
                    textColor = ColorStateList.valueOf(
                            Library.getColorByThemeAttr(context, R.attr.floatingMenuOnSurface)
                    );
                    rippleColor = ColorStateList.valueOf(
                            Library.getColorByThemeAttr(context, R.attr.floatingMenuSelected)
                    );
                    break;
            }

            // Apply colors to button
            actionButton.setBackgroundTintList(backgroundTint);
            actionButton.setIconTint(iconTint);
            actionButton.setTextColor(textColor);
            actionButton.setRippleColor(rippleColor);

            Log.v(TAG, "Applied " + action.getType() + " styling to " + action.getLabel());
        }
    }

    // ==================== ADAPTER METHODS ====================

    @NonNull
    @Override
    public ActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create MaterialButton with proper styling
        MaterialButton button = new MaterialButton(mContext);

        // Set layout parameters for horizontal RecyclerView
        RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
        );

        // Margin between buttons
        int margin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8,
                parent.getContext().getResources().getDisplayMetrics()
        );
        layoutParams.setMargins(margin, 0, margin, 0);
        button.setLayoutParams(layoutParams);

        // Base button styling
        button.setCornerRadius((int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 12,
                mContext.getResources().getDisplayMetrics()
        ));

        return new ActionViewHolder(button);
    }

    @Override
    public void onBindViewHolder(@NonNull ActionViewHolder holder, int position) {
        EventsBottomSelectionToolbar.EventAction action = mActions.get(position);

        // Set button content
//        holder.actionButton.setText(action.getLabel());
        holder.actionButton.setIcon(ContextCompat.getDrawable(mContext, action.getIconRes()));

        // Apply styling based on action type
        holder.applyActionStyling(mContext, action);

        // Set click listener with enhanced feedback
        holder.actionButton.setOnClickListener(v -> {
            if (mClickListener != null) {
                // Enhanced haptic feedback
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

                // Visual feedback animation
                animateActionClick(holder.actionButton);

                // Trigger callback
                mClickListener.onActionClicked(action);

                Log.d(TAG, "Action clicked: " + action.getLabel());
            }
        });

        // Accessibility
        holder.actionButton.setContentDescription(
                "Azione: " + action.getLabel() + " per eventi selezionati"
        );
    }

    @Override
    public int getItemCount() {
        return mActions.size();
    }

    // ==================== ANIMATION ====================

    /**
     * 🎬 Enhanced visual feedback animation
     */
    private void animateActionClick(MaterialButton button) {
        button.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    button.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .start();
                })
                .start();
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * 🔄 Update actions list
     */
    public void updateActions(List<EventsBottomSelectionToolbar.EventAction> actions) {
        mActions = new ArrayList<>(actions);
        notifyDataSetChanged();

        Log.d(TAG, "✅ Actions updated: " + actions.size() + " items");
    }

    /**
     * 🧹 Clear all data
     */
    public void clearData() {
        mActions.clear();
        notifyDataSetChanged();

        Log.d(TAG, "✅ Actions cleared");
    }

    /**
     * 📡 Set action click listener
     */
    public void setOnActionClickListener(OnActionClickListener listener) {
        mClickListener = listener;
    }

    // ==================== INTERFACE ====================

    /**
     * 📋 Callback interface for action clicks
     */
    public interface OnActionClickListener {
        void onActionClicked(EventsBottomSelectionToolbar.EventAction action);
    }
}