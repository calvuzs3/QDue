/**
 * Utility Classes and Helper Components
 * <p>
 * Contains utility classes providing common functionality used
 * throughout the SmartShifts application. These classes are
 * stateless and provide pure functions for data manipulation,
 * validation, and formatting.
 *
 * <h2>Utility Classes</h2>
 * <ul>
 *   <li><strong>DateTimeHelper</strong> - Date/time operations + formatting + i18n</li>
 *   <li><strong>ColorHelper</strong> - Color manipulation + contrast calculation</li>
 *   <li><strong>StringHelper</strong> - String formatting + localization support</li>
 *   <li><strong>ValidationHelper</strong> - Input validation + error handling</li>
 *   <li><strong>JsonHelper</strong> - JSON serialization + validation</li>
 *   <li><strong>NotificationHelper</strong> - Shift notification system + channels</li>
 * </ul>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Internationalization</strong> - Full i18n support for all utilities</li>
 *   <li><strong>Type Safety</strong> - Generic methods with proper type constraints</li>
 *   <li><strong>Error Handling</strong> - Consistent error handling patterns</li>
 *   <li><strong>Performance</strong> - Optimized algorithms for common operations</li>
 * </ul>
 *
 * <h2>Usage Patterns</h2>
 * <ul>
 *   <li>Static methods for stateless operations</li>
 *   <li>Context-aware methods for Android-specific operations</li>
 *   <li>Builder patterns for complex object creation</li>
 *   <li>Null-safe operations with Optional returns</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * Utilities are injected via Hilt where needed but can also be
 * used as static utility classes for simple operations.
 *
 * @author SmartShifts Team
 * @since Phase 2 - Business Logic
 */
package net.calvuz.qdue.smartshifts.utils;