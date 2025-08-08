/**
 * Models Package for QDue Quattrodue Shift Management System.
 * <p>
 * This package contains core data models for shift management with Google Calendar-like
 * functionality, featuring UUID-based identification system and comprehensive internationalization
 * support. All models are designed to be dependency injection compliant and follow immutable
 * value object patterns where applicable.
 *
 * <h3>Core Models:</h3>
 *
 * <h4>{@link net.calvuz.qdue.quattrodue.models.ShiftType} - Enhanced Shift Type Model</h4>
 * <p>
 * Central model representing a shift template with scheduling, duration, and visual properties.
 * Enhanced with UUID support while maintaining full backward compatibility.
 * </p>
 *
 * <h4>Key Features:</h4>
 * <ul>
 *   <li><strong>UUID Identification</strong>: Each ShiftType has a unique UUID for global identification</li>
 *   <li><strong>Backward Compatibility</strong>: Existing code continues to work without modifications</li>
 *   <li><strong>Automatic ID Generation</strong>: UUIDs are generated automatically when not provided</li>
 *   <li><strong>Google Calendar-like Design</strong>: Structured for calendar integration patterns</li>
 *   <li><strong>Internationalization Ready</strong>: Uses {@link net.calvuz.qdue.core.architecture.common.i18n.I18nUtils}</li>
 *   <li><strong>Immutable Design</strong>: Value object with builder-like methods for updates</li>
 *   <li><strong>Color Management</strong>: Automatic text color calculation for optimal readability</li>
 * </ul>
 *
 * <h3>UUID Integration Architecture:</h3>
 * <pre>
 * ShiftType Creation Flow:
 * ┌─────────────────────────────────────────────────────────────┐
 * │                    New ShiftType                            │
 * ├─────────────────────────────────────────────────────────────┤
 * │ Constructor with ID  → Use provided UUID                    │
 * │ Constructor without ID → Auto-generate UUID                 │
 * │ Copy Constructor     → Generate new UUID                    │
 * │ Legacy Data Load     → Generate UUID for existing data     │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Cache System:
 * ┌──────────────────┬─────────────────────┬──────────────────────┐
 * │ ID Cache         │ Name Cache          │ Index Cache          │
 * │ UUID → ShiftType │ String → ShiftType  │ Int → ShiftType      │
 * ├──────────────────┼─────────────────────┼──────────────────────┤
 * │ Primary Lookup   │ Backward Compat.    │ Backward Compat.     │
 * │ Fast O(1) access │ Legacy name lookup  │ Legacy index lookup  │
 * └──────────────────┴─────────────────────┴──────────────────────┘
 * </pre>
 *
 * <h3>Usage Examples:</h3>
 *
 * <h4>Creating New ShiftTypes:</h4>
 * <pre>
 * {@code
 * // Modern approach with explicit ID
 * ShiftType morning = new ShiftType(
 *     "custom-uuid-here",     // ID (can be null for auto-generation)
 *     "Morning Shift",        // Name
 *     "Early morning hours",  // Description
 *     6, 0,                   // Start: 06:00
 *     8, 0,                   // Duration: 8 hours
 *     Color.BLUE             // Color
 * );
 *
 * // Backward compatible approach (auto-generates UUID)
 * ShiftType afternoon = new ShiftType(
 *     "Afternoon Shift",      // Name
 *     "Afternoon hours",      // Description
 *     14, 0,                  // Start: 14:00
 *     8, 0,                   // Duration: 8 hours
 *     Color.GREEN            // Color
 * );
 *
 * // Copy with new UUID
 * ShiftType cloned = new ShiftType(morning);
 *
 * // Copy with specific UUID
 * ShiftType migrated = new ShiftType(afternoon, "specific-uuid");
 * }
 * </pre>
 *
 * <h4>Factory Usage Examples:</h4>
 * <pre>
 * {@code
 * // Initialize factory (backward compatible)
 * ShiftTypeFactory.initialize(context, 3).thenAccept(success -> {
 *     if (success) {
 *         // Access by UUID (new method)
 *         ShiftType shift = ShiftTypeFactory.getShiftTypeById("uuid-here");
 *
 *         // Access by name (existing method)
 *         ShiftType morning = ShiftTypeFactory.getShiftType("Morning");
 *
 *         // Access by index (existing method)
 *         ShiftType first = ShiftTypeFactory.getShiftType(0);
 *
 *         // Get all UUIDs
 *         List<String> allIds = ShiftTypeFactory.getAllShiftTypeIds();
 *     }
 * });
 *
 * // Update by UUID
 * ShiftType updated = morning.withUpdatedProperties(
 *     "New Morning",    // New name
 *     null,             // Keep description
 *     Color.RED         // New color
 * );
 * ShiftTypeFactory.updateShiftTypeById(morning.getId(), updated);
 * }
 * </pre>
 *
 * <h3>Data Migration Strategy:</h3>
 * <p>
 * The system automatically handles migration from legacy data without UUIDs:
 * </p>
 * <ul>
 *   <li><strong>Storage Version</strong>: Cache version incremented to detect legacy data</li>
 *   <li><strong>Auto-UUID Generation</strong>: Missing UUIDs are generated during load</li>
 *   <li><strong>Transparent Upgrade</strong>: Data is automatically re-saved with UUIDs</li>
 *   <li><strong>Zero Breaking Changes</strong>: All existing code continues to function</li>
 * </ul>
 *
 * <h3>Internationalization Integration:</h3>
 * <p>
 * Models integrate with {@link net.calvuz.qdue.core.infrastructure.common.i18n.LocaleManager}
 * for localized content formatting and display.
 * </p>
 * <pre>
 * {@code
 * // Standard time range formatting
 * String timeRange = shiftType.getTimeRange();
 *
 * // Get localized context using LocaleManager
 * LocaleManager localeManager = new LocaleManager(context);
 * Context localizedContext = localeManager.updateContextLocale(context);
 * }
 * </pre>
 *
 * <h3>Dependency Injection Compatibility:</h3>
 * <p>
 * The models and factory are designed to work seamlessly with the core DI system
 * defined in {@link net.calvuz.qdue.core.infrastructure.di}:
 * </p>
 * <ul>
 *   <li><strong>No Static Dependencies</strong>: Factory methods accept Context parameters</li>
 *   <li><strong>Service Layer Ready</strong>: Compatible with ShiftTypeService implementations</li>
 *   <li><strong>Testable Design</strong>: Easy to mock and test in isolation</li>
 *   <li><strong>Async Operations</strong>: CompletableFuture support for non-blocking operations</li>
 * </ul>
 *
 * <h3>Performance Considerations:</h3>
 * <ul>
 *   <li><strong>Multi-Cache Strategy</strong>: O(1) lookup by ID, name, or index</li>
 *   <li><strong>Lazy Initialization</strong>: Factory initializes only when needed</li>
 *   <li><strong>Memory Efficient</strong>: Immutable objects with shared references</li>
 *   <li><strong>Thread Safe</strong>: ConcurrentHashMap for cache storage</li>
 * </ul>
 *
 * <h3>Testing Support:</h3>
 * <p>
 * The design facilitates comprehensive testing:
 * </p>
 * <pre>
 * {@code
 * // Test UUID generation
 * ShiftType shift1 = new ShiftType("Test", "Test", 8, 0, 8, 0, Color.BLUE);
 * ShiftType shift2 = new ShiftType("Test", "Test", 8, 0, 8, 0, Color.BLUE);
 * assertNotEquals(shift1.getId(), shift2.getId()); // Different UUIDs
 *
 * // Test backward compatibility
 * ShiftType legacy = new ShiftType("Morning", "Morning shift", 6, 0, 8, 0, Color.BLUE);
 * assertNotNull(legacy.getId()); // UUID was generated
 *
 * // Test factory caching
 * ShiftTypeFactory.addShiftType(shift1);
 * assertEquals(shift1, ShiftTypeFactory.getShiftTypeById(shift1.getId()));
 * }
 * </pre>
 *
 * @author Luke (original ShiftType implementation)
 * @author Updated 08/08/2025 - UUID Support and Enhanced Architecture
 * @version 2.0
 * @since QDue 1.0
 *
 * @see net.calvuz.qdue.core.infrastructure.common.i18n.LocaleManager
 * @see net.calvuz.qdue.core.infrastructure.di.ServiceProvider
 * @see net.calvuz.qdue.core.infrastructure.services.ShiftTypeService
 */
package net.calvuz.qdue.quattrodue.models;