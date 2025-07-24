/**
 * Export/Import User Interface (Phase 4D)
 * <p>
 * Complete user interface for the export/import system providing
 * intuitive access to data backup, restore, and migration features.
 * Supports multiple formats and cloud storage integration.
 *
 * <h2>Components</h2>
 * <ul>
 *   <li><strong>SmartShiftsExportImportActivity</strong> - Complete UI (800+ LOC)</li>
 *   <li><strong>ExportImportViewModel</strong> - Business logic (600+ LOC)</li>
 *   <li><strong>RecentOperationsAdapter</strong> - Operation history adapter</li>
 * </ul>
 *
 * <h2>User Interface Features</h2>
 * <ul>
 *   <li>Tabbed interface (Export/Import)</li>
 *   <li>Format selection with recommendations</li>
 *   <li>Progress tracking with cancellation</li>
 *   <li>Recent operations history</li>
 *   <li>Cloud storage integration</li>
 * </ul>
 *
 * <h2>Export Options</h2>
 * <ul>
 *   <li>Complete data export</li>
 *   <li>Selective export by data type</li>
 *   <li>Calendar-specific export</li>
 *   <li>Quick export with FAB</li>
 * </ul>
 *
 * <h2>Import Options</h2>
 * <ul>
 *   <li>File picker integration</li>
 *   <li>Cloud storage import</li>
 *   <li>Backup restoration</li>
 *   <li>Conflict resolution dialogs</li>
 * </ul>
 *
 * @author SmartShifts Team
 * @since Phase 4D - Export/Import System
 */
package net.calvuz.qdue.smartshifts.ui.exportimport;