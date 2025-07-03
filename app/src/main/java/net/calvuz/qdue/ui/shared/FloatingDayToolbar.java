package net.calvuz.qdue.ui.shared;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.shared.enums.ToolbarAction;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.ui.shared.interfaces.DayLongClickListener;
import net.calvuz.qdue.utils.Log;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Floating toolbar that appears near selected day item
 * Provides quick actions for day-related operations
 */
public class FloatingDayToolbar {
    private static final String TAG = "FloatingDayToolbar";

    private final Context mContext;
    private PopupWindow mPopupWindow;
    private View mToolbarView;
    private RecyclerView mActionsRecyclerView;
    private ToolbarActionsAdapter mActionsAdapter;

    // Current context
    private Day mCurrentDay;
    private LocalDate mCurrentDate;
    private DayLongClickListener mListener;

    // Animation
    private ObjectAnimator mShowAnimator;
    private ObjectAnimator mHideAnimator;

    public FloatingDayToolbar(@NonNull Context context) {
        mContext = context;
        initializeToolbar();
    }

    /**
     * Initialize the floating toolbar components
     */
    private void initializeToolbar() {
        // Inflate toolbar layout
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mToolbarView = inflater.inflate(R.layout.floating_day_toolbar, null);

        // Setup RecyclerView for actions
        mActionsRecyclerView = mToolbarView.findViewById(R.id.rv_toolbar_actions);
        mActionsRecyclerView.setLayoutManager(
                new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false)
        );

        // Setup adapter
        mActionsAdapter = new ToolbarActionsAdapter();
        mActionsRecyclerView.setAdapter(mActionsAdapter);

        // Create popup window
        mPopupWindow = new PopupWindow(
                mToolbarView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true // focusable for dismiss on outside touch
        );

        // Setup popup properties
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setAnimationStyle(0); // We'll handle animation manually

        Log.d(TAG, "Floating toolbar initialized");
    }

    /**
     * Show toolbar near the specified anchor view
     */
    public void show(@NonNull View anchorView, @NonNull Day day, @NonNull LocalDate date,
                     @NonNull DayLongClickListener listener) {

        mCurrentDay = day;
        mCurrentDate = date;
        mListener = listener;

        // Update actions based on day context
        updateActions();

        // Calculate position
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);

        Rect anchorRect = new Rect();
        anchorView.getGlobalVisibleRect(anchorRect);

        // Position toolbar above or below anchor based on screen space
        int xOffset = calculateXOffset(anchorView);
        int yOffset = calculateYOffset(anchorView, anchorRect);

        // Show popup
        mPopupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY,
                location[0] + xOffset, location[1] + yOffset);

        // Animate in
        animateIn();

        Log.d(TAG, "Toolbar shown for date: " + date);
    }

    /**
     * Hide the toolbar with animation
     */
    public void hide() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            animateOut(() -> {
                mPopupWindow.dismiss();
                mCurrentDay = null;
                mCurrentDate = null;
                mListener = null;
                Log.d(TAG, "Toolbar hidden");
            });
        }
    }

    /**
     * Check if toolbar is currently showing
     */
    public boolean isShowing() {
        return mPopupWindow != null && mPopupWindow.isShowing();
    }

    /**
     * Calculate horizontal offset for positioning
     */
    private int calculateXOffset(View anchorView) {
        // Center the toolbar on the anchor view
        int anchorWidth = anchorView.getWidth();
        int toolbarWidth = mToolbarView.getMeasuredWidth();

        if (toolbarWidth == 0) {
            // Measure if not measured yet
            mToolbarView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            toolbarWidth = mToolbarView.getMeasuredWidth();
        }

        return (anchorWidth - toolbarWidth) / 2;
    }

    /**
     * Calculate vertical offset for positioning
     */
    private int calculateYOffset(View anchorView, Rect anchorRect) {
        int anchorHeight = anchorView.getHeight();
        int toolbarHeight = mToolbarView.getMeasuredHeight();

        if (toolbarHeight == 0) {
            mToolbarView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            toolbarHeight = mToolbarView.getMeasuredHeight();
        }

        // Try to position above anchor first
        int spaceAbove = anchorRect.top;
        int spaceBelow = mContext.getResources().getDisplayMetrics().heightPixels - anchorRect.bottom;

        if (spaceAbove >= toolbarHeight + 16) { // 16dp margin
            // Position above
            return -(toolbarHeight + 16);
        } else {
            // Position below
            return anchorHeight + 16;
        }
    }

    /**
     * Update available actions based on current day context
     */
    private void updateActions() {
        List<ToolbarAction> actions;

        if (mCurrentDay != null) {
            // Show all actions - could be filtered based on day properties
            actions = Arrays.asList(ToolbarAction.getAllActions());
        } else {
            // Fallback to basic actions
            actions = Arrays.asList(ToolbarAction.getQuickEventActions());
        }

        mActionsAdapter.updateActions(actions);
    }

    /**
     * Animate toolbar appearance
     */
    private void animateIn() {
        mToolbarView.setAlpha(0f);
        mToolbarView.setScaleX(0.8f);
        mToolbarView.setScaleY(0.8f);

        mShowAnimator = ObjectAnimator.ofFloat(mToolbarView, "alpha", 0f, 1f);
        mShowAnimator.setDuration(200);
        mShowAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mToolbarView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start();
            }
        });
        mShowAnimator.start();
    }

    /**
     * Animate toolbar disappearance
     */
    private void animateOut(Runnable onComplete) {
        mHideAnimator = ObjectAnimator.ofFloat(mToolbarView, "alpha", 1f, 0f);
        mHideAnimator.setDuration(150);
        mHideAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mToolbarView.animate()
                        .scaleX(0.8f)
                        .scaleY(0.8f)
                        .setDuration(150)
                        .start();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        mHideAnimator.start();
    }

    /**
     * RecyclerView adapter for toolbar actions
     */
    private class ToolbarActionsAdapter extends RecyclerView.Adapter<ToolbarActionViewHolder> {
        private List<ToolbarAction> mActions = Arrays.asList(ToolbarAction.getAllActions());

        @NonNull
        @Override
        public ToolbarActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_toolbar_action, parent, false);
            return new ToolbarActionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ToolbarActionViewHolder holder, int position) {
            ToolbarAction action = mActions.get(position);
            holder.bind(action, mListener, mCurrentDay, mCurrentDate);
        }

        @Override
        public int getItemCount() {
            return mActions.size();
        }

        public void updateActions(List<ToolbarAction> actions) {
            mActions = actions;
            notifyDataSetChanged();
        }
    }

    /**
     * ViewHolder for individual toolbar action buttons
     */
    private static class ToolbarActionViewHolder extends RecyclerView.ViewHolder {
        private final MaterialButton mActionButton;

        public ToolbarActionViewHolder(@NonNull View itemView) {
            super(itemView);
            mActionButton = itemView.findViewById(R.id.btn_toolbar_action);
        }

        public void bind(ToolbarAction action, DayLongClickListener listener, Day day, LocalDate date) {
            mActionButton.setIconResource(action.getIconRes());
            mActionButton.setText(action.getLabelRes());

            mActionButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onToolbarActionSelected(action, day, date);
                }
            });
        }
    }
}