/**
 * Domain Calendar Package - Clean Architecture Implementation for QDue Work Schedule Management.
 *
 * <p>This package implements a clean architecture domain layer for calendar and work schedule
 * management, independent from external frameworks and legacy QuattroDue dependencies.
 * The domain models follow pure business logic principles with no external dependencies.</p>
 *
 * <h3>Clean Architecture Principles:</h3>
 * <ul>
 *   <li><strong>Independence</strong>: No dependencies on frameworks, UI, or database</li>
 *   <li><strong>Testability</strong>: Pure domain models easily unit testable</li>
 *   <li><strong>Business Logic Focus</strong>: Models represent core business concepts</li>
 *   <li><strong>Immutable Design</strong>: Thread-safe value objects and entities</li>
 * </ul>
 *
 * <h3>Domain Models Overview:</h3>
 *
 * <h4>{@link net.calvuz.qdue.domain.calendar.models.Shift} - Shift Type Definitions</h4>
 * <p>
 * Core domain model for shift type definitions (formerly ShiftType). Represents
 * the template and configuration for different types of work shifts with complete
 * timing, break, and display properties.
 * </p>
 * <ul>
 *   <li><strong>Timing Management</strong>: Start/end times with midnight crossing support</li>
 *   <li><strong>Break Management</strong>: Optional break time with inclusion flexibility</li>
 *   <li><strong>Visual Properties</strong>: Color management for calendar display</li>
 *   <li><strong>Duration Calculation</strong>: Intelligent work vs total duration</li>
 *   <li><strong>UUID Identification</strong>: Unique identification for database persistence</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.domain.calendar.models.Team} - Work Team Management</h4>
 * <p>
 * Represents work teams in the schedule system with full metadata and business logic.
 * Designed to replace the legacy HalfTeam concept with a more robust team model.
 * </p>
 * <ul>
 *   <li><strong>Immutable Design</strong>: Thread-safe team definitions</li>
 *   <li><strong>Business Identity</strong>: Proper entity semantics with ID and name</li>
 *   <li><strong>Display Support</strong>: Methods for UI presentation</li>
 *   <li><strong>Builder Pattern</strong>: Fluent API for team creation</li>
 *   <li><strong>Standard Teams</strong>: Factory methods for QuattroDue teams (A-I)</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.domain.calendar.models.WorkScheduleShift} - Daily Shift Instances</h4>
 * <p>
 * Represents actual shift instances within a specific day, linking teams to shift types
 * with timing information. Uses domain Shift.ShiftType for categorization.
 * </p>
 * <ul>
 *   <li><strong>Team Assignment</strong>: Multiple teams per shift support</li>
 *   <li><strong>Timing Information</strong>: Start/end times and duration calculation</li>
 *   <li><strong>Midnight Crossing</strong>: Support for shifts spanning midnight</li>
 *   <li><strong>Display Methods</strong>: UI-friendly formatting and summaries</li>
 *   <li><strong>Cloneable Support</strong>: For template-based generation</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.domain.calendar.models.WorkScheduleDay} - Daily Schedule Container</h4>
 * <p>
 * Represents a complete calendar day with all associated work shifts and team assignments.
 * Equivalent to the legacy Day model but with clean architecture principles.
 * </p>
 * <ul>
 *   <li><strong>Multi-Shift Support</strong>: Contains multiple shifts per day</li>
 *   <li><strong>Off-Work Tracking</strong>: Teams that are not working on the day</li>
 *   <li><strong>Business Logic</strong>: Team working status and shift lookup</li>
 *   <li><strong>Calendar Integration</strong>: Display methods for calendar views</li>
 *   <li><strong>Template Support</strong>: Cloning for pattern-based generation</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.domain.calendar.models.WorkScheduleEvent} - Calendar Event Integration</h4>
 * <p>
 * Volatile events generated from work schedule calculations for calendar integration.
 * These events are not persisted but generated on-demand for display purposes.
 * </p>
 * <ul>
 *   <li><strong>Volatile Nature</strong>: Generated on-demand, not stored</li>
 *   <li><strong>Calendar Integration</strong>: Designed for calendar display</li>
 *   <li><strong>Pattern Metadata</strong>: Includes cycle position and pattern info</li>
 *   <li><strong>User Context</strong>: User-specific relevance and filtering</li>
 *   <li><strong>Display Properties</strong>: Title, description, and color information</li>
 * </ul>
 *
 * <h3>Repository Pattern:</h3>
 *
 * <h4>{@link net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository} - Data Access Interface</h4>
 * <p>
 * Clean architecture repository interface defining all work schedule data operations.
 * Completely independent from external frameworks and legacy dependencies.
 * </p>
 * <ul>
 *   <li><strong>Async Operations</strong>: CompletableFuture-based operations</li>
 *   <li><strong>OperationResult Pattern</strong>: Consistent error handling</li>
 *   <li><strong>Domain Model Usage</strong>: Only domain models in interfaces</li>
 *   <li><strong>Business Operations</strong>: High-level business logic methods</li>
 * </ul>
 *
 * <h3>QuattroDue Pattern Implementation:</h3>
 *
 * <h4>Pattern Details:</h4>
 * <ul>
 *   <li><strong>18-Day Cycle</strong>: Complete rotation cycle for all teams</li>
 *   <li><strong>4-2 Pattern</strong>: 4 consecutive work days, 2 rest days</li>
 *   <li><strong>9 Teams</strong>: Teams A through I in rotation</li>
 *   <li><strong>3 Shifts per Day</strong>: Morning, Afternoon, Night shifts</li>
 * </ul>
 *
 * <h4>Independent Algorithm:</h4>
 * <p>
 * The domain implementation includes a completely independent QuattroDue pattern
 * calculation algorithm that does not depend on the legacy QuattroDue singleton.
 * This ensures clean separation and testability.
 * </p>
 *
 * <h3>Integration Strategy:</h3>
 *
 * <h4>Backward Compatibility:</h4>
 * <p>
 * The {@link net.calvuz.qdue.core.services.impl.WorkScheduleServiceAdapter} provides
 * a bridge between the legacy WorkScheduleService interface and the new clean
 * architecture repository, ensuring existing UI code continues to work unchanged.
 * </p>
 *
 * <h4>Model Conversions:</h4>
 * <ul>
 *   <li>Domain WorkScheduleDay ↔ Legacy Day</li>
 *   <li>Domain Team ↔ Legacy Team + HalfTeam</li>
 *   <li>Domain Shift ↔ Legacy ShiftType</li>
 *   <li>Domain WorkScheduleEvent ↔ Legacy WorkScheduleEvent</li>
 * </ul>
 *
 * <h3>Configuration and Preferences:</h3>
 *
 * <h4>Pattern Configuration:</h4>
 * <ul>
 *   <li><strong>Scheme Start Date</strong>: Reference date for pattern calculations</li>
 *   <li><strong>Team Configuration</strong>: Standard teams A through I</li>
 *   <li><strong>Shift Configuration</strong>: Standard shift types and timing</li>
 *   <li><strong>Preference Integration</strong>: Uses {@link net.calvuz.qdue.preferences} package</li>
 * </ul>
 *
 * <h4>Internationalization:</h4>
 * <ul>
 *   <li><strong>I18n Support</strong>: Uses {@link net.calvuz.qdue.core.common.i18n} for localization</li>
 *   <li><strong>String Resources</strong>: All user-facing strings externalized</li>
 *   <li><strong>Cultural Adaptation</strong>: Date/time formatting based on locale</li>
 * </ul>
 *
 * <h3>Testing Strategy:</h3>
 *
 * <h4>Unit Testing:</h4>
 * <ul>
 *   <li><strong>Pure Domain Models</strong>: Easily testable without mocking</li>
 *   <li><strong>Business Logic Testing</strong>: Pattern calculations and validations</li>
 *   <li><strong>Builder Pattern Testing</strong>: Fluent API validation</li>
 * </ul>
 *
 * <h4>Integration Testing:</h4>
 * <ul>
 *   <li><strong>Repository Testing</strong>: Data access and caching behavior</li>
 *   <li><strong>Service Adapter Testing</strong>: Legacy compatibility verification</li>
 *   <li><strong>Pattern Algorithm Testing</strong>: QuattroDue cycle calculations</li>
 * </ul>
 *
 * <h3>Performance Considerations:</h3>
 *
 * <h4>Caching Strategy:</h4>
 * <ul>
 *   <li><strong>Schedule Caching</strong>: Calculated days cached by date</li>
 *   <li><strong>Team Caching</strong>: Standard teams cached in memory</li>
 *   <li><strong>Configuration Caching</strong>: Pattern settings cached</li>
 * </ul>
 *
 * <h4>Memory Management:</h4>
 * <ul>
 *   <li><strong>Immutable Objects</strong>: Reduced memory allocations</li>
 *   <li><strong>Cached Calculations</strong>: Expensive operations cached</li>
 *   <li><strong>Lazy Loading</strong>: Services initialized on demand</li>
 * </ul>
 *
 * <h3>Future Extensions:</h3>
 *
 * <h4>Planned Enhancements:</h4>
 * <ul>
 *   <li><strong>Custom Patterns</strong>: Support for patterns other than 4-2</li>
 *   <li><strong>Shift Templates</strong>: User-configurable shift definitions</li>
 *   <li><strong>Team Hierarchies</strong>: Department and role-based team organization</li>
 *   <li><strong>Exception Handling</strong>: Individual schedule overrides and exceptions</li>
 * </ul>
 *
 * <h4>Database Integration:</h4>
 * <ul>
 *   <li><strong>Shift Persistence</strong>: Database storage for custom shift types</li>
 *   <li><strong>Team Management</strong>: CRUD operations for team definitions</li>
 *   <li><strong>User Assignments</strong>: Persistent user-team associations</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 2.0.0 - Clean Architecture Implementation
 * @since Database Version 6
 */
package net.calvuz.qdue.domain.calendar;