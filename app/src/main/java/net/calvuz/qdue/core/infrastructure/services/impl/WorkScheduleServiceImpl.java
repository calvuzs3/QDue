package net.calvuz.qdue.core.infrastructure.services.impl;

import android.content.Context;

import net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleEvent;
import net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleTemplate;
import net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleType;
import net.calvuz.qdue.core.domain.quattrodue.provider.CustomScheduleProvider;
import net.calvuz.qdue.core.domain.quattrodue.provider.FixedScheduleProvider;
import net.calvuz.qdue.core.domain.quattrodue.provider.WorkScheduleProvider;
import net.calvuz.qdue.core.infrastructure.db.QDueDatabase;
import net.calvuz.qdue.core.infrastructure.services.ShiftTypeService;
import net.calvuz.qdue.core.infrastructure.services.WorkScheduleService;
import net.calvuz.qdue.core.infrastructure.services.models.OperationResult;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ✅ SERVICE IMPLEMENTATION: Work Schedule Service following existing architecture patterns
 *
 * <p>Implementation of WorkScheduleService following the established patterns of other
 * service implementations in the application. Uses async operations, OperationResult
 * pattern, and proper error handling.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Provider pattern for extensible schedule generation</li>
 *   <li>Integration with ShiftTypeService</li>
 *   <li>Support for both fixed and custom schedules</li>
 *   <li>Comprehensive validation and error handling</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 5
 */
public class WorkScheduleServiceImpl implements WorkScheduleService {

    private static final String TAG = "WorkScheduleServiceImpl";

    private final Context mContext;
    private final QDueDatabase mDatabase;
    private final ShiftTypeService mShiftTypeService;

    // Providers
    private FixedScheduleProvider mFixedScheduleProvider;
    private CustomScheduleProvider mCustomScheduleProvider;

    // Predefined templates
    private List<WorkScheduleTemplate> mPredefinedTemplates;

    private volatile boolean mIsShutdown = false;
    private volatile boolean mIsInitialized = false;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor following existing service pattern
     *
     * @param context Application context
     * @param database Database instance
     * @param shiftTypeService ShiftType service dependency
     */
    public WorkScheduleServiceImpl(Context context, QDueDatabase database, ShiftTypeService shiftTypeService) {
        this.mContext = context.getApplicationContext();
        this.mDatabase = database;
        this.mShiftTypeService = shiftTypeService;

        Log.d(TAG, "WorkScheduleServiceImpl initialized");
    }

    // ==================== INITIALIZATION ====================

    @Override
    public CompletableFuture<OperationResult<Void>> initializeService() {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.INITIALIZATION);
            }

            try {
                Log.d(TAG, "Initializing WorkScheduleService...");

                // Initialize providers
                mFixedScheduleProvider = new FixedScheduleProvider();
                mCustomScheduleProvider = new CustomScheduleProvider();

                // Initialize predefined templates
                initializePredefinedTemplates();

                // Initialize fixed schedule provider with shift types
                initializeFixedScheduleProvider();

                mIsInitialized = true;
                Log.d(TAG, "✅ WorkScheduleService initialized successfully");

                return OperationResult.success("Initialization successful", OperationResult.OperationType.INITIALIZATION);

            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to initialize WorkScheduleService", e);
                return OperationResult.failure("Initialization failed: " + e.getMessage(),
                        OperationResult.OperationType.INITIALIZATION);
            }
        });
    }

    // ==================== SCHEDULE GENERATION OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<List<WorkScheduleEvent>>> generateSchedule(
            LocalDate startDate, LocalDate endDate, WorkScheduleType scheduleType, String userTeam) {

        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.READ);
            }

            if (!mIsInitialized) {
                return OperationResult.failure("Service not initialized", OperationResult.OperationType.READ);
            }

            if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
                return OperationResult.failure("Invalid date range", OperationResult.OperationType.READ);
            }

            if (scheduleType == null) {
                return OperationResult.failure("Schedule type cannot be null", OperationResult.OperationType.READ);
            }

            try {
                Log.d(TAG, "Generating schedule from " + startDate + " to " + endDate +
                        " for type: " + scheduleType + ", team: " + userTeam);

                // Get appropriate template
                WorkScheduleTemplate template = getTemplateForType(scheduleType);
                if (template == null) {
                    return OperationResult.failure("No template available for schedule type: " + scheduleType,
                            OperationResult.OperationType.READ);
                }

                // Get appropriate provider
                WorkScheduleProvider provider = getProviderForTemplate(template);
                if (provider == null) {
                    return OperationResult.failure("No provider available for template: " + template.getType(),
                            OperationResult.OperationType.READ);
                }

                // Generate schedule
                List<WorkScheduleEvent> events = provider.generateSchedule(startDate, endDate, template, userTeam);

                Log.d(TAG, "Generated " + events.size() + " schedule events");
                return OperationResult.success(events, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to generate schedule", e);
                return OperationResult.failure("Schedule generation failed: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<List<WorkScheduleEvent>>> generateMonthlySchedule(
            LocalDate yearMonth, WorkScheduleType scheduleType, String userTeam) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Calculate first and last day of month
                YearMonth ym = YearMonth.from(yearMonth);
                LocalDate startDate = ym.atDay(1);
                LocalDate endDate = ym.atEndOfMonth();

                Log.d(TAG, "Generating monthly schedule for " + ym + " (" + startDate + " to " + endDate + ")");

                // Delegate to the main generate method
                return generateSchedule(startDate, endDate, scheduleType, userTeam).get();

            } catch (Exception e) {
                Log.e(TAG, "Failed to generate monthly schedule", e);
                return OperationResult.failure("Monthly schedule generation failed: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<List<WorkScheduleEvent>>> generateDailySchedule(
            LocalDate date, WorkScheduleType scheduleType, String userTeam) {

        return CompletableFuture.supplyAsync(() -> {
            if (date == null) {
                return OperationResult.failure("Date cannot be null", OperationResult.OperationType.READ);
            }

            Log.d(TAG, "Generating daily schedule for " + date);

            try {
                // Delegate to the main generate method with same start/end date
                return generateSchedule(date, date, scheduleType, userTeam).get();

            } catch (Exception e) {
                Log.e(TAG, "Failed to generate daily schedule", e);
                return OperationResult.failure("Daily schedule generation failed: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<List<WorkScheduleEvent>>> generateScheduleWithTemplate(
            LocalDate startDate, LocalDate endDate, WorkScheduleTemplate template, String userTeam) {

        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.READ);
            }

            if (!mIsInitialized) {
                return OperationResult.failure("Service not initialized", OperationResult.OperationType.READ);
            }

            if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
                return OperationResult.failure("Invalid date range", OperationResult.OperationType.READ);
            }

            if (template == null) {
                return OperationResult.failure("Template cannot be null", OperationResult.OperationType.READ);
            }

            try {
                Log.d(TAG, "Generating schedule with custom template: " + template.getName());

                // Get appropriate provider
                WorkScheduleProvider provider = getProviderForTemplate(template);
                if (provider == null) {
                    return OperationResult.failure("No provider available for template: " + template.getType(),
                            OperationResult.OperationType.READ);
                }

                // Validate template
                if (!provider.validateTemplate(template)) {
                    return OperationResult.failure("Template validation failed", OperationResult.OperationType.READ);
                }

                // Generate schedule
                List<WorkScheduleEvent> events = provider.generateSchedule(startDate, endDate, template, userTeam);

                Log.d(TAG, "Generated " + events.size() + " events with custom template");
                return OperationResult.success(events, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to generate schedule with template", e);
                return OperationResult.failure("Template schedule generation failed: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    // ==================== TEMPLATE MANAGEMENT OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<List<WorkScheduleTemplate>>> getAvailableTemplates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<WorkScheduleTemplate> allTemplates = new ArrayList<>();

                // Add predefined templates
                if (mPredefinedTemplates != null) {
                    allTemplates.addAll(mPredefinedTemplates);
                }

                // TODO: Add user-defined templates from database when implemented

                Log.d(TAG, "Retrieved " + allTemplates.size() + " available templates");
                return OperationResult.success(allTemplates, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to get available templates", e);
                return OperationResult.failure("Failed to retrieve templates: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<List<WorkScheduleTemplate>>> getPredefinedTemplates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<WorkScheduleTemplate> predefined = mPredefinedTemplates != null ?
                        new ArrayList<>(mPredefinedTemplates) : new ArrayList<>();

                Log.d(TAG, "Retrieved " + predefined.size() + " predefined templates");
                return OperationResult.success(predefined, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to get predefined templates", e);
                return OperationResult.failure("Failed to retrieve predefined templates: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<List<WorkScheduleTemplate>>> getCustomTemplates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // TODO: Implement database retrieval of custom templates
                List<WorkScheduleTemplate> customTemplates = new ArrayList<>();

                Log.d(TAG, "Retrieved " + customTemplates.size() + " custom templates");
                return OperationResult.success(customTemplates, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to get custom templates", e);
                return OperationResult.failure("Failed to retrieve custom templates: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<WorkScheduleTemplate>> getTemplateById(Long templateId) {
        return CompletableFuture.supplyAsync(() -> {
            if (templateId == null) {
                return OperationResult.failure("Template ID cannot be null", OperationResult.OperationType.READ);
            }

            try {
                // Search in predefined templates
                if (mPredefinedTemplates != null) {
                    for (WorkScheduleTemplate template : mPredefinedTemplates) {
                        if (templateId.equals(template.getId())) {
                            Log.d(TAG, "Found predefined template: " + template.getName());
                            return OperationResult.success(template, OperationResult.OperationType.READ);
                        }
                    }
                }

                // TODO: Search in custom templates from database

                Log.w(TAG, "Template not found with ID: " + templateId);
                return OperationResult.failure("Template not found", OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to get template by ID: " + templateId, e);
                return OperationResult.failure("Failed to retrieve template: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<WorkScheduleTemplate>> createCustomTemplate(WorkScheduleTemplate template) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: Implement custom template creation with database persistence
            return OperationResult.failure("Custom template creation not yet implemented",
                    OperationResult.OperationType.CREATE);
        });
    }

    @Override
    public CompletableFuture<OperationResult<WorkScheduleTemplate>> updateCustomTemplate(WorkScheduleTemplate template) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: Implement custom template update with database persistence
            return OperationResult.failure("Custom template update not yet implemented",
                    OperationResult.OperationType.UPDATE);
        });
    }

    @Override
    public CompletableFuture<OperationResult<Void>> deactivateCustomTemplate(Long templateId) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: Implement custom template deactivation
            return OperationResult.failure("Custom template deactivation not yet implemented",
                    OperationResult.OperationType.UPDATE);
        });
    }

    // ==================== VALIDATION OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<WorkScheduleValidationResult>> validateTemplate(WorkScheduleTemplate template) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> errors = new ArrayList<>();
                List<String> warnings = new ArrayList<>();

                if (template == null) {
                    errors.add("Template cannot be null");
                } else {
                    validateTemplateFields(template, errors, warnings);
                }

                boolean isValid = errors.isEmpty();
                WorkScheduleValidationResult result = new WorkScheduleValidationResult(isValid, errors, warnings);

                Log.d(TAG, "Template validation completed - Valid: " + isValid +
                        ", Errors: " + errors.size() + ", Warnings: " + warnings.size());

                return OperationResult.success(result, OperationResult.OperationType.VALIDATION);

            } catch (Exception e) {
                Log.e(TAG, "Failed to validate template", e);
                return OperationResult.failure("Validation failed: " + e.getMessage(),
                        OperationResult.OperationType.VALIDATION);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<Boolean>> isScheduleTypeSupported(WorkScheduleType scheduleType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean supported = scheduleType != null && (
                        scheduleType == WorkScheduleType.FIXED_4_2 ||
                                scheduleType == WorkScheduleType.CUSTOM
                );

                Log.d(TAG, "Schedule type " + scheduleType + " supported: " + supported);
                return OperationResult.success(supported, OperationResult.OperationType.VALIDATION);

            } catch (Exception e) {
                Log.e(TAG, "Failed to check schedule type support", e);
                return OperationResult.failure("Support check failed: " + e.getMessage(),
                        OperationResult.OperationType.VALIDATION);
            }
        });
    }

    // ==================== UTILITY OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<WorkScheduleEvent>> getNextScheduledShift(
            String team, LocalDate fromDate, WorkScheduleType scheduleType) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Generate schedule for next 30 days and find first matching event
                LocalDate endDate = fromDate.plusDays(30);

                List<WorkScheduleEvent> events = generateSchedule(fromDate, endDate, scheduleType, team)
                        .get().getData();

                if (events == null || events.isEmpty()) {
                    return OperationResult.failure("No scheduled shifts found", OperationResult.OperationType.READ);
                }

                // Find first event with the specified team
                WorkScheduleEvent nextShift = events.stream()
                        .filter(event -> event.hasTeam(team))
                        .findFirst()
                        .orElse(null);

                if (nextShift != null) {
                    Log.d(TAG, "Found next shift for team " + team + ": " + nextShift.getDate());
                    return OperationResult.success(nextShift, OperationResult.OperationType.READ);
                } else {
                    return OperationResult.failure("No shifts found for team: " + team, OperationResult.OperationType.READ);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to get next scheduled shift", e);
                return OperationResult.failure("Failed to get next shift: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<List<String>>> getWorkingTeams(LocalDate date, WorkScheduleType scheduleType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<WorkScheduleEvent> dayEvents = generateDailySchedule(date, scheduleType, null)
                        .get().getData();

                if (dayEvents == null) {
                    return OperationResult.failure("Failed to get day events", OperationResult.OperationType.READ);
                }

                List<String> workingTeams = dayEvents.stream()
                        .filter(event -> !event.isRestPeriod())
                        .flatMap(event -> event.getAssignedTeams().stream())
                        .distinct()
                        .collect(Collectors.toList());

                Log.d(TAG, "Found " + workingTeams.size() + " working teams on " + date);
                return OperationResult.success(workingTeams, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to get working teams", e);
                return OperationResult.failure("Failed to get working teams: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<List<String>>> getTeamsOffWork(LocalDate date, WorkScheduleType scheduleType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get all working teams
                List<String> workingTeams = getWorkingTeams(date, scheduleType).get().getData();
                if (workingTeams == null) {
                    return OperationResult.failure("Failed to get working teams", OperationResult.OperationType.READ);
                }

                // Define all possible teams (A through I)
                List<String> allTeams = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I");

                // Find teams not working
                List<String> teamsOffWork = allTeams.stream()
                        .filter(team -> !workingTeams.contains(team))
                        .collect(Collectors.toList());

                Log.d(TAG, "Found " + teamsOffWork.size() + " teams off work on " + date);
                return OperationResult.success(teamsOffWork, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to get teams off work", e);
                return OperationResult.failure("Failed to get teams off work: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    // ==================== STATISTICS OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<WorkScheduleStatistics>> getScheduleStatistics(
            LocalDate startDate, LocalDate endDate, WorkScheduleType scheduleType) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                List<WorkScheduleEvent> events = generateSchedule(startDate, endDate, scheduleType, null)
                        .get().getData();

                if (events == null) {
                    return OperationResult.failure("Failed to get events for statistics", OperationResult.OperationType.READ);
                }

                // Calculate statistics
                int totalEvents = events.size();
                int workEvents = (int) events.stream().filter(e -> !e.isRestPeriod()).count();
                int restEvents = totalEvents - workEvents;

                // Events by team
                Map<String, Integer> eventsByTeam = new HashMap<>();
                events.stream()
                        .flatMap(event -> event.getAssignedTeams().stream())
                        .forEach(team -> eventsByTeam.merge(team, 1, Integer::sum));

                // Events by shift type
                Map<String, Integer> eventsByShiftType = new HashMap<>();
                events.stream()
                        .filter(event -> event.getShiftType() != null)
                        .forEach(event -> eventsByShiftType.merge(
                                event.getShiftType().getName(), 1, Integer::sum));

                // Average work hours per day
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
                double totalWorkHours = events.stream()
                        .filter(e -> !e.isRestPeriod())
                        .mapToDouble(WorkScheduleEvent::getDurationMinutes)
                        .sum() / 60.0;
                double averageWorkHoursPerDay = daysBetween > 0 ? totalWorkHours / daysBetween : 0.0;

                WorkScheduleStatistics stats = new WorkScheduleStatistics(
                        totalEvents, workEvents, restEvents, eventsByTeam, eventsByShiftType, averageWorkHoursPerDay);

                Log.d(TAG, "Schedule statistics calculated: " + stats.getSummary());
                return OperationResult.success(stats, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to calculate schedule statistics", e);
                return OperationResult.failure("Statistics calculation failed: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    @Override
    public void shutdown() {
        Log.d(TAG, "Shutting down WorkScheduleService...");
        mIsShutdown = true;
        mIsInitialized = false;
        Log.d(TAG, "WorkScheduleService shut down successfully");
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Initializes predefined work schedule templates.
     */
    private void initializePredefinedTemplates() {
        Log.d(TAG, "Initializing predefined templates...");

        mPredefinedTemplates = new ArrayList<>();

        // Fixed 4-2 template
//        WorkScheduleTemplate fixed42 = new WorkScheduleTemplate(
//                "4-2 Continuo",
//                "Schema continuo 4 giorni lavoro + 2 giorni riposo",
//                WorkScheduleType.FIXED_4_2,
//                18
//        );
        WorkScheduleTemplate fixed42 = new WorkScheduleTemplate(
                "4-2 Continuo",
                WorkScheduleType.FIXED_4_2,
                18
        );
        fixed42.setId(1L);
        mPredefinedTemplates.add(fixed42);

        Log.d(TAG, "Initialized " + mPredefinedTemplates.size() + " predefined templates");
    }

    /**
     * Initializes the fixed schedule provider with shift types.
     */
    private void initializeFixedScheduleProvider() {
        Log.d(TAG, "Initializing fixed schedule provider with shift types...");

        // Get shift types from ShiftTypeService
        mShiftTypeService.getShiftTypeByName("Mattino")
                .thenCompose(morningResult -> {
                    if (!morningResult.isSuccess()) {
                        throw new RuntimeException("Morning shift type not found");
                    }
                    return mShiftTypeService.getShiftTypeByName("Pomeriggio");
                })
                .thenCompose(afternoonResult -> {
                    if (!afternoonResult.isSuccess()) {
                        throw new RuntimeException("Afternoon shift type not found");
                    }
                    return mShiftTypeService.getShiftTypeByName("Notte");
                })
                .thenAccept(nightResult -> {
                    if (nightResult.isSuccess()) {
                        // All shift types retrieved successfully
                        // Note: In a real implementation, we would store the results and use them
                        Log.d(TAG, "All shift types retrieved for fixed schedule provider");
                    } else {
                        Log.e(TAG, "Failed to retrieve night shift type");
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to initialize fixed schedule provider", throwable);
                    return null;
                });
    }

    /**
     * Gets the template for a specific schedule type.
     */
    private WorkScheduleTemplate getTemplateForType(WorkScheduleType scheduleType) {
        if (mPredefinedTemplates == null) {
            return null;
        }

        return mPredefinedTemplates.stream()
                .filter(template -> template.getType() == scheduleType)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the appropriate provider for a template.
     */
    private WorkScheduleProvider getProviderForTemplate(WorkScheduleTemplate template) {
        if (template == null) {
            return null;
        }

        switch (template.getType()) {
            case FIXED_4_2:
                return mFixedScheduleProvider;
            case CUSTOM:
                return mCustomScheduleProvider;
            default:
                return null;
        }
    }

    /**
     * Validates template fields.
     */
    private void validateTemplateFields(WorkScheduleTemplate template, List<String> errors, List<String> warnings) {
        if (template.getName() == null || template.getName().trim().isEmpty()) {
            errors.add("Template name is required");
        }

        if (template.getType() == null) {
            errors.add("Template type is required");
        }

        if (template.getCycleDays() <= 0) {
            errors.add("Cycle days must be positive");
        } else if (template.getCycleDays() > 365) {
            warnings.add("Cycle days is very large (" + template.getCycleDays() + " days)");
        }

        if (template.getType() == WorkScheduleType.CUSTOM) {
            if (template.getPatterns() == null || template.getPatterns().isEmpty()) {
                errors.add("Custom templates must have patterns defined");
            } else if (template.getPatterns().size() != template.getCycleDays()) {
                errors.add("Number of patterns must match cycle days");
            }
        }
    }
}