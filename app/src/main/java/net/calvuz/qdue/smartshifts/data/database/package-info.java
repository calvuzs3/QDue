/**
 * Database Infrastructure Components
 * <p>
 * Contains Room database configuration, type converters, and database
 * initialization with predefined data and internationalization support.
 *
 * <h2>Components</h2>
 * <ul>
 *   <li><strong>SmartShiftsDatabase</strong> - Main Room database configuration</li>
 *   <li><strong>SmartShiftsConverters</strong> - Type converters for JSON serialization</li>
 *   <li><strong>DatabaseInitializer</strong> - Predefined patterns and shift types</li>
 * </ul>
 *
 * <h2>Database Schema</h2>
 * <ul>
 *   <li>5 core entities with proper relationships</li>
 *   <li>Optimized indices for frequent queries</li>
 *   <li>JSON storage for complex pattern data</li>
 *   <li>Soft delete support for data integrity</li>
 * </ul>
 *
 * <h2>Initialization</h2>
 * Database is pre-populated with:
 * <ul>
 *   <li>4 shift types (Morning, Afternoon, Night, Rest)</li>
 *   <li>4 predefined patterns (4-2, 3-2, 5-2, 6-1 cycles)</li>
 *   <li>Localized strings from resources</li>
 * </ul>
 *
 * @see androidx.room.Database
 * @see androidx.room.TypeConverter
 * @author SmartShifts Team
 * @since Phase 1 - Core Database & Models
 */
package net.calvuz.qdue.smartshifts.data.database;
