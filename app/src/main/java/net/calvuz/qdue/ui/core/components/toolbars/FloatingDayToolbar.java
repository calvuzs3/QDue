package net.calvuz.qdue.ui.core.components.toolbars;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.ui.core.common.enums.ToolbarAction;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.ui.core.common.interfaces.DayLongClickListener;
import net.calvuz.qdue.ui.core.common.utils.Library;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Floating toolbar that appears near selected day item
 * Provides quick actions for day-related operations
 */
public class FloatingDayToolbar {
    private static final String TAG = "FloatingDayToolbar";
    private static final int MAX_ACTIONS = 5;
    private static final int ANIMATION_DURATION = 200;

    // ==================== CORE COMPONENTS ====================
    private final Context mContext;
    private PopupWindow mPopupWindow;
    private View mToolbarView;
    private TextView mToolbarHeader;
    private RecyclerView mActionsRecyclerView;
    private BottomSelectionToolbarAdapter mActionsAdapter;
    private boolean mIsDestroyed = false;

    // Current state
    private Day mCurrentDay;
    private LocalDate mCurrentDate;
    private DayLongClickListener mListener;
    private HalfTeam mCurrentUserTeam;

    // Animations
    private ObjectAnimator mShowAnimator;
    private ObjectAnimator mHideAnimator;

    public FloatingDayToolbar(@NonNull Context context, @Nullable HalfTeam currentUserTeam) {
        mContext = context;
        mCurrentUserTeam = currentUserTeam;
        initializeToolbar();
    }

    // ==================== INITIALIZATION ====================

    private void initializeToolbar() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mToolbarView = inflater.inflate(R.layout.floating_day_toolbar, null);

        // Initialize header
        mToolbarHeader = mToolbarView.findViewById(R.id.tv_toolbar_title);

        // Setup RecyclerView
        mActionsRecyclerView = mToolbarView.findViewById(R.id.rv_toolbar_actions);
        mActionsRecyclerView.setLayoutManager(
                new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false)
        );

        // Setup enhanced adapter
        mActionsAdapter = new BottomSelectionToolbarAdapter(mContext);
        mActionsRecyclerView.setAdapter(mActionsAdapter);

        // Create popup with enhanced properties
        mPopupWindow = new PopupWindow(
                mToolbarView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setAnimationStyle(0); // Custom animations
        mPopupWindow.setElevation(8f); // Material elevation

        Log.d(TAG, "Floating toolbar initialized");
    }

    // ==================== ENHANCED ACTION MANAGEMENT ====================

//    /**
//     * Update available actions based on current day context
//     */
//    private void updateActions() {
//        List<ToolbarAction> actions;
//
//        if (mCurrentDay != null) {
//            // Show all actions - could be filtered based on day properties
//            actions = Arrays.asList(ToolbarAction.getAllActions());
//        } else {
//            // Fallback to basic actions
//            actions = Arrays.asList(ToolbarAction.getQuickEventActions());
//        }
//
//        mActionsAdapter.updateActions(actions);
//    }

    /**
     * 🎯 PRIORITY-BASED ACTION SELECTION
     * Intelligently selects actions based on context and business priorities
     */
    private void updateActions() {
        List<ToolbarAction> actions = selectPriorityActions();

        mActionsAdapter.updateActions(actions);
        updateToolbarHeader(actions);

        // Add subtle animation for action updates
        animateActionUpdate();

        Log.d(TAG, "Updated with " + actions.size() + " priority actions for " + mCurrentDate);
    }

    /**
     * 🧠 SMART ACTION SELECTION LOGIC
     */
    private List<ToolbarAction> selectPriorityActions() {
        List<ToolbarAction> actions = new ArrayList<>();

        // Step 0. User Team

        // Step 1: Add view action if events exist
        if (mCurrentDay != null && mCurrentDay.hasEvents()) {
            actions.add(ToolbarAction.VIEW_EVENTS);
        }

        // Step 2: Add context-specific actions
        if (isPastDate(mCurrentDate)) {
            // Past dates - read-only focus
            if (!actions.contains(ToolbarAction.VIEW_EVENTS)) {
                actions.add(ToolbarAction.VIEW_EVENTS);
            }
            actions.add(ToolbarAction.ADD_EVENT);

        } else if (isWeekend(mCurrentDate, this)) {
            // Weekend - limited work actions
            actions.add(ToolbarAction.STRAORDINARIO);
            actions.add(ToolbarAction.ADD_EVENT);

        } else {
            // Regular workday - priority business actions
            actions.addAll(getWorkdayPriorityActions());
        }

        // Step 3: Ensure we don't exceed max actions
        if (actions.size() > MAX_ACTIONS) {
            actions = actions.subList(0, MAX_ACTIONS);
        }

        // Step 4: Always ensure ADD_EVENT is available if space permits
        if (!actions.contains(ToolbarAction.ADD_EVENT) && actions.size() < MAX_ACTIONS) {
            actions.add(ToolbarAction.ADD_EVENT);
        }

        return actions;
    }

    /**
     * 📋 Priority actions for regular workdays
     */
    private List<ToolbarAction> getWorkdayPriorityActions() {
        return Arrays.asList(
                ToolbarAction.FERIE,         // Priority 2: Most common
                ToolbarAction.MALATTIA,      // Priority 1: Urgent/health
                //ToolbarAction.PERMESSO,      // Priority 3: Common
                ToolbarAction.LEGGE_104,     // Priority 4: Special needs
                ToolbarAction.ADD_EVENT      // Priority 5: General
        );
    }

    // ==================== ENHANCED UI MANAGEMENT ====================

    /**
     * 🎨 CONTEXTUAL HEADER UPDATES
     */
    private void updateToolbarHeader(List<ToolbarAction> actions) {
        if (mToolbarHeader == null) return;

        String headerText = generateContextualHeader();

        if (headerText != null && !headerText.isEmpty()) {
            mToolbarHeader.setText(headerText);
            mToolbarHeader.setVisibility(View.VISIBLE);

            // Apply custom styling to header
            mToolbarHeader.setTextColor(Library.getColorByThemeAttr(mContext, R.attr.floatingMenuOnBackground));
        } else {
            mToolbarHeader.setVisibility(View.GONE);
        }
    }

    /**
     * 📝 Generate smart header text based on context
     */
    private String generateContextualHeader() {
        if (mCurrentDate == null) return null;

        if (isToday(mCurrentDate)) {
            return "Oggi";
        } else if (isTomorrow(mCurrentDate)) {
            return "Domani";
        } else if (isPastDate(mCurrentDate)) {
            return "Visualizza";
        } else if (isWeekend(mCurrentDate, this)) {
            return "Weekend";
        } else if (mCurrentDay != null && mCurrentDay.hasEvents()) {
            return "Azioni per " + formatDate(mCurrentDate);
        } else {
            return "Azioni rapide";
        }
    }

    /**
     * 🎬 Smooth animation for action updates
     */
    private void animateActionUpdate() {
        if (mActionsRecyclerView == null) return;

        mActionsRecyclerView.animate()
                .alpha(0.7f)
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    mActionsRecyclerView.animate()
                            .alpha(1.0f)
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    // ==================== ENHANCED ANIMATIONS ====================

    /**
     * 🎬 Enhanced entrance animation
     */
    private void animateIn() {
        mToolbarView.setAlpha(0f);
        mToolbarView.setScaleX(0.8f);
        mToolbarView.setScaleY(0.8f);
        mToolbarView.setTranslationY(20f);

        mShowAnimator = ObjectAnimator.ofFloat(mToolbarView, "alpha", 0f, 1f);
        mShowAnimator.setDuration(ANIMATION_DURATION);
        mShowAnimator.setInterpolator(new DecelerateInterpolator());

        mShowAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mToolbarView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationY(0f)
                        .setDuration(ANIMATION_DURATION)
                        .setInterpolator(new OvershootInterpolator(1.1f))
                        .start();
            }
        });

        mShowAnimator.start();
    }

    /**
     * 🎬 Enhanced exit animation
     */
    private void animateOut(Runnable onComplete) {
        mHideAnimator = ObjectAnimator.ofFloat(mToolbarView, "alpha", 1f, 0f);
        mHideAnimator.setDuration(150);
        mHideAnimator.setInterpolator(new AccelerateInterpolator());

        mHideAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mToolbarView.animate()
                        .scaleX(0.8f)
                        .scaleY(0.8f)
                        .translationY(10f)
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

    // ==================== PUBLIC API ====================

    /**
     * 🚀 Show enhanced toolbar with context
     */
    public void show(@NonNull View anchorView, @NonNull Day day, @NonNull LocalDate date,
                     @NonNull DayLongClickListener listener) {

        mCurrentDay = day;
        mCurrentDate = date;
        mListener = listener;

        // Update actions and UI
        updateActions();

        // Calculate optimal position
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);

        Rect anchorRect = new Rect();
        anchorView.getGlobalVisibleRect(anchorRect);

        int xOffset = calculateXOffset(anchorView);
        int yOffset = calculateYOffset(anchorView, anchorRect);

        // Show with enhanced animation
        mPopupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY,
                location[0] + xOffset, location[1] + yOffset);

        animateIn();

        Log.d(TAG, "Enhanced toolbar shown for " + date + " with " +
                mActionsAdapter.getItemCount() + " actions");
    }

    /**
     * 🔄 Hide toolbar with animation
     */
    public void hide() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            animateOut(() -> {
                mPopupWindow.dismiss();
                mCurrentDay = null;
                mCurrentDate = null;
                mListener = null;
                Log.d(TAG, "Enhanced toolbar hidden");
            });
        }
    }

    /**
     * ❓ Check if currently showing
     */
    public boolean isShowing() {
        return mPopupWindow != null && mPopupWindow.isShowing();
    }

    // ==================== DESTROY METHOD ====================

    /**
     * 🧹 MAIN DESTROY METHOD
     * Pulisce completamente la memoria e rilascia tutte le risorse
     * <p>
     * ⚠️ IMPORTANTE: Chiamare sempre quando il toolbar non è più necessario
     * per evitare memory leaks
     */
    public void destroy() {
        if (mIsDestroyed) {
            Log.w(TAG, "destroy() called multiple times - already destroyed");
            return;
        }

        Log.d(TAG, "🧹 Starting FloatingDayToolbar destruction...");

        try {
            // Step 1: Stop and cleanup animations
            cleanupAnimations();

            // Step 2: Hide and dismiss popup
            cleanupPopupWindow();

            // Step 3: Cleanup RecyclerView and adapter
            cleanupRecyclerView();

            // Step 4: Clear all references
            clearAllReferences();

            // Step 5: Cleanup view references
            cleanupViews();

            // Step 6: Mark as destroyed
            mIsDestroyed = true;

            Log.d(TAG, "✅ FloatingDayToolbar destruction completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error during FloatingDayToolbar destruction", e);
            // Ensure we mark as destroyed even if cleanup fails
            mIsDestroyed = true;
        }
    }

    // ==================== CLEANUP METHODS ====================

    /**
     * 🎬 Cleanup animations and prevent memory leaks
     */
    private void cleanupAnimations() {
        Log.v(TAG, "Cleaning up animations...");

        try {
            // Stop and cleanup show animator
            if (mShowAnimator != null) {
                if (mShowAnimator.isRunning()) {
                    mShowAnimator.cancel();
                }
                mShowAnimator.removeAllListeners();
                mShowAnimator = null;
            }

            // Stop and cleanup hide animator
            if (mHideAnimator != null) {
                if (mHideAnimator.isRunning()) {
                    mHideAnimator.cancel();
                }
                mHideAnimator.removeAllListeners();
                mHideAnimator = null;
            }

            // Cancel any pending view animations
            if (mToolbarView != null) {
                mToolbarView.clearAnimation();
                mToolbarView.animate().cancel();
            }

            if (mActionsRecyclerView != null) {
                mActionsRecyclerView.clearAnimation();
                mActionsRecyclerView.animate().cancel();
            }

            Log.v(TAG, "✅ Animations cleanup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up animations", e);
        }
    }

    /**
     * 🪟 Cleanup PopupWindow
     */
    private void cleanupPopupWindow() {
        Log.v(TAG, "Cleaning up PopupWindow...");

        try {
            if (mPopupWindow != null) {
                // Dismiss if showing
                if (mPopupWindow.isShowing()) {
                    mPopupWindow.dismiss();
                }

                // Clear content view
                mPopupWindow.setContentView(null);

                // Clear focus and touch listeners
                mPopupWindow.setOutsideTouchable(false);
                mPopupWindow.setFocusable(false);

                // Nullify reference
                mPopupWindow = null;
            }

            Log.v(TAG, "✅ PopupWindow cleanup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up PopupWindow", e);
            // Force nullify even if dismiss fails
            mPopupWindow = null;
        }
    }

    /**
     * 📋 Cleanup RecyclerView and Adapter
     */
    private void cleanupRecyclerView() {
        Log.v(TAG, "Cleaning up RecyclerView and Adapter...");

        try {
            if (mActionsRecyclerView != null) {
                // Stop any pending scroll operations
                mActionsRecyclerView.stopScroll();

                // Clear adapter
                mActionsRecyclerView.setAdapter(null);

                // Clear layout manager
                mActionsRecyclerView.setLayoutManager(null);

                // Clear all item decorations
                while (mActionsRecyclerView.getItemDecorationCount() > 0) {
                    mActionsRecyclerView.removeItemDecorationAt(0);
                }

                // Clear recycled view pool
                RecyclerView.RecycledViewPool recycledViewPool = mActionsRecyclerView.getRecycledViewPool();
                recycledViewPool.clear();

                // Nullify reference
                mActionsRecyclerView = null;
            }

            // Cleanup adapter separately
            if (mActionsAdapter != null) {
                // Clear adapter data if it has a clear method
                try {
                    (mActionsAdapter).clearData();
                } catch (Exception e) {
                    Log.w(TAG, "Error clearing adapter data", e);
                }

                mActionsAdapter = null;
            }

            Log.v(TAG, "✅ RecyclerView cleanup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up RecyclerView", e);
            // Force nullify
            mActionsRecyclerView = null;
            mActionsAdapter = null;
        }
    }

    /**
     * 🔗 Clear all object references
     */
    private void clearAllReferences() {
        Log.v(TAG, "Clearing all references...");

        try {
            // Clear current state
            mCurrentDay = null;
            mCurrentDate = null;
            mListener = null;

            // Clear user team reference (if using enhanced version)
            if (mCurrentUserTeam != null) {
                mCurrentUserTeam = null;
            }

            // Clear context reference (be careful with this)
            // Note: mContext might be needed for some cleanup, so clear last

            Log.v(TAG, "✅ References cleanup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error clearing references", e);
        }
    }

    /**
     * 👁️ Cleanup view references
     */
    private void cleanupViews() {
        Log.v(TAG, "Cleaning up view references...");

        try {
            // Clear header view
            if (mToolbarHeader != null) {
                mToolbarHeader.clearAnimation();
                mToolbarHeader = null;
            }

            // Clear main toolbar view
            if (mToolbarView != null) {
                // Remove from parent if attached
//                ViewParent parent = mToolbarView.getParent();
//                if (parent instanceof ViewGroup) {
//                    ((ViewGroup) parent).removeView(mToolbarView);
//                }

                // Clear all view references
                mToolbarView.clearAnimation();
                mToolbarView = null;
            }

            Log.v(TAG, "✅ Views cleanup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up views", e);
            // Force nullify
            mToolbarHeader = null;
            mToolbarView = null;
        }
    }

    // ==================== POSITIONING LOGIC ====================

    /**
     * 📐 Calculate horizontal offset for optimal positioning
     */
    private int calculateXOffset(View anchorView) {
        int anchorWidth = anchorView.getWidth();
        int toolbarWidth = mToolbarView.getMeasuredWidth();

        if (toolbarWidth == 0) {
            mToolbarView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            toolbarWidth = mToolbarView.getMeasuredWidth();
        }

        return (anchorWidth - toolbarWidth) / 2;
    }

    /**
     * 📐 Calculate vertical offset with screen edge detection
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

        // Smart positioning based on available space
        int spaceAbove = anchorRect.top;
        int spaceBelow = mContext.getResources().getDisplayMetrics().heightPixels - anchorRect.bottom;
        int margin = 16; // 16dp margin

        if (spaceAbove >= toolbarHeight + margin) {
            // Position above with margin
            return -(toolbarHeight + margin);
        } else if (spaceBelow >= toolbarHeight + margin) {
            // Position below with margin
            return anchorHeight + margin;
        } else {
            // Position below as fallback (may overlap)
            return anchorHeight + 8;
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * 📅 Date utility methods
     */
    private static boolean isToday(LocalDate date) {
        return date.equals(LocalDate.now());
    }

    private static boolean isTomorrow(LocalDate date) {
        return date.equals(LocalDate.now().plusDays(1));
    }

    private static boolean isPastDate(LocalDate date) {
        return date.isBefore(LocalDate.now());
    }

    /**
     * Use shifts logic to determine if user is enrolled at work
     */
    private static boolean isWeekend(LocalDate date, FloatingDayToolbar instance) {
        return instance.isUserWorkingDay(date);
    }

    private boolean isUserWorkingDay(LocalDate date) {
        if (mCurrentUserTeam != null && mCurrentDay != null) {
            // Check user enrolled in a shift
            // Not enrolled
            return mCurrentDay.getInWichTeamIsHalfTeam(mCurrentUserTeam) < 0;
        }
        // Enrolled
        return false;
    }

    private static String formatDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        return date.format(formatter);
    }
}