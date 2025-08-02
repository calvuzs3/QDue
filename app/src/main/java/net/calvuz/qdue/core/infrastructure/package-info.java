/**
 * Infrastructure layer providing technical services and data persistence for QDue.
 *
 * <p>This package contains all technical concerns including Room database access,
 * hybrid dependency injection system, backup services, and external integrations.
 * It implements the interfaces defined in the domain layer using Android-specific
 * technologies and frameworks.</p>
 *
 * <h3>Infrastructure Packages:</h3>
 *
 * <h4>{@link net.calvuz.qdue.core.infrastructure.di} - Hybrid Dependency Injection</h4>
 * <ul>
 *   <li><strong>ServiceProvider</strong> - Manual DI for core business services</li>
 *   <li><strong>ServiceProviderImpl</strong> - Concrete implementation with lazy initialization</li>
 *   <li><strong>Injectable</strong> - Interface for components requiring DI</li>
 *   <li><strong>Hilt Integration</strong> - UI components use Hilt DI (version 2.56.2)</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.core.infrastructure.db} - Room Database Layer</h4>
 * <ul>
 *   <li><strong>QDueDatabase</strong> - Unified Room database (version 5)</li>
 *   <li><strong>Entities</strong>: LocalEvent, TurnException, User, Establishment, Departments, ShiftType</li>
 *   <li><strong>DAOs</strong> - Data Access Objects with complex queries and relationships</li>
 *   <li><strong>Migrations</strong> - Database schema evolution with fallback strategies</li>
 *   <li><strong>Type Converters</strong> - Serialization for LocalDateTime, enums, and domain objects</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.core.infrastructure.services} - Service Implementations</h4>
 * <ul>
 *   <li><strong>EventsService</strong> - Calendar events CRUD with backup integration</li>
 *   <li><strong>UserService</strong> - User management and Google OAuth authentication</li>
 *   <li><strong>OrganizationService</strong> - Company hierarchy and department management</li>
 *   <li><strong>WorkScheduleService</strong> - Quattrodue pattern generation and team rotation</li>
 *   <li><strong>ShiftTypeService</strong> - Shift definitions and scheduling metadata</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.core.infrastructure.backup} - Data Backup System</h4>
 * <ul>
 *   <li><strong>CoreBackupManager</strong> - Centralized backup orchestration</li>
 *   <li><strong>DatabaseBackupService</strong> - Entity-specific backup operations</li>
 *   <li><strong>RestoreManager</strong> - Data restore with conflict resolution</li>
 *   <li><strong>EventPackageJson</strong> - Standardized backup format with SSL validation</li>
 * </ul>
 *
 * <h3>Technology Integration:</h3>
 *
 * <h4>Room Database (Version 5):</h4>
 * <pre>
 * QDueDatabase (Unified)
 * ├── Events Domain: LocalEvent, TurnException
 * ├── User Domain: User, Establishment, MacroDepartment, SubDepartment
 * └── Schedule Domain: ShiftTypeEntity
 * </pre>
 *
 * <h4>Dependency Injection Architecture:</h4>
 * <pre>
 * Manual ServiceProvider          Hilt DI Framework
 * ├── Core Services              ├── Activities/Fragments
 * ├── Database Access            ├── ViewModels
 * ├── Background Operations      ├── UI Components
 * └── Business Logic             └── Presentation Layer
 * </pre>
 *
 * <h3>Service Implementation Patterns:</h3>
 * <ul>
 *   <li><strong>Constructor Injection</strong>: All services use constructor-based DI</li>
 *   <li><strong>OperationResult Pattern</strong>: Consistent error handling across operations</li>
 *   <li><strong>CompletableFuture</strong>: Asynchronous operations with proper error handling</li>
 *   <li><strong>Automatic Backup</strong>: Data modifications trigger backup operations</li>
 *   <li><strong>Thread Safety</strong>: Concurrent access protection with proper synchronization</li>
 * </ul>
 *
 * <h3>Database Design Principles:</h3>
 * <ul>
 *   <li><strong>Single Source of Truth</strong>: Unified QDueDatabase for all entities</li>
 *   <li><strong>Referential Integrity</strong>: Foreign key relationships with cascade operations</li>
 *   <li><strong>Performance Optimization</strong>: Strategic indexing for calendar queries</li>
 *   <li><strong>Schema Evolution</strong>: Robust migration system with data preservation</li>
 * </ul>
 *
 * <h3>External Integration Support:</h3>
 * <ul>
 *   <li><strong>Google Calendar Sync</strong>: OAuth integration with calendar API</li>
 *   <li><strong>Backup/Restore</strong>: JSON-based data exchange with SSL validation</li>
 *   <li><strong>File Import/Export</strong>: EventPackageJson format for data portability</li>
 *   <li><strong>Network Operations</strong>: HTTP/HTTPS with proper error handling</li>
 * </ul>
 *
 * <h3>Performance and Reliability:</h3>
 * <ul>
 *   <li><strong>Lazy Initialization</strong>: Services created on-demand to reduce startup time</li>
 *   <li><strong>Connection Pooling</strong>: Efficient database connection management</li>
 *   <li><strong>Background Processing</strong>: Heavy operations moved off main thread</li>
 *   <li><strong>Error Recovery</strong>: Robust error handling with automatic retries</li>
 * </ul>
 *
 * <h3>Testing Support:</h3>
 * <ul>
 *   <li><strong>Mock Services</strong>: TestServiceProvider for unit testing</li>
 *   <li><strong>In-Memory Database</strong>: Room in-memory DB for integration tests</li>
 *   <li><strong>Dependency Injection</strong>: Easy mocking through constructor injection</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 2.0.0
 * @since API Level 21
 */
package net.calvuz.qdue.core.infrastructure;