/**
 * Calendar Use Cases Package
 *
 * <p>Contains business use cases for calendar and work schedule operations.
 * Each use case represents a single business operation and encapsulates
 * the associated business rules and validation logic.</p>
 *
 * <h3>Available Use Cases:</h3>
 * <ul>
 *   <li>{@link GetWorkScheduleForMonthUseCase} - Get work schedule for entire month</li>
 *   <li>{@link GetWorkScheduleForDateUseCase} - Get work schedule for specific date</li>
 *   <li>{@link IsWorkingDayUseCase} - Check if date is working day</li>
 *   <li>{@link GetUserWorkScheduleUseCase} - Get user-specific work schedule</li>
 * </ul>
 *
 * <h3>Usage Patterns:</h3>
 * <pre>
 * // Direct usage
 * GetWorkScheduleForMonthUseCase useCase = new GetWorkScheduleForMonthUseCase(repository);
 * CompletableFuture&lt;OperationResult&lt;Map&lt;LocalDate, Day&gt;&gt;&gt; result = useCase.execute(YearMonth.now());
 *
 * // Factory usage (recommended)
 * UseCaseFactory factory = new UseCaseFactory(repository);
 * GetWorkScheduleForMonthUseCase useCase = factory.getWorkScheduleForMonthUseCase();
 * </pre>
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li><strong>Single Responsibility</strong>: Each use case handles one business operation</li>
 *   <li><strong>Business Rules</strong>: Input validation and business logic centralized</li>
 *   <li><strong>Async by Default</strong>: All operations return CompletableFuture</li>
 *   <li><strong>Error Handling</strong>: Consistent OperationResult pattern</li>
 *   <li><strong>Testability</strong>: Easy to unit test with mock repositories</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since Clean Architecture Migration
 */
package net.calvuz.qdue.domain.calendar.usecases;