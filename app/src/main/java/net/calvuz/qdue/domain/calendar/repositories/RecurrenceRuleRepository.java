package net.calvuz.qdue.domain.calendar.repositories;

import androidx.annotation.NonNull;

import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * RecurrenceRuleRepository - Domain interface for recurrence rule persistence.
 *
 * <p>Defines business-oriented operations for managing schedule recurrence
 * patterns without any infrastructure dependencies.</p>
 */
public interface RecurrenceRuleRepository {

    /**
     * Get recurrence rule by ID.
     *
     * @param ruleId Recurrence rule ID
     * @return CompletableFuture with RecurrenceRule or null if not found
     */
    @NonNull
    CompletableFuture<RecurrenceRule> getRecurrenceRuleById(@NonNull String ruleId);

    /**
     * Get all active recurrence rules.
     *
     * @return CompletableFuture with List of active RecurrenceRule objects
     */
    @NonNull
    CompletableFuture<List<RecurrenceRule>> getAllActiveRecurrenceRules();

    /**
     * Get recurrence rules by frequency type.
     *
     * @param frequency Frequency type (DAILY, WEEKLY, etc.)
     * @return CompletableFuture with List of matching RecurrenceRule objects
     */
    @NonNull
    CompletableFuture<List<RecurrenceRule>> getRecurrenceRulesByFrequency(
            @NonNull RecurrenceRule.Frequency frequency);

    /**
     * Save recurrence rule.
     *
     * @param recurrenceRule RecurrenceRule to save
     * @return CompletableFuture with saved RecurrenceRule
     */
    @NonNull
    CompletableFuture<RecurrenceRule> saveRecurrenceRule(@NonNull RecurrenceRule recurrenceRule);

    /**
     * Delete recurrence rule by ID.
     *
     * @param ruleId Recurrence rule ID to delete
     * @return CompletableFuture with boolean indicating success
     */
    @NonNull
    CompletableFuture<Boolean> deleteRecurrenceRule(@NonNull String ruleId);

    /**
     * Check if recurrence rule is in use by any assignments.
     *
     * @param ruleId Recurrence rule ID to check
     * @return CompletableFuture with boolean indicating usage
     */
    @NonNull
    CompletableFuture<Boolean> isRecurrenceRuleInUse(@NonNull String ruleId);
}