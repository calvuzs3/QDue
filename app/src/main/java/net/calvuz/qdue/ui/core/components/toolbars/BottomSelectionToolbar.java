/**
 * 🎨 Enhanced Bottom Selection Toolbar with Smart Validation
 * Modern Material 3 implementation with programmatic approach
 * Based on EventsBottomSelectionToolbar architecture but maintaining complex validation
 */
package net.calvuz.qdue.ui.core.components.toolbars;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.ui.core.common.enums.ToolbarAction;
import net.calvuz.qdue.ui.core.common.interfaces.DayLongClickListener;
import net.calvuz.qdue.ui.core.common.utils.Library;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 🎨 Enhanced Bottom Selection Toolbar with Smart Validation
 * <p>
 * Features:
 * - Programmatic Material 3 design with custom styling
 * - Smart action validation based on work schedule
 * - Enhanced animations and haptic feedback
 * - Robust layout parameter handling for any container
 * - Business rules enforcement
 * - Maximum 5 actions limit for clean UI
 */
public class BottomSelectionToolbar {

    private static final String TAG = "BottomSelectionToolbar";

    // 🎯 UX CONSTANTS
    private static final int MAX_ACTIONS = 5;              // Limit actions for clean UI
    private static final int ANIMATION_DURATION = 250;     // Smooth animations
    public static final int ICON_SIZE_DP = 32;           // Larger icons for visibility
    public static final int BUTTON_SIZE_DP = 32;         // Larger touch targets

    // Views
    private final Context mContext;
    private MaterialCardView mToolbarContainer;
    private RecyclerView mQuickActionsRecyclerView;

    // Data and state
    private BottomSelectionToolbarAdapter mActionsAdapter;
    private DayLongClickListener mListener;
    private Set<LocalDate> mSelectedDates = new HashSet<>();
    private boolean mIsVisible = false;
    private boolean mIsDestroyed = false;
    private HalfTeam mCurrentUserTeam;
    private Map<LocalDate, Day> mDayMap = new HashMap<>();

    // Enhanced animations
    private AnimatorSet mShowAnimation;
    private AnimatorSet mHideAnimation;

    public BottomSelectionToolbar(@NonNull Context context, @Nullable HalfTeam userTeam) {
        mContext = context;
        mCurrentUserTeam = userTeam;
        initializeViews();
        setupListeners();
    }

    // ==================== SMART ACTION VALIDATION SYSTEM ====================

    /**
     * 🧠 Smart Action Validation System
     * Validates which actions are appropriate based on selected dates and user work schedule
     */
    private static class SmartActionValidator {

        // Business rules constants
        private static final int MIN_WORK_DAYS_FOR_SICK_LEAVE = 1;
        private static final int MAX_CONSECUTIVE_VACATION_DAYS = 30;

        /**
         * 🎯 Validate and filter actions based on selected dates and user context
         */
        public static List<ToolbarAction> getValidActionsForSelection(
                Set<LocalDate> selectedDates,
                HalfTeam userTeam,
                Map<LocalDate, Day> dayMap) {

            if (selectedDates == null || selectedDates.isEmpty()) {
                return new ArrayList<>();
            }

            ValidationContext context = analyzeSelection(selectedDates, userTeam, dayMap);

            Log.d(TAG, "Validating actions for " + selectedDates.size() + " dates: " +
                    "workDays=" + context.workDays + ", offDays=" + context.offDays +
                    ", pastDays=" + context.pastDays);

            List<ToolbarAction> validActions = new ArrayList<>();

            // Validate each action type
            if (isValidForFerie(context)) {
                validActions.add(ToolbarAction.VACATION);
            }

            if (isValidForMalattia(context)) {
                validActions.add(ToolbarAction.SICK_LEAVE);
            }

            if (isValidForPermesso(context)) {
                validActions.add(ToolbarAction.PERSONAL_LEAVE);
            }

            if (isValidForLegge104(context)) {
                validActions.add(ToolbarAction.SPECIAL_LEAVE);
            }

            if (isValidForStraordinario(context)) {
                validActions.add(ToolbarAction.OVERTIME);
            }

            // ADD_EVENT è sempre valido (fallback)
            validActions.add(ToolbarAction.ADD_EVENT);

            // Apply MAX_ACTIONS limit with smart prioritization
            if (validActions.size() > MAX_ACTIONS) {
                validActions = prioritizeActions(validActions, selectedDates.size());
            }

            Log.d(TAG, "Valid actions: " + validActions.size() + " for " + selectedDates.size() + " dates");
            return validActions;
        }

        /**
         * 📊 Analyze selection to create validation context
         */
        private static ValidationContext analyzeSelection(Set<LocalDate> selectedDates,
                                                          HalfTeam userTeam,
                                                          Map<LocalDate, Day> dayMap) {
            ValidationContext context = new ValidationContext();

            for (LocalDate date : selectedDates) {
                // Basic date categorization
                if (isPastDate(date)) {
                    context.pastDays++;
                    continue; // Skip work analysis for past dates
                }

                if (isWeekend(date)) {
                    context.weekendDays++;
                }

                // Work schedule analysis
                Day day = dayMap != null ? dayMap.get(date) : null;
                if (day != null) {
                    if (hasExistingEvents(day)) {
                        context.hasExistingEvents = true;
                    }

                    if (userTeam != null) {
                        if (isUserWorkingOnDay(day, userTeam)) {
                            context.workDays++;
                        } else {
                            context.offDays++;
                        }
                    } else {
                        // Fallback: assume traditional work schedule
                        if (!isWeekend(date)) {
                            context.workDays++;
                        } else {
                            context.offDays++;
                        }
                    }
                } else {
                    // No day data available - use traditional schedule
                    if (!isWeekend(date)) {
                        context.workDays++;
                    } else {
                        context.offDays++;
                    }
                }
            }

            context.totalDays = selectedDates.size();
            context.futureDays = context.totalDays - context.pastDays;

            return context;
        }

        /**
         * 🏖️ VACATION - Valid for work days only
         */
        private static boolean isValidForFerie(ValidationContext context) {
            if (context.workDays == 0) {
                Log.d(TAG, "VACATION invalid: No work days in selection");
                return false;
            }

            if (context.pastDays > 0) {
                Log.d(TAG, "VACATION invalid: Contains past days");
                return false;
            }

            if (context.workDays > MAX_CONSECUTIVE_VACATION_DAYS) {
                Log.d(TAG, "VACATION invalid: Exceeds maximum consecutive days");
                return false;
            }

            return true;
        }

        /**
         * 🏥 SICK_LEAVE - Valid for work days, special rules for timing
         */
        private static boolean isValidForMalattia(ValidationContext context) {
            if (context.workDays < MIN_WORK_DAYS_FOR_SICK_LEAVE) {
                Log.d(TAG, "SICK_LEAVE invalid: Need at least " + MIN_WORK_DAYS_FOR_SICK_LEAVE + " work day(s)");
                return false;
            }

            return true;
        }

        /**
         * ⏰ PERSONAL_LEAVE - Valid for work days, more flexible than ferie
         */
        private static boolean isValidForPermesso(ValidationContext context) {
            if (context.workDays == 0) {
                Log.d(TAG, "PERSONAL_LEAVE invalid: No work days in selection");
                return false;
            }

            if (context.pastDays > 0) {
                Log.d(TAG, "PERSONAL_LEAVE invalid: Contains past days (limited retroactive allowed)");
                return false;
            }

            return true;
        }

        /**
         * ♿ SPECIAL_LEAVE - Special protected leave, very flexible
         */
        private static boolean isValidForLegge104(ValidationContext context) {
            if (context.workDays == 0) {
                Log.d(TAG, "SPECIAL_LEAVE invalid: No work days in selection");
                return false;
            }

            return true;
        }

        /**
         * ⚡ OVERTIME - Valid for off days or weekends
         */
        private static boolean isValidForStraordinario(ValidationContext context) {
            if (context.offDays == 0 && context.weekendDays == 0) {
                Log.d(TAG, "OVERTIME invalid: No off-days or weekends in selection");
                return false;
            }

            if (context.pastDays > 0) {
                Log.d(TAG, "OVERTIME invalid: Contains past days");
                return false;
            }

            if (context.totalDays > 3) {
                Log.d(TAG, "OVERTIME warning: Large selection, might not be appropriate");
            }

            return true;
        }

        /**
         * 🎯 Prioritize actions when exceeding MAX_ACTIONS
         */
        private static List<ToolbarAction> prioritizeActions(List<ToolbarAction> actions, int selectionCount) {
            List<ToolbarAction> priorityOrder = Arrays.asList(
                    ToolbarAction.VACATION,         // High priority - most common
                    ToolbarAction.SICK_LEAVE,      // High priority - quite common
                    ToolbarAction.PERSONAL_LEAVE,      // Medium priority - common
                    ToolbarAction.SPECIAL_LEAVE,     // Medium priority - protected
                    ToolbarAction.ADD_EVENT,     // Medium priority - fallback
                    ToolbarAction.OVERTIME  // Lower priority - occasional
            );

            List<ToolbarAction> prioritized = new ArrayList<>();

            for (ToolbarAction priority : priorityOrder) {
                if (actions.contains(priority) && prioritized.size() < MAX_ACTIONS) {
                    prioritized.add(priority);
                }
            }

            return prioritized;
        }

        /**
         * 👤 Check if user is working on a specific day
         */
        private static boolean isUserWorkingOnDay(Day day, HalfTeam userTeam) {
            if (day == null || userTeam == null) {
                return false;
            }

            for (Shift shift : day.getShifts()) {
                for (HalfTeam team : shift.getHalfTeams()) {
                    if (team.isSameTeamAs(userTeam)) {
                        return true;
                    }
                }
            }

            return false;
        }

        /**
         * 📅 Check if day has existing events
         */
        private static boolean hasExistingEvents(Day day) {
            return day != null; // Placeholder - implement actual event checking
        }

        /**
         * 📅 Date utility methods
         */
        private static boolean isPastDate(LocalDate date) {
            return date.isBefore(LocalDate.now());
        }

        private static boolean isWeekend(LocalDate date) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        }

        /**
         * 📋 Validation context data structure
         */
        private static class ValidationContext {
            int totalDays = 0;
            int workDays = 0;
            int offDays = 0;
            int pastDays = 0;
            int futureDays = 0;
            int weekendDays = 0;
            boolean hasExistingEvents = false;
        }
    }

    // ==================== USER FEEDBACK SYSTEM ====================

    /**
     * 💬 User feedback for invalid actions
     */
    private static class ActionValidationFeedback {

        /**
         * 🗨️ Show validation feedback to user
         */
        public static void showValidationMessage(Context context, ToolbarAction action,
                                                 Set<LocalDate> selectedDates, String reason) {
            String message = generateFeedbackMessage(action, selectedDates, reason);
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            Log.d(TAG, "Validation feedback shown: " + message);
        }

        /**
         * 📝 Generate user-friendly feedback message
         */
        private static String generateFeedbackMessage(ToolbarAction action,
                                                      Set<LocalDate> selectedDates, String reason) {
            int dayCount = selectedDates.size();
            String dayText = dayCount == 1 ? "giorno" : "giorni";

            switch (action) {
                case VACATION:
                    if (reason.contains("No work days")) {
                        return "❌ Le ferie possono essere richieste solo per giorni lavorativi";
                    } else if (reason.contains("past days")) {
                        return "❌ Non è possibile richiedere ferie per giorni passati";
                    }
                    return "❌ Ferie non disponibili per " + dayCount + " " + dayText + " selezionati";

                case SICK_LEAVE:
                    if (reason.contains("work day")) {
                        return "❌ La malattia deve includere almeno un giorno lavorativo";
                    }
                    return "❌ Malattia non disponibile per la selezione corrente";

                case PERSONAL_LEAVE:
                    if (reason.contains("No work days")) {
                        return "❌ I permessi possono essere richiesti solo per giorni lavorativi";
                    } else if (reason.contains("past days")) {
                        return "❌ Non è possibile richiedere permessi per giorni passati";
                    }
                    return "❌ Permesso non disponibile per " + dayCount + " " + dayText + " selezionati";

                case OVERTIME:
                    if (reason.contains("off-days")) {
                        return "❌ Lo straordinario è disponibile solo per giorni non lavorativi";
                    } else if (reason.contains("past days")) {
                        return "❌ Non è possibile registrare straordinario per giorni passati";
                    }
                    return "❌ Straordinario non disponibile per la selezione corrente";

                default:
                    return "❌ Azione non disponibile per " + dayCount + " " + dayText + " selezionati";
            }
        }
    }

    // ==================== PROGRAMMATIC INITIALIZATION ====================

    /**
     * 🎨 PROGRAMMATIC: Initialize toolbar views with explicit sizing
     */
    private void initializeViews() {
        // ✅ Try to use layout XML like Events toolbar
        try {
            LayoutInflater inflater = LayoutInflater.from(mContext);

            // Create MaterialCardView container
            mToolbarContainer = new MaterialCardView(mContext);

            // ✅ Try to inflate enhanced layout XML
            View contentView;
            try {
//                contentView = inflater.inflate(R.layout.enhanced_bottom_selection_toolbar, null);
                contentView = inflater.inflate(R.layout.bottom_selection_toolbar, null);
                Log.d(TAG, "✅ Using bottom_selection_toolbar.xml");
            } catch (Exception e) {
                // Fallback to events layout
                Log.e(TAG, "Enhanced layout not found, using events layout");
                return;
            }

            // Add content to card
            mToolbarContainer.addView(contentView);

            // Get view references
            mQuickActionsRecyclerView = contentView.findViewById(R.id.rv_quick_actions);

            if (mQuickActionsRecyclerView == null) {
                Log.e(TAG, "❌ rv_quick_actions not found in layout!");
                createProgrammaticFallback();
                return;
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Layout inflation failed, using programmatic fallback: " + e.getMessage());
            createProgrammaticFallback();
            return;
        }

        // ✅ Apply styling to the inflated card
        applyCardStyling();

        // ✅ Setup RecyclerView
        setupRecyclerView();

        // Initially hide
        mToolbarContainer.setVisibility(View.GONE);
        mToolbarContainer.setTranslationY(dpToPx(80));
        mToolbarContainer.setAlpha(0f);

        Log.d(TAG, "✅ Enhanced toolbar initialized with XML layout approach");
    }

    /**
     * 🔧 FALLBACK: Create programmatic layout if XML fails
     */
    private void createProgrammaticFallback() {
        Log.d(TAG, "Creating programmatic fallback layout");

        mToolbarContainer = new MaterialCardView(mContext);

        // Create content layout
        LinearLayout mainLayout = new LinearLayout(mContext);
        mainLayout.setOrientation(LinearLayout.HORIZONTAL);
        mainLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        mainLayout.setGravity(Gravity.CENTER_VERTICAL);

        // Create RecyclerView
        mQuickActionsRecyclerView = new RecyclerView(mContext);
        LinearLayout.LayoutParams recyclerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dpToPx(48)
        );
        mQuickActionsRecyclerView.setLayoutParams(recyclerParams);

        // Add RecyclerView to main layout
        mainLayout.addView(mQuickActionsRecyclerView);

        // Add main layout to card
        mToolbarContainer.addView(mainLayout);
    }

    /**
     * 🔧 ENHANCED: Card styling to match Events size
     */
    private void applyCardStyling() {
        if (mToolbarContainer == null) return;

        // Background color
        mToolbarContainer.setCardBackgroundColor(
                Library.getColorByThemeAttr(mContext, R.attr.floatingMenuBackground)
        );

        // 🔧 Match Events toolbar elevation and styling
        mToolbarContainer.setCardElevation(dpToPx(6));
        mToolbarContainer.setRadius(dpToPx(24));

        // Stroke
        mToolbarContainer.setStrokeWidth(dpToPx(1));
        mToolbarContainer.setStrokeColor(
                Library.getColorByThemeAttr(mContext, R.attr.floatingMenuOnBackground)
        );

        // 🔧 CRITICAL: High Z-index to ensure visibility above bottom nav
        mToolbarContainer.setTranslationZ(dpToPx(24)); // Higher than bottom nav
        mToolbarContainer.setMaxCardElevation(dpToPx(24));

        Log.d(TAG, "✅ Card styling applied to match Events toolbar");
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

        mActionsAdapter = new BottomSelectionToolbarAdapter(mContext);
        mQuickActionsRecyclerView.setAdapter(mActionsAdapter);

        // Performance optimizations
        mQuickActionsRecyclerView.setHasFixedSize(false);
        mQuickActionsRecyclerView.setItemViewCacheSize(MAX_ACTIONS);
        mQuickActionsRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mQuickActionsRecyclerView.setNestedScrollingEnabled(false);
    }

    /**
     * 👆 Setup click listeners
     */
    private void setupListeners() {
        // Setup adapter click handling
        mActionsAdapter.setOnActionClickListener(this::onEnhancedActionClicked);
    }

    // ==================== ENHANCED LAYOUT HANDLING ====================

    /**
     * 🔧 CRITICAL: Create optimal layout parameters for any container type
     */
    private ViewGroup.LayoutParams createOptimalLayoutParams(ViewGroup container) {
        ViewGroup.LayoutParams layoutParams;

        String containerType = container.getClass().getSimpleName();
        Log.d(TAG, "Creating layout params for container type: " + containerType);

        if (container instanceof FrameLayout) {
            FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            frameParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            frameParams.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(16));
            layoutParams = frameParams;

        } else if (container instanceof CoordinatorLayout) {
            CoordinatorLayout.LayoutParams coordParams = new CoordinatorLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            coordParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            coordParams.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(16));
            layoutParams = coordParams;

        } else if (container instanceof LinearLayout) {
            LinearLayout linearLayout = (LinearLayout) container;
            if (linearLayout.getOrientation() == LinearLayout.VERTICAL) {
                LinearLayout.LayoutParams linearParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                linearParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                linearParams.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(16));
                layoutParams = linearParams;
            } else {
                layoutParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }

        } else if (container instanceof RelativeLayout) {
            RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            relativeParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            relativeParams.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(16));
            layoutParams = relativeParams;

        } else {
            layoutParams = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        Log.d(TAG, "✅ Created " + layoutParams.getClass().getSimpleName() +
                " with width=" + layoutParams.width + ", height=" + layoutParams.height);

        return layoutParams;
    }

    // ==================== ACTION HANDLING ====================

    /**
     * 🎯 Enhanced action click with feedback and validation
     */
    private void onEnhancedActionClicked(ToolbarAction action) {
        Log.d(TAG, "Enhanced action clicked: " + action + " for " + mSelectedDates.size() + " dates");

        if (mSelectedDates.isEmpty()) {
            Log.w(TAG, "No dates selected for action: " + action);
            return;
        }

        // 🧠 SMART VALIDATION: Double-check action validity before execution
        List<ToolbarAction> validActions = SmartActionValidator.getValidActionsForSelection(
                mSelectedDates, mCurrentUserTeam, mDayMap
        );

        if (!validActions.contains(action)) {
            // Action is no longer valid - show feedback
            ActionValidationFeedback.showValidationMessage(
                    mContext, action, mSelectedDates, "Action no longer valid"
            );

            // Refresh available actions
            updateAvailableActions();
            return;
        }

        // Haptic feedback for all action clicks
        if (mToolbarContainer != null) {
            mToolbarContainer.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }

        // Delegate to listener
        if (mListener != null) {
            for (LocalDate date : mSelectedDates) {
                mListener.onToolbarActionSelected(action, null, date);
            }
        }
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * 🚀 Enhanced show method with smart validation setup
     */
    public void show(@NonNull ViewGroup container, @NonNull Set<LocalDate> selectedDates,
                     @NonNull DayLongClickListener listener) {

        if (mIsDestroyed) {
            Log.w(TAG, "Cannot show destroyed toolbar");
            return;
        }

        // 🔧 Setup validation context FIRST
        setupValidationContext(selectedDates);

        mSelectedDates = new HashSet<>(selectedDates);
        mListener = listener;

        // Update available actions with smart validation
        updateAvailableActions();

        // ✅ CRITICAL: Set layout params based on container type BEFORE adding
        ViewGroup.LayoutParams layoutParams = createOptimalLayoutParams(container);
        mToolbarContainer.setLayoutParams(layoutParams);

        // Add to container if not already added
        if (mToolbarContainer.getParent() == null) {
            container.addView(mToolbarContainer);
        }

        // ✅ CRITICAL: Force layout to ensure proper sizing
        mToolbarContainer.requestLayout();
        container.requestLayout();

        // Show with enhanced animation
        animateIn();
        mIsVisible = true;

        Log.d(TAG, "Enhanced toolbar shown for " + selectedDates.size() + " selected dates");
    }

    /**
     * 🔧 Setup validation context before showing
     */
    private void setupValidationContext(Set<LocalDate> selectedDates) {
        for (LocalDate date : selectedDates) {
            if (!mDayMap.containsKey(date)) {
                Day day = loadDayForDate(date);
                if (day != null) {
                    mDayMap.put(date, day);
                }
            }
        }
    }

    /**
     * 🗃️ Load day data for validation (to be implemented)
     */
    private Day loadDayForDate(LocalDate date) {
        // TODO: Implement actual day loading logic
        return null; // Placeholder
    }

    /**
     * 🔄 Hide toolbar
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
            mSelectedDates.clear();
            mListener = null;
        });
    }

    /**
     * 🔄 Update available actions based on smart validation
     */
    private void updateAvailableActions() {
        if (mActionsAdapter == null) return;

        List<ToolbarAction> actions = SmartActionValidator.getValidActionsForSelection(
                mSelectedDates, mCurrentUserTeam, mDayMap
        );
        mActionsAdapter.updateActions(actions);

        Log.v(TAG, "Updated to " + actions.size() + " validated actions for selection");
    }

    /**
     * 🆕 Set user team for validation
     */
    public void setUserTeam(HalfTeam userTeam) {
        mCurrentUserTeam = userTeam;

        if (mIsVisible && !mSelectedDates.isEmpty()) {
            updateAvailableActions();
        }
    }

    /**
     * 🆕 Set day map for validation
     */
    public void setDayMap(Map<LocalDate, Day> dayMap) {
        mDayMap = dayMap != null ? dayMap : new HashMap<>();

        if (mIsVisible && !mSelectedDates.isEmpty()) {
            updateAvailableActions();
        }
    }

    /**
     * 🔄 Update selection from external source with smart validation
     */
    public void updateSelection(@NonNull Set<LocalDate> selectedDates) {
        if (mIsDestroyed) return;

        setupValidationContext(selectedDates);
        mSelectedDates = new HashSet<>(selectedDates);
        updateAvailableActions();

        Log.d(TAG, "Selection updated: " + selectedDates.size() + " dates");
    }

    /**
     * ❓ Check if toolbar is visible
     */
    public boolean isVisible() {
        return mIsVisible && !mIsDestroyed;
    }

    // ==================== ENHANCED ANIMATIONS ====================

    /**
     * 🎬 Enhanced entrance animation
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
     * 🎬 Enhanced exit animation
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

    // ==================== LIFECYCLE METHODS ====================

    /**
     * 🔄 onPause - cleanup temporary state
     */
    public void onPause() {
        if (!checkNotDestroyed("onPause()")) {
            return;
        }

        if (mShowAnimation != null && mShowAnimation.isRunning()) {
            mShowAnimation.pause();
        }

        if (mHideAnimation != null && mHideAnimation.isRunning()) {
            mHideAnimation.pause();
        }

        Log.v(TAG, "BottomSelectionToolbar paused");
    }

    /**
     * 🔄 onResume - resume state
     */
    public void onResume() {
        if (!checkNotDestroyed("onResume()")) {
            return;
        }

        if (mShowAnimation != null && mShowAnimation.isPaused()) {
            mShowAnimation.resume();
        }

        if (mHideAnimation != null && mHideAnimation.isPaused()) {
            mHideAnimation.resume();
        }

        Log.v(TAG, "BottomSelectionToolbar resumed");
    }

    // ==================== DESTROY METHODS ====================

    /**
     * 🧹 Destroy toolbar and cleanup resources
     */
    public void destroy() {
        if (mIsDestroyed) {
            Log.w(TAG, "destroy() called multiple times - already destroyed");
            return;
        }

        Log.d(TAG, "🧹 Starting BottomSelectionToolbar destruction...");

        try {
            // Step 1: Hide toolbar immediately
            hideImmediately();

            // Step 2: Stop and cleanup animations
            cleanupAnimations();

            // Step 3: Cleanup RecyclerView and adapter
            cleanupRecyclerView();

            // Step 4: Clear all data references
            clearAllReferences();

            // Step 5: Cleanup view references
            cleanupViews();

            // Step 6: Mark as destroyed
            mIsDestroyed = true;

            Log.d(TAG, "✅ BottomSelectionToolbar destruction completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error during BottomSelectionToolbar destruction", e);
            mIsDestroyed = true;
        }
    }

    /**
     * 🚀 Immediately hide toolbar without animations
     */
    private void hideImmediately() {
        Log.v(TAG, "Hiding toolbar immediately...");

        try {
            if (mToolbarContainer != null) {
                mToolbarContainer.setVisibility(View.GONE);
                mToolbarContainer.setTranslationY(200f);

                ViewParent parent = mToolbarContainer.getParent();
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(mToolbarContainer);
                }
            }

            mIsVisible = false;
            Log.v(TAG, "✅ Toolbar hidden immediately");

        } catch (Exception e) {
            Log.e(TAG, "Error hiding toolbar immediately", e);
        }
    }

    /**
     * 🎬 Cleanup animations and prevent memory leaks
     */
    private void cleanupAnimations() {
        Log.v(TAG, "Cleaning up animations...");

        try {
            if (mShowAnimation != null) {
                if (mShowAnimation.isRunning()) {
                    mShowAnimation.cancel();
                }
                mShowAnimation.removeAllListeners();
                mShowAnimation = null;
            }

            if (mHideAnimation != null) {
                if (mHideAnimation.isRunning()) {
                    mHideAnimation.cancel();
                }
                mHideAnimation.removeAllListeners();
                mHideAnimation = null;
            }

            if (mToolbarContainer != null) {
                mToolbarContainer.clearAnimation();
                mToolbarContainer.animate().cancel();
            }

            if (mQuickActionsRecyclerView != null) {
                mQuickActionsRecyclerView.clearAnimation();
                mQuickActionsRecyclerView.animate().cancel();
            }

            Log.v(TAG, "✅ Animations cleanup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up animations", e);
        }
    }

    /**
     * 📋 Cleanup RecyclerView and Adapter
     */
    private void cleanupRecyclerView() {
        Log.v(TAG, "Cleaning up RecyclerView and Adapter...");

        try {
            if (mQuickActionsRecyclerView != null) {
                mQuickActionsRecyclerView.stopScroll();
                mQuickActionsRecyclerView.setAdapter(null);
                mQuickActionsRecyclerView.setLayoutManager(null);

                while (mQuickActionsRecyclerView.getItemDecorationCount() > 0) {
                    mQuickActionsRecyclerView.removeItemDecorationAt(0);
                }

                RecyclerView.RecycledViewPool recycledViewPool = mQuickActionsRecyclerView.getRecycledViewPool();
                if (recycledViewPool != null) {
                    recycledViewPool.clear();
                }

                mQuickActionsRecyclerView.clearAnimation();
                mQuickActionsRecyclerView.animate().cancel();
                mQuickActionsRecyclerView = null;
            }

            if (mActionsAdapter != null) {
                mActionsAdapter.clearData();
                mActionsAdapter = null;
            }

            Log.v(TAG, "✅ RecyclerView cleanup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up RecyclerView", e);
            mQuickActionsRecyclerView = null;
            mActionsAdapter = null;
        }
    }

    /**
     * 🔗 Clear all object references
     */
    private void clearAllReferences() {
        Log.v(TAG, "Clearing all references...");

        try {
            mListener = null;

            if (mSelectedDates != null) {
                mSelectedDates.clear();
                mSelectedDates = null;
            }

            mCurrentUserTeam = null;

            if (mDayMap != null) {
                mDayMap.clear();
                mDayMap = null;
            }

            mIsVisible = false;

            Log.v(TAG, "✅ References cleanup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error clearing references", e);
            mListener = null;
            mSelectedDates = null;
            mCurrentUserTeam = null;
            mDayMap = null;
            mIsVisible = false;
        }
    }

    /**
     * 👁️ Cleanup view references
     */
    private void cleanupViews() {
        Log.v(TAG, "Cleaning up view references...");

        try {
            if (mToolbarContainer != null) {
                ViewParent parent = mToolbarContainer.getParent();
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(mToolbarContainer);
                }

                mToolbarContainer.clearAnimation();
                mToolbarContainer.animate().cancel();
                mToolbarContainer = null;
            }

            Log.v(TAG, "✅ Views cleanup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up views", e);
            mToolbarContainer = null;
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * 🛡️ Prevent operations on destroyed toolbar
     */
    private boolean checkNotDestroyed(String operation) {
        if (mIsDestroyed) {
            Log.w(TAG, "Attempted " + operation + " on destroyed BottomSelectionToolbar");
            return false;
        }
        return true;
    }

    /**
     * 🛠️ Convert dp to pixels
     */
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

    // ==================== STATIC UTILITY FOR MEMORY MANAGEMENT ====================

    /**
     * 🧹 STATIC UTILITY: Mass cleanup for multiple toolbars
     */
    public static void destroyAll(List<BottomSelectionToolbar> toolbars) {
        if (toolbars == null || toolbars.isEmpty()) {
            return;
        }

        Log.d(TAG, "🧹 Destroying " + toolbars.size() + " BottomSelectionToolbar instances...");

        for (BottomSelectionToolbar toolbar : toolbars) {
            if (toolbar != null) {
                toolbar.destroy();
            }
        }

        toolbars.clear();
        Log.d(TAG, "✅ Mass destruction completed");
    }

    /**
     * 🧹 STATIC UTILITY: Cleanup with null check
     */
    public static void safeDestroy(@Nullable BottomSelectionToolbar toolbar) {
        if (toolbar != null && !toolbar.isDestroyed()) {
            toolbar.destroy();
        }
    }

    /**
     * ❓ Check if toolbar is destroyed
     */
    public boolean isDestroyed() {
        return mIsDestroyed;
    }

    /**
     * 🔧 DEBUG: Add method to verify layout loading
     */
    private void debugLayoutStructure() {
        Log.d(TAG, "=== LAYOUT DEBUG ===");
        Log.d(TAG, "Toolbar container: " + (mToolbarContainer != null ? "✅" : "❌"));
        Log.d(TAG, "RecyclerView: " + (mQuickActionsRecyclerView != null ? "✅" : "❌"));

        if (mToolbarContainer != null) {
            Log.d(TAG, "Container class: " + mToolbarContainer.getClass().getSimpleName());
            Log.d(TAG, "Container children: " + mToolbarContainer.getChildCount());

            // List all child views
            for (int i = 0; i < mToolbarContainer.getChildCount(); i++) {
                View child = mToolbarContainer.getChildAt(i);
                Log.d(TAG, "  Child " + i + ": " + child.getClass().getSimpleName() +
                        " (id: " + child.getId() + ")");
            }
        }

        if (mQuickActionsRecyclerView != null) {
            Log.d(TAG, "RecyclerView class: " + mQuickActionsRecyclerView.getClass().getSimpleName());
            Log.d(TAG, "RecyclerView adapter: " + (mQuickActionsRecyclerView.getAdapter() != null ? "✅" : "❌"));
        }

        Log.d(TAG, "==================");
    }

    // ==================== EXAMPLE USAGE AND INTEGRATION ====================

    /**
     * 📋 Example integration method (for documentation purposes)
     * This shows how to properly initialize and use the enhanced toolbar
     */
    public static BottomSelectionToolbar createEnhancedToolbar(Context context, HalfTeam userTeam) {
        // Create enhanced toolbar with user team
        BottomSelectionToolbar toolbar = new BottomSelectionToolbar(context, userTeam);

        // Setup day map for validation (this would come from your repository)
        Map<LocalDate, Day> dayMap = new HashMap<>(); // Load from your data source
        toolbar.setDayMap(dayMap);

        Log.d(TAG, "Enhanced toolbar created with smart validation enabled");
        return toolbar;
    }

    /**
     * 🔧 Example method to show proper usage pattern
     */
    public void exampleUsage(ViewGroup container, Set<LocalDate> selectedDates,
                             DayLongClickListener listener) {

        // 1. Setup validation context
        setupValidationContext(selectedDates);

        // 2. Show toolbar with smart validation
        show(container, selectedDates, listener);

        // 3. The toolbar will automatically:
        //    - Validate actions based on user work schedule
        //    - Show only appropriate actions (max 5)
        //    - Provide user feedback for invalid actions
        //    - Apply business rules (ferie only for work days, etc.)

        Log.d(TAG, "Enhanced toolbar shown with smart validation active");
    }
}