/**
 * Enhanced Calendar Domain Package - Google Calendar-style Scheduling with QuattroDue Integration
 *
 * <p>This package implements a comprehensive calendar and work schedule management system
 * following clean architecture principles. The domain models support Google Calendar-style
 * recurrence patterns (RRULE), advanced exception handling, and multi-user scheduling
 * while maintaining compatibility with QuattroDue 4-2 cycle patterns.</p>
 *
 * <h2>🏗️ Architecture Overview</h2>
 *
 * <h3>Domain Models Hierarchy</h3>
 * <pre>
 * Domain Models
 * ├── Core Schedule Models (Phase 1)
 * │   ├── WorkScheduleDay         # Daily schedule with shifts
 * │   ├── WorkScheduleShift       # Individual shift instances
 * │   ├── Team                    # Work team definitions
 * │   ├── Shift                   # Shift type templates
 * │   ├── RecurrenceRule          # Google Calendar-style RRULE patterns
 * │   ├── ShiftException          # Schedule exceptions and modifications
 * │   ├── UserScheduleAssignment  # User-to-team assignments
 * │   └── WorkScheduleEvent       # Volatile calendar events
 * └── Future Extensions (Phase 3)
 *     ├── ScheduleTemplate        # Reusable schedule patterns
 *     ├── ScheduleConflict        # Conflict detection and resolution
 *     └── ScheduleApproval        # Approval workflow models
 * </pre>
 *
 * <h3>Integration Points</h3>
 * <ul>
 *   <li><strong>CalendarDatabase</strong>: Persistence layer with Room entities</li>
 *   <li><strong>WorkScheduleRepository</strong>: Data access abstraction</li>
 *   <li><strong>QuattroDueCalendarEngine</strong>: Calculation engine (NEW)</li>
 *   <li><strong>RecurrenceCalculator</strong>: RRULE processing (NEW)</li>
 *   <li><strong>ExceptionResolver</strong>: Exception application (NEW)</li>
 * </ul>
 *
 * <h2>🌍 Universal Internationalization (i18n) Pattern</h2>
 *
 * <h3>Clean Architecture Localization</h3>
 * <p>
 * All domain models in this package follow the Universal Domain Localization Pattern
 * from {@code net.calvuz.qdue.domain.common} package. This pattern ensures complete
 * independence from Android framework while providing flexible localization support.
 * </p>
 *
 * <h4>Key Components:</h4>
 * <ul>
 *   <li><strong>LocalizableDomainModel</strong>: Base class with localization infrastructure</li>
 *   <li><strong>DomainLocalizer</strong>: Optional localization interface (dependency injection)</li>
 *   <li><strong>LocalizableBuilder</strong>: Builder pattern with localization support</li>
 *   <li><strong>Localizable</strong>: Marker interface for localizable models</li>
 * </ul>
 *
 * <h4>Implementation Pattern:</h4>
 * <pre>
 * public class RecurrenceRule extends LocalizableDomainModel {
 *
 *     private static final String LOCALIZATION_SCOPE = "recurrence";
 *
 *     private RecurrenceRule(Builder builder) {
 *         super(builder.mLocalizer, LOCALIZATION_SCOPE);
 *         // ... field assignments
 *     }
 *
 *     // Localized display methods with fallback
 *     &#64;NonNull
 *     public String getDisplayName() {
 *         if (name != null && !name.trim().isEmpty()) {
 *             return name;
 *         }
 *         return localize("frequency." + frequency.name().toLowerCase(),
 *                        frequency.name(), // fallback
 *                        frequency.name());
 *     }
 *
 *     // Factory methods with optional localizer
 *     &#64;NonNull
 *     public static RecurrenceRule createQuattroDueCycle(&#64;NonNull LocalDate startDate) {
 *         return createQuattroDueCycle(startDate, null);
 *     }
 *
 *     &#64;NonNull
 *     public static RecurrenceRule createQuattroDueCycle(&#64;NonNull LocalDate startDate,
 *                                                        &#64;Nullable DomainLocalizer localizer) {
 *         return builder()
 *                 .frequency(Frequency.QUATTRODUE_CYCLE)
 *                 .startDate(startDate)
 *                 .localizer(localizer)
 *                 .build();
 *     }
 *
 *     // Localizable implementation
 *     &#64;Override
 *     &#64;NonNull
 *     public RecurrenceRule withLocalizer(&#64;NonNull DomainLocalizer localizer) {
 *         return builder().copyFrom(this).localizer(localizer).build();
 *     }
 *
 *     // Builder extends LocalizableBuilder
 *     public static class Builder extends LocalizableBuilder&lt;RecurrenceRule, Builder&gt; {
 *         // ... implementation
 *     }
 * }
 * </pre>
 *
 * <h4>Localization Key Conventions:</h4>
 * <pre>
 * // Pattern: {scope}.{category}.{item}
 * "recurrence.frequency.daily"           // → "Giornaliero"
 * "recurrence.frequency.weekly"          // → "Settimanale"
 * "recurrence.end.never"                 // → "Mai"
 * "shift_exception.type.vacation"        // → "Ferie"
 * "shift_exception.status.pending"       // → "In attesa"
 * "user_assignment.priority.high"        // → "Alta"
 * </pre>
 *
 * <h4>Usage in Presentation Layer:</h4>
 * <pre>
 * // DI Module provides localized domain objects
 * public class CalendarModule {
 *     private final DomainLocalizer mLocalizer;
 *
 *     public CalendarModule(Context context) {
 *         this.mLocalizer = new DomainLocalizerImpl(context, null);
 *     }
 *
 *     public RecurrenceRule createLocalizedQuattroDueCycle(LocalDate startDate) {
 *         return RecurrenceRule.createQuattroDueCycle(startDate, mLocalizer);
 *     }
 * }
 * </pre>
 *
 * <h3>Benefits of Universal Pattern</h3>
 * <ul>
 *   <li><strong>Clean Architecture Compliance</strong>: Zero framework dependencies in domain</li>
 *   <li><strong>Optional Localization</strong>: Domain models work with or without i18n</li>
 *   <li><strong>Testability</strong>: Complete isolation for unit testing</li>
 *   <li><strong>Dependency Injection</strong>: Integrates with existing ServiceProvider pattern</li>
 *   <li><strong>Performance</strong>: Lazy loading and scoped localizers</li>
 *   <li><strong>Consistency</strong>: Uniform pattern across all domain models</li>
 * </ul>
 *
 * <h2>📄 Recurrence System Design</h2>
 *
 * <h3>RecurrenceRule - Google Calendar RRULE Support</h3>
 * <p>
 * Implements RFC 5545 RRULE specification with extensions for QuattroDue patterns:
 * </p>
 * <ul>
 *   <li><strong>Standard Frequencies</strong>: DAILY, WEEKLY, MONTHLY, YEARLY</li>
 *   <li><strong>QuattroDue Extension</strong>: QUATTRODUE_CYCLE for 4-2 patterns</li>
 *   <li><strong>Complex Patterns</strong>: BYDAY, BYMONTHDAY, INTERVAL support</li>
 *   <li><strong>End Conditions</strong>: NEVER, COUNT, UNTIL_DATE</li>
 * </ul>
 *
 * <h4>RRULE Examples:</h4>
 * <pre>
 * // Every weekday: FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR
 * // QuattroDue 4-2: FREQ=QUATTRODUE_CYCLE;INTERVAL=1
 * // Every 2nd Monday: FREQ=WEEKLY;INTERVAL=2;BYDAY=MO
 * // First Monday of month: FREQ=MONTHLY;BYDAY=1MO
 * </pre>
 *
 * <h3>ShiftException - Comprehensive Exception Handling</h3>
 * <p>
 * Handles all deviations from standard schedule patterns with business rule support:
 * </p>
 * <ul>
 *   <li><strong>Absence Types</strong>: Vacation, sick leave, special leave</li>
 *   <li><strong>Change Types</strong>: Company changes, colleague swaps, special coverage</li>
 *   <li><strong>Reduction Types</strong>: Personal reductions, ROL, union activities</li>
 *   <li><strong>Approval Workflow</strong>: Status tracking and approval chain</li>
 *   <li><strong>Priority System</strong>: Conflict resolution with priority levels</li>
 * </ul>
 *
 * <h4>Exception Processing Order:</h4>
 * <ol>
 *   <li><strong>Base Schedule</strong>: Calculate from RecurrenceRule + Team assignment</li>
 *   <li><strong>Apply Exceptions</strong>: Process ShiftExceptions by priority</li>
 *   <li><strong>Resolve Conflicts</strong>: Handle overlapping exceptions</li>
 *   <li><strong>Generate Final Schedule</strong>: Create WorkScheduleDay objects</li>
 * </ol>
 *
 * <h3>UserScheduleAssignment - Multi-User Team Management</h3>
 * <p>
 * Links users to teams and recurrence patterns with time boundaries:
 * </p>
 * <ul>
 *   <li><strong>User-Team Mapping</strong>: Assign users to specific teams</li>
 *   <li><strong>Time Boundaries</strong>: Support temporary and permanent assignments</li>
 *   <li><strong>Priority System</strong>: Handle conflicting assignments</li>
 *   <li><strong>Team Transfers</strong>: Manage team changes over time</li>
 * </ul>
 *
 * <h2>🚀 Performance Strategy</h2>
 *
 * <h3>Cache Architecture</h3>
 * <ul>
 *   <li><strong>L1 Cache</strong>: In-memory WorkScheduleDay cache (hot data)</li>
 *   <li><strong>L2 Cache</strong>: Calculated patterns cache (warm data)</li>
 *   <li><strong>L3 Cache</strong>: Database persistence layer (cold data)</li>
 * </ul>
 *
 * <h3>Pre-calculation Strategy</h3>
 * <ul>
 *   <li><strong>Pattern Pre-calc</strong>: Calculate base patterns for next 6 months</li>
 *   <li><strong>Exception Hot-calc</strong>: Apply exceptions in real-time</li>
 *   <li><strong>User Schedule Cache</strong>: Cache user-specific schedules</li>
 *   <li><strong>Background Refresh</strong>: Periodic cache refresh for future dates</li>
 * </ul>
 *
 * <h2>🔧 Migration Strategy</h2>
 *
 * <h3>Phase 1: Foundation (COMPLETED)</h3>
 * <ul>
 *   <li>✅ Core domain models (WorkScheduleDay, Team, Shift)</li>
 *   <li>✅ Basic WorkScheduleRepository interface</li>
 *   <li>✅ CalendarDatabase with entities</li>
 *   <li>✅ QuattroDue bridge implementation</li>
 * </ul>
 *
 * <h3>Phase 2: Recurrence & Exceptions (CURRENT)</h3>
 * <ul>
 *   <li>✅ Enhanced domain models (RecurrenceRule, ShiftException, UserScheduleAssignment)</li>
 *   <li>✅ Universal localization pattern implementation</li>
 *   <li>🚧 Database extensions for new entities</li>
 *   <li>🚧 QuattroDueCalendarEngine implementation</li>
 *   <li>🚧 RecurrenceCalculator with RRULE support</li>
 *   <li>🚧 ExceptionResolver for applying exceptions</li>
 * </ul>
 *
 * <h3>Phase 3: Advanced Features (PLANNED)</h3>
 * <ul>
 *   <li>🔮 ScheduleTemplate for reusable patterns</li>
 *   <li>🔮 Advanced conflict detection and resolution</li>
 *   <li>🔮 Approval workflow system</li>
 *   <li>🔮 Schedule optimization algorithms</li>
 *   <li>🔮 Integration with external calendar systems</li>
 * </ul>
 *
 * <h2>🎯 Implementation Roadmap</h2>
 *
 * <h3>Step 1: Engine Implementation (NEXT)</h3>
 * <ul>
 *   <li>Implement QuattroDueCalendarEngine</li>
 *   <li>Create RecurrenceCalculator with RRULE support</li>
 *   <li>Implement ExceptionResolver with priority handling</li>
 *   <li>Add performance optimization and caching</li>
 * </ul>
 *
 * <h3>Step 2: Repository Extensions</h3>
 * <ul>
 *   <li>Extend WorkScheduleRepository interface</li>
 *   <li>Create specialized repositories (RecurrenceRepository, ExceptionRepository)</li>
 *   <li>Implement database-backed repository classes</li>
 *   <li>Add caching layer integration</li>
 * </ul>
 *
 * <h3>Step 3: Use Case Implementation</h3>
 * <ul>
 *   <li>Create user-centric use cases with OperationResult pattern</li>
 *   <li>Implement schedule generation workflows</li>
 *   <li>Add exception management use cases</li>
 *   <li>Integrate with UI layer through DI modules</li>
 * </ul>
 *
 * <h2>📋 Business Rules</h2>
 *
 * <h3>Schedule Generation Rules</h3>
 * <ol>
 *   <li><strong>Base Pattern</strong>: Start with RecurrenceRule for user's team</li>
 *   <li><strong>Active Assignment</strong>: Use highest priority active UserScheduleAssignment</li>
 *   <li><strong>Apply Exceptions</strong>: Process ShiftExceptions by priority and date</li>
 *   <li><strong>Conflict Resolution</strong>: Higher priority exceptions override lower ones</li>
 *   <li><strong>Validation</strong>: Ensure no gaps or overlaps in final schedule</li>
 * </ol>
 *
 * <h3>Exception Priority Rules</h3>
 * <ol>
 *   <li><strong>URGENT (10)</strong>: Emergency exceptions, override everything</li>
 *   <li><strong>HIGH (8)</strong>: Temporary assignments, important changes</li>
 *   <li><strong>NORMAL (5)</strong>: Standard exceptions, vacation, sick leave</li>
 *   <li><strong>LOW (1)</strong>: Optional exceptions, preferences</li>
 * </ol>
 *
 * <h3>Approval Workflow Rules</h3>
 * <ul>
 *   <li><strong>Automatic Approval</strong>: Sick leave, ROL, union time</li>
 *   <li><strong>Supervisor Approval</strong>: Vacation, personal reductions</li>
 *   <li><strong>Management Approval</strong>: Team transfers, special coverage</li>
 *   <li><strong>Mutual Agreement</strong>: Colleague shift swaps</li>
 * </ul>
 *
 * <h2>🧪 Testing Strategy</h2>
 *
 * <h3>Unit Testing</h3>
 * <ul>
 *   <li><strong>Domain Models</strong>: Builder patterns, validation, business methods</li>
 *   <li><strong>Localization</strong>: Test both with and without localizer</li>
 *   <li><strong>RecurrenceCalculator</strong>: RRULE parsing and date generation</li>
 *   <li><strong>ExceptionResolver</strong>: Priority handling and conflict resolution</li>
 *   <li><strong>Engine Components</strong>: Schedule generation algorithms</li>
 * </ul>
 *
 * <h3>Integration Testing</h3>
 * <ul>
 *   <li><strong>Database Operations</strong>: CRUD operations for all entities</li>
 *   <li><strong>Repository Layer</strong>: Data access and caching behavior</li>
 *   <li><strong>Engine Integration</strong>: End-to-end schedule generation</li>
 *   <li><strong>Localization Integration</strong>: DI modules with localizers</li>
 *   <li><strong>Performance Testing</strong>: Cache efficiency and response times</li>
 * </ul>
 *
 * <h3>Business Logic Testing</h3>
 * <ul>
 *   <li><strong>QuattroDue Patterns</strong>: Verify 4-2 cycle accuracy</li>
 *   <li><strong>Exception Scenarios</strong>: Complex exception combinations</li>
 *   <li><strong>Multi-user Scenarios</strong>: Team assignments and transfers</li>
 *   <li><strong>Edge Cases</strong>: Leap years, daylight saving, timezone handling</li>
 * </ul>
 *
 * <p><strong>Note</strong>: This package represents a significant evolution from the original
 * QuattroDue system, maintaining backward compatibility while providing modern calendar
 * functionality and multi-user support. The domain models are designed to be framework-independent
 * and easily testable, following clean architecture principles and the universal localization
 * pattern throughout.</p>
 *
 * @author QDue Development Team
 * @version 2.0.0 - Calendar Engine with Universal Localization Pattern
 * @since Clean Architecture Phase 2
 */
package net.calvuz.qdue.domain.calendar.models;