/**
 * UI Features layer containing all presentation logic and user interface components.
 *
 * <p>This package organizes UI components by feature areas, each representing
 * a distinct user-facing functionality. Each feature is self-contained with
 * its own views, adapters, fragments, and view models.</p>
 *
 * <h3>Feature Packages:</h3>
 *
 * <h4>{@link com.yourapp.calendar.ui.features.calendar} - Calendar View Feature</h4>
 * <ul>
 *   <li>Monthly, weekly, and daily calendar views</li>
 *   <li>Event visualization with work schedule integration</li>
 *   <li>Calendar navigation and date selection</li>
 *   <li>Event details display and quick actions</li>
 * </ul>
 *
 * <h4>{@link com.yourapp.calendar.ui.features.dayslist} - Days List Feature</h4>
 * <ul>
 *   <li>Linear list view of calendar days</li>
 *   <li>Day-based navigation alternative to calendar grid</li>
 *   <li>Compact event summary per day</li>
 *   <li>Work schedule visualization in list format</li>
 * </ul>
 *
 * <h4>{@link com.yourapp.calendar.ui.features.events} - Events Management Feature</h4>
 * <ul>
 *   <li>Event creation and editing forms</li>
 *   <li>Event list management and filtering</li>
 *   <li>Recurrence rule configuration</li>
 *   <li>Work schedule exception handling</li>
 *   <li>Overtime and additional shifts management</li>
 * </ul>
 *
 * <h4>{@link com.yourapp.calendar.ui.features.settings} - Settings Feature</h4>
 * <ul>
 *   <li>User preferences configuration</li>
 *   <li>Team selection and work schedule setup</li>
 *   <li>UI theme and display preferences</li>
 *   <li>Notification and sync settings</li>
 * </ul>
 *
 * <h4>{@link com.yourapp.calendar.ui.features.welcome} - Welcome/Onboarding Feature</h4>
 * <ul>
 *   <li>First-time user onboarding flow</li>
 *   <li>Application introduction and setup</li>
 *   <li>Initial configuration wizard</li>
 *   <li>User profile setup</li>
 * </ul>
 *
 * <h4>{@link com.yourapp.calendar.ui.features.common} - Common UI Components</h4>
 * <ul>
 *   <li>Shared custom views and components</li>
 *   <li>Base activity and fragment classes</li>
 *   <li>Common adapters and view holders</li>
 *   <li>Reusable UI utilities and extensions</li>
 * </ul>
 *
 * <h3>UI Architecture Patterns:</h3>
 * <ul>
 *   <li><strong>MVVM Pattern</strong> - ViewModel for presentation logic</li>
 *   <li><strong>Observer Pattern</strong> - UI observes domain state changes</li>
 *   <li><strong>Adapter Pattern</strong> - RecyclerView adapters for lists</li>
 *   <li><strong>Fragment Pattern</strong> - Modular UI composition</li>
 * </ul>
 *
 * <h3>Feature Structure:</h3>
 * <pre>
 * Each feature contains:
 * ├── view/        → Activities and custom views
 * ├── adapter/     → RecyclerView adapters
 * ├── fragment/    → UI fragments
 * └── viewmodel/   → Presentation logic
 * </pre>
 *
 * <h3>Key UI Principles:</h3>
 * <ul>
 *   <li>Features are self-contained and independently testable</li>
 *   <li>ViewModels handle presentation logic and domain interactions</li>
 *   <li>Views are passive and delegate user actions to ViewModels</li>
 *   <li>Common components are shared via the common package</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since API Level 21
 */
package net.calvuz.qdue.ui.features;
