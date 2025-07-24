/**
 * SmartShifts Presentation Layer
 * <p>
 * Contains all user interface components implementing MVVM architecture
 * with Material Design 3 compliance. The UI layer is organized by
 * feature modules with clear separation of concerns between Activities,
 * Fragments, and ViewModels.
 *
 * <h2>Architecture</h2>
 * <pre>
 * ui/
 * ├── main/          → Main activity and navigation
 * ├── setup/         → Initial setup wizard (4 steps)
 * ├── calendar/      → Calendar view system
 * ├── patterns/      → Pattern management UI
 * ├── contacts/      → Team contacts UI
 * ├── settings/      → Settings and configuration
 * └── exportimport/  → Export/Import UI (Phase 4D)
 * </pre>
 *
 * <h2>Design System</h2>
 * <ul>
 *   <li><strong>Material Design 3</strong> - Latest design system compliance</li>
 *   <li><strong>Dark Mode Support</strong> - System theme awareness</li>
 *   <li><strong>Responsive Layouts</strong> - Tablet and phone optimization</li>
 *   <li><strong>Accessibility</strong> - Screen reader and navigation support</li>
 * </ul>
 *
 * <h2>Navigation Structure</h2>
 * <ul>
 *   <li>Bottom Navigation with 4 main sections</li>
 *   <li>Fragment-based navigation within sections</li>
 *   <li>Deep linking support for external access</li>
 * </ul>
 *
 * @author SmartShifts Team
 * @since Phase 3 - UI Foundation
 */
package net.calvuz.qdue.smartshifts.ui;