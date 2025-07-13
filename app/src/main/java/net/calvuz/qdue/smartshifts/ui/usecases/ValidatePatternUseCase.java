package net.calvuz.qdue.smartshifts.ui.usecases;

import net.calvuz.qdue.smartshifts.data.repository.ShiftPatternRepository;
import net.calvuz.qdue.smartshifts.domain.validators.ContinuousCycleValidator;
import net.calvuz.qdue.smartshifts.domain.generators.RecurrenceRuleParser;
import net.calvuz.qdue.smartshifts.domain.models.RecurrenceRule;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

/**
 * Use case for validating shift patterns
 */
public class ValidatePatternUseCase {

    private final ContinuousCycleValidator validator;
    private final ShiftPatternRepository patternRepository;
    private final RecurrenceRuleParser ruleParser;

    @Inject
    public ValidatePatternUseCase(
            ContinuousCycleValidator validator,
            ShiftPatternRepository patternRepository
    ) {
        this.validator = validator;
        this.patternRepository = patternRepository;
        this.ruleParser = new RecurrenceRuleParser();
    }

    /**
     * Validate pattern JSON
     */
    public PatternValidationResult validatePattern(String recurrenceRuleJson) {
        try {
            // Parse the rule
            RecurrenceRule rule = ruleParser.parseRecurrenceRule(recurrenceRuleJson);

            PatternValidationResult result = new PatternValidationResult();
            result.isValid = true;
            result.isContinuousCycle = validator.validatePattern(recurrenceRuleJson);
            result.maxTeams = calculateMaxTeams(rule.getCycleLength());
            result.workingDaysPerCycle = rule.getWorkingDays().size();

            return result;

        } catch (Exception e) {
            PatternValidationResult result = new PatternValidationResult();
            result.isValid = false;
            result.errorMessage = e.getMessage();
            return result;
        }
    }

    /**
     * Validate pattern asynchronously with detailed checks
     */
    public CompletableFuture<DetailedValidationResult> validatePatternDetailed(String recurrenceRuleJson) {
        return CompletableFuture.supplyAsync(() -> {
            DetailedValidationResult result = new DetailedValidationResult();

            try {
                RecurrenceRule rule = ruleParser.parseRecurrenceRule(recurrenceRuleJson);

                result.basicValidation = validatePattern(recurrenceRuleJson);
                result.coverageValidation = validator.validateCoverage(rule);
                result.teamRequirements = validator.calculateTeamRequirements(rule);

                result.isValid = result.basicValidation.isValid;

            } catch (Exception e) {
                result.isValid = false;
                result.errorMessage = e.getMessage();
            }

            return result;
        });
    }

    /**
     * Calculate maximum teams for cycle length
     */
    private int calculateMaxTeams(int cycleLength) {
        if (cycleLength == 18) return 9;
        if (cycleLength == 15) return 5;
        if (cycleLength == 7) return 1;
        return Math.max(1, cycleLength / 2);
    }

    // ===== RESULT CLASSES =====

    public static class PatternValidationResult {
        public boolean isValid;
        public boolean isContinuousCycle;
        public int maxTeams;
        public int workingDaysPerCycle;
        public String errorMessage;
    }

    public static class DetailedValidationResult {
        public boolean isValid;
        public String errorMessage;
        public PatternValidationResult basicValidation;
        public ContinuousCycleValidator.CoverageValidationResult coverageValidation;
        public ContinuousCycleValidator.TeamRequirements teamRequirements;
    }
}
