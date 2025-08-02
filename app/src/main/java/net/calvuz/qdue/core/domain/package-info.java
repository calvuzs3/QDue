/**
 * Domain layer containing the core business logic of the QDue work schedule application.
 *
 * <p>This package contains all domain models, business rules, and domain services
 * following Domain-Driven Design principles. It is completely independent of
 * Android frameworks and infrastructure concerns, ensuring pure business logic.</p>
 *
 * <h3>Domain Packages:</h3>
 *
 * <h4>{@link net.calvuz.qdue.core.domain.preferences} - User Preferences Domain</h4>
 * <ul>
 *   <li><strong>Interface preferences</strong>: Theme, colors, view modes (Calendar/DaysList)</li>
 *   <li><strong>Business preferences</strong>: Team assignment, notifications, quattrodue settings</li>
 *   <li><strong>Display preferences</strong>: Calendar visualization and language settings</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.core.domain.user} - User Management Domain</h4>
 * <ul>
 *   <li><strong>User authentication</strong>: Google OAuth integration and profile management</li>
 *   <li><strong>Organization hierarchy</strong>: Establishment → MacroDepartment → SubDepartment</li>
 *   <li><strong>Team membership</strong>: 9-team quattrodue system assignment</li>
 *   <li><strong>User profiles</strong>: Employee data and work assignment tracking</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.core.domain.quattrodue} - Work Schedule Domain</h4>
 * <ul>
 *   <li><strong>Quattrodue pattern</strong>: 4 work days + 2 rest days continuous cycle</li>
 *   <li><strong>Schedule templates</strong>: Predefined and custom work patterns</li>
 *   <li><strong>Shift types</strong>: Morning, afternoon, night shifts with colors and timing</li>
 *   <li><strong>Team rotation</strong>: 9-team coordination and schedule generation</li>
 *   <li><strong>Provider pattern</strong>: Extensible schedule generation system</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.core.domain.events} - Events Management Domain</h4>
 * <ul>
 *   <li><strong>Calendar events</strong>: CRUD operations with Google Calendar-like functionality</li>
 *   <li><strong>Work schedule exceptions</strong>: Overtime, vacation, sick leave, shift swaps</li>
 *   <li><strong>Industrial events</strong>: Planned/unplanned stops, maintenance, emergencies</li>
 *   <li><strong>Event classification</strong>: Types, priorities, sources, and metadata</li>
 *   <li><strong>Import/Export system</strong>: EventPackageJson format with SSL validation</li>
 *   <li><strong>Recurrence rules</strong>: Repeating events and schedule pattern integration</li>
 * </ul>
 *
 * <h3>Domain Patterns Implementation:</h3>
 * <ul>
 *   <li><strong>Repository Pattern</strong>: Abstract data access with interface contracts</li>
 *   <li><strong>Service Pattern</strong>: Business logic encapsulation in domain services</li>
 *   <li><strong>Manager Pattern</strong>: High-level operation orchestration across services</li>
 *   <li><strong>Provider Pattern</strong>: Pluggable work schedule generation algorithms</li>
 *   <li><strong>Value Objects</strong>: Immutable domain models with business validation</li>
 * </ul>
 *
 * <h3>Business Rules and Constraints:</h3>
 * <ul>
 *   <li><strong>Quattrodue Cycle</strong>: Exactly 6 days per cycle (4 work + 2 rest)</li>
 *   <li><strong>Team Coordination</strong>: 9 teams ensure continuous coverage</li>
 *   <li><strong>Event Priority</strong>: Work schedule exceptions override base patterns</li>
 *   <li><strong>Data Integrity</strong>: Validation rules for all business entities</li>
 * </ul>
 *
 * <h3>Key Architectural Rules:</h3>
 * <ul>
 *   <li><strong>Pure Business Logic</strong>: No Android dependencies, only Java/Kotlin</li>
 *   <li><strong>Interface-driven Design</strong>: Repository and service interfaces define contracts</li>
 *   <li><strong>Immutable Models</strong>: Domain entities are immutable where possible</li>
 *   <li><strong>Validation at Boundaries</strong>: Input validation in domain services</li>
 *   <li><strong>Clear Separation</strong>: Domain models separate from persistence entities</li>
 * </ul>
 *
 * <h3>Integration with Infrastructure:</h3>
 * <ul>
 *   <li><strong>Repository Implementation</strong>: Infrastructure layer implements repository interfaces</li>
 *   <li><strong>Service Implementation</strong>: Domain services injected via ServiceProvider</li>
 *   <li><strong>Database Mapping</strong>: Room entities map to/from domain models</li>
 *   <li><strong>External Integration</strong>: Clean interfaces for calendar sync and backup</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 2.0.0
 * @since API Level 21
 */
package net.calvuz.qdue.core.domain;