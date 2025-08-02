/**
 * Core layer of the QDue application following Clean Architecture principles.
 *
 * <p>This package contains all business logic and infrastructure concerns for the
 * work schedule management system, organized into domain and infrastructure layers
 * with clear separation of concerns.</p>
 *
 * <h3>Architecture Overview:</h3>
 * <pre>
 * core/
 * ├── domain/           → Business Logic Layer (Pure Java)
 * └── infrastructure/   → Technical Infrastructure Layer (Android/Room)
 * </pre>
 *
 * <h3>Domain Layer ({@link net.calvuz.qdue.core.domain}):</h3>
 * <ul>
 *   <li><strong>preferences</strong> - Application preferences and user settings</li>
 *   <li><strong>user</strong> - User management, authentication, and organization hierarchy</li>
 *   <li><strong>quattrodue</strong> - Work schedule provider system (4-2 pattern)</li>
 *   <li><strong>events</strong> - Calendar events management and work schedule exceptions</li>
 * </ul>
 *
 * <h3>Infrastructure Layer ({@link net.calvuz.qdue.core.infrastructure}):</h3>
 * <ul>
 *   <li><strong>di</strong> - Hybrid dependency injection (ServiceProvider + Hilt)</li>
 *   <li><strong>db</strong> - Room database with unified QDueDatabase (version 5)</li>
 *   <li><strong>services</strong> - Infrastructure service implementations</li>
 *   <li><strong>backup</strong> - Data backup and restore functionality</li>
 * </ul>
 *
 * <h3>Dependency Flow:</h3>
 * <pre>
 * UI Features → Domain Services → Infrastructure Services → Room Database
 *             ↓
 *         ServiceProvider (Manual DI) + Hilt (UI DI)
 * </pre>
 *
 * <h3>Key Architectural Principles:</h3>
 * <ul>
 *   <li><strong>Domain Independence</strong>: Domain layer has no Android dependencies</li>
 *   <li><strong>Interface Segregation</strong>: Infrastructure implements domain interfaces</li>
 *   <li><strong>Hybrid DI</strong>: ServiceProvider for services, Hilt for UI components</li>
 *   <li><strong>Provider Pattern</strong>: Extensible quattrodue schedule generation</li>
 *   <li><strong>Unified Database</strong>: Single Room database for all entities</li>
 * </ul>
 *
 * <h3>Technology Stack:</h3>
 * <ul>
 *   <li><strong>Database</strong>: Room 2.x with type converters and migrations</li>
 *   <li><strong>DI Framework</strong>: Hilt 2.56.2 + Manual ServiceProvider</li>
 *   <li><strong>Architecture</strong>: Clean Architecture with MVVM for UI</li>
 *   <li><strong>Async</strong>: CompletableFuture for background operations</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 2.0.0
 * @since API Level 21
 */
package net.calvuz.qdue.core;