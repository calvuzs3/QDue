/**
 * 🎨 Enhanced Actions Adapter for Bottom Toolbar
 * Material 3 implementation with custom styling support
 * Based on EventsToolbarActionsAdapter architecture but adapted for ToolbarAction enum
 */
package net.calvuz.qdue.ui.core.components.toolbars;

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
import net.calvuz.qdue.ui.core.common.enums.ToolbarAction;
import net.calvuz.qdue.ui.core.common.utils.Library;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for Toolbar Action Buttons
 * Supports different action types with custom styling
 */
public class BottomSelectionToolbarAdapter extends RecyclerView.Adapter<BottomSelectionToolbarAdapter.ActionViewHolder> {

    private static final String TAG = "EnhancedActionsAdapter";

    private final Context mContext;
    private List<ToolbarAction> mActions = new ArrayList<>();
    private OnActionClickListener mClickListener;

    public BottomSelectionToolbarAdapter(@NonNull Context context) {
        mContext = context;
    }

    // ==================== ACTION TYPE MAPPING ====================

    /**
     * 🎨 Action Type Classification for Styling
     */
    public enum ActionType {
        PRIMARY,     // Main actions like ADD_EVENT
        SECONDARY,   // Common actions like FERIE, PERMESSO
        SPECIAL,     // Protected actions like LEGGE_104
        DESTRUCTIVE  // Not used in this context but kept for consistency
    }

    /**
     * 🎯 Map ToolbarAction to ActionType for styling
     */
    private ActionType getActionType(ToolbarAction action) {
        switch (action) {
            case ADD_EVENT:
                return ActionType.PRIMARY;

            case LEGGE_104:
                return ActionType.SPECIAL;

            case FERIE:
            case MALATTIA:
            case PERMESSO:
            case STRAORDINARIO:
            default:
                return ActionType.SECONDARY;
        }
    }

    /**
     * 🎨 Get action display properties
     */
    private ActionDisplayInfo getActionDisplayInfo(ToolbarAction action) {
        switch (action) {
            case FERIE:
                return new ActionDisplayInfo("Ferie", R.drawable.ic_rounded_beach_access_24);
            case MALATTIA:
                return new ActionDisplayInfo("Malattia", R.drawable.ic_rounded_local_hospital_24);
            case PERMESSO:
                return new ActionDisplayInfo("Permesso", R.drawable.ic_rounded_schedule_24);
            case LEGGE_104:
                return new ActionDisplayInfo("L.104", R.drawable.ic_rounded_accessible_24);
            case STRAORDINARIO:
                return new ActionDisplayInfo("Straord.", R.drawable.ic_rounded_overtime_gears_24);
            case ADD_EVENT:
                return new ActionDisplayInfo("Evento", R.drawable.ic_rounded_calendar_add_on_24);
            default:
                return new ActionDisplayInfo(action.name(), R.drawable.ic_rounded_help_24);
        }
    }

    /**
     * 📋 Action display information
     */
    private static class ActionDisplayInfo {
        final String label;
        final int iconRes;

        ActionDisplayInfo(String label, int iconRes) {
            this.label = label;
            this.iconRes = iconRes;
        }
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
        void applyActionStyling(Context context, ToolbarAction action, ActionType actionType) {
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
            actionButton.setMinHeight(minTouchTarget);

            // Apply colors based on action type
            applyActionColors(context, actionType);
        }

        /**
         * 🎨 Apply colors based on action type using floatingMenu attributes
         */
        private void applyActionColors(Context context, ActionType actionType) {
            ColorStateList backgroundTint;
            ColorStateList iconTint;
            ColorStateList textColor;
            ColorStateList rippleColor;

            switch (actionType) {
                case PRIMARY:
                    // Primary blue colors for main actions (ADD_EVENT)
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

                case SPECIAL:
                    // Special colors for protected actions (LEGGE_104)
                    backgroundTint = ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.purple_500)
                    );
                    iconTint = ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.grey_50)
                    );
                    textColor = ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.grey_50)
                    );
                    rippleColor = ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.purple_300)
                    );
                    break;

                case DESTRUCTIVE:
                    // Red colors for destructive actions (if needed)
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

                case SECONDARY:
                default:
                    // Surface colors for secondary actions (FERIE, MALATTIA, etc.)
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

            Log.v(TAG, "Applied " + actionType + " styling to " + actionButton.getText());
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
        ToolbarAction action = mActions.get(position);
        ActionDisplayInfo displayInfo = getActionDisplayInfo(action);
        ActionType actionType = getActionType(action);

        // Set button content - icon only for clean look
        holder.actionButton.setIcon(ContextCompat.getDrawable(mContext, displayInfo.iconRes));
        // Optionally set text for accessibility or debug
        // holder.actionButton.setText(displayInfo.label);

        // Apply styling based on action type
        holder.applyActionStyling(mContext, action, actionType);

        // Set click listener with enhanced feedback
        holder.actionButton.setOnClickListener(v -> {
            if (mClickListener != null) {
                // Enhanced haptic feedback
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

                // Visual feedback animation
                animateActionClick(holder.actionButton);

                // Trigger callback
                mClickListener.onActionClicked(action);

                Log.d(TAG, "Action clicked: " + action);
            }
        });

        // Accessibility
        holder.actionButton.setContentDescription(
                "Azione: " + displayInfo.label + " per giorni selezionati"
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
    public void updateActions(List<ToolbarAction> actions) {
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
        void onActionClicked(ToolbarAction action);
    }
}