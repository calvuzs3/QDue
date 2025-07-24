/**
 * Generic selection toolbar with dependency injection support
 * Reusable across different selection contexts
 */
package net.calvuz.qdue.ui.core.components.selection;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.core.common.utils.Library;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionAction;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionContext;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionValidationResult;
import net.calvuz.qdue.ui.core.components.selection.styling.SelectionActionStyleProvider;
import net.calvuz.qdue.ui.core.components.selection.validation.SelectionValidator;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Generic selection toolbar that can be used with any selection type
 *
 * @param <T> The type of items being selected
 */
public class SelectionToolbar<T> {

    private static final String TAG = "SelectionToolbar";

    // Configuration
    private static final int MAX_ACTIONS_DEFAULT = 5;
    private static final int ANIMATION_DURATION = 250;

    // Dependencies (injected)
    private final Context mContext;
    private final List<SelectionAction<T>> mAvailableActions;
    private final SelectionValidator<T> mValidator;
    private final SelectionActionStyleProvider mStyleProvider;
    private final int mMaxActions;

    // Views
    private MaterialCardView mToolbarContainer;
    private RecyclerView mActionsRecyclerView;
    private SelectionToolbarAdapter<T> mAdapter;

    // State
    private SelectionContext<T> mCurrentContext;
    private boolean mIsVisible = false;
    private boolean mIsDestroyed = false;

    // Animations
    private AnimatorSet mShowAnimation;
    private AnimatorSet mHideAnimation;

    /**
     * Constructor with dependency injection
     *
     * @param context Android context
     * @param availableActions All possible actions for this toolbar
     * @param validator Validator for action availability
     * @param styleProvider Provider for action styling
     * @param maxActions Maximum number of actions to display
     */
    @Inject
    public SelectionToolbar(
            @NonNull Context context,
            @NonNull @Named("availableActions") List<SelectionAction<T>> availableActions,
            @NonNull SelectionValidator<T> validator,
            @NonNull SelectionActionStyleProvider styleProvider,
            @Named("maxActions") int maxActions) {

        mContext = context;
        mAvailableActions = availableActions;
        mValidator = validator;
        mStyleProvider = styleProvider;
        mMaxActions = maxActions > 0 ? maxActions : MAX_ACTIONS_DEFAULT;

        initializeViews();
    }

    /**
     * Initialize toolbar views
     */
    private void initializeViews() {
        // Create container
        mToolbarContainer = new MaterialCardView(mContext);
        applyContainerStyling();

        // Create RecyclerView
        mActionsRecyclerView = new RecyclerView(mContext);
        setupRecyclerView();

        // Add to container
        mToolbarContainer.addView(mActionsRecyclerView);

        // Initially hidden
        mToolbarContainer.setVisibility(View.GONE);
        mToolbarContainer.setAlpha(0f);

        Log.d(TAG, "SelectionToolbar initialized with " + mAvailableActions.size() + " available actions");
    }

    /**
     * Apply Material 3 styling to container
     */
    private void applyContainerStyling() {
        // Background
        mToolbarContainer.setCardBackgroundColor(
                Library.getColorByThemeAttr(mContext, R.attr.floatingMenuBackground)
        );

        // Elevation and shape
        mToolbarContainer.setCardElevation(dpToPx(12));
        mToolbarContainer.setRadius(dpToPx(20));

        // Stroke
        mToolbarContainer.setStrokeWidth(dpToPx(1));
        mToolbarContainer.setStrokeColor(
                Library.getColorByThemeAttr(mContext, R.attr.floatingMenuOnBackground)
        );

        // Z-index for visibility
        mToolbarContainer.setTranslationZ(dpToPx(24));
    }

    /**
     * Setup RecyclerView for actions
     */
    private void setupRecyclerView() {
        // Layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                mContext, LinearLayoutManager.HORIZONTAL, false
        );
        mActionsRecyclerView.setLayoutManager(layoutManager);

        // Adapter with dependencies
        mAdapter = new SelectionToolbarAdapter<>(mContext, mStyleProvider);
        mAdapter.setOnActionClickListener(this::onActionClicked);
        mActionsRecyclerView.setAdapter(mAdapter);

        // Performance
        mActionsRecyclerView.setHasFixedSize(false);
        mActionsRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // Padding
        int padding = dpToPx(16);
        mActionsRecyclerView.setPadding(padding, padding, padding, padding);
    }

    /**
     * Show the toolbar with a selection context
     *
     * @param container Parent container to add toolbar to
     * @param context Selection context with items and metadata
     */
    public void show(@NonNull ViewGroup container, @NonNull SelectionContext<T> context) {
        if (mIsDestroyed) {
            Log.w(TAG, "Cannot show destroyed toolbar");
            return;
        }

        mCurrentContext = context;

        // Validate and filter actions
        List<SelectionAction<T>> validActions = mValidator.filterValidActions(
                mAvailableActions, context, mMaxActions
        );

        if (validActions.isEmpty()) {
            Log.w(TAG, "No valid actions for selection");
            Toast.makeText(mContext, R.string.selection_toolbar_no_actions_available, Toast.LENGTH_SHORT).show();
            return;
        }

        // Update adapter
        mAdapter.updateActions(validActions);

        // Add to container if needed
        if (mToolbarContainer.getParent() == null) {
            ViewGroup.LayoutParams params = createLayoutParams(container);
            mToolbarContainer.setLayoutParams(params);
            container.addView(mToolbarContainer);
        }

        // Animate in
        animateIn();
        mIsVisible = true;

        Log.d(TAG, "Toolbar shown with " + validActions.size() + " valid actions");
    }

    /**
     * Hide the toolbar
     */
    public void hide() {
        if (mIsDestroyed || !mIsVisible) {
            return;
        }

        animateOut(() -> {
            if (mToolbarContainer.getParent() instanceof ViewGroup) {
                ((ViewGroup) mToolbarContainer.getParent()).removeView(mToolbarContainer);
            }

            mIsVisible = false;
            mCurrentContext = null;
        });
    }

    /**
     * Update the selection context
     */
    public void updateSelection(@NonNull SelectionContext<T> context) {
        if (mIsDestroyed || !mIsVisible) {
            return;
        }

        mCurrentContext = context;

        // Re-validate actions
        List<SelectionAction<T>> validActions = mValidator.filterValidActions(
                mAvailableActions, context, mMaxActions
        );

        mAdapter.updateActions(validActions);

        if (validActions.isEmpty()) {
            hide();
        }
    }

    /**
     * Handle action click
     */
    private void onActionClicked(@NonNull SelectionAction<T> action) {
        if (mCurrentContext == null) {
            Log.w(TAG, "No context available for action: " + action.getId());
            return;
        }

        // Validate action again before execution
        SelectionValidationResult result = mValidator.validate(action, mCurrentContext);

        if (!result.isValid()) {
            String message = mValidator.getValidationMessage(mContext, action, mCurrentContext, result);
            Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();

            // Refresh actions
            updateSelection(mCurrentContext);
            return;
        }

        // Haptic feedback
        mToolbarContainer.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        // Execute action
        action.execute(mCurrentContext, new SelectionAction.ActionCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Action executed successfully: " + action.getId());
                hide();
            }

            @Override
            public void onSuccess(@StringRes int messageResId) {
                Log.d(TAG, "Action executed successfully: " + action.getId());
                Toast.makeText(mContext, messageResId, Toast.LENGTH_SHORT).show();
                hide();
            }

            @Override
            public void onError(@StringRes int messageResId) {
                Log.e(TAG, "Action failed: " + action.getId());
                Toast.makeText(mContext, messageResId, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(@StringRes int messageResId, Object... formatArgs) {
                Log.e(TAG, "Action failed: " + action.getId());
                String message = mContext.getString(messageResId, formatArgs);
                Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled() {
                Log.d(TAG, "Action cancelled: " + action.getId());
            }
        });
    }

    /**
     * Create appropriate layout params for container
     */
    private ViewGroup.LayoutParams createLayoutParams(ViewGroup container) {
        if (container instanceof androidx.coordinatorlayout.widget.CoordinatorLayout) {
            androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params =
                    new androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
            params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            params.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(16));
            return params;
        }

        // Default params
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(16));
        return params;
    }

    /**
     * Animate toolbar entrance
     */
    private void animateIn() {
        cancelAnimations();

        mToolbarContainer.setVisibility(View.VISIBLE);
        mToolbarContainer.setTranslationY(dpToPx(80));
        mToolbarContainer.setAlpha(0f);
        mToolbarContainer.setScaleX(0.95f);
        mToolbarContainer.setScaleY(0.95f);

        mShowAnimation = new AnimatorSet();

        ObjectAnimator translateY = ObjectAnimator.ofFloat(mToolbarContainer, "translationY", dpToPx(80), 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(mToolbarContainer, "alpha", 0f, 1f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mToolbarContainer, "scaleX", 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mToolbarContainer, "scaleY", 0.95f, 1f);

        translateY.setInterpolator(new DecelerateInterpolator(1.2f));
        alpha.setInterpolator(new DecelerateInterpolator());
        scaleX.setInterpolator(new OvershootInterpolator(1.1f));
        scaleY.setInterpolator(new OvershootInterpolator(1.1f));

        mShowAnimation.playTogether(translateY, alpha, scaleX, scaleY);
        mShowAnimation.setDuration(ANIMATION_DURATION);
        mShowAnimation.start();
    }

    /**
     * Animate toolbar exit
     */
    private void animateOut(Runnable onComplete) {
        cancelAnimations();

        mHideAnimation = new AnimatorSet();

        ObjectAnimator translateY = ObjectAnimator.ofFloat(mToolbarContainer, "translationY", 0f, dpToPx(60));
        ObjectAnimator alpha = ObjectAnimator.ofFloat(mToolbarContainer, "alpha", 1f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mToolbarContainer, "scaleX", 1f, 0.9f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mToolbarContainer, "scaleY", 1f, 0.9f);

        mHideAnimation.playTogether(translateY, alpha, scaleX, scaleY);
        mHideAnimation.setDuration((int)(ANIMATION_DURATION * 0.8f));

        mHideAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mToolbarContainer != null) {
                    mToolbarContainer.setVisibility(View.GONE);
                }
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });

        mHideAnimation.start();
    }

    /**
     * Cancel running animations
     */
    private void cancelAnimations() {
        if (mShowAnimation != null && mShowAnimation.isRunning()) {
            mShowAnimation.cancel();
        }
        if (mHideAnimation != null && mHideAnimation.isRunning()) {
            mHideAnimation.cancel();
        }
    }

    /**
     * Check if toolbar is visible
     */
    public boolean isVisible() {
        return mIsVisible && !mIsDestroyed;
    }

    /**
     * Destroy the toolbar
     */
    public void destroy() {
        if (mIsDestroyed) {
            return;
        }

        Log.d(TAG, "Destroying SelectionToolbar");

        hide();
        cancelAnimations();

        if (mActionsRecyclerView != null) {
            mActionsRecyclerView.setAdapter(null);
            mActionsRecyclerView = null;
        }

        if (mAdapter != null) {
            mAdapter.clearData();
            mAdapter = null;
        }

        mToolbarContainer = null;
        mCurrentContext = null;
        mIsDestroyed = true;
    }

    /**
     * Convert dp to pixels
     */
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                mContext.getResources().getDisplayMetrics()
        );
    }
}