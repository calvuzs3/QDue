/**
 * SwipeCalendar Presentation Layer - UI Components and Activities
 *
 * <p>This package contains the presentation layer components for the SwipeCalendar feature,
 * implementing a google-calendar-like month-to-month navigation with finite range boundaries.
 * All components follow clean architecture principles with full dependency injection support
 * and comprehensive internationalization.</p>
 *
 * <h3>Architecture Overview:</h3>
 * <ul>
 *   <li><strong>Clean Architecture</strong>: Strict separation between UI, domain, and data layers</li>
 *   <li><strong>Dependency Injection</strong>: Injectable interface implementation throughout</li>
 *   <li><strong>Internationalization</strong>: LocaleManager integration for multi-language support</li>
 *   <li><strong>Material Design</strong>: Google Material Design 3 components and patterns</li>
 * </ul>
 *
 * <h3>Core Components:</h3>
 *
 * <h4>{@link net.calvuz.qdue.ui.features.swipecalendar.presentation.SwipeCalendarFragment}</h4>
 * <ul>
 *   <li><strong>Main Fragment</strong>: Core calendar implementation with ViewPager2</li>
 *   <li><strong>Finite Range</strong>: January 1900 to December 2100 (2400 months)</li>
 *   <li><strong>State Management</strong>: Persistent position across sessions</li>
 *   <li><strong>Event Integration</strong>: Full EventsService and WorkScheduleRepository support</li>
 *   <li><strong>Navigation Controls</strong>: Previous/Next month, "Go to Today" functionality</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.ui.features.swipecalendar.presentation.SwipeCalendarActivity}</h4>
 * <ul>
 *   <li><strong>Debug Container</strong>: Simplified hosting activity for development</li>
 *   <li><strong>Isolation Testing</strong>: Clean environment without QDueMainActivity complexity</li>
 *   <li><strong>Dependency Injection</strong>: Full ServiceProvider integration</li>
 *   <li><strong>Locale Support</strong>: Dynamic language switching capabilities</li>
 *   <li><strong>Intent Parameters</strong>: Support for initial date and user ID configuration</li>
 * </ul>
 *
 * <h3>Dependency Injection Pattern:</h3>
 * <pre>
 * // All presentation components implement Injectable:
 * public class SwipeCalendarActivity extends AppCompatActivity implements Injectable {
 *
 *     private ServiceProvider mServiceProvider;
 *     private LocaleManager mLocaleManager;
 *
 *     {@literal @}Override
 *     public void inject(ServiceProvider serviceProvider) {
 *         mServiceProvider = serviceProvider;
 *         mLocaleManager = new LocaleManager(getApplicationContext());
 *     }
 *
 *     {@literal @}Override
 *     public boolean areDependenciesReady() {
 *         return mServiceProvider != null && mServiceProvider.areServicesReady();
 *     }
 * }
 * </pre>
 *
 * <h3>Internationalization Integration:</h3>
 * <pre>
 * // LocaleManager usage for string localization:
 * private String getLocalizedString(String key, String fallback) {
 *     return mLocaleManager.getLocalizedString(this, key, fallback);
 * }
 *
 * // Example usage:
 * String title = getLocalizedString("swipe_calendar_title", "Calendar");
 * String todayButton = getLocalizedString("button_go_to_today", "Today");
 * </pre>
 *
 * <h3>Navigation and State Management:</h3>
 * <ul>
 *   <li><strong>ViewPager2 Navigation</strong>: Smooth horizontal swipe between months</li>
 *   <li><strong>Position Mapping</strong>: Adapter position ↔ YearMonth conversion</li>
 *   <li><strong>Range Boundaries</strong>: Automatic swipe blocking at 1900/2100 limits</li>
 *   <li><strong>State Persistence</strong>: SharedPreferences integration for last position</li>
 *   <li><strong>Animation Support</strong>: Hardware-accelerated transitions</li>
 * </ul>
 *
 * <h3>Integration with Core Services:</h3>
 * <ul>
 *   <li><strong>EventsService</strong>: Event data retrieval and management</li>
 *   <li><strong>UserService</strong>: User context and preferences</li>
 *   <li><strong>WorkScheduleRepository</strong>: Work schedule and shift information</li>
 *   <li><strong>SwipeCalendarModule</strong>: Feature-specific dependency injection</li>
 * </ul>
 *
 * <h3>Development and Debug Features:</h3>
 * <ul>
 *   <li><strong>SwipeCalendarActivity</strong>: Isolated testing environment</li>
 *   <li><strong>Debug Logging</strong>: Comprehensive log messages with TAG classification</li>
 *   <li><strong>Parameter Support</strong>: Initial date and user configuration via Intent</li>
 *   <li><strong>Error Handling</strong>: Graceful degradation and error reporting</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 *
 * <h4>Basic Fragment Integration:</h4>
 * <pre>
 * // In any Activity with ServiceProvider:
 * SwipeCalendarFragment fragment = SwipeCalendarFragment.newInstance(null, null);
 * getSupportFragmentManager()
 *     .beginTransaction()
 *     .replace(R.id.container, fragment)
 *     .commit();
 * </pre>
 *
 * <h4>Debug Activity Launch:</h4>
 * <pre>
 * // Launch for debugging with specific date:
 * Intent intent = SwipeCalendarActivity.createIntent(context, LocalDate.now());
 * startActivity(intent);
 *
 * // Launch with user context:
 * Intent intent = SwipeCalendarActivity.createIntent(context, null, userID);
 * startActivity(intent);
 * </pre>
 *
 * <h4>Navigation Integration:</h4>
 * <pre>
 * // Add to mobile_navigation.xml:
 * &lt;fragment
 *     android:id="@+id/nav_swipe_calendar"
 *     android:name="net.calvuz.qdue.ui.features.swipecalendar.presentation.SwipeCalendarFragment"
 *     android:label="@string/menu_swipe_calendar" /&gt;
 * </pre>
 *
 * <h3>String Resources Required:</h3>
 * <ul>
 *   <li><strong>debug_swipe_calendar_title</strong>: Activity title for debug mode</li>
 *   <li><strong>debug_loading_calendar</strong>: Loading message</li>
 *   <li><strong>debug_info_title</strong>: Debug panel title</li>
 *   <li><strong>debug_close</strong>: Close button text</li>
 *   <li><strong>debug_mode_active</strong>: Debug status message</li>
 *   <li><strong>navigation_back</strong>: Back button content description</li>
 * </ul>
 *
 * <h3>Drawable Resources Required:</h3>
 * <ul>
 *   <li><strong>ic_bug_report_24</strong>: Debug icon for info panel</li>
 *   <li><strong>ic_debug_24</strong>: Debug status icon</li>
 * </ul>
 *
 * <h3>Best Practices:</h3>
 * <ul>
 *   <li><strong>Dependency Injection</strong>: Always inject dependencies through constructor or ServiceProvider</li>
 *   <li><strong>Error Handling</strong>: Use comprehensive try-catch blocks with meaningful log messages</li>
 *   <li><strong>Internationalization</strong>: Never hardcode strings - use LocaleManager for all text</li>
 *   <li><strong>State Management</strong>: Handle configuration changes and process death gracefully</li>
 *   <li><strong>Performance</strong>: Use ViewPager2 offscreen page limit and optimize fragment lifecycle</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - SwipeCalendar Presentation Layer
 * @since SwipeCalendar Feature Implementation
 */
package net.calvuz.qdue.ui.features.swipecalendar.presentation;