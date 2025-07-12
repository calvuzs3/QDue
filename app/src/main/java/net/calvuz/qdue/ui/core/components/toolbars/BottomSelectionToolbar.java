package net.calvuz.qdue.ui.core.components.toolbars;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.ui.core.common.enums.ToolbarAction;
import net.calvuz.qdue.ui.core.common.interfaces.DayLongClickListener;
import net.calvuz.qdue.ui.core.common.utils.Log;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Bottom Selection Toolbar for Material 3 Design
 * Replaces FloatingDayToolbar with a bottom-anchored toolbar
 * that appears during selection mode
 */
public class BottomSelectionToolbar {

    private static final String TAG = "BottomSelectionToolbar";

    // Constants
    private static final int ANIMATION_DURATION = 300;

    // Views
    private final Context mContext;
    private MaterialCardView mToolbarContainer;
    private TextView mSelectionCountText;
    private MaterialButton mCloseSelectionButton;
    private MaterialButton mMoreActionsButton;
    private RecyclerView mQuickActionsRecyclerView;

    // Data
    private ToolbarActionsAdapter mActionsAdapter;
    private DayLongClickListener mListener;
    private Set<LocalDate> mSelectedDates;
    private boolean mIsVisible = false;

    // Animation
    private AnimatorSet mShowAnimation;
    private AnimatorSet mHideAnimation;

    /**
     * Constructor
     */
    public BottomSelectionToolbar(@NonNull Context context) {
        mContext = context;
        initializeViews();
        setupListeners();
    }

    /**
     * Initialize toolbar views
     */
    private void initializeViews() {
        // Inflate toolbar layout
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mToolbarContainer = (MaterialCardView) inflater.inflate(R.layout.bottom_selection_toolbar, null);

        // Get view references
        mSelectionCountText = mToolbarContainer.findViewById(R.id.tv_selection_count);
        mCloseSelectionButton = mToolbarContainer.findViewById(R.id.btn_close_selection);
        mMoreActionsButton = mToolbarContainer.findViewById(R.id.btn_more_actions);
        mQuickActionsRecyclerView = mToolbarContainer.findViewById(R.id.rv_quick_actions);

        // Setup RecyclerView
        mQuickActionsRecyclerView.setLayoutManager(
                new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false)
        );

        mActionsAdapter = new ToolbarActionsAdapter();
        mQuickActionsRecyclerView.setAdapter(mActionsAdapter);

        // Initially hide toolbar
        mToolbarContainer.setVisibility(View.GONE);
        mToolbarContainer.setTranslationY(200f); // Start below screen

        Log.d(TAG, "Bottom selection toolbar initialized");
    }

    /**
     * Setup click listeners
     */
    private void setupListeners() {
        // Close selection mode
        mCloseSelectionButton.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onSelectionModeChanged(false, 0);
            }
            hide();
        });

        // More actions menu
        mMoreActionsButton.setOnClickListener(v -> {
            showMoreActionsMenu();
        });

        // Setup adapter callbacks
        mActionsAdapter.setOnActionClickListener(this::onQuickActionClicked);
    }

    /**
     * Show toolbar with animation
     */
    public void show(@NonNull ViewGroup parentContainer, @NonNull Set<LocalDate> selectedDates,
                     @NonNull DayLongClickListener listener) {

        if (mIsVisible) return;

        mSelectedDates = selectedDates;
        mListener = listener;

        // Add to parent container if not already added
        if (mToolbarContainer.getParent() == null) {
            parentContainer.addView(mToolbarContainer);
        }

        // Update content
        updateSelectionCount();
        updateQuickActions();

        // Show with animation
        animateIn();

        mIsVisible = true;
        Log.d(TAG, "Toolbar shown with " + selectedDates.size() + " selected dates");
    }

    /**
     * Hide toolbar with animation
     */
    public void hide() {
        if (!mIsVisible) return;

        animateOut(() -> {
            mToolbarContainer.setVisibility(View.GONE);
            if (mToolbarContainer.getParent() instanceof ViewGroup) {
                ((ViewGroup) mToolbarContainer.getParent()).removeView(mToolbarContainer);
            }
        });

        mIsVisible = false;
        Log.d(TAG, "Toolbar hidden");
    }

    /**
     * Update selection count display
     */
    public void updateSelectionCount() {
        if (mSelectedDates == null || mSelectionCountText == null) return;

        int count = mSelectedDates.size();
        String text;

        if (count == 1) {
            text = "1 giorno selezionato";
        } else {
            text = count + " giorni selezionati";
        }

        mSelectionCountText.setText(text);
    }

    /**
     * Update quick actions based on selected dates
     */
    private void updateQuickActions() {
        if (mSelectedDates == null || mSelectedDates.isEmpty()) {
            mActionsAdapter.setActions(ToolbarAction.getDefaultActions());
            return;
        }

        // Get actions based on selection context
        List<ToolbarAction> actions = ToolbarAction.getActionsForDates(mSelectedDates);
        mActionsAdapter.setActions(actions);
    }

    /**
     * Handle quick action click
     */
    private void onQuickActionClicked(ToolbarAction action) {
        if (mListener == null || mSelectedDates == null) return;

        Log.d(TAG, "Quick action clicked: " + action.name());

        // Handle action for all selected dates
        for (LocalDate date : mSelectedDates) {
            // Get day object (this might need to be passed from adapter)
            Day day = null; // TODO: Get actual day object
            mListener.onToolbarActionSelected(action, day, date);
        }

        // Exit selection mode after action
        if (mListener != null) {
            mListener.onSelectionModeChanged(false, 0);
        }
        hide();
    }

    /**
     * Show more actions menu
     */
    private void showMoreActionsMenu() {
        // TODO: Implement overflow menu with additional actions
        Log.d(TAG, "More actions menu requested");
    }

    /**
     * Animate toolbar in from bottom
     */
    private void animateIn() {
        if (mShowAnimation != null && mShowAnimation.isRunning()) {
            mShowAnimation.cancel();
        }

        mToolbarContainer.setVisibility(View.VISIBLE);

        ObjectAnimator translateY = ObjectAnimator.ofFloat(mToolbarContainer, "translationY", 200f, 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(mToolbarContainer, "alpha", 0f, 1f);

        mShowAnimation = new AnimatorSet();
        mShowAnimation.playTogether(translateY, alpha);
        mShowAnimation.setDuration(ANIMATION_DURATION);
        mShowAnimation.start();
    }

    /**
     * Animate toolbar out to bottom
     */
    private void animateOut(@Nullable Runnable onComplete) {
        if (mHideAnimation != null && mHideAnimation.isRunning()) {
            mHideAnimation.cancel();
        }

        ObjectAnimator translateY = ObjectAnimator.ofFloat(mToolbarContainer, "translationY", 0f, 200f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(mToolbarContainer, "alpha", 1f, 0f);

        mHideAnimation = new AnimatorSet();
        mHideAnimation.playTogether(translateY, alpha);
        mHideAnimation.setDuration(ANIMATION_DURATION);

        if (onComplete != null) {
            mHideAnimation.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    onComplete.run();
                }
            });
        }

        mHideAnimation.start();
    }

    /**
     * Check if toolbar is currently visible
     */
    public boolean isVisible() {
        return mIsVisible;
    }

    /**
     * Get the toolbar container view
     */
    public View getToolbarView() {
        return mToolbarContainer;
    }

    /**
     * Update selection when dates change
     */
    public void updateSelection(@NonNull Set<LocalDate> selectedDates) {
        mSelectedDates = selectedDates;
        updateSelectionCount();
        updateQuickActions();
    }

    /**
     * Cleanup resources
     */
    public void destroy() {
        if (mShowAnimation != null) {
            mShowAnimation.cancel();
        }
        if (mHideAnimation != null) {
            mHideAnimation.cancel();
        }

        if (mToolbarContainer != null && mToolbarContainer.getParent() instanceof ViewGroup) {
            ((ViewGroup) mToolbarContainer.getParent()).removeView(mToolbarContainer);
        }

        mListener = null;
        mSelectedDates = null;
    }
}