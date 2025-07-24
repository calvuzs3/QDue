/**
 * Domain Models and Data Transfer Objects
 * <p>
 * Contains domain-specific models that represent business concepts
 * independent of database entities or UI concerns. These models
 * encapsulate business rules and validation logic.
 *
 * <h2>Core Models</h2>
 * <ul>
 *   <li><strong>CalendarDay</strong> - Calendar display model with shift indicators</li>
 *   <li><strong>ShiftRecurrenceRule</strong> - Recurrence rule domain model</li>
 *   <li><strong>PatternValidationResult</strong> - Pattern validation results</li>
 *   <li><strong>RecurrenceRule</strong> - Advanced recurrence handling</li>
 * </ul>
 *
 * <h2>Design Features</h2>
 * <ul>
 *   <li>Immutable objects where appropriate</li>
 *   <li>Built-in validation logic</li>
 *   <li>Clear separation from database entities</li>
 *   <li>Type safety with proper generics</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * These models are used by Use Cases to represent business data
 * and are converted to/from database entities by Repository classes.
 *
 * @author SmartShifts Team
 * @since Phase 2 - Business Logic
 */
package net.calvuz.qdue.smartshifts.domain.models;