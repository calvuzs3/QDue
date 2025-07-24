/**
 * Data Access Objects (DAO)
 * <p>
 * Contains Room DAO interfaces providing type-safe database access
 * with 80+ optimized query methods across 5 core DAOs. Each DAO
 * handles CRUD operations and complex queries for its entity.
 *
 * <h2>DAO Classes</h2>
 * <ul>
 *   <li><strong>ShiftTypeDao</strong> - 15+ methods for shift type management</li>
 *   <li><strong>ShiftPatternDao</strong> - 20+ methods for pattern operations</li>
 *   <li><strong>UserShiftAssignmentDao</strong> - 15+ assignment methods</li>
 *   <li><strong>SmartShiftEventDao</strong> - 25+ complex event queries</li>
 *   <li><strong>TeamContactDao</strong> - 20+ contact management methods</li>
 * </ul>
 *
 * <h2>Query Optimization</h2>
 * <ul>
 *   <li>Efficient queries with proper indexing</li>
 *   <li>LiveData support for reactive UI updates</li>
 *   <li>Bulk operations for performance</li>
 *   <li>Transaction support for data consistency</li>
 * </ul>
 *
 * <h2>Advanced Features</h2>
 * <ul>
 *   <li>Complex join queries across entities</li>
 *   <li>Date range queries with optimization</li>
 *   <li>Statistical queries for analytics</li>
 *   <li>Conflict detection and resolution</li>
 * </ul>
 *
 * @see androidx.room.Dao
 * @see androidx.room.Query
 * @see androidx.lifecycle.LiveData
 * @author SmartShifts Team
 * @since Phase 1 - Core Database & Models
 */
package net.calvuz.qdue.smartshifts.data.dao;