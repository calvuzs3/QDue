/**
 * Hilt Dependency Injection Modules
 * <p>
 * Contains all Hilt dependency injection modules providing clean
 * separation of concerns and proper dependency management throughout
 * the SmartShifts application. Implements best practices for
 * Android dependency injection.
 *
 * <h2>Module Structure</h2>
 * <ul>
 *   <li><strong>DatabaseModule</strong> - Database and DAO providers</li>
 *   <li><strong>RepositoryModule</strong> - Repository pattern implementation</li>
 *   <li><strong>DomainModule</strong> - Business logic components</li>
 *   <li><strong>UtilityModule</strong> - Helper classes and utilities</li>
 *   <li><strong>UseCaseModule</strong> - Use case dependencies</li>
 *   <li><strong>ApplicationModule</strong> - App-level dependencies</li>
 * </ul>
 *
 * <h2>Dependency Scopes</h2>
 * <ul>
 *   <li><strong>@Singleton</strong> - Database, repositories, and engines</li>
 *   <li><strong>@ViewModelScoped</strong> - Use cases and view model dependencies</li>
 *   <li><strong>@ActivityScoped</strong> - Activity-specific components</li>
 * </ul>
 *
 * <h2>Qualifiers</h2>
 * <ul>
 *   <li><strong>@LegacyQDueDb</strong> - Distinguishes from existing QDue database</li>
 *   <li><strong>@SmartShiftsDb</strong> - SmartShifts-specific database components</li>
 * </ul>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Constructor injection preferred over field injection</li>
 *   <li>Interface dependencies for testability</li>
 *   <li>Proper scope management for lifecycle awareness</li>
 *   <li>Zero circular dependencies</li>
 * </ul>
 *
 * @see dagger.hilt.InstallIn
 * @see dagger.hilt.components.SingletonComponent
 * @author SmartShifts Team
 * @since Phase 1 - Core Database & Models
 */
package net.calvuz.qdue.smartshifts.di;