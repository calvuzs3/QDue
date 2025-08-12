/**
 * QuattroDue Work Schedule Management Domain - Database-Integrated Edition.
 *
 * <p>This package provides a comprehensive work schedule management system based on the
 * Italian "quattrodue" (4-2) shift pattern. The system has been enhanced with Room database
 * integration for persistent storage while maintaining full backward compatibility with
 * existing APIs and patterns.</p>
 *
 * <h3>Core Architecture Overview:</h3>
 *
 * <h4>{@link net.calvuz.qdue.quattrodue.QuattroDue} - Central Singleton Controller</h4>
 * <ul>
 *   <li><strong>Pattern Calculation</strong>: Generates work schedules based on 18-day rotation cycles</li>
 *   <li><strong>Database Integration</strong>: Persistent storage for shift types with caching layer</li>
 *   <li><strong>Team Management</strong>: Handles 9 half-teams (A-I) across multiple shift patterns</li>
 *   <li><strong>Preference Integration</strong>: User settings for calendars, stops, and team assignments</li>
 *   <li><strong>Dependency Injection</strong>: Supports both singleton and DI-based initialization</li>
 * </ul>
 *
 * <h3>Domain Models:</h3>
 *
 * <h4>{@link net.calvuz.qdue.quattrodue.models.ShiftType} - Database Entity (UPDATED)</h4>
 * <ul>
 *   <li><strong>Room Entity</strong>: Full Room database integration with @Entity annotations</li>
 *   <li><strong>Backward Compatibility</strong>: Maintains exact same API as original model</li>
 *   <li><strong>Database Features</strong>: Auto-generated IDs, indexed queries, soft delete support</li>
 *   <li><strong>Builder Pattern</strong>: Fluent API for creating and updating shift types</li>
 *   <li><strong>Validation</strong>: Built-in constraints for time ranges and duration values</li>
 *   <li><strong>Metadata</strong>: Creation/update timestamps, active status, user-defined flags</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.quattrodue.models.Shift} - Schedule Instance</h4>
 * <ul>
 *   <li><strong>Daily Shifts</strong>: Represents actual shift instances on specific dates</li>
 *   <li><strong>Team Assignment</strong>: Links half-teams to specific shifts</li>
 *   <li><strong>Stop Support</strong>: Handles plant maintenance periods</li>
 *   <li><strong>ShiftType Association</strong>: References database-backed shift type definitions</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.quattrodue.models.Day} - Daily Schedule Container</h4>
 * <ul>
 *   <li><strong>Multi-Shift Support</strong>: Contains multiple shifts per day (typically 3)</li>
 *   <li><strong>Date Association</strong>: Links schedules to specific calendar dates</li>
 *   <li><strong>Clone Support</strong>: Efficient copying for pattern generation</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.quattrodue.models.HalfTeam} - Team Management</h4>
 * <ul>
 *   <li><strong>9 Teams</strong>: Standard A through I team designation</li>
 *   <li><strong>Rotation Logic</strong>: Automatic team assignment across patterns</li>
 *   <li><strong>User Assignment</strong>: Links users to specific teams via preferences</li>
 * </ul>
 *
 * <h3>Database Access Layer:</h3>
 *
 * <h4>{@link net.calvuz.qdue.quattrodue.dao.ShiftTypeDao} - Database Operations</h4>
 * <ul>
 *   <li><strong>CRUD Operations</strong>: Complete create, read, update, delete support</li>
 *   <li><strong>Indexed Queries</strong>: Fast lookups by ID, name, and index position</li>
 *   <li><strong>Filtering Support</strong>: Active/inactive, user-defined/system, work/rest types</li>
 *   <li><strong>Batch Operations</strong>: Multi-record operations for seeding and updates</li>
 *   <li><strong>Statistics</strong>: Count and status queries for monitoring</li>
 *   <li><strong>Compatibility Methods</strong>: API-compatible methods for ShiftTypeFactory migration</li>
 * </ul>
 *
 * <h3>Factory and Utility Classes:</h3>
 *
 * <h4>{@link net.calvuz.qdue.quattrodue.utils.ShiftTypeFactory} - Migration Adapter (UPDATED)</h4>
 * <ul>
 *   <li><strong>Facade Pattern</strong>: Maintains existing API while delegating to database</li>
 *   <li><strong>Database Seeding</strong>: Initializes database with default shift types</li>
 *   <li><strong>Backward Compatibility</strong>: Zero-breaking-change migration for existing code</li>
 *   <li><strong>Deprecation Path</strong>: Guides developers toward direct QuattroDue usage</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.quattrodue.utils.ShiftFactory} - Shift Instance Creation</h4>
 * <ul>
 *   <li><strong>Shift Generation</strong>: Creates Shift instances from database-backed ShiftTypes</li>
 *   <li><strong>Builder Pattern</strong>: Fluent API for complex shift configurations</li>
 *   <li><strong>Team Integration</strong>: Automatic team assignment based on patterns</li>
 *   <li><strong>Validation</strong>: Input validation and error handling</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.quattrodue.utils.HalfTeamFactory} - Team Management</h4>
 * <ul>
 *   <li><strong>Static Teams</strong>: Manages the standard 9 half-teams</li>
 *   <li><strong>Name Resolution</strong>: Converts team names to objects</li>
 *   <li><strong>Validation</strong>: Ensures valid team assignments</li>
 * </ul>
 *
 * <h3>Integration Patterns:</h3>
 *
 * <h4>Database Integration Pattern:</h4>
 * <pre>
 * QuattroDue Singleton
 * ├── QDueDatabase → Room Database
 * │   └── ShiftTypeDao → CRUD Operations
 * ├── Caching Layer → Performance Optimization
 * │   ├── Cached ShiftTypes (5min TTL)
 * │   └── Cache Invalidation on Updates
 * └── Fallback Strategy → ShiftTypeFactory (Legacy)
 * </pre>
 *
 * <h4>Factory Migration Pattern:</h4>
 * <pre>
 * Legacy Code
 * ├── ShiftTypeFactory.getAllShiftTypes()
 * │   └── Delegates to → QuattroDue.getShiftTypes()
 * │       └── Database Query → ShiftTypeDao.getAllActiveShiftTypes()
 * ├── ShiftFactory.createShift(index, date)
 * │   └── Uses → QuattroDue.getShiftTypeByIndex(index)
 * │       └── Database Query → ShiftTypeDao.getShiftTypeByIndex(index)
 * └── Full API Compatibility → Zero Breaking Changes
 * </pre>
 *
 * <h4>Service Integration Pattern:</h4>
 * <pre>
 * WorkScheduleRepositoryImpl
 * ├── QuattroDue.getInstance(context, database)
 * │   └── Dependency Injection → Database Instance
 * ├── getAllShiftTypes() → Database-backed Results
 * ├── getShiftTypeById(id) → Direct Database Lookup
 * └── Caching Strategy → Automatic Performance Optimization
 * </pre>
 *
 * <h3>Database Schema:</h3>
 *
 * <h4>shift_types Table Structure:</h4>
 * <pre>
 * CREATE TABLE shift_types (
 *   id INTEGER PRIMARY KEY AUTOINCREMENT,
 *   name TEXT NOT NULL UNIQUE COLLATE NOCASE,
 *   description TEXT NOT NULL,
 *   start_hour INTEGER NOT NULL CHECK (start_hour >= 0 AND start_hour <= 23),
 *   start_minute INTEGER NOT NULL CHECK (start_minute >= 0 AND start_minute <= 59),
 *   duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
 *   color INTEGER NOT NULL,
 *   is_rest_type INTEGER NOT NULL DEFAULT 0,
 *   is_active INTEGER NOT NULL DEFAULT 1,
 *   is_user_defined INTEGER NOT NULL DEFAULT 0,
 *   created_at INTEGER NOT NULL,
 *   updated_at INTEGER NOT NULL
 * );
 * </pre>
 *
 * <h4>Database Indexes for Performance:</h4>
 * <ul>
 *   <li><strong>UNIQUE INDEX</strong>: index_shift_types_name (name)</li>
 *   <li><strong>FILTER INDEX</strong>: index_shift_types_is_active (is_active)</li>
 *   <li><strong>TYPE INDEX</strong>: index_shift_types_is_user_defined (is_user_defined)</li>
 *   <li><strong>COMPOSITE INDEX</strong>: index_shift_types_active_user_defined (is_active, is_user_defined)</li>
 *   <li><strong>ORDER INDEX</strong>: index_shift_types_name_id (name ASC, id ASC)</li>
 * </ul>
 *
 * <h3>Migration Strategy:</h3>
 *
 * <h4>Backward Compatibility Approach:</h4>
 * <ul>
 *   <li><strong>Zero Breaking Changes</strong>: All existing APIs work unchanged</li>
 *   <li><strong>Automatic Migration</strong>: Database seeded from existing ShiftTypeFactory defaults</li>
 *   <li><strong>Gradual Adoption</strong>: Developers can migrate to new APIs at their own pace</li>
 *   <li><strong>Performance Improvement</strong>: Database operations with caching for better performance</li>
 * </ul>
 *
 * <h4>Migration Steps:</h4>
 * <ol>
 *   <li><strong>Database Creation</strong>: Migration_4_5 creates shift_types table and indexes</li>
 *   <li><strong>Data Seeding</strong>: QuattroDue automatically seeds database with default shift types</li>
 *   <li><strong>Factory Adaptation</strong>: ShiftTypeFactory becomes a facade over database operations</li>
 *   <li><strong>API Enhancement</strong>: New database-backed methods added to QuattroDue</li>
 *   <li><strong>Cache Integration</strong>: Automatic caching for improved performance</li>
 * </ol>
 *
 * <h3>Usage Patterns:</h3>
 *
 * <h4>Basic Schedule Generation:</h4>
 * <pre>
 * {@code
 * // Get QuattroDue instance (database-backed)
 * QuattroDue quattroDue = QuattroDue.getInstance(context);
 *
 * // Generate monthly schedule
 * List<Day> monthlySchedule = quattroDue.getShiftsForMonth(LocalDate.now());
 *
 * // Access shift types (database-backed)
 * List<ShiftType> shiftTypes = quattroDue.getShiftTypes();
 * ShiftType morningShift = quattroDue.getShiftTypeByName("Morning");
 * }
 * </pre>
 *
 * <h4>Database Operations:</h4>
 * <pre>
 * {@code
 * // Create custom shift type
 * ShiftType customShift = new ShiftType("Evening", "Evening shift", 18, 0, 6, 0, Color.PURPLE);
 * Long id = quattroDue.saveShiftType(customShift);
 *
 * // Update existing shift type
 * ShiftType updated = customShift.toBuilder()
 *     .setDescription("Updated evening shift")
 *     .build();
 * quattroDue.saveShiftType(updated);
 *
 * // Soft delete (deactivate)
 * quattroDue.deleteShiftType(id);
 * }
 * </pre>
 *
 * <h4>Service Integration:</h4>
 * <pre>
 * {@code
 * // WorkScheduleRepositoryImpl usage
 * QuattroDue quattroDue = QuattroDue.getInstance(context, database);
 * List<ShiftType> shifts = quattroDue.getShiftTypes(); // Database-backed
 * ShiftType shift = quattroDue.getShiftTypeById(1L);   // Direct lookup
 * }
 * </pre>
 *
 * <h3>Performance Optimizations:</h3>
 * <ul>
 *   <li><strong>Database Caching</strong>: 5-minute TTL cache for shift types</li>
 *   <li><strong>Indexed Queries</strong>: All common lookups use database indexes</li>
 *   <li><strong>Lazy Loading</strong>: Database initialized only when first accessed</li>
 *   <li><strong>Batch Operations</strong>: Multi-record operations for efficiency</li>
 *   <li><strong>Connection Pooling</strong>: Room manages database connections efficiently</li>
 * </ul>
 *
 * <h3>Error Handling and Resilience:</h3>
 * <ul>
 *   <li><strong>Graceful Degradation</strong>: Falls back to ShiftTypeFactory if database fails</li>
 *   <li><strong>Automatic Recovery</strong>: Database recreation on corruption</li>
 *   <li><strong>Validation</strong>: Input validation at entity level</li>
 *   <li><strong>Logging</strong>: Comprehensive error logging for debugging</li>
 * </ul>
 *
 * <h3>Testing Support:</h3>
 * <ul>
 *   <li><strong>In-Memory Database</strong>: Fast testing with Room in-memory implementation</li>
 *   <li><strong>Factory Reset</strong>: QuattroDue.resetInstance() for test isolation</li>
 *   <li><strong>Migration Testing</strong>: Automated migration validation</li>
 * </ul>
 *
 * @author Original QuattroDue Team
 * @author Updated 21/05/2025 - Database Integration
 * @version 2.0.0 - Database Edition
 * @since API Level 21
 * @see net.calvuz.qdue.quattrodue.QuattroDue
 * @see net.calvuz.qdue.quattrodue.models.ShiftType
 * @see net.calvuz.qdue.quattrodue.dao.ShiftTypeDao
 * @see net.calvuz.qdue.core.db.QDueDatabase
 */
package net.calvuz.qdue.quattrodue;