/**
 * Infrastructure layer providing technical services and data persistence.
 *
 * <p>This package contains all technical concerns including database access,
 * dependency injection, external service integrations, and shared utilities.
 * It implements the interfaces defined in the domain layer.</p>
 *
 * <h3>Infrastructure Packages:</h3>
 *
 * <h4>{@link com.yourapp.calendar.core.infrastructure.common} - Common Utilities</h4>
 * <ul>
 *   <li><strong>constants</strong> - Application-wide constants and configuration</li>
 *   <li><strong>extensions</strong> - Utility extension methods</li>
 *   <li><strong>utils</strong> - Helper classes for date/time, validation, colors</li>
 *   <li><strong>exceptions</strong> - Custom exception types</li>
 * </ul>
 *
 * <h4>{@link com.yourapp.calendar.core.infrastructure.di} - Dependency Injection</h4>
 * <ul>
 *   <li><strong>modules</strong> - Dependency provider modules</li>
 *   <li><strong>qualifiers</strong> - Custom scope and qualifier annotations</li>
 *   <li><strong>ServiceLocator</strong> - Manual DI orchestrator</li>
 * </ul>
 *
 * <h4>{@link com.yourapp.calendar.core.infrastructure.db} - Database Layer</h4>
 * <ul>
 *   <li><strong>entities</strong> - Database table entities</li>
 *   <li><strong>dao</strong> - Data Access Objects</li>
 *   <li><strong>migrations</strong> - Database schema migration scripts</li>
 *   <li><strong>converters</strong> - Type converters for complex data types</li>
 * </ul>
 *
 * <h4>{@link com.yourapp.calendar.core.infrastructure.services} - Infrastructure Services</h4>
 * <ul>
 *   <li><strong>ServiceManager</strong> - Central service locator</li>
 *   <li><strong>DatabaseService</strong> - Database connection management</li>
 *   <li><strong>SyncService</strong> - Background synchronization</li>
 *   <li><strong>NotificationService</strong> - System notifications</li>
 *   <li><strong>BackupService</strong> - Data backup operations</li>
 * </ul>
 *
 * <h3>Infrastructure Patterns:</h3>
 * <ul>
 *   <li><strong>Service Locator</strong> - Manual dependency injection</li>
 *   <li><strong>Repository Implementation</strong> - Domain repository contracts</li>
 *   <li><strong>DAO Pattern</strong> - Database access abstraction</li>
 *   <li><strong>Migration Pattern</strong> - Database schema evolution</li>
 * </ul>
 *
 * <h3>Key Implementation Notes:</h3>
 * <ul>
 *   <li>All infrastructure implementations are replaceable</li>
 *   <li>Database entities separate from domain models</li>
 *   <li>Type converters handle complex domain objects</li>
 *   <li>Service implementations inject via constructor parameters</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since API Level 21
 */
package net.calvuz.qdue.core.infrastructure;
