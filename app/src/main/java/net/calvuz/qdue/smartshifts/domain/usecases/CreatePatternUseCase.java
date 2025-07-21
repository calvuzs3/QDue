package net.calvuz.qdue.smartshifts.domain.usecases;

import android.content.Context;

import net.calvuz.qdue.R;
import net.calvuz.qdue.smartshifts.data.entities.ShiftPattern;
import net.calvuz.qdue.smartshifts.data.repository.ShiftPatternRepository;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

/**
 * Use case for creating and validating shift patterns
 */
public class CreatePatternUseCase {

    private final ShiftPatternRepository patternRepository;
    private final ValidatePatternUseCase validateUseCase;
    private final Context context;

    @Inject
    public CreatePatternUseCase(
            ShiftPatternRepository patternRepository,
            ValidatePatternUseCase validateUseCase,
            Context context
    ) {
        this.patternRepository = patternRepository;
        this.validateUseCase = validateUseCase;
        this.context = context;
    }

    /**
     * Create new custom pattern with validation
     */
    public CompletableFuture<CreatePatternResult> createCustomPattern(
            String userId,
            String name,
            String description,
            int cycleLengthDays,
            String recurrenceRuleJson
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate pattern first
                ValidatePatternUseCase.PatternValidationResult validation = validateUseCase.validatePattern(recurrenceRuleJson);
                if (!validation.isValid) {
                    CreatePatternResult result = new CreatePatternResult();
                    result.success = false;
                    result.errorMessage = validation.errorMessage;
                    return result;
                }

                // Check for duplicate names
                boolean nameExists = checkPatternNameExists(userId, name);
                if (nameExists) {
                    CreatePatternResult result = new CreatePatternResult();
                    result.success = false;
                    result.errorMessage = context.getString(
                            R.string.validation_name_exists
                    );
                    return result;
                }

                // Create pattern
                String patternId = patternRepository.createCustomPattern(
                        userId, name, description, cycleLengthDays, recurrenceRuleJson
                ).get();

                CreatePatternResult result = new CreatePatternResult();
                result.success = patternId != null;
                result.patternId = patternId;
                result.validationResult = validation;

                return result;

            } catch (Exception e) {
                CreatePatternResult result = new CreatePatternResult();
                result.success = false;
                result.errorMessage = e.getMessage();
                return result;
            }
        });
    }

    /**
     * Update existing pattern
     */
    public CompletableFuture<Boolean> updatePattern(ShiftPattern pattern) {
        return patternRepository.updatePattern(pattern);
    }

    /**
     * Delete pattern
     */
    public CompletableFuture<Boolean> deletePattern(String patternId) {
        return patternRepository.deletePattern(patternId);
    }

    /**
     * Check if pattern name already exists for user
     */
    private boolean checkPatternNameExists(String userId, String name) {
        // TODO: Implement name checking logic
        return false;
    }

    // ===== RESULT CLASSES =====

    public static class CreatePatternResult {
        public boolean success;
        public String patternId;
        public String errorMessage;
        public ValidatePatternUseCase.PatternValidationResult validationResult;
    }
}
