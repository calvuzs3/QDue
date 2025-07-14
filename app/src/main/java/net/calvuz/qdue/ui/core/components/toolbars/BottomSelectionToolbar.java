package net.calvuz.qdue.ui.core.components.toolbars;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
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
 *
 * Key improvements:
 * - Custom color palette integration
 * - Larger icons (24dp) and buttons (56dp) for better accessibility
 * - Maximum 4 actions limit for clean UI
 * - Smart validation based on user work schedule
 * - Business rules enforcement
 * - Enhanced visual feedback and animations
 */
public class BottomSelectionToolbar {

    private static final String TAG = "BottomSelectionToolbar";

    // 🎯 UX CONSTANTS
    private static final int MAX_ACTIONS = 5;              // Limit actions for clean UI
    private static final int ANIMATION_DURATION = 250;     // Smooth animations
    private static final int ICON_SIZE_DP = 24;           // Larger icons for visibility
    private static final int BUTTON_SIZE_DP = 56;         // Larger touch targets

    // Views
    private final Context mContext;
    private MaterialCardView mToolbarContainer;
//    private MaterialButton mCloseSelectionButton;
    private RecyclerView mQuickActionsRecyclerView;
//    private TextView mSelectionCountText;

    // Data and state
    private EnhancedToolbarActionsAdapter mActionsAdapter;
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
        initializeEnhancedViews();
        setupEnhancedListeners();
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
                validActions.add(ToolbarAction.FERIE);
            }

            if (isValidForMalattia(context)) {
                validActions.add(ToolbarAction.MALATTIA);
            }

            if (isValidForPermesso(context)) {
                validActions.add(ToolbarAction.PERMESSO);
            }

            if (isValidForLegge104(context)) {
                validActions.add(ToolbarAction.LEGGE_104);
            }

            if (isValidForStraordinario(context)) {
                validActions.add(ToolbarAction.STRAORDINARIO);
            }

            // ADD_EVENT è sempre valido (fallback)
            validActions.add(ToolbarAction.ADD_EVENT);

            // VIEW_EVENTS se ci sono eventi esistenti
//            if (context.hasExistingEvents) {
//                validActions.add(ToolbarAction.VIEW_EVENTS);
//            }

            Log.d(TAG, "Valid actions: " + validActions.size() + " of " + ToolbarAction.values().length);
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
         * 🏖️ FERIE - Valid for work days only
         */
        private static boolean isValidForFerie(ValidationContext context) {
            // Business Rule: Ferie can only be applied to work days
            if (context.workDays == 0) {
                Log.d(TAG, "FERIE invalid: No work days in selection");
                return false;
            }

            // Business Rule: No ferie for past days
            if (context.pastDays > 0) {
                Log.d(TAG, "FERIE invalid: Contains past days");
                return false;
            }

            // Business Rule: Check maximum consecutive days
            if (context.workDays > MAX_CONSECUTIVE_VACATION_DAYS) {
                Log.d(TAG, "FERIE invalid: Exceeds maximum consecutive days");
                return false;
            }

            return true;
        }

        /**
         * 🏥 MALATTIA - Valid for work days, special rules for timing
         */
        private static boolean isValidForMalattia(ValidationContext context) {
            // Business Rule: Malattia needs at least one work day impact
            if (context.workDays < MIN_WORK_DAYS_FOR_SICK_LEAVE) {
                Log.d(TAG, "MALATTIA invalid: Need at least " + MIN_WORK_DAYS_FOR_SICK_LEAVE + " work day(s)");
                return false;
            }

            // Business Rule: Malattia can be retroactive (past days allowed)
            // Business Rule: Weekend days are ok if there are work days too
            return true;
        }

        /**
         * ⏰ PERMESSO - Valid for work days, more flexible than ferie
         */
        private static boolean isValidForPermesso(ValidationContext context) {
            // Business Rule: Permesso can only be applied to work days
            if (context.workDays == 0) {
                Log.d(TAG, "PERMESSO invalid: No work days in selection");
                return false;
            }

            // Business Rule: Limited retroactive permission
            if (context.pastDays > 0) {
                Log.d(TAG, "PERMESSO invalid: Contains past days (limited retroactive allowed)");
                return false;
            }

            return true;
        }

        /**
         * ♿ LEGGE_104 - Special protected leave, very flexible
         */
        private static boolean isValidForLegge104(ValidationContext context) {
            // Business Rule: Legge 104 is very flexible, can be applied to work days
            if (context.workDays == 0) {
                Log.d(TAG, "LEGGE_104 invalid: No work days in selection");
                return false;
            }

            // Business Rule: Retroactive allowed for Legge 104
            // Business Rule: Can span weekends if needed
            return true;
        }

        /**
         * ⚡ STRAORDINARIO - Valid for off days or weekends
         */
        private static boolean isValidForStraordinario(ValidationContext context) {
            // Business Rule: Straordinario is typically for off-days or weekends
            if (context.offDays == 0 && context.weekendDays == 0) {
                Log.d(TAG, "STRAORDINARIO invalid: No off-days or weekends in selection");
                return false;
            }

            // Business Rule: No straordinario for past days
            if (context.pastDays > 0) {
                Log.d(TAG, "STRAORDINARIO invalid: Contains past days");
                return false;
            }

            // Business Rule: Single day selections preferred for straordinario
            if (context.totalDays > 3) {
                Log.d(TAG, "STRAORDINARIO warning: Large selection, might not be appropriate");
                // Still allow but with warning
            }

            return true;
        }

        /**
         * 👤 Check if user is working on a specific day
         */
        private static boolean isUserWorkingOnDay(Day day, HalfTeam userTeam) {
            if (day == null || userTeam == null) {
                return false;
            }

            // Check if user team is in any shift
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
            // TODO: Implement actual event checking based on your event system
            return day != null; // Placeholder
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

            // Show user-friendly message
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
                case FERIE:
                    if (reason.contains("No work days")) {
                        return "❌ Le ferie possono essere richieste solo per giorni lavorativi";
                    } else if (reason.contains("past days")) {
                        return "❌ Non è possibile richiedere ferie per giorni passati";
                    }
                    return "❌ Ferie non disponibili per " + dayCount + " " + dayText + " selezionati";

                case MALATTIA:
                    if (reason.contains("work day")) {
                        return "❌ La malattia deve includere almeno un giorno lavorativo";
                    }
                    return "❌ Malattia non disponibile per la selezione corrente";

                case PERMESSO:
                    if (reason.contains("No work days")) {
                        return "❌ I permessi possono essere richiesti solo per giorni lavorativi";
                    } else if (reason.contains("past days")) {
                        return "❌ Non è possibile richiedere permessi per giorni passati";
                    }
                    return "❌ Permesso non disponibile per " + dayCount + " " + dayText + " selezionati";

                case STRAORDINARIO:
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

    // ==================== ENHANCED INITIALIZATION ====================

    /**
     * 🎨 Initialize enhanced views with custom styling
     */
    private void initializeEnhancedViews() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mToolbarContainer = (MaterialCardView) inflater.inflate(
                R.layout.enhanced_bottom_selection_toolbar, null
        );

        // Get view references
//        mCloseSelectionButton = mToolbarContainer.findViewById(R.id.btn_close_selection);
        mQuickActionsRecyclerView = mToolbarContainer.findViewById(R.id.rv_quick_actions);
//        mSelectionCountText = mToolbarContainer.findViewById(R.id.tv_selection_count);

        // 🎨 Apply custom styling
        applyCustomStyling();

        // Setup enhanced RecyclerView
        setupEnhancedRecyclerView();

        // Initially hide with custom animation start position
        mToolbarContainer.setVisibility(View.GONE);
        mToolbarContainer.setTranslationY(dpToPx(80)); // Start below screen
        mToolbarContainer.setAlpha(0f);

        Log.d(TAG, "Enhanced bottom selection toolbar initialized");
    }

    /**
     * 🎨 Apply custom color palette and styling
     */
    private void applyCustomStyling() {
        // Apply custom background colors
        mToolbarContainer.setCardBackgroundColor(
                Library.getColorByThemeAttr(mContext, R.attr.floatingMenuBackground)
        );

        // Enhanced elevation and corner radius
        mToolbarContainer.setCardElevation(dpToPx(12));
        mToolbarContainer.setRadius(dpToPx(20));

        // Subtle stroke with custom colors
        mToolbarContainer.setStrokeWidth(dpToPx(1));
        mToolbarContainer.setStrokeColor(
                ColorStateList.valueOf(Library.getColorByThemeAttr(mContext, R.attr.floatingMenuOnBackground))
        );

//        // Apply custom colors to close button
//        if (mCloseSelectionButton != null) {
//            mCloseSelectionButton.setIconTint(
//                    ColorStateList.valueOf(Library.getColorByThemeAttr(mContext, R.attr.floatingMenuOnBackground))
//            );
//            mCloseSelectionButton.setBackgroundTintList(
//                    ColorStateList.valueOf(Color.TRANSPARENT)
//            );
//            mCloseSelectionButton.setRippleColor(
//                    ColorStateList.valueOf(Library.getColorByThemeAttr(mContext, R.attr.floatingMenuSelected))
//            );
//        }

        // Apply custom colors to selection count text
//        if (mSelectionCountText != null) {
//            mSelectionCountText.setTextColor(
//                    Library.getColorByThemeAttr(mContext, R.attr.floatingMenuOnBackground)
//            );
//        }
    }

    /**
     * 📋 Setup enhanced RecyclerView with optimizations
     */
    private void setupEnhancedRecyclerView() {
        // Enhanced layout manager with optimizations
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                mContext, LinearLayoutManager.HORIZONTAL, false
        );
        layoutManager.setInitialPrefetchItemCount(MAX_ACTIONS);
        mQuickActionsRecyclerView.setLayoutManager(layoutManager);

        // Enhanced adapter
        mActionsAdapter = new EnhancedToolbarActionsAdapter();
        mQuickActionsRecyclerView.setAdapter(mActionsAdapter);

        // Performance optimizations
        mQuickActionsRecyclerView.setHasFixedSize(true);
        mQuickActionsRecyclerView.setItemViewCacheSize(MAX_ACTIONS);
        mQuickActionsRecyclerView.setDrawingCacheEnabled(true);

        // Enhanced scrolling behavior
        mQuickActionsRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mQuickActionsRecyclerView.setNestedScrollingEnabled(false);
    }

    /**
     * 👆 Setup enhanced click listeners with haptic feedback
     */
    private void setupEnhancedListeners() {
        // Enhanced close button with haptic feedback
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
        mActionsAdapter.setOnActionClickListener(this::onEnhancedActionClicked);
    }

    // ==================== ENHANCED ACTION MANAGEMENT ====================

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

        // Optional: Hide toolbar after action (configurable)
        // hide();
    }

    /**
     * 🎯 Smart action selection with validation and priority
     */
    private List<ToolbarAction> getEnhancedActionsForSelection(Set<LocalDate> selectedDates) {
        if (selectedDates == null || selectedDates.isEmpty()) {
            return new ArrayList<>();
        }

        // 🧠 USE SMART VALIDATION
        List<ToolbarAction> validActions = SmartActionValidator.getValidActionsForSelection(
                selectedDates, mCurrentUserTeam, mDayMap
        );

        // Apply UI limits (max 4 actions)
        if (validActions.size() > MAX_ACTIONS) {
            // Prioritize based on business importance
            validActions = prioritizeActions(validActions, selectedDates);
        }

        Log.d(TAG, "Smart selection: " + validActions.size() + " valid actions for " +
                selectedDates.size() + " dates");

        return validActions;
    }

    /**
     * 🎯 Prioritize actions when we need to limit to MAX_ACTIONS
     */
    private List<ToolbarAction> prioritizeActions(List<ToolbarAction> actions, Set<LocalDate> selectedDates) {
        // Priority order based on business importance and frequency
        List<ToolbarAction> priorityOrder = Arrays.asList(
                ToolbarAction.FERIE,         // High priority - most common
                ToolbarAction.MALATTIA,      // High priority - quite common
                ToolbarAction.PERMESSO,      // Medium priority - common
                ToolbarAction.LEGGE_104,     // Medium priority - protected
                ToolbarAction.ADD_EVENT,     // Medium priority - fallback
                ToolbarAction.STRAORDINARIO,  // Lower priority - occasional
                ToolbarAction.VIEW_EVENTS     // Lowest priority - informational

        );

        List<ToolbarAction> prioritized = new ArrayList<>();

        // Add actions in priority order
        for (ToolbarAction priority : priorityOrder) {
            if (actions.contains(priority) && prioritized.size() < MAX_ACTIONS) {
                prioritized.add(priority);
            }
        }

        // Add any remaining actions up to limit
        for (ToolbarAction action : actions) {
            if (!prioritized.contains(action) && prioritized.size() < MAX_ACTIONS) {
                prioritized.add(action);
            }
        }

        return prioritized;
    }

    // ==================== ENHANCED UI METHODS ====================

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

        // Update UI content with smart validation
//        updateSelectionDisplay();
        updateAvailableActions();

        // Add to container if not already added
        if (mToolbarContainer.getParent() == null) {
            container.addView(mToolbarContainer);
        }

        // Show with enhanced animation
        animateIn();

        mIsVisible = true;
        Log.d(TAG, "Enhanced toolbar shown for " + selectedDates.size() + " selected dates");
    }

    /**
     * 🔧 Setup validation context before showing
     */
    private void setupValidationContext(Set<LocalDate> selectedDates) {
        // Load day data for selected dates if not already available
        // This would typically come from your repository/service layer

        for (LocalDate date : selectedDates) {
            if (!mDayMap.containsKey(date)) {
                // Load day data - this is a placeholder
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
        // This would typically use your repository pattern
        return null; // Placeholder
    }

    /**
     * 🔄 Enhanced hide method with smooth animations
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
            mSelectedDates.clear();
            mListener = null;

            Log.d(TAG, "Enhanced toolbar hidden");
        });
    }

//    /**
//     * 📊 Update selection display with enhanced formatting
//     */
//    private void updateSelectionDisplay() {
//        if (mSelectionCountText == null || mSelectedDates == null) return;
//
//        int count = mSelectedDates.size();
//        String displayText;
//
//        if (count == 1) {
//            displayText = "1 giorno";
//        } else {
//            displayText = count + " giorni";
//        }
//
//        mSelectionCountText.setText(displayText);
//    }

    /**
     * 🔄 Update available actions based on smart validation
     */
    private void updateAvailableActions() {
        if (mActionsAdapter == null) return;

        // 🧠 Get smart-validated actions
        List<ToolbarAction> actions = getEnhancedActionsForSelection(mSelectedDates);
        mActionsAdapter.updateActions(actions);

        Log.v(TAG, "Updated to " + actions.size() + " validated actions for selection");
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * 🆕 Set user team for validation
     */
    public void setUserTeam(HalfTeam userTeam) {
        mCurrentUserTeam = userTeam;

        // Refresh actions if toolbar is currently visible
        if (mIsVisible && !mSelectedDates.isEmpty()) {
            updateAvailableActions();
        }
    }

    /**
     * 🆕 Set day map for validation
     */
    public void setDayMap(Map<LocalDate, Day> dayMap) {
        mDayMap = dayMap != null ? dayMap : new HashMap<>();

        // Refresh actions if toolbar is currently visible
        if (mIsVisible && !mSelectedDates.isEmpty()) {
            updateAvailableActions();
        }
    }

    /**
     * 🔄 Update selection from external source with smart validation
     */
    public void updateSelection(@NonNull Set<LocalDate> selectedDates) {
        if (mIsDestroyed) return;

        // 🔧 Setup validation context for new selection
        setupValidationContext(selectedDates);

        mSelectedDates = new HashSet<>(selectedDates);
//        updateSelectionDisplay();
        updateAvailableActions();

        Log.d(TAG, "Selection updated: " + selectedDates.size() + " dates");
    }

    /**
     * ❓ Check if toolbar is visible
     */
    public boolean isVisible() {
        return mIsVisible && !mIsDestroyed;
    }

    /**
     * ❓ Check if toolbar is destroyed
     */
    public boolean isDestroyed() {
        return mIsDestroyed;
    }

    // ==================== ENHANCED ANIMATIONS ====================

    /**
     * 🎬 Enhanced entrance animation
     */
    private void animateIn() {
        if (mToolbarContainer == null) return;

        // Cancel any running animations
        cancelRunningAnimations();

        // Set initial state
        mToolbarContainer.setVisibility(View.VISIBLE);
        mToolbarContainer.setTranslationY(dpToPx(80));
        mToolbarContainer.setAlpha(0f);
        mToolbarContainer.setScaleX(0.95f);
        mToolbarContainer.setScaleY(0.95f);

        // Create enhanced animation set
        mShowAnimation = new AnimatorSet();

        ObjectAnimator translateY = ObjectAnimator.ofFloat(mToolbarContainer, "translationY", dpToPx(80), 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(mToolbarContainer, "alpha", 0f, 1f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mToolbarContainer, "scaleX", 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mToolbarContainer, "scaleY", 0.95f, 1f);

        // Enhanced interpolators
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

        // Cancel any running animations
        cancelRunningAnimations();

        // Create enhanced animation set
        mHideAnimation = new AnimatorSet();

        ObjectAnimator translateY = ObjectAnimator.ofFloat(mToolbarContainer, "translationY", 0f, dpToPx(60));
        ObjectAnimator alpha = ObjectAnimator.ofFloat(mToolbarContainer, "alpha", 1f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mToolbarContainer, "scaleX", 1f, 0.9f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mToolbarContainer, "scaleY", 1f, 0.9f);

        // Enhanced interpolators for exit
        translateY.setInterpolator(new AccelerateInterpolator(1.2f));
        alpha.setInterpolator(new AccelerateInterpolator());
        scaleX.setInterpolator(new AccelerateInterpolator());
        scaleY.setInterpolator(new AccelerateInterpolator());

        mHideAnimation.playTogether(translateY, alpha, scaleX, scaleY);
        mHideAnimation.setDuration((int)(ANIMATION_DURATION * 0.8f)); // Slightly faster exit

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
     * 🎬 Enhanced button press animation
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
     * 🛑 Cancel any running animations
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

        // Pause any running animations
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

        // Resume any paused animations
        if (mShowAnimation != null && mShowAnimation.isPaused()) {
            mShowAnimation.resume();
        }

        if (mHideAnimation != null && mHideAnimation.isPaused()) {
            mHideAnimation.resume();
        }

        Log.v(TAG, "BottomSelectionToolbar resumed");
    }

    // ==================== DESTROY METHOD ====================

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

            // Step 4: Cleanup button listeners and references
            cleanupButtons();

            // Step 5: Clear all data references
            clearAllReferences();

            // Step 6: Cleanup view references
            cleanupViews();

            // Step 7: Mark as destroyed
            mIsDestroyed = true;

            Log.d(TAG, "✅ BottomSelectionToolbar destruction completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error during BottomSelectionToolbar destruction", e);
            // Ensure we mark as destroyed even if cleanup fails
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

                // Remove from parent if attached
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
            // Stop and cleanup show animation
            if (mShowAnimation != null) {
                if (mShowAnimation.isRunning()) {
                    mShowAnimation.cancel();
                }
                mShowAnimation.removeAllListeners();
                mShowAnimation = null;
            }

            // Stop and cleanup hide animation
            if (mHideAnimation != null) {
                if (mHideAnimation.isRunning()) {
                    mHideAnimation.cancel();
                }
                mHideAnimation.removeAllListeners();
                mHideAnimation = null;
            }

            // Cancel any pending view animations on container
            if (mToolbarContainer != null) {
                mToolbarContainer.clearAnimation();
                mToolbarContainer.animate().cancel();
            }

            // Cancel animations on RecyclerView
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
                // Stop any pending scroll operations
                mQuickActionsRecyclerView.stopScroll();

                // Clear adapter
                mQuickActionsRecyclerView.setAdapter(null);

                // Clear layout manager
                mQuickActionsRecyclerView.setLayoutManager(null);

                // Clear all item decorations
                while (mQuickActionsRecyclerView.getItemDecorationCount() > 0) {
                    mQuickActionsRecyclerView.removeItemDecorationAt(0);
                }

                // Clear recycled view pool
                RecyclerView.RecycledViewPool recycledViewPool = mQuickActionsRecyclerView.getRecycledViewPool();
                if (recycledViewPool != null) {
                    recycledViewPool.clear();
                }

                // Clear animations
                mQuickActionsRecyclerView.clearAnimation();
                mQuickActionsRecyclerView.animate().cancel();

                // Nullify reference
                mQuickActionsRecyclerView = null;
            }

            // Cleanup adapter separately
            if (mActionsAdapter != null) {
                mActionsAdapter.clearData();
                mActionsAdapter = null;
            }

            Log.v(TAG, "✅ RecyclerView cleanup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up RecyclerView", e);
            // Force nullify
            mQuickActionsRecyclerView = null;
            mActionsAdapter = null;
        }
    }

    /**
     * 🔘 Cleanup button listeners and references
     */
    private void cleanupButtons() {
        Log.v(TAG, "Cleaning up buttons...");

        try {
            // Clean close selection button
//            if (mCloseSelectionButton != null) {
//                mCloseSelectionButton.setOnClickListener(null);
//                mCloseSelectionButton.setOnLongClickListener(null);
//                mCloseSelectionButton.clearAnimation();
//                mCloseSelectionButton.animate().cancel();
//                mCloseSelectionButton = null;
//            }

            Log.v(TAG, "✅ Buttons cleanup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up buttons", e);
            // Force nullify
//            mCloseSelectionButton = null;
        }
    }

    /**
     * 🔗 Clear all object references
     */
    private void clearAllReferences() {
        Log.v(TAG, "Clearing all references...");

        try {
            // Clear listener
            mListener = null;

            // Clear selected dates
            if (mSelectedDates != null) {
                mSelectedDates.clear();
                mSelectedDates = null;
            }

            // Clear user team reference
            mCurrentUserTeam = null;

            // Clear day map
            if (mDayMap != null) {
                mDayMap.clear();
                mDayMap = null;
            }

            // Clear state
            mIsVisible = false;

            Log.v(TAG, "✅ References cleanup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error clearing references", e);
            // Force clear
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
            // Clear selection count text
//            if (mSelectionCountText != null) {
//                mSelectionCountText.clearAnimation();
//                mSelectionCountText = null;
//            }

            // Clear main toolbar container
            if (mToolbarContainer != null) {
                // Remove from parent if still attached
                ViewParent parent = mToolbarContainer.getParent();
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(mToolbarContainer);
                }

                // Clear all view references
                mToolbarContainer.clearAnimation();
                mToolbarContainer.animate().cancel();
                mToolbarContainer = null;
            }

            Log.v(TAG, "✅ Views cleanup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up views", e);
            // Force nullify
//            mSelectionCountText = null;
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
        //    - Show only appropriate actions (max 4)
        //    - Provide user feedback for invalid actions
        //    - Apply business rules (ferie only for work days, etc.)

        Log.d(TAG, "Enhanced toolbar shown with smart validation active");
    }
}