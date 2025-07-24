/**
 * Business Logic Use Cases
 * <p>
 * Contains use case classes implementing specific business operations
 * following the Single Responsibility Principle. Each use case
 * encapsulates one business workflow and coordinates with repositories
 * to achieve the desired outcome.
 *
 * <h2>Use Case Classes</h2>
 * <ul>
 *   <li><strong>GetUserShiftsUseCase</strong> - User shift retrieval + analytics</li>
 *   <li><strong>CreatePatternUseCase</strong> - Pattern creation + validation</li>
 *   <li><strong>AssignPatternUseCase</strong> - Pattern assignment + team logic</li>
 *   <li><strong>ValidatePatternUseCase</strong> - Pattern continuity validation</li>
 *   <li><strong>ManageContactsUseCase</strong> - Team contact operations + sync</li>
 * </ul>
 *
 * <h2>Design Pattern</h2>
 * Each use case follows a consistent pattern:
 * <ul>
 *   <li>Single public method for the main operation</li>
 *   <li>Clear input/output parameters</li>
 *   <li>Error handling with meaningful exceptions</li>
 *   <li>Repository coordination for data access</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * Use cases are injected into ViewModels via Hilt and provide
 * the business logic layer between UI and data repositories.
 *
 * @see net.calvuz.qdue.smartshifts.ui
 * @see net.calvuz.qdue.smartshifts.data.repository
 * @author SmartShifts Team
 * @since Phase 2 - Business Logic
 */
package net.calvuz.qdue.smartshifts.domain.usecases;