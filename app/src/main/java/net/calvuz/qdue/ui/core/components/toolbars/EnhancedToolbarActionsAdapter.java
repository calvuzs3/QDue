package net.calvuz.qdue.ui.core.components.toolbars;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.ui.core.common.enums.ToolbarAction;
import net.calvuz.qdue.ui.core.common.interfaces.DayLongClickListener;
import net.calvuz.qdue.ui.core.common.utils.Library;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 🔧 Enhanced adapter with semantic styling and accessibility
 * Fixed version with proper state management and complete implementation
 */
public class EnhancedToolbarActionsAdapter extends RecyclerView.Adapter<EnhancedToolbarActionsAdapter.EnhancedActionViewHolder> {

    private static final String TAG = "EnhancedToolbarAdapter";

    // Adapter state
    private List<ToolbarAction> mActions = new ArrayList<>();
    private DayLongClickListener mListener;
    private Day mCurrentDay;
    private LocalDate mCurrentDate;
    private boolean mIsDestroyed = false;
    private OnActionClickListener mActionClickListener;

    // ==================== INTERFACES ====================

    /**
     * Interface for action click callbacks
     */
    public interface OnActionClickListener {
        void onActionClick(ToolbarAction action);
    }

    /**
     * Constructor
     */
    public EnhancedToolbarActionsAdapter() {
        // Initialize with empty state
    }

    // ==================== ADAPTER METHODS ====================

    @NonNull
    @Override
    public EnhancedActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_toolbar_action, parent, false);
        return new EnhancedActionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EnhancedActionViewHolder holder, int position) {
        if (mIsDestroyed) {
            Log.w(TAG, "Attempting to bind to destroyed adapter");
            return;
        }

        ToolbarAction action = mActions.get(position);
        holder.bind(action, mListener, mCurrentDay, mCurrentDate, this);
    }

    @Override
    public int getItemCount() {
        return mActions.size();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        // Additional cleanup when adapter is detached
        clearData();
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * Update actions list
     */
    public void updateActions(List<ToolbarAction> actions) {
        if (mIsDestroyed) return;

        mActions.clear();
        if (actions != null) {
            mActions.addAll(actions);
        }
        notifyDataSetChanged();
    }

    /**
     * Set action click listener
     */
    public void setOnActionClickListener(OnActionClickListener listener) {
        mActionClickListener = listener;
    }

    /**
     * Set current context for binding
     */
    public void setContext(DayLongClickListener listener, Day currentDay, LocalDate currentDate) {
        mListener = listener;
        mCurrentDay = currentDay;
        mCurrentDate = currentDate;
    }

    /**
     * 🧹 Clear adapter data for memory cleanup
     */
    public void clearData() {
        if (mIsDestroyed) return;

        Log.v(TAG, "Clearing adapter data...");

        try {
            int itemCount = mActions.size();

            // Clear references
            mListener = null;
            mCurrentDay = null;
            mCurrentDate = null;
            mActionClickListener = null;

            // Clear actions
            mActions.clear();

            // Notify adapter of range removal
            if (itemCount > 0) {
                notifyItemRangeRemoved(0, itemCount);
            }

            mIsDestroyed = true;
            Log.v(TAG, "✅ Adapter data cleared");

        } catch (Exception e) {
            Log.e(TAG, "Error clearing adapter data", e);
            // Force clear
            mActions.clear();
            mListener = null;
            mCurrentDay = null;
            mCurrentDate = null;
            mActionClickListener = null;
            mIsDestroyed = true;
        }
    }

    // ==================== GETTER METHODS ====================

    /**
     * Check if adapter is destroyed
     */
    public boolean isDestroyed() {
        return mIsDestroyed;
    }

    /**
     * Get current actions list (defensive copy)
     */
    public List<ToolbarAction> getCurrentActions() {
        return new ArrayList<>(mActions);
    }

    /**
     * Get current context info
     */
    public boolean hasValidContext() {
        return mListener != null && !mIsDestroyed;
    }

    /**
     * Get action click listener (package access for ViewHolder)
     */
    OnActionClickListener getActionClickListener() {
        return mActionClickListener;
    }

    // ==================== ENHANCED VIEWHOLDER ====================

    /**
     * 🎨 Enhanced ViewHolder with semantic styling and rich interactions
     */
    public static class EnhancedActionViewHolder extends RecyclerView.ViewHolder {
        private final MaterialButton mActionButton;

        public EnhancedActionViewHolder(@NonNull View itemView) {
            super(itemView);
            mActionButton = itemView.findViewById(R.id.btn_toolbar_action);
        }

        public void bind(ToolbarAction action, DayLongClickListener listener, Day day, LocalDate date,
                         EnhancedToolbarActionsAdapter adapter) {
            if (action == null) {
                Log.w(TAG, "Cannot bind null action");
                return;
            }

            Context context = itemView.getContext();

            // 🎯 Set icon and content
            mActionButton.setIconResource(action.getIconRes());

            // 🎨 Apply semantic styling
            applySemanticStyling(action, context);

            // ♿ Enhanced accessibility
            setupAccessibility(action, date, context);

            // 👆 Enhanced interactions
            setupInteractions(action, listener, day, date, adapter);
        }

        /**
         * 🎨 Apply semantic color styling based on action type
         */
        private void applySemanticStyling(ToolbarAction action, Context context) {
            ColorStateList backgroundTint;
            ColorStateList iconTint;

            switch (action) {
                case MALATTIA:
                    // Red for urgent/health actions
                    backgroundTint = ColorStateList.valueOf(
                            Library.getColorByThemeAttr(context, R.attr.floatingMenuDestructive)
                    );
                    iconTint = ColorStateList.valueOf(Color.WHITE);
                    mActionButton.setElevation(dpToPx(context, 4));
                    break;

                case FERIE:
                    // Primary blue for planned actions
                    backgroundTint = ColorStateList.valueOf(
                            Library.getColorByThemeAttr(context, R.attr.floatingMenuPrimary)
                    );
                    iconTint = ColorStateList.valueOf(Color.WHITE);
                    mActionButton.setElevation(dpToPx(context, 2));
                    break;

                case LEGGE_104:
                    // Purple for special/protected actions
                    backgroundTint = ColorStateList.valueOf(Color.parseColor("#9C27B0"));
                    iconTint = ColorStateList.valueOf(Color.WHITE);
                    mActionButton.setElevation(dpToPx(context, 2));
                    break;

                case VIEW_EVENTS:
                    // Neutral for informational actions
                    backgroundTint = ColorStateList.valueOf(
                            Library.getColorByThemeAttr(context, R.attr.floatingMenuSurface)
                    );
                    iconTint = ColorStateList.valueOf(
                            Library.getColorByThemeAttr(context, R.attr.floatingMenuOnBackground)
                    );
                    mActionButton.setElevation(0f);
                    break;

                case STRAORDINARIO:
                    // Orange for work-related special actions
                    backgroundTint = ColorStateList.valueOf(Color.parseColor("#FF9800"));
                    iconTint = ColorStateList.valueOf(Color.WHITE);
                    mActionButton.setElevation(dpToPx(context, 1));
                    break;

                default:
                    // Default accent styling
                    backgroundTint = ColorStateList.valueOf(
                            Library.getColorByThemeAttr(context, R.attr.floatingMenuSelected)
                    );
                    iconTint = ColorStateList.valueOf(
                            Library.getColorByThemeAttr(context, R.attr.floatingMenuOnBackground)
                    );
                    mActionButton.setElevation(0f);
                    break;
            }

            mActionButton.setBackgroundTintList(backgroundTint);
            mActionButton.setIconTint(iconTint);

            // Add ripple effect
            mActionButton.setRippleColor(
                    ColorStateList.valueOf(Library.getColorByThemeAttr(context, R.attr.floatingMenuSelected))
            );
        }

        /**
         * ♿ Enhanced accessibility setup
         */
        private void setupAccessibility(ToolbarAction action, LocalDate date, Context context) {
            // Content description
            String description = context.getString(action.getLabelRes());
            mActionButton.setContentDescription(description);

            // State description for context
            String stateDescription = "";
            if (date != null) {
                if (isPastDate(date)) {
                    stateDescription = "Giorno passato";
                } else if (isWeekend(date)) {
                    stateDescription = "Fine settimana";
                } else if (isToday(date)) {
                    stateDescription = "Oggi";
                }
            }

            if (!stateDescription.isEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    mActionButton.setStateDescription(stateDescription);
                }
            }

            // Accessibility actions
            mActionButton.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                @Override
                public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK);
                    }
                }
            });
        }

        /**
         * 👆 Enhanced interaction setup
         */
        private void setupInteractions(ToolbarAction action, DayLongClickListener listener,
                                       Day day, LocalDate date, EnhancedToolbarActionsAdapter adapter) {
            // Enhanced click with haptic feedback and animation
            mActionButton.setOnClickListener(v -> {
                // Haptic feedback
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

                // Visual feedback animation
                animateButtonPress(v);

                // Execute action via DayLongClickListener (primary)
                if (listener != null) {
                    listener.onToolbarActionSelected(action, day, date);
                }

                // Also execute via OnActionClickListener (secondary/alternative)
                if (adapter != null && adapter.getActionClickListener() != null) {
                    adapter.getActionClickListener().onActionClick(action);
                }
            });

            // Long click for additional info/tooltip
            mActionButton.setOnLongClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                showActionTooltip(action, v);
                return true;
            });
        }

        /**
         * 🎬 Button press animation
         */
        private void animateButtonPress(View button) {
            button.animate()
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(80)
                    .withEndAction(() -> {
                        button.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(80)
                                .setInterpolator(new OvershootInterpolator(1.2f))
                                .start();
                    })
                    .start();
        }

        /**
         * 💬 Show tooltip on long press
         */
        private void showActionTooltip(ToolbarAction action, View anchor) {
            // Simple tooltip implementation
            Toast tooltip = Toast.makeText(
                    anchor.getContext(),
                    anchor.getContext().getString(action.getLabelRes()),
                    Toast.LENGTH_SHORT
            );

            // Position tooltip above the button
            int[] location = new int[2];
            anchor.getLocationOnScreen(location);
            tooltip.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                    location[0], location[1] - anchor.getHeight());

            tooltip.show();
        }

        // ==================== UTILITY METHODS ====================

        /**
         * 📅 Date utility methods
         */
        private static boolean isToday(LocalDate date) {
            return date.equals(LocalDate.now());
        }

        private static boolean isPastDate(LocalDate date) {
            return date.isBefore(LocalDate.now());
        }

        private static boolean isWeekend(LocalDate date) {
            int dayOfWeek = date.getDayOfWeek().getValue();
            return dayOfWeek == 6 || dayOfWeek == 7; // Saturday = 6, Sunday = 7
        }



        /**
         * 🛠️ Convert dp to pixels
         */
        private static float dpToPx(Context context, int dp) {
            return dp * context.getResources().getDisplayMetrics().density;
        }
    }
}