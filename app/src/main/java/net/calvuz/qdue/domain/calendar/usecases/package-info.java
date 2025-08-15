/**
 * Calendar Use Cases Package
 *
 * <p>Contains business use cases for calendar and work schedule operations.
 * Each use case represents a single business operation and encapsulates
 * the associated business rules and validation logic.</p>
 *
 * <h3>Available Use Cases:</h3>
 * <ul>
 *   <li>{@link net.calvuz.qdue.domain.calendar.usecases.GenerateUserScheduleUseCase} - Advanced user schedule generation</li>
 *   <li>{@link net.calvuz.qdue.domain.calendar.usecases.GenerateTeamScheduleUseCase} - Team coordination and management</li>
 *   <li>{@link net.calvuz.qdue.domain.calendar.usecases.ApplyShiftExceptionsUseCase} - Exception handling workflow</li>
 *   <li>{@link net.calvuz.qdue.domain.calendar.usecases.GetScheduleStatsUseCase} - Analytics and validation</li>
 * </ul>
 *
 * <h3>Single User Application:</h3>
 * <p>This application is designed for single user usage. The {@link net.calvuz.qdue.domain.calendar.usecases.GenerateUserScheduleUseCase}
 * provides comprehensive month and date range operations for work schedule generation with a default user approach.</p>
 *
 * <h3>Usage Patterns:</h3>
 * <pre>
 * // Direct usage for month operations
 * GenerateUserScheduleUseCase useCase = new GenerateUserScheduleUseCase(repository);
 * CompletableFuture&lt;OperationResult&lt;Map&lt;LocalDate, WorkScheduleDay&gt;&gt;&gt; result =
 *     useCase.executeForMonth(userId, YearMonth.now());
 *
 * // Factory usage (recommended)
 * UseCaseFactory factory = new UseCaseFactory(repository);
 * GenerateUserScheduleUseCase useCase = factory.getUserScheduleUseCase();
 *
 * // Single user with default ID
 * Long defaultUserId = 1L;
 * CompletableFuture&lt;OperationResult&lt;Map&lt;LocalDate, WorkScheduleDay&gt;&gt;&gt; monthResult =
 *     useCase.executeForMonth(defaultUserId, YearMonth.now());
 *
 * // Date range operations
 * CompletableFuture&lt;OperationResult&lt;Map&lt;LocalDate, WorkScheduleDay&gt;&gt;&gt; rangeResult =
 *     useCase.executeForDateRange(defaultUserId, startDate, endDate);
 *
 * // Single date operations
 * CompletableFuture&lt;OperationResult&lt;WorkScheduleDay&gt;&gt; dayResult =
 *     useCase.executeForDate(defaultUserId, LocalDate.now());
 * </pre>
 *
 * <h3>Calendar Integration:</h3>
 * <p>For calendar-like applications, the {@link net.calvuz.qdue.domain.calendar.usecases.GenerateUserScheduleUseCase}
 * provides the following key methods:</p>
 * <ul>
 *   <li><strong>executeForMonth(Long, YearMonth)</strong>: Get all work schedule days for a specific month</li>
 *   <li><strong>executeForDateRange(Long, LocalDate, LocalDate)</strong>: Get work schedule for custom date ranges</li>
 *   <li><strong>executeForDate(Long, LocalDate)</strong>: Get work schedule for a single date</li>
 * </ul>
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li><strong>Single Responsibility</strong>: Each use case handles one business operation</li>
 *   <li><strong>Business Rules</strong>: Input validation and business logic centralized</li>
 *   <li><strong>Async by Default</strong>: All operations return CompletableFuture</li>
 *   <li><strong>Error Handling</strong>: Consistent OperationResult pattern</li>
 *   <li><strong>Testability</strong>: Easy to unit test with mock repositories</li>
 *   <li><strong>Single User Optimized</strong>: Streamlined for single user applications</li>
 * </ul>
 *
 * <h3>Clean Architecture Compliance:</h3>
 * <ul>
 *   <li><strong>Repository Pattern</strong>: All use cases depend on repository interfaces</li>
 *   <li><strong>Dependency Injection</strong>: Constructor-based DI with UseCaseFactory</li>
 *   <li><strong>Domain Logic Isolation</strong>: Business rules encapsulated in use cases</li>
 *   <li><strong>Infrastructure Independence</strong>: No direct infrastructure dependencies</li>
 * </ul>
 *
 * <h3>UI Integration Example:</h3>
 * <pre>
 * // SwipeCalendarModule usage
 * SwipeCalendarModule module = new SwipeCalendarModule(context, eventsService,
 *     userService, workScheduleRepository);
 *
 * // Get use case for calendar operations
 * GenerateUserScheduleUseCase scheduleUseCase = module.getUserScheduleUseCase();
 *
 * // Load month data for calendar display
 * scheduleUseCase.executeForMonth(module.getCurrentUserId(), YearMonth.now())
 *     .thenAccept(result -> {
 *         if (result.isSuccess()) {
 *             Map&lt;LocalDate, WorkScheduleDay&gt; monthSchedule = result.getData();
 *             // Update calendar UI with work schedule data
 *         }
 *     });
 * </pre>
 *
 * @author QDue Development Team
 * @version 1.1.0 - Single User Implementation
 * @since Clean Architecture Migration
 */
package net.calvuz.qdue.domain.calendar.usecases;