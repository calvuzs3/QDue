/**
 * QDue Integration Components
 * <p>
 * Contains components responsible for seamless integration with
 * the existing QDue application. Provides entry points and
 * coordination between SmartShifts and the main application
 * without breaking existing functionality.
 *
 * <h2>Integration Strategy</h2>
 * <ul>
 *   <li><strong>Zero Breaking Changes</strong> - No impact on existing QDue code</li>
 *   <li><strong>Separate Database</strong> - Independent data storage</li>
 *   <li><strong>Shared Resources</strong> - Coordinated theme and styling</li>
 *   <li><strong>Navigation Integration</strong> - Added to existing drawer menu</li>
 * </ul>
 *
 * <h2>Components</h2>
 * <ul>
 *   <li><strong>SmartShiftsLauncher</strong> - Main entry point from QDue</li>
 * </ul>
 *
 * <h2>Integration Points</h2>
 * <ul>
 *   <li>Navigation drawer menu item</li>
 *   <li>Shared application context</li>
 *   <li>Common theme and styling resources</li>
 *   <li>Notification channels coordination</li>
 * </ul>
 *
 * <h2>Rollback Safety</h2>
 * The integration is designed to be completely reversible:
 * <ul>
 *   <li>Independent database can be safely removed</li>
 *   <li>No modifications to existing QDue classes</li>
 *   <li>Resources are namespaced to avoid conflicts</li>
 * </ul>
 *
 * @author SmartShifts Team
 * @since Phase 1 - Core Implementation
 */
package net.calvuz.qdue.smartshifts.integration;