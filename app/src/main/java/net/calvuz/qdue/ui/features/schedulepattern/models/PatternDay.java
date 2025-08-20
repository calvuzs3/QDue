package net.calvuz.qdue.ui.features.schedulepattern.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.calendar.models.Shift;

import java.util.Objects;

/**
 * PatternDay - UI model representing a single day in user's work schedule pattern.
 *
 * <p>This model represents a single "tile" or "tassello" in the user's work schedule pattern.
 * Each PatternDay corresponds to one day in the repeating cycle and can either contain
 * a work shift or represent a rest day (no shift).</p>
 *
 * <h3>Key Concepts:</h3>
 * <ul>
 *   <li><strong>Day Number</strong>: Position in the pattern sequence (1-based)</li>
 *   <li><strong>Shift Assignment</strong>: Work shift for this day, null for rest days</li>
 *   <li><strong>Rest Day Handling</strong>: Rest represented as absence of shift (null)</li>
 *   <li><strong>Pattern Integration</strong>: Will be converted to RecurrenceRule components</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>
 * // Work day with morning shift
 * PatternDay workDay = new PatternDay(1, morningShift);
 *
 * // Rest day
 * PatternDay restDay = new PatternDay(2, null);
 *
 * // Check if rest day
 * boolean isRestDay = restDay.isRestDay();
 * </pre>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Initial Implementation
 * @since Clean Architecture Phase 2
 */
public class PatternDay {

    // ==================== FIELDS ====================

    private int mDayNumber;
    private Shift mShift;  // null for rest days
    private boolean mIsEditable;

    // ==================== CONSTRUCTORS ====================

    /**
     * Create a new PatternDay.
     *
     * @param dayNumber Position in pattern sequence (1-based)
     * @param shift Work shift for this day, null for rest day
     */
    public PatternDay(int dayNumber, @Nullable Shift shift) {
        this.mDayNumber = dayNumber;
        this.mShift = shift;
        this.mIsEditable = true; // All pattern days are editable by default
    }

    /**
     * Create a new PatternDay with editability control.
     *
     * @param dayNumber Position in pattern sequence (1-based)
     * @param shift Work shift for this day, null for rest day
     * @param isEditable Whether this day can be modified
     */
    public PatternDay(int dayNumber, @Nullable Shift shift, boolean isEditable) {
        this.mDayNumber = dayNumber;
        this.mShift = shift;
        this.mIsEditable = isEditable;
    }

    // ==================== GETTERS AND SETTERS ====================

    /**
     * Get the day number in the pattern sequence.
     *
     * @return Day number (1-based)
     */
    public int getDayNumber() {
        return mDayNumber;
    }

    /**
     * Set the day number in the pattern sequence.
     *
     * @param dayNumber Day number (1-based)
     */
    public void setDayNumber(int dayNumber) {
        this.mDayNumber = dayNumber;
    }

    /**
     * Get the work shift assigned to this day.
     *
     * @return Shift object or null for rest days
     */
    @Nullable
    public Shift getShift() {
        return mShift;
    }

    /**
     * Set the work shift for this day.
     *
     * @param shift Shift object or null for rest day
     */
    public void setShift(@Nullable Shift shift) {
        this.mShift = shift;
    }

    /**
     * Check if this day can be modified.
     *
     * @return true if editable, false otherwise
     */
    public boolean isEditable() {
        return mIsEditable;
    }

    /**
     * Set the editability of this day.
     *
     * @param editable true if editable, false otherwise
     */
    public void setEditable(boolean editable) {
        this.mIsEditable = editable;
    }

    // ==================== BUSINESS LOGIC ====================

    /**
     * Check if this is a rest day (no shift assigned).
     *
     * @return true if rest day, false if work day
     */
    public boolean isRestDay() {
        return mShift == null;
    }

    /**
     * Check if this is a work day (has shift assigned).
     *
     * @return true if work day, false if rest day
     */
    public boolean isWorkDay() {
        return mShift != null;
    }

    /**
     * Get display name for this pattern day.
     * Returns shift name for work days, "Rest" for rest days.
     *
     * @return Display name string
     */
    @NonNull
    public String getDisplayName() {
        if (isRestDay()) {
            return "Rest"; // TODO: Localize this string
        } else {
            return mShift.getName();
        }
    }

    /**
     * Get display description for this pattern day.
     * Includes shift details for work days.
     *
     * @return Display description string
     */
    @NonNull
    public String getDisplayDescription() {
        if (isRestDay()) {
            return "Rest day - no shifts"; // TODO: Localize this string
        } else {
            // Format: "Morning Shift (06:00 - 14:00)"
            return String.format("%s (%s - %s)",
                    mShift.getName(),
                    mShift.getStartTime() != null ? mShift.getStartTime().toString() : "?",
                    mShift.getEndTime() != null ? mShift.getEndTime().toString() : "?");
        }
    }

    /**
     * Get the pattern day type for categorization.
     *
     * @return PatternDayType enum value
     */
    @NonNull
    public PatternDayType getType() {
        if (isRestDay()) {
            return PatternDayType.REST_DAY;
        } else {
            // Could classify by shift type in the future
            return PatternDayType.WORK_DAY;
        }
    }

    /**
     * Create a copy of this PatternDay.
     *
     * @return New PatternDay with same values
     */
    @NonNull
    public PatternDay copy() {
        return new PatternDay(mDayNumber, mShift, mIsEditable);
    }

    /**
     * Create a copy with a new day number.
     *
     * @param newDayNumber New day number for the copy
     * @return New PatternDay with updated day number
     */
    @NonNull
    public PatternDay copyWithDayNumber(int newDayNumber) {
        return new PatternDay(newDayNumber, mShift, mIsEditable);
    }

    // ==================== VALIDATION ====================

    /**
     * Validate this pattern day for consistency.
     *
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        // Day number must be positive
        if (mDayNumber <= 0) {
            return false;
        }

        // If shift is present, it should be valid
        if (mShift != null) {
            // Basic shift validation - can be extended
            return mShift.getId() != null && !mShift.getId().trim().isEmpty();
        }

        // Rest days are always valid
        return true;
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        PatternDay that = (PatternDay) obj;
        return mDayNumber == that.mDayNumber &&
                mIsEditable == that.mIsEditable &&
                Objects.equals(mShift, that.mShift);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mDayNumber, mShift, mIsEditable);
    }

    @Override
    @NonNull
    public String toString() {
        return "PatternDay{" +
                "dayNumber=" + mDayNumber +
                ", shift=" + (mShift != null ? mShift.getName() : "REST") +
                ", editable=" + mIsEditable +
                '}';
    }

    // ==================== ENUMS ====================

    /**
     * Pattern day type classification.
     */
    public enum PatternDayType {
        /**
         * Regular work day with assigned shift.
         */
        WORK_DAY,

        /**
         * Rest day with no shifts.
         */
        REST_DAY,

        /**
         * Special day (could be extended for holidays, etc.).
         */
        SPECIAL_DAY
    }
}