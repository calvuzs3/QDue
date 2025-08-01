/**
 * Core layer of the Calendar application following Clean Architecture principles.
 *
 * <p>This package contains all business logic and infrastructure concerns, organized
 * into two main sub-packages:</p>
 *
 * <h3>Architecture Overview:</h3>
 * <pre>
 * core/
 * ├── domain/           → Business Logic Layer
 * └── infrastructure/   → Technical Infrastructure Layer
 * </pre>
 *
 * <h3>Domain Layer ({@link com.yourapp.calendar.core.domain}):</h3>
 * <ul>
 *   <li><strong>preferences</strong> - Application preferences and user settings</li>
 *   <li><strong>user</strong> - User management and authentication</li>
 *   <li><strong>quattrodue</strong> - Work schedule provider system</li>
 *   <li><strong>events</strong> - Calendar events and work schedule exceptions</li>
 * </ul>
 *
 * <h3>Infrastructure Layer ({@link com.yourapp.calendar.core.infrastructure}):</h3>
 * <ul>
 *   <li><strong>common</strong> - Shared utilities, constants, and extensions</li>
 *   <li><strong>di</strong> - Dependency injection configuration</li>
 *   <li><strong>db</strong> - Database entities, DAOs, and migrations</li>
 *   <li><strong>backup</strong> - Data backup and restore functionality</li>
 *   <li><strong>services</strong> - Infrastructure services and service locator</li>
 * </ul>
 *
 * <h3>Dependency Flow:</h3>
 * <pre>
 * UI Features → Domain → Infrastructure → External Systems
 * </pre>
 *
 * <p><strong>Key Principles:</strong></p>
 * <ul>
 *   <li>Domain layer is independent of infrastructure concerns</li>
 *   <li>Infrastructure layer implements domain interfaces</li>
 *   <li>Manual dependency injection via ServiceManager</li>
 *   <li>Provider pattern for extensible work schedule generation</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since API Level 21
 */
package net.calvuz.qdue.core;
