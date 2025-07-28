/**
 * 🎨 Enhanced Bottom Selection Toolbar for EventsListFragment
 * Modern Material 3 implementation for event selection actions
 */
package net.calvuz.qdue.ui.features.events.components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.ui.core.common.utils.Library;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 🎨 Bottom Selection Toolbar for Events List
 * <p>
 * Features:
 * - Material 3 design with custom styling
 * - Event-specific actions (Edit, Delete, Share, Export)
 * - Smart action validation based on selection
 * - Enhanced animations and haptic feedback
 * - Integration with existing EventsListFragment selection system
 */
public class EventsBottomSelectionToolbar {

    private static final String TAG = "EventsBottomToolbar";

    // 🎯 UX CONSTANTS
    private static final int MAX_ACTIONS = 5;              // Limit actions for clean UI
    private static final int ANIMATION_DURATION = 250;     // Smooth animations
    private static final int ICON_SIZE_DP = 24;           // Larger icons for visibility
    private static final int BUTTON_SIZE_DP = 48;         // Larger touch targets

    // Views
    private final Context mContext;
    private MaterialCardView mToolbarContainer;
    private MaterialButton mCloseSelectionButton;
    private RecyclerView mQuickActionsRecyclerView;
    private TextView mSelectionCountText;

    // Data and state
    private EventsToolbarActionsAdapter mActionsAdapter;
    private EventsSelectionListener mListener;
    private Set<String> mSelectedEventIds;
    private List<LocalEvent> mSelectedEvents;
    private boolean mIsVisible = false;
    private boolean mIsDestroyed = false;

    // Enhanced animations
    private AnimatorSet mShowAnimation;
    private AnimatorSet mHideAnimation;

    public EventsBottomSelectionToolbar(@NonNull Context context) {
        mContext = context;
        initializeViews();
        setupListeners();
    }

    // ==================== EVENT-SPECIFIC ACTIONS ====================

    /**
     * 🎯 Event-specific action types
     */
    public enum EventAction {
//        EDIT("Modifica", R.drawable.ic_rounded_edit_24, ActionType.PRIMARY), // simple click
        DELETE("Elimina", R.drawable.ic_rounded_delete_24, ActionType.DESTRUCTIVE),
        SHARE("Condividi", R.drawable.ic_rounded_share_24, ActionType.SECONDARY),
//        EXPORT("Esporta", R.drawable.ic_rounded_save_24, ActionType.SECONDARY),
        COPY("Copia", R.drawable.ic_rounded_content_copy_24, ActionType.SECONDARY),
//        DUPLICATE("Duplica", R.drawable.ic_rounded_content_copy_24, ActionType.SECONDARY),
        ADD_TO_CALENDAR("Calendario", R.drawable.ic_rounded_calendar_add_on_24, ActionType.SECONDARY);

        private final String label;
        private final int iconRes;
        private final ActionType type;

        EventAction(String label, int iconRes, ActionType type) {
            this.label = label;
            this.iconRes = iconRes;
            this.type = type;
        }

        public String getLabel() { return label; }
        public int getIconRes() { return iconRes; }
        public ActionType getType() { return type; }
    }

    public enum ActionType {
        PRIMARY, SECONDARY, DESTRUCTIVE
    }

    // ==================== SMART ACTION VALIDATION ====================

    /**
     * 🧠 Smart Action Validation for Events
     * Determines which actions are appropriate based on selected events
     */
    private static class EventsActionValidator {

        /**
         * 🎯 Get valid actions for current selection
         */
        public static List<EventAction> getValidActionsForSelection(
                Set<String> selectedEventIds,
                List<LocalEvent> selectedEvents) {

            if (selectedEventIds == null || selectedEventIds.isEmpty()) {
                return new ArrayList<>();
            }

            List<EventAction> validActions = new ArrayList<>();
            int selectionCount = selectedEventIds.size();
            boolean hasEditableEvents = hasEditableEvents(selectedEvents);
            boolean hasExportableEvents = hasExportableEvents(selectedEvents);

            Log.d(TAG, "Validating actions for " + selectionCount + " events");

            // 📝 EDIT - Only for single selection and editable events
//            if (selectionCount == 1 && hasEditableEvents) {
//                validActions.add(EventAction.EDIT);
//            }

            // 🗑️ DELETE - Always available (with confirmation)
            validActions.add(EventAction.DELETE);

            // 📤 SHARE - Always available
            validActions.add(EventAction.SHARE);

//            // 💾 EXPORT - Only if events are exportable
//            if (hasExportableEvents) {
//                validActions.add(EventAction.EXPORT);
//            }

            // 📋 COPY - Only for single selection
            if (selectionCount == 1) {
                validActions.add(EventAction.COPY);
            }

//            // 📊 DUPLICATE - Only for single selection
//            if (selectionCount == 1) {
//                validActions.add(EventAction.DUPLICATE);
//            }

            // 📅 ADD_TO_CALENDAR - Limited to reasonable selection size
            if (selectionCount <= 5) {
                validActions.add(EventAction.ADD_TO_CALENDAR);
            }

            // Apply MAX_ACTIONS limit with smart prioritization
            if (validActions.size() > MAX_ACTIONS) {
                validActions = prioritizeEventActions(validActions, selectionCount);
            }

            Log.d(TAG, "Valid actions: " + validActions.size() + " for " + selectionCount + " events");
            return validActions;
        }

        /**
         * 🎯 Prioritize actions when exceeding MAX_ACTIONS
         */
        private static List<EventAction> prioritizeEventActions(List<EventAction> actions, int selectionCount) {
            // Priority order based on user needs and frequency
            List<EventAction> priorityOrder = Arrays.asList(
                    EventAction.DELETE,           // Highest - common action
//                    EventAction.EDIT,            // High - single selection
                    EventAction.SHARE,           // High - always useful
//                    EventAction.EXPORT,          // Medium - batch operations
                    EventAction.COPY,            // Medium - single selection
//                    EventAction.DUPLICATE,       // Lower - single selection
                    EventAction.ADD_TO_CALENDAR  // Lower - occasional use
            );

            List<EventAction> prioritized = new ArrayList<>();

            // Add actions in priority order
            for (EventAction priority : priorityOrder) {
                if (actions.contains(priority) && prioritized.size() < MAX_ACTIONS) {
                    prioritized.add(priority);
                }
            }

            return prioritized;
        }

        /**
         * 📝 Check if selection contains editable events
         */
        private static boolean hasEditableEvents(List<LocalEvent> events) {
            if (events == null || events.isEmpty()) return false;

            // All events are editable in this implementation
            // Could be enhanced to check for read-only events
            return true;
        }

        /**
         * 💾 Check if selection contains exportable events
         */
        private static boolean hasExportableEvents(List<LocalEvent> events) {
            if (events == null || events.isEmpty()) return false;

            // All events are exportable in this implementation
            // Could be enhanced to check for export restrictions
            return true;
        }
    }

    // ==================== INITIALIZATION ====================
//
//    /**
//     * 🎨 Initialize toolbar views with Material 3 styling
//     */
//    private void initializeViews() {
//        LayoutInflater inflater = LayoutInflater.from(mContext);
//        mToolbarContainer = (MaterialCardView) inflater.inflate(
//                R.layout.events_bottom_selection_toolbar, null
//        );
//
//        // Get view references
//        mCloseSelectionButton = mToolbarContainer.findViewById(R.id.btn_close_selection);
//        mQuickActionsRecyclerView = mToolbarContainer.findViewById(R.id.rv_quick_actions);
//        mSelectionCountText = mToolbarContainer.findViewById(R.id.tv_selection_count);
//
//        // Apply custom styling
//        applyCustomStyling();
//
//        // Setup RecyclerView
//        setupRecyclerView();
//
//        // Initially hide
//        mToolbarContainer.setVisibility(View.GONE);
//        mToolbarContainer.setTranslationY(dpToPx(80));
//        mToolbarContainer.setAlpha(0f);
//
//        Log.d(TAG, "Events bottom selection toolbar initialized");
//    }

    /**
     * 🎨 FIXED: Initialize toolbar views with EXPLICIT sizing
     */
    private void initializeViews() {
        LayoutInflater inflater = LayoutInflater.from(mContext);

        // ✅ CRITICAL FIX: Create with explicit LayoutParams
        mToolbarContainer = new MaterialCardView(mContext);

        // ✅ FIXED: Set explicit dimensions
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,  // Width: //full width
                ViewGroup.LayoutParams.WRAP_CONTENT   // Height: wrap content
        );
        cardParams.gravity = Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL;
        cardParams.setMargins(
                dpToPx(16), // left
                0,          // top
                dpToPx(16), // right
                dpToPx(16)  // bottom
        );
        mToolbarContainer.setLayoutParams(cardParams);

        // ✅ Apply styling to the card
        applyCardStyling();

        // ✅ Inflate and add content to the card
        View contentView = inflater.inflate(R.layout.bottom_selection_toolbar, mToolbarContainer, false);
        mToolbarContainer.addView(contentView);

        // ✅ Get view references from content
        mQuickActionsRecyclerView = contentView.findViewById(R.id.rv_quick_actions);

        // Setup RecyclerView
        setupRecyclerView();

        // Initially hide
        mToolbarContainer.setVisibility(View.GONE);
        mToolbarContainer.setTranslationY(dpToPx(80));
        mToolbarContainer.setAlpha(0f);

        Log.d(TAG, "✅ Events bottom selection toolbar initialized with explicit sizing");
    }

    /**
     * 🎨 Apply custom styling using floatingMenu attributes
     */
    private void applyCustomStyling() {
        // Apply custom background colors
        mToolbarContainer.setCardBackgroundColor(
                Library.getColorByThemeAttr(mContext, R.attr.floatingMenuBackground)
        );

        // Enhanced elevation and corner radius
        mToolbarContainer.setCardElevation(dpToPx(12));
        mToolbarContainer.setRadius(dpToPx(20));

        // Subtle stroke
        mToolbarContainer.setStrokeWidth(dpToPx(1));
        mToolbarContainer.setStrokeColor(
                ColorStateList.valueOf(Library.getColorByThemeAttr(mContext, R.attr.floatingMenuOnBackground))
        );

        // Style close button
        if (mCloseSelectionButton != null) {
            mCloseSelectionButton.setIconTint(
                    ColorStateList.valueOf(Library.getColorByThemeAttr(mContext, R.attr.floatingMenuOnBackground))
            );
            mCloseSelectionButton.setBackgroundTintList(
                    ColorStateList.valueOf(Color.TRANSPARENT)
            );
            mCloseSelectionButton.setRippleColor(
                    ColorStateList.valueOf(Library.getColorByThemeAttr(mContext, R.attr.floatingMenuSelected))
            );
        }

        // Style selection count text
        if (mSelectionCountText != null) {
            mSelectionCountText.setTextColor(
                    Library.getColorByThemeAttr(mContext, R.attr.floatingMenuOnBackground)
            );
        }
    }

    /**
     * 🎨 Apply card styling programmatically
     */
    private void applyCardStyling() {
        if (mToolbarContainer == null) return;

        // Background color
        mToolbarContainer.setCardBackgroundColor(
                Library.getColorByThemeAttr(mContext, R.attr.floatingMenuBackground)
        );

        // Elevation and corners
        mToolbarContainer.setCardElevation(dpToPx(12));
        mToolbarContainer.setRadius(dpToPx(20));

        // Stroke
        mToolbarContainer.setStrokeWidth(dpToPx(1));
        mToolbarContainer.setStrokeColor(
                Library.getColorByThemeAttr(mContext, R.attr.floatingMenuOnBackground)
        );

        // ✅ CRITICAL: Set maximum height to prevent expansion
        mToolbarContainer.setMaxCardElevation(dpToPx(16));

        Log.d(TAG, "✅ Card styling applied");
    }

    /**
     * 📋 Setup RecyclerView for action buttons
     */
    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                mContext, LinearLayoutManager.HORIZONTAL, false
        );
        layoutManager.setInitialPrefetchItemCount(MAX_ACTIONS);
        mQuickActionsRecyclerView.setLayoutManager(layoutManager);

        mActionsAdapter = new EventsToolbarActionsAdapter(mContext);
        mQuickActionsRecyclerView.setAdapter(mActionsAdapter);

        // Performance optimizations
        mQuickActionsRecyclerView.setHasFixedSize(false);  //true
        mQuickActionsRecyclerView.setItemViewCacheSize(MAX_ACTIONS);
        mQuickActionsRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mQuickActionsRecyclerView.setNestedScrollingEnabled(false);
    }

    /**
     * 👆 Setup click listeners
     */
    private void setupListeners() {
//        // Close button
//        mCloseSelectionButton.setOnClickListener(v -> {
//            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
//            animateButtonPress(v);
//
//            if (mListener != null) {
//                mListener.onSelectionModeChanged(false, 0);
//            }
//            hide();
//        });

        // Setup adapter click handling
        mActionsAdapter.setOnActionClickListener(this::onActionClicked);
    }

    // ==================== ACTION HANDLING ====================

    /**
     * 🎯 Handle action clicks with validation
     */
    private void onActionClicked(EventAction action) {
        Log.d(TAG, "Action clicked: " + action + " for " +
                (mSelectedEventIds != null ? mSelectedEventIds.size() : 0) + " events");

        if (mSelectedEventIds == null || mSelectedEventIds.isEmpty()) {
            Log.w(TAG, "No events selected for action: " + action);
            return;
        }

        // Haptic feedback
        if (mToolbarContainer != null) {
            mToolbarContainer.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }

        // Delegate to listener
        if (mListener != null) {
            mListener.onEventActionSelected(action, mSelectedEventIds, mSelectedEvents);
        }

        // Hide toolbar after destructive actions
        if (action.getType() == ActionType.DESTRUCTIVE) {
            hide();
        }
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * 🚀 Show toolbar with event selection
     */
    public void show(@NonNull ViewGroup container,
                     @NonNull Set<String> selectedEventIds,
                     @NonNull List<LocalEvent> selectedEvents,
                     @NonNull EventsSelectionListener listener) {

        if (mIsDestroyed) {
            Log.w(TAG, "Cannot show destroyed toolbar");
            return;
        }

        mSelectedEventIds = selectedEventIds;
        mSelectedEvents = selectedEvents;
        mListener = listener;

        // Update UI content
        updateSelectionDisplay();
        updateAvailableActions();

        // ✅ CRITICAL: Set layout params based on container type BEFORE adding
        ViewGroup.LayoutParams layoutParams = createOptimalLayoutParams(container);
        mToolbarContainer.setLayoutParams(layoutParams);

        // Add to container if not already added
            container.addView(mToolbarContainer);

        // ✅ CRITICAL: Force layout to ensure proper sizing
        mToolbarContainer.requestLayout();
        container.requestLayout();

        // ✅ DEBUG: Log container and toolbar info
        debugContainerInfo(container);

        // Show with animation
        animateIn();
        mIsVisible = true;

        Log.d(TAG, "Events toolbar shown for " + selectedEventIds.size() + " selected events");
    }


    /**
     * 🔧 CRITICAL: Create optimal layout parameters for any container type
     */
    private ViewGroup.LayoutParams createOptimalLayoutParams(ViewGroup container) {
        ViewGroup.LayoutParams layoutParams;

        String containerType = container.getClass().getSimpleName();
        Log.d(TAG, "Creating layout params for container type: " + containerType);

        if (container instanceof FrameLayout) {
            // ✅ FrameLayout: Use gravity bottom with explicit sizing
            FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,  //MATCH_PARENT
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
//            frameParams.gravity = Gravity.BOTTOM;
            frameParams.gravity = Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL;
            frameParams.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(16));
            layoutParams = frameParams;

        } else if (container instanceof CoordinatorLayout) {
            // ✅ CoordinatorLayout: Use proper behavior
            CoordinatorLayout.LayoutParams coordParams = new CoordinatorLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, // MATCH_PARENT
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
//            coordParams.gravity = Gravity.BOTTOM;
            coordParams.gravity = Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL;
            coordParams.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(16));
            layoutParams = coordParams;

        } else if (container instanceof LinearLayout) {
            // ✅ LinearLayout: Force to bottom
            LinearLayout linearLayout = (LinearLayout) container;
            if (linearLayout.getOrientation() == LinearLayout.VERTICAL) {
                LinearLayout.LayoutParams linearParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, // MATCH_PARENT
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
//                linearParams.gravity = Gravity.BOTTOM;
                linearParams.gravity = Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL;
                linearParams.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(16));
                layoutParams = linearParams;
            } else {
                // Horizontal LinearLayout - use standard params
                layoutParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, // MATCH_PARENT
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }

        } else if (container instanceof RelativeLayout) {
            // ✅ RelativeLayout: Use alignParentBottom
            RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            relativeParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            relativeParams.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(16));
            layoutParams = relativeParams;

        } else {
            // ✅ Generic ViewGroup: Use standard params with explicit size
            layoutParams = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        Log.d(TAG, "✅ Created " + layoutParams.getClass().getSimpleName() +
                " with width=" + layoutParams.width + ", height=" + layoutParams.height);

        return layoutParams;
    }

    /**
     * 🔧 DEBUG: Container and toolbar information
     */
    private void debugContainerInfo(ViewGroup container) {
        Log.d(TAG, "=== CONTAINER & TOOLBAR DEBUG ===");

        // Container info
        Log.d(TAG, "Container: " + container.getClass().getSimpleName());
        Log.d(TAG, "Container size: " + container.getWidth() + "x" + container.getHeight());
        Log.d(TAG, "Container children: " + container.getChildCount());

        // Toolbar info
        if (mToolbarContainer != null) {
            ViewGroup.LayoutParams params = mToolbarContainer.getLayoutParams();
            if (params != null) {
                Log.d(TAG, "Toolbar LayoutParams: " + params.getClass().getSimpleName());
                Log.d(TAG, "Toolbar Layout size: " + params.width + "x" + params.height);

                if (params instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams frameParams = (FrameLayout.LayoutParams) params;
                    Log.d(TAG, "FrameLayout gravity: " + frameParams.gravity);
                }
            }

            Log.d(TAG, "Toolbar actual size: " + mToolbarContainer.getWidth() + "x" + mToolbarContainer.getHeight());
            Log.d(TAG, "Toolbar visibility: " + mToolbarContainer.getVisibility());
        }

        Log.d(TAG, "=== END DEBUG ===");
    }





    /**
     * 🔄 Hide toolbar
     */
    public void hide() {
        if (mIsDestroyed || !mIsVisible) {
            return;
        }

        animateOut(() -> {
            // Remove from container after animation
            if (mToolbarContainer.getParent() instanceof ViewGroup) {
                ((ViewGroup) mToolbarContainer.getParent()).removeView(mToolbarContainer);
            }

            mIsVisible = false;
            mSelectedEventIds = null;
            mSelectedEvents = null;
            mListener = null;
        });
    }

    /**
     * 🔄 Update selection display
     */
    private void updateSelectionDisplay() {
        if (mSelectionCountText == null || mSelectedEventIds == null) return;

        int count = mSelectedEventIds.size();
        String displayText = count == 1 ? "1 evento" : count + " eventi";
        mSelectionCountText.setText(displayText);
    }

    /**
     * 🔄 Update available actions
     */
    private void updateAvailableActions() {
        if (mActionsAdapter == null) return;

        List<EventAction> actions = EventsActionValidator.getValidActionsForSelection(
                mSelectedEventIds, mSelectedEvents
        );
        mActionsAdapter.updateActions(actions);

        Log.v(TAG, "Updated to " + actions.size() + " validated actions");
    }

    /**
     * 🔄 Update selection from external source
     */
    public void updateSelection(@NonNull Set<String> selectedEventIds,
                                @NonNull List<LocalEvent> selectedEvents) {
        if (mIsDestroyed) return;

        mSelectedEventIds = selectedEventIds;
        mSelectedEvents = selectedEvents;
        updateSelectionDisplay();
        updateAvailableActions();
    }

    /**
     * ❓ Check if toolbar is visible
     */
    public boolean isVisible() {
        return mIsVisible && !mIsDestroyed;
    }

    // ==================== ANIMATIONS ====================

    /**
     * 🎬 Entrance animation
     */
    private void animateIn() {
        if (mToolbarContainer == null) return;

        cancelRunningAnimations();

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
     * 🎬 Exit animation
     */
    private void animateOut(Runnable onComplete) {
        if (mToolbarContainer == null) return;

        cancelRunningAnimations();

        mHideAnimation = new AnimatorSet();

        ObjectAnimator translateY = ObjectAnimator.ofFloat(mToolbarContainer, "translationY", 0f, dpToPx(60));
        ObjectAnimator alpha = ObjectAnimator.ofFloat(mToolbarContainer, "alpha", 1f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mToolbarContainer, "scaleX", 1f, 0.9f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mToolbarContainer, "scaleY", 1f, 0.9f);

        translateY.setInterpolator(new AccelerateInterpolator(1.2f));
        alpha.setInterpolator(new AccelerateInterpolator());
        scaleX.setInterpolator(new AccelerateInterpolator());
        scaleY.setInterpolator(new AccelerateInterpolator());

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
     * 🎬 Button press animation
     */
    private void animateButtonPress(View button) {
        button.animate()
                .scaleX(0.90f)
                .scaleY(0.90f)
                .setDuration(100)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    button.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .setInterpolator(new OvershootInterpolator(1.3f))
                            .start();
                })
                .start();
    }

    /**
     * 🛑 Cancel running animations
     */
    private void cancelRunningAnimations() {
        if (mShowAnimation != null && mShowAnimation.isRunning()) {
            mShowAnimation.cancel();
        }
        if (mHideAnimation != null && mHideAnimation.isRunning()) {
            mHideAnimation.cancel();
        }
    }

    // ==================== CLEANUP ====================

    /**
     * 🧹 Destroy toolbar and cleanup resources
     */
    public void destroy() {
        if (mIsDestroyed) return;

        Log.d(TAG, "🧹 Destroying EventsBottomSelectionToolbar...");

        hideImmediately();
        cancelRunningAnimations();

        if (mActionsAdapter != null) {
            mActionsAdapter.clearData();
            mActionsAdapter = null;
        }

        if (mQuickActionsRecyclerView != null) {
            mQuickActionsRecyclerView.setAdapter(null);
            mQuickActionsRecyclerView = null;
        }

        mCloseSelectionButton = null;
        mSelectionCountText = null;
        mToolbarContainer = null;
        mListener = null;
        mSelectedEventIds = null;
        mSelectedEvents = null;
        mIsDestroyed = true;

        Log.d(TAG, "✅ EventsBottomSelectionToolbar destroyed");
    }

    private void hideImmediately() {
        if (mToolbarContainer != null) {
            mToolbarContainer.setVisibility(View.GONE);
            ViewParent parent = mToolbarContainer.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(mToolbarContainer);
            }
        }
        mIsVisible = false;
    }

    // ==================== UTILITY METHODS ====================

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                mContext.getResources().getDisplayMetrics()
        );
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                mContext.getResources().getDisplayMetrics()
        );
    }

    // ==================== INTERFACES ====================

    /**
     * 📋 Listener interface for event selection actions
     */
    public interface EventsSelectionListener {
        /**
         * Called when selection mode should be changed
         */
        void onSelectionModeChanged(boolean isSelectionMode, int selectedCount);

        /**
         * Called when an action is selected for the currently selected events
         */
        void onEventActionSelected(EventAction action, Set<String> selectedEventIds, List<LocalEvent> selectedEvents);
    }
}