/**
 * SmartShifts Data Layer
 * <p>
 * Contains all persistence-related components including database entities,
 * Data Access Objects (DAOs), and repository implementations following
 * the Repository Pattern for clean data access abstraction.
 *
 * <h2>Architecture</h2>
 * <pre>
 * data/
 * ├── database/     → Room database configuration and initialization
 * ├── entities/     → Room entities (5 core business entities)
 * ├── dao/          → Data Access Objects with optimized queries
 * └── repository/   → Repository pattern implementation
 * </pre>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li><strong>SmartShiftsDatabase</strong> - Room database with migration support</li>
 *   <li><strong>5 Core Entities</strong> - ShiftType, ShiftPattern, UserShiftAssignment,
 *       SmartShiftEvent, TeamContact</li>
 *   <li><strong>5 DAO Interfaces</strong> - 80+ optimized query methods</li>
 *   <li><strong>4 Repository Classes</strong> - Business logic data coordination</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li>Single Source of Truth with Room database</li>
 *   <li>Reactive data access with LiveData</li>
 *   <li>Thread-safe operations with background executors</li>
 *   <li>Optimized queries with proper indexing</li>
 * </ul>
 *
 * @see net.calvuz.qdue.smartshifts.data.database.SmartShiftsDatabase
 * @see net.calvuz.qdue.smartshifts.data.repository
 * @author SmartShifts Team
 * @since Phase 1 - Core Database & Models
 */
package net.calvuz.qdue.smartshifts.data;