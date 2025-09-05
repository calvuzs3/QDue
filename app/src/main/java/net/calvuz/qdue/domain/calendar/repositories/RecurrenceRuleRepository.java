package net.calvuz.qdue.domain.calendar.repositories;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.services.models.OperationResult;
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

    // ==================== RECURRENCE RULE CRUD OPERATIONS ====================

    /**
     * Save recurrence rule.
     *
     * @param recurrenceRule RecurrenceRule to save
     * @return CompletableFuture with saved RecurrenceRule
     */
    @NonNull
    CompletableFuture<RecurrenceRule>
    saveRecurrenceRule(@NonNull RecurrenceRule recurrenceRule);

    /**
     * Insert recurrence rule.
     *
     * @param recurrenceRule RecurrenceRule to insert
     * @return CompletableFuture with boolean indicating success
    */
    @NonNull
    CompletableFuture<Boolean>
    insertRecurrenceRule(@NonNull RecurrenceRule recurrenceRule);

    /**
     * Update recurrence rule.
     *
     * @param recurrenceRule RecurrenceRule to update
     * @return CompletableFuture with boolean indicating success
     */
    @NonNull
    CompletableFuture<Boolean>
    updateRecurrenceRule(@NonNull RecurrenceRule recurrenceRule);

    /**
     * Delete recurrence rule by ID.
     *
     * @param ruleId Recurrence rule ID to delete
     * @return CompletableFuture with boolean indicating success
     */
    @NonNull
    CompletableFuture<Boolean>
    deleteRecurrenceRule(@NonNull String ruleId);

    /**
     * Check if recurrence rule is in use by any assignments.
     *
     * @param ruleId Recurrence rule ID to check
     * @return CompletableFuture with boolean indicating usage
     */
    @NonNull
    CompletableFuture<Boolean>
    isRecurrenceRuleInUse(@NonNull String ruleId);

    // ==================== RECURRENCE RULE QUERY OPERATIONS ====================

    /**
     * Get all active recurrence rules.
     *
     * @return CompletableFuture with List of active RecurrenceRule objects
     */
    @NonNull
    CompletableFuture<List<RecurrenceRule>>
    getAllActiveRecurrenceRules();

    /**
     * Get active RecurrenceRules created by specific user.
     *
     * @param userId User ID to filter by
     * @return CompletableFuture with user's active patterns
     */
    @NonNull
    CompletableFuture<OperationResult<List<RecurrenceRule>>>
    getActiveUserRecurrenceRules(@NonNull String userId);

    /**
     * Get all recurrence rules.
     *
     * @return List of RecurrenceRule objects
     */
    @NonNull
    CompletableFuture<List<RecurrenceRule>>
    getAllRecurrenceRules();

    /**
     * Get recurrence rule by ID.
     *
     * @param ruleId Recurrence rule ID
     * @return CompletableFuture with RecurrenceRule or null if not found
     */
    @NonNull
    CompletableFuture<RecurrenceRule>
    getRecurrenceRuleById(@NonNull String ruleId);

    /**
     * Get RecurrenceRule by name with fallback alternatives.
     *
     * @param name Primary name to search
     * @return OperationResult that supports .or() chaining
     */
    @NonNull
    CompletableFuture<OperationResult<RecurrenceRule>>
    getRecurrenceRuleByName(@NonNull String name);

    /**
     * Get recurrence rules by frequency type.
     *
     * @param frequency Frequency type (DAILY, WEEKLY, etc.)
     * @return CompletableFuture with List of matching RecurrenceRule objects
     */
    @NonNull
    CompletableFuture<List<RecurrenceRule>>
    getRecurrenceRulesByFrequency(@NonNull RecurrenceRule.Frequency frequency);
}