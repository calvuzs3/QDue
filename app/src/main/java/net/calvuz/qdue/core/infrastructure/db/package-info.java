/**
 * Room Database Infrastructure - Unified Data Persistence Layer for QDue.
 *
 * <p>This package provides the complete database infrastructure implementing a unified
 * Room database architecture that consolidates events management, user authentication,
 * organizational hierarchy, and work schedule data into a single, cohesive system
 * with comprehensive migration support and performance optimizations.</p>
 *
 * <h3>Database Architecture Overview:</h3>
 *
 * <h4>{@link net.calvuz.qdue.core.infrastructure.db.QDueDatabase} - Unified Main Database</h4>
 * <ul>
 *   <li><strong>Database Version</strong>: 5 (Current unified version)</li>
 *   <li><strong>Architecture</strong>: Single Room database replacing multiple separate databases</li>
 *   <li><strong>Entities</strong>: 7 core entities covering all business domains</li>
 *   <li><strong>Migration Strategy</strong>: Incremental migrations with fallback to destructive</li>
 *   <li><strong>Performance</strong>: Strategic indexing and query optimization</li>
 * </ul>
 *
 * <h3>Entity Organization by Domain:</h3>
 *
 * <h4>Events Domain Entities:</h4>
 * <ul>
 *   <li><strong>{@link net.calvuz.qdue.core.domain.events.models.LocalEvent}</strong>
 *       <ul>
 *         <li>Google Calendar-like event storage with full metadata</li>
 *         <li>Support for all-day events, recurrence, and custom properties</li>
 *         <li>EventType classification (work, maintenance, meetings, stops)</li>
 *         <li>EventPriority levels (low, normal, high, urgent)</li>
 *         <li>EventSource tracking (local, imported, external packages)</li>
 *         <li>Indexes: start_time, event_type, priority, package_id, time_range</li>
 *       </ul>
 *   </li>
 *   <li><strong>{@link net.calvuz.qdue.core.domain.events.models.TurnException}</strong>
 *       <ul>
 *         <li>Work schedule exceptions and modifications</li>
 *         <li>Override base quattrodue patterns with specific events</li>
 *         <li>Foreign key relationships to User and organizational entities</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h4>User Management Domain Entities:</h4>
 * <ul>
 *   <li><strong>{@link net.calvuz.qdue.core.domain.user.data.entities.User}</strong>
 *       <ul>
 *         <li>Complete user profile with authentication data</li>
 *         <li>Google OAuth integration (google_id, auth_provider)</li>
 *         <li>Employee information and work assignment</li>
 *         <li>Profile completion tracking and settings</li>
 *       </ul>
 *   </li>
 *   <li><strong>{@link net.calvuz.qdue.core.domain.user.data.entities.Establishment}</strong>
 *       <ul>
 *         <li>Company/organization root level entities</li>
 *         <li>Establishment codes and organizational metadata</li>
 *       </ul>
 *   </li>
 *   <li><strong>{@link net.calvuz.qdue.core.domain.user.data.entities.MacroDepartment}</strong>
 *       <ul>
 *         <li>High-level department organization within establishments</li>
 *         <li>Foreign key relationship to Establishment</li>
 *       </ul>
 *   </li>
 *   <li><strong>{@link net.calvuz.qdue.core.domain.user.data.entities.SubDepartment}</strong>
 *       <ul>
 *         <li>Detailed department hierarchy within macro departments</li>
 *         <li>Foreign key relationship to MacroDepartment</li>
 *         <li>Complete organizational tree: Establishment → Macro → Sub</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h4>Work Schedule Domain Entities:</h4>
 * <ul>
 *   <li><strong>{@link net.calvuz.qdue.core.infrastructure.db.entities.ShiftTypeEntity}</strong>
 *       <ul>
 *         <li>Shift type definitions for quattrodue pattern system</li>
 *         <li>Shift timing, colors, and visual representation</li>
 *         <li>Integration with schedule generation algorithms</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>Data Access Objects (DAO) Architecture:</h3>
 *
 * <h4>Events Domain DAOs:</h4>
 * <ul>
 *   <li><strong>{@link net.calvuz.qdue.core.domain.events.dao.EventDao}</strong>
 *       <ul>
 *         <li>Complex calendar queries with date range filtering</li>
 *         <li>Event type and priority-based searches</li>
 *         <li>Package-based event management for imports</li>
 *         <li>Performance-optimized queries for calendar views</li>
 *       </ul>
 *   </li>
 *   <li><strong>{@link net.calvuz.qdue.core.domain.events.dao.TurnExceptionDao}</strong>
 *       <ul>
 *         <li>Schedule exception queries with user relationships</li>
 *         <li>Integration with quattrodue pattern generation</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h4>User Management DAOs:</h4>
 * <ul>
 *   <li><strong>{@link net.calvuz.qdue.core.domain.user.data.dao.UserDao}</strong>
 *       <ul>
 *         <li>Authentication queries (email, Google ID, employee ID)</li>
 *         <li>Profile completion status and user state management</li>
 *         <li>Complex joins with organizational entities</li>
 *       </ul>
 *   </li>
 *   <li><strong>{@link net.calvuz.qdue.core.domain.user.data.dao.EstablishmentDao}</strong></li>
 *   <li><strong>{@link net.calvuz.qdue.core.domain.user.data.dao.MacroDepartmentDao}</strong></li>
 *   <li><strong>{@link net.calvuz.qdue.core.domain.user.data.dao.SubDepartmentDao}</strong>
 *       <ul>
 *         <li>Hierarchical organizational queries</li>
 *         <li>Cross-entity relationship management</li>
 *         <li>Cascade operations for organizational changes</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h4>Work Schedule DAOs:</h4>
 * <ul>
 *   <li><strong>{@link net.calvuz.qdue.core.infrastructure.db.dao.ShiftTypeDao}</strong>
 *       <ul>
 *         <li>Shift type definition queries and management</li>
 *         <li>Integration with schedule generation services</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>Database Migration Strategy:</h3>
 *
 * <h4>Migration History:</h4>
 * <pre>
 * Version 1: Events only (LocalEvent)
 * Version 2: Events + TurnException
 * Version 3: Events + TurnException + User management (UNIFIED)
 * Version 4: Complete organizational hierarchy
 * Version 5: Work schedule system integration (CURRENT)
 * </pre>
 *
 * <h4>Migration Implementation:</h4>
 * <ul>
 *   <li><strong>{@link net.calvuz.qdue.core.infrastructure.db.migrations.Migration_4_5}</strong>
 *       <ul>
 *         <li>ShiftTypeEntity table creation</li>
 *         <li>Default shift type seeding</li>
 *         <li>Work schedule system integration</li>
 *       </ul>
 *   </li>
 *   <li><strong>Fallback Strategy</strong>: Destructive migration for development</li>
 * </ul>
 *
 * <h3>Type Conversion System:</h3>
 *
 * <h4>{@link net.calvuz.qdue.core.infrastructure.db.converters.QDueTypeConverters}:</h4>
 * <ul>
 *   <li><strong>LocalDateTime Conversion</strong>: ISO-8601 string serialization</li>
 *   <li><strong>LocalDate Conversion</strong>: Date-only string serialization</li>
 *   <li><strong>LocalTime Conversion</strong>: Time-only string serialization</li>
 *   <li><strong>Enum Conversion</strong>: EventType, EventPriority, EventSource serialization</li>
 *   <li><strong>Map Conversion</strong>: JSON serialization for custom properties</li>
 * </ul>
 *
 * <h3>Database Seeding System:</h3>
 * <ul>
 *   <li><strong>{@link net.calvuz.qdue.core.infrastructure.db.seeders.ShiftTypeSeeder}</strong>
 *       <ul>
 *         <li>Default shift type creation on database initialization</li>
 *         <li>Quattrodue pattern shift definitions</li>
 *         <li>Color and timing configuration</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>Performance Optimization Features:</h3>
 *
 * <h4>Strategic Indexing:</h4>
 * <ul>
 *   <li><strong>LocalEvent Indexes</strong>: start_time, event_type, priority, package_id</li>
 *   <li><strong>Composite Indexes</strong>: time_range (start_time, end_time) for range queries</li>
 *   <li><strong>Foreign Key Indexes</strong>: Automatic indexing for relationships</li>
 * </ul>
 *
 * <h4>Query Optimization:</h4>
 * <ul>
 *   <li><strong>Calendar View Queries</strong>: Optimized date range filtering</li>
 *   <li><strong>Hierarchical Queries</strong>: Efficient organizational tree traversal</li>
 *   <li><strong>Pagination Support</strong>: Large dataset handling for event lists</li>
 * </ul>
 *
 * <h3>Database Connection and Lifecycle:</h3>
 * <ul>
 *   <li><strong>Singleton Pattern</strong>: Thread-safe database instance management</li>
 *   <li><strong>Connection Pooling</strong>: Efficient connection reuse across services</li>
 *   <li><strong>Callback System</strong>: Database creation and open callbacks for seeding</li>
 *   <li><strong>Memory Management</strong>: Proper cleanup and resource management</li>
 * </ul>
 *
 * <h3>Backup and Restore Integration:</h3>
 * <ul>
 *   <li><strong>Entity Backup</strong>: Complete entity serialization support</li>
 *   <li><strong>Relationship Preservation</strong>: Foreign key relationships maintained in backups</li>
 *   <li><strong>Incremental Backup</strong>: Change tracking for efficient backup operations</li>
 * </ul>
 *
 * <h3>Testing Support:</h3>
 * <ul>
 *   <li><strong>In-Memory Database</strong>: Fast testing with Room in-memory implementation</li>
 *   <li><strong>Database Testing</strong>: Comprehensive DAO testing support</li>
 *   <li><strong>Migration Testing</strong>: Automated migration validation</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 2.0.0
 * @since API Level 21
 * @see net.calvuz.qdue.core.infrastructure.db.QDueDatabase
 * @see net.calvuz.qdue.core.infrastructure.db.converters.QDueTypeConverters
 * @see net.calvuz.qdue.core.infrastructure.db.migrations.Migration_4_5
 */
package net.calvuz.qdue.core.infrastructure.db;