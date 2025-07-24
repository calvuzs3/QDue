/**
 * Main Activity and Navigation
 * <p>
 * Contains the main entry point activity and navigation coordination
 * for the SmartShifts application. Implements bottom navigation
 * with 4 main sections and handles app-level state management.
 *
 * <h2>Components</h2>
 * <ul>
 *   <li><strong>SmartShiftsActivity</strong> - Main activity with bottom navigation</li>
 *   <li><strong>SmartShiftsViewModel</strong> - App-level state management</li>
 * </ul>
 *
 * <h2>Navigation Sections</h2>
 * <ul>
 *   <li><strong>Calendar</strong> - Shift calendar view</li>
 *   <li><strong>Patterns</strong> - Pattern management</li>
 *   <li><strong>Contacts</strong> - Team coordination</li>
 *   <li><strong>Settings</strong> - Configuration and preferences</li>
 * </ul>
 *
 * <h2>State Management</h2>
 * <ul>
 *   <li>First-time setup detection</li>
 *   <li>Active assignment checking</li>
 *   <li>Navigation state preservation</li>
 *   <li>Error handling and user feedback</li>
 * </ul>
 *
 * @author SmartShifts Team
 * @since Phase 3 - UI Foundation
 */
package net.calvuz.qdue.smartshifts.ui.main;