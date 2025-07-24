/**
 * Repository Pattern Implementation
 * <p>
 * Contains repository classes implementing the Repository Pattern to
 * provide a clean abstraction layer between the domain layer and
 * data sources. Each repository coordinates multiple data sources
 * and implements business logic for data operations.
 *
 * <h2>Repository Classes</h2>
 * <ul>
 *   <li><strong>SmartShiftsRepository</strong> - Main coordinator repository</li>
 *   <li><strong>ShiftPatternRepository</strong> - Pattern-specific operations</li>
 *   <li><strong>UserAssignmentRepository</strong> - User assignment logic</li>
 *   <li><strong>TeamContactRepository</strong> - Contact management</li>
 * </ul>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Coordinate multiple DAOs for complex operations</li>
 *   <li>Implement business logic for data validation</li>
 *   <li>Provide reactive data streams with LiveData</li>
 *   <li>Handle error cases and data consistency</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * Repositories are injected into Use Cases via Hilt dependency injection
 * and serve as the single source of truth for domain layer operations.
 *
 * @see net.calvuz.qdue.smartshifts.domain.usecases
 * @author SmartShifts Team
 * @since Phase 2 - Business Logic
 */
package net.calvuz.qdue.smartshifts.data.repository;