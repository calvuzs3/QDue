/**
 * Calendar Domain Models Package
 *
 * <p>This package contains domain model classes for calendar-related operations within the QDue clean
 * architecture. All models are independent of external frameworks and focus on representing business
 * concepts in a framework-agnostic way.</p>
 *
 * <h2>Package Structure</h2>
 * <pre>
 * net.calvuz.qdue.domain.calendar.models/
 * ├── WorkScheduleDay.java          # Main day model with shifts
 * ├── WorkScheduleShift.java        # Individual shift model
 * └── package-info.java             # This documentation
 * </pre>
 *
 * <h2>Clean Architecture Principles</h2>
 *
 * <h3>Domain Independence</h3>
 * <p>All models in this package are designed to be:</p>
 * <ul>
 *   <li><strong>Framework Independent</strong>: No dependencies on Android, UI, or external libraries</li>
 *   <li><strong>Database Independent</strong>: No knowledge of data persistence implementation</li>
 *   <li><strong>UI Independent</strong>: No coupling to specific presentation requirements</li>
 *   <li><strong>Testable</strong>: Easy to unit test without external dependencies</li>
 * </ul>
 *
 * <h3>Immutability and Thread Safety</h3>
 * <p>Domain models prioritize immutable design for:</p>
 * <ul>
 *   <li><strong>Predictable State</strong>: Objects cannot change unexpectedly</li>
 *   <li><strong>Thread Safety</strong>: Safe to use across multiple threads</li>
 *   <li><strong>Simplified Reasoning</strong>: No hidden state mutations</li>
 *   <li><strong>Cache Friendly</strong>: Safe to cache and reuse instances</li>
 * </ul>
 *
 * <h2>Core Domain Models</h2>
 *
 * <h3>WorkScheduleDay</h3>
 * <p>Central domain model representing a calendar day with work schedule information:</p>
 * <ul>
 *   <li><strong>Date Representation</strong>: {@code LocalDate} for the calendar day</li>
 *   <li><strong>Shift Management</strong>: Collection of {@code WorkScheduleShift} objects</li>
 *   <li><strong>Team Tracking</strong>: Lists of working and off-duty teams</li>
 *   <li><strong>Business Logic</strong>: Methods for team queries and shift analysis</li>
 *   <li><strong>Builder Pattern</strong>: Fluent API for instance construction</li>
 *   <li><strong>Template Support</strong>: Cloning capabilities for pattern generation</li>
 * </ul>
 *
 * <h3>WorkScheduleShift</h3>
 * <p>Represents individual work shifts within a day:</p>
 * <ul>
 *   <li><strong>Shift Types</strong>: Morning, Afternoon, Night enumeration</li>
 *   <li><strong>Time Management</strong>: Start and end times with midnight handling</li>
 *   <li><strong>Team Assignment</strong>: Teams assigned to work the shift</li>
 *   <li><strong>Duration Calculation</strong>: Automatic shift length computation</li>
 *   <li><strong>Display Support</strong>: Methods for UI presentation</li>
 *   <li><strong>Factory Methods</strong>: Standard shift creation utilities</li>
 * </ul>
 *
 * <h2>Usage Patterns</h2>
 *
 * <h3>Creating WorkScheduleDay</h3>
 * <pre>
 * {@code
 * // Using builder pattern
 * WorkScheduleDay day = WorkScheduleDay.builder(LocalDate.now())
 *     .addShift(WorkScheduleShift.createMorningShift()
 *         .toBuilder()
 *         .addTeam("A")
 *         .addTeam("B")
 *         .build())
 *     .addOffWorkTeam("C")
 *     .build();
 *
 * // Query operations
 * boolean hasWork = day.hasShifts();
 * boolean teamWorking = day.isTeamWorking("A");
 * int shiftIndex = day.findTeamShiftIndex("B");
 * }
 * </pre>
 *
 * <h3>Creating WorkScheduleShift</h3>
 * <pre>
 * {@code
 * // Standard shifts
 * WorkScheduleShift morning = WorkScheduleShift.createMorningShift();
 * WorkScheduleShift afternoon = WorkScheduleShift.createAfternoonShift();
 * WorkScheduleShift night = WorkScheduleShift.createNightShift();
 *
 * // Custom shift
 * WorkScheduleShift custom = WorkScheduleShift.builder(ShiftType.AFTERNOON)
 *     .startTime(LocalTime.of(15, 0))
 *     .endTime(LocalTime.of(23, 0))
 *     .addTeam("X")
 *     .addTeam("Y")
 *     .description("Extended afternoon shift")
 *     .build();
 *
 * // Query operations
 * long duration = custom.getDurationMinutes();
 * boolean spansNext = custom.spansMidnight();
 * String timeRange = custom.getTimeRange();
 * }
 * </pre>
 *
 * <h2>Integration with Legacy Models</h2>
 *
 * <h3>QuattroDue Compatibility</h3>
 * <p>Domain models are designed for smooth migration from legacy QuattroDue models:</p>
 * <ul>
 *   <li><strong>Day Conversion</strong>: Repository layer converts {@code Day} to {@code WorkScheduleDay}</li>
 *   <li><strong>Shift Mapping</strong>: Legacy {@code Shift} objects mapped to {@code WorkScheduleShift}</li>
 *   <li><strong>Team Translation</strong>: {@code HalfTeam} names become team name strings</li>
 *   <li><strong>Pattern Preservation</strong>: QuattroDue 4-2 pattern logic maintained</li>
 * </ul>
 *
 * <h3>Conversion Strategy</h3>
 * <p>The repository implementation handles conversion transparently:</p>
 * <pre>
 * {@code
 * // Repository converts automatically
 * CompletableFuture<OperationResult<WorkScheduleDay>> future =
 *     repository.getWorkScheduleForDate(LocalDate.now(), userId);
 *
 * // Clean domain model received
 * WorkScheduleDay day = future.join().getData();
 *
 * // Use domain methods
 * List<WorkScheduleShift> shifts = day.getShifts();
 * }
 * </pre>
 *
 * <h2>Design Patterns</h2>
 *
 * <h3>Builder Pattern</h3>
 * <p>Both models support builder pattern for flexible construction:</p>
 * <ul>
 *   <li><strong>Fluent API</strong>: Method chaining for readable construction</li>
 *   <li><strong>Validation</strong>: Build-time validation of required fields</li>
 *   <li><strong>Immutability</strong>: Builders create immutable instances</li>
 *   <li><strong>Flexibility</strong>: Optional fields with sensible defaults</li>
 * </ul>
 *
 * <h3>Factory Methods</h3>
 * <p>Convenient creation methods for common scenarios:</p>
 * <ul>
 *   <li><strong>Standard Shifts</strong>: Pre-configured morning/afternoon/night shifts</li>
 *   <li><strong>Default Values</strong>: Reasonable defaults for typical use cases</li>
 *   <li><strong>Type Safety</strong>: Compile-time safety for shift types</li>
 * </ul>
 *
 * <h3>Value Objects</h3>
 * <p>Models implement value object semantics:</p>
 * <ul>
 *   <li><strong>Equality by Value</strong>: Objects equal if contents are equal</li>
 *   <li><strong>Hash Code Consistency</strong>: Reliable hashing for collections</li>
 *   <li><strong>String Representation</strong>: Meaningful toString() implementations</li>
 *   <li><strong>Cloning Support</strong>: Deep copying for template operations</li>
 * </ul>
 *
 * <h2>Validation and Error Handling</h2>
 *
 * <h3>Input Validation</h3>
 * <p>Models validate inputs at construction time:</p>
 * <ul>
 *   <li><strong>Null Safety</strong>: {@code @NonNull} annotations and null checks</li>
 *   <li><strong>Range Validation</strong>: Time and date range verification</li>
 *   <li><strong>Business Rules</strong>: Domain-specific validation logic</li>
 *   <li><strong>Clear Errors</strong>: Descriptive exception messages</li>
 * </ul>
 *
 * <h3>Defensive Programming</h3>
 * <p>Models are designed to be robust:</p>
 * <ul>
 *   <li><strong>Immutable Collections</strong>: Defensive copying of mutable inputs</li>
 *   <li><strong>Boundary Checks</strong>: Array and list bounds validation</li>
 *   <li><strong>Type Safety</strong>: Strong typing with enums where appropriate</li>
 *   <li><strong>Fail Fast</strong>: Early detection of invalid states</li>
 * </ul>
 *
 * <h2>Performance Considerations</h2>
 *
 * <h3>Memory Efficiency</h3>
 * <ul>
 *   <li><strong>Cached Values</strong>: Expensive computations cached at construction</li>
 *   <li><strong>Shared Immutables</strong>: Safe to share instances across contexts</li>
 *   <li><strong>Lazy Loading</strong>: Complex operations computed on demand</li>
 *   <li><strong>Collection Optimization</strong>: Efficient collection implementations</li>
 * </ul>
 *
 * <h3>Computation Efficiency</h3>
 * <ul>
 *   <li><strong>Duration Calculation</strong>: Optimized time arithmetic</li>
 *   <li><strong>String Operations</strong>: Efficient string building and caching</li>
 *   <li><strong>Comparison Operations</strong>: Fast equality and hash code computation</li>
 * </ul>
 *
 * <h2>Testing Strategy</h2>
 *
 * <h3>Unit Testing</h3>
 * <p>Models are designed for comprehensive testing:</p>
 * <ul>
 *   <li><strong>No Dependencies</strong>: No mocking required for unit tests</li>
 *   <li><strong>Deterministic Behavior</strong>: Predictable outputs for given inputs</li>
 *   <li><strong>Edge Case Coverage</strong>: Boundary conditions easily testable</li>
 *   <li><strong>State Verification</strong>: Clear ways to verify object state</li>
 * </ul>
 *
 * <h3>Property-Based Testing</h3>
 * <p>Immutable models are ideal for property testing:</p>
 * <ul>
 *   <li><strong>Invariant Testing</strong>: Business rules always hold</li>
 *   <li><strong>Round-Trip Testing</strong>: Serialization/deserialization consistency</li>
 *   <li><strong>Transformation Testing</strong>: Builder operations preserve invariants</li>
 * </ul>
 *
 * <h2>Internationalization</h2>
 *
 * <p>All user-facing strings should be externalized using the
 * {@code net.calvuz.qdue.core.architecture.common.i18n} package. Display methods
 * in domain models provide raw data that can be formatted by presentation layer.</p>
 *
 * <h2>Migration Guide</h2>
 *
 * <h3>From QuattroDue Models</h3>
 * <p>When migrating code that uses legacy models:</p>
 * <ol>
 *   <li><strong>Repository Layer</strong>: Use new repository interface returning domain models</li>
 *   <li><strong>Use Case Layer</strong>: Update business logic to use domain model methods</li>
 *   <li><strong>Presentation Layer</strong>: Adapt UI code to domain model API</li>
 *   <li><strong>Testing</strong>: Update tests to use new model constructors</li>
 * </ol>
 *
 * <h3>Gradual Migration</h3>
 * <p>Migration can be done incrementally:</p>
 * <ul>
 *   <li><strong>Dual Support</strong>: Repository provides both legacy and domain models</li>
 *   <li><strong>Conversion Utilities</strong>: Helper methods for model transformation</li>
 *   <li><strong>Compatibility Layer</strong>: Temporary adapters during transition</li>
 * </ul>
 *
 * <h2>Future Extensions</h2>
 *
 * <h3>Planned Enhancements</h3>
 * <ul>
 *   <li><strong>Validation Framework</strong>: Pluggable validation system</li>
 *   <li><strong>Serialization Support</strong>: JSON/XML serialization capabilities</li>
 *   <li><strong>Query DSL</strong>: Domain-specific query language for complex searches</li>
 *   <li><strong>Event Sourcing</strong>: Event-based state change tracking</li>
 * </ul>
 *
 * <h2>See Also</h2>
 * <ul>
 *   <li>{@link net.calvuz.qdue.domain.calendar.repositories} - Repository interfaces</li>
 *   <li>{@link net.calvuz.qdue.domain.calendar.usecases} - Business use cases</li>
 *   <li>{@link net.calvuz.qdue.data.repositories} - Repository implementations</li>
 *   <li>{@link net.calvuz.qdue.core.architecture.common.i18n} - Internationalization</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Implementation
 * @since Database Version 6
 */
package net.calvuz.qdue.domain.calendar.models;