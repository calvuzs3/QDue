/**
 * Domain layer containing the core business logic of the Calendar application.
 *
 * <p>This package contains all domain models, business rules, and domain services
 * following Domain-Driven Design principles. It is completely independent of
 * infrastructure concerns and external frameworks.</p>
 *
 * <h3>Domain Packages:</h3>
 *
 * <h4>{@link com.yourapp.calendar.core.domain.preferences} - Preferences Domain</h4>
 * <ul>
 *   <li>User interface preferences (theme, colors, view modes)</li>
 *   <li>Business preferences (team assignment, notifications)</li>
 *   <li>Display preferences (calendar visualization settings)</li>
 * </ul>
 *
 * <h4>{@link com.yourapp.calendar.core.domain.user} - User Domain</h4>
 * <ul>
 *   <li>User authentication and profile management</li>
 *   <li>Team membership and role management</li>
 *   <li>External calendar synchronization</li>
 * </ul>
 *
 * <h4>{@link com.yourapp.calendar.core.domain.quattrodue} - Work Schedule Domain</h4>
 * <ul>
 *   <li>Work schedule template management (4-2 pattern and custom schemas)</li>
 *   <li>LegacyShift type definitions with colors and timing</li>
 *   <li>Volatile schedule generation via Provider pattern</li>
 *   <li>Team-based schedule templates</li>
 * </ul>
 *
 * <h4>{@link com.yourapp.calendar.core.domain.events} - Events Domain</h4>
 * <ul>
 *   <li>Calendar event management (CRUD operations)</li>
 *   <li>Work schedule exception handling</li>
 *   <li>Event metadata for classification (normal, work shift, overtime)</li>
 *   <li>Recurrence rule processing</li>
 *   <li>Event merging (base schedule + exceptions)</li>
 * </ul>
 *
 * <h3>Domain Patterns Used:</h3>
 * <ul>
 *   <li><strong>Repository Pattern</strong> - Data access abstraction</li>
 *   <li><strong>Service Pattern</strong> - Business logic encapsulation</li>
 *   <li><strong>Manager Pattern</strong> - High-level operation orchestration</li>
 *   <li><strong>Provider Pattern</strong> - Extensible work schedule generation</li>
 * </ul>
 *
 * <h3>Key Architectural Rules:</h3>
 * <ul>
 *   <li>Domain models contain no framework dependencies</li>
 *   <li>Repository interfaces define data access contracts</li>
 *   <li>Services contain pure business logic</li>
 *   <li>Managers orchestrate complex multi-service operations</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since API Level 21
 */
package net.calvuz.qdue.core.domain;
