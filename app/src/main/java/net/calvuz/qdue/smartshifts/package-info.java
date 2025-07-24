/**
 * SmartShifts - Advanced Shift Management System for QDue
 * <p>
 * SmartShifts is a comprehensive shift scheduling and management system designed
 * as a modular extension to the QDue application. It provides advanced features
 * for continuous cycle shift patterns, team coordination, and data management.
 *
 * <h2>Architecture Overview</h2>
 * The system follows Clean Architecture principles with clear separation of concerns:
 * <ul>
 *   <li><strong>Data Layer</strong> - Database entities, DAOs, and repositories</li>
 *   <li><strong>Domain Layer</strong> - Business logic, use cases, and algorithms</li>
 *   <li><strong>Presentation Layer</strong> - UI components, ViewModels, and fragments</li>
 * </ul>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Advanced shift generation with continuous cycle validation</li>
 *   <li>4 predefined patterns (4-2, 3-2, 5-2, 6-1) + custom pattern support</li>
 *   <li>Team coordination and contact management</li>
 *   <li>Comprehensive export/import system (JSON, CSV, XML, iCal, Excel)</li>
 *   <li>Material Design 3 compliant UI with dark mode support</li>
 *   <li>Complete internationalization (i18n) support</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>Hilt for Dependency Injection</li>
 *   <li>Room for Database management</li>
 *   <li>Material Design 3 for UI components</li>
 *   <li>LiveData/ViewModel for MVVM architecture</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * SmartShifts integrates seamlessly with existing QDue functionality through
 * {@link net.calvuz.qdue.smartshifts.integration.SmartShiftsLauncher} without
 * affecting existing code or data.
 *
 * @author SmartShifts Development Team
 * @version 1.0.0
 * @since Phase 1 - Core Implementation
 */
package net.calvuz.qdue.smartshifts;