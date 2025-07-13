package net.calvuz.qdue.smartshifts.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import net.calvuz.qdue.smartshifts.data.dao.ShiftPatternDao;
import net.calvuz.qdue.smartshifts.data.dao.ShiftTypeDao;
import net.calvuz.qdue.smartshifts.data.entities.ShiftPattern;
import net.calvuz.qdue.smartshifts.data.entities.ShiftType;
import net.calvuz.qdue.smartshifts.domain.validators.ContinuousCycleValidator;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repository for managing shift patterns
 */
@Singleton
public class ShiftPatternRepository {

    private final ShiftPatternDao patternDao;
    private final ShiftTypeDao shiftTypeDao;
    private final ContinuousCycleValidator validator;
    private final Context context;

    @Inject
    public ShiftPatternRepository(
            ShiftPatternDao patternDao,
            ShiftTypeDao shiftTypeDao,
            ContinuousCycleValidator validator,
            Context context
    ) {
        this.patternDao = patternDao;
        this.shiftTypeDao = shiftTypeDao;
        this.validator = validator;
        this.context = context;
    }

    /**
     * Get all active patterns
     */
    public LiveData<List<ShiftPattern>> getAllActivePatterns() {
        return patternDao.getAllActivePatterns();
    }

    /**
     * Get predefined patterns only
     */
    public LiveData<List<ShiftPattern>> getPredefinedPatterns() {
        return patternDao.getPredefinedPatterns();
    }

    /**
     * Get custom patterns for user
     */
    public LiveData<List<ShiftPattern>> getCustomPatternsForUser(String userId) {
        return patternDao.getCustomPatternsForUser(userId);
    }

    /**
     * Get pattern by ID
     */
    public LiveData<ShiftPattern> getPatternById(String patternId) {
        return patternDao.getPatternByIdLive(patternId);
    }

    /**
     * Create new custom pattern
     */
    public CompletableFuture<String> createCustomPattern(
            String userId,
            String name,
            String description,
            int cycleLengthDays,
            String recurrenceRuleJson
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate pattern
                boolean isContinuous = validator.validatePattern(recurrenceRuleJson);

                String patternId = UUID.randomUUID().toString();
                ShiftPattern pattern = new ShiftPattern(
                        patternId,
                        name,
                        description,
                        cycleLengthDays,
                        recurrenceRuleJson,
                        isContinuous,
                        false, // not predefined
                        userId
                );

                patternDao.insert(pattern);
                return patternId;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Update existing pattern
     */
    public CompletableFuture<Boolean> updatePattern(ShiftPattern pattern) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                pattern.updatedAt = System.currentTimeMillis();
                patternDao.update(pattern);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Delete pattern (soft delete)
     */
    public CompletableFuture<Boolean> deletePattern(String patternId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                patternDao.softDelete(patternId, System.currentTimeMillis());
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Get available shift types
     */
    public LiveData<List<ShiftType>> getAvailableShiftTypes() {
        return shiftTypeDao.getAllActiveShiftTypes();
    }
}

// =====================================================================

