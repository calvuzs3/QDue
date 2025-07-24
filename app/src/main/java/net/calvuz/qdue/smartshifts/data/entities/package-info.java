/**
 * Room Database Entities
 * <p>
 * Contains all Room entity classes representing the core business data
 * model for the SmartShifts system. Each entity corresponds to a database
 * table with proper relationships and constraints.
 *
 * <h2>Core Entities</h2>
 * <ul>
 *   <li><strong>ShiftType</strong> - Work shift types (Morning, Afternoon, Night, Rest)</li>
 *   <li><strong>ShiftPattern</strong> - Recurring shift patterns with JSON rules</li>
 *   <li><strong>UserShiftAssignment</strong> - User assignments to shift patterns</li>
 *   <li><strong>SmartShiftEvent</strong> - Generated shift events from patterns</li>
 *   <li><strong>TeamContact</strong> - Team coordination contact information</li>
 * </ul>
 *
 * <h2>Design Features</h2>
 * <ul>
 *   <li>Proper foreign key relationships</li>
 *   <li>Optimized indices for query performance</li>
 *   <li>Soft delete support with isActive flags</li>
 *   <li>Timestamp tracking (createdAt, updatedAt)</li>
 *   <li>JSON storage for complex data structures</li>
 * </ul>
 *
 * <h2>Validation</h2>
 * All entities include proper validation constraints and null safety
 * annotations for data integrity.
 *
 * @see androidx.room.Entity
 * @see androidx.room.PrimaryKey
 * @see androidx.room.ForeignKey
 * @author SmartShifts Team
 * @since Phase 1 - Core Database & Models
 */
package net.calvuz.qdue.smartshifts.data.entities;