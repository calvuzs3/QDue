/**
 * Calendar feature package providing Google Calendar-like functionality.
 *
 * <p>This package implements a comprehensive calendar view system that integrates
 * work schedules with events to provide a unified scheduling experience. The system
 * follows clean architecture principles with dependency injection and modular design.</p>
 *
 * <h3>Feature Architecture:</h3>
 * <pre>
 * ui.features.calendar/
 * ├── presentation/          → UI Layer (Activities, Fragments)
 * ├── adapters/             → RecyclerView Adapters
 * ├── components/           → Custom View Components
 * ├── interfaces/           → Feature Contracts
 * ├── models/              → UI Data Models
 * ├── providers/           → Data Provider Implementations
 * └── di/                  → Dependency Injection Module
 * </pre>
 *
 * <h3>Core Components:</h3>
 *
 * <h4>{@link net.calvuz.qdue.ui.features.calendar.presentation.CalendarActivity} - Main Activity</h4>
 * <ul>
 *   <li><strong>Injectable Architecture</strong> - Dependency injection compliance</li>
 *   <li><strong>Navigation Setup</strong> - Integration with Navigation Component</li>
 *   <li><strong>Back Handling</strong> - Custom back press handling service</li>
 *   <li><strong>Module Management</strong> - CalendarModule lifecycle management</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.ui.features.calendar.presentation.CalendarViewFragment} - Main Fragment</h4>
 * <ul>
 *   <li><strong>ViewPager2 Integration</strong> - Smooth month navigation with swipe gestures</li>
 *   <li><strong>Loading States</strong> - Skeleton UI and loading indicators</li>
 *   <li><strong>Event Handling</strong> - Comprehensive day and event interactions</li>
 *   <li><strong>Data Integration</strong> - Work schedule and events combination</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.ui.features.calendar.adapters.CalendarPagerAdapter} - Month Navigation</h4>
 * <ul>
 *   <li><strong>Infinite Scroll</strong> - ViewPager2 with intelligent position mapping</li>
 *   <li><strong>Memory Efficient</strong> - ViewHolder recycling and caching</li>
 *   <li><strong>Data Synchronization</strong> - Month data change notifications</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.ui.features.calendar.adapters.MonthViewAdapter} - Month Grid</h4>
 * <ul>
 *   <li><strong>42-Cell Grid</strong> - Fixed 7x6 layout for consistent UI</li>
 *   <li><strong>Overflow Handling</strong> - Previous/next month days included</li>
 *   <li><strong>Dynamic Updates</strong> - Individual day updates and bulk refresh</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.ui.features.calendar.components.CalendarDayViewHolder} - Day Cell</h4>
 * <ul>
 *   <li><strong>Shift Integration</strong> - Background colors from ShiftType.colorHex</li>
 *   <li><strong>Event Indicators</strong> - EventType icons and count badges</li>
 *   <li><strong>Visual States</strong> - Today, weekend, current month styling</li>
 *   <li><strong>Interactive</strong> - Click and long-click handling</li>
 * </ul>
 *
 * <h3>Data Flow Architecture:</h3>
 *
 * <h4>Service Integration</h4>
 * <pre>
 * CalendarDataProvider
 * ├── WorkScheduleService    → Base shift schedules (4-2 pattern)
 * ├── EventsService         → Events that modify schedules
 * ├── UserService          → User context and filtering
 * └── ShiftTypeService     → Effective shift calculation
 * </pre>
 *
 * <h4>Data Transformation Pipeline</h4>
 * <pre>
 * 1. Load Work Schedules     → List&lt;WorkScheduleEvent&gt;
 * 2. Load Events            → List&lt;LocalEvent&gt;
 * 3. Calculate Effective    → Map&lt;LocalDate, ShiftTypeEntity&gt;
 * 4. Merge Data            → List&lt;CalendarDay&gt;
 * 5. Apply UI Styling      → Background colors, indicators
 * </pre>
 *
 * <h3>Dependency Injection Pattern:</h3>
 *
 * <h4>{@link net.calvuz.qdue.ui.features.calendar.di.CalendarModule} - Feature DI Module</h4>
 * <ul>
 *   <li><strong>Service Dependencies</strong> - EventsService, WorkScheduleService, UserService</li>
 *   <li><strong>Provider Creation</strong> - CalendarDataProvider with caching</li>
 *   <li><strong>Adapter Management</strong> - CalendarPagerAdapter and MonthViewAdapter</li>
 *   <li><strong>Lifecycle Compliance</strong> - Proper cleanup and resource management</li>
 * </ul>
 *
 * <h4>Service Provider Integration</h4>
 * <pre>
 * CalendarActivity implements Injectable {
 *     &#64;Override
 *     public void inject(ServiceProvider serviceProvider) {
 *         mEventsService = serviceProvider.getEventsService();
 *         mWorkScheduleService = serviceProvider.getWorkScheduleService();
 *         mUserService = serviceProvider.getUserService();
 *     }
 * }
 * </pre>
 *
 * <h3>Key Features Implementation:</h3>
 *
 * <h4>Shift Color Integration</h4>
 * <pre>
 * // ShiftType background colors
 * ShiftTypeEntity shift = calendarDay.getEffectiveShift();
 * String colorHex = shift.getColorHex(); // e.g., "#FFE4B5"
 * int backgroundColor = Color.parseColor(colorHex);
 * dayView.setBackgroundColor(backgroundColor);
 * </pre>
 *
 * <h4>Event Type Icons</h4>
 * <pre>
 * // EventType drawable icons
 * EventType eventType = event.getEventType();
 * int iconRes = eventType.getIconRes(); // ic_rounded_*_24
 * eventIcon.setImageResource(iconRes);
 * eventIcon.setColorFilter(eventType.getColor());
 * </pre>
 *
 * <h4>Loading States</h4>
 * <pre>
 * // Skeleton UI during data loading
 * CalendarLoadingState state = dataProvider.getLoadingState();
 * switch (state) {
 *     case LOADING:   showSkeletonUI(); break;
 *     case LOADED:    showCalendarData(); break;
 *     case ERROR:     showErrorState(); break;
 * }
 * </pre>
 *
 * <h3>Usage Examples:</h3>
 *
 * <h4>Launch Calendar Activity</h4>
 * <pre>
 * // Launch with today's date
 * Intent intent = CalendarActivity.createIntent(context);
 * startActivity(intent);
 *
 * // Launch with specific date and view mode
 * LocalDate targetDate = LocalDate.of(2025, 1, 15);
 * Intent intent = CalendarActivity.createIntent(
 *     context, targetDate, ViewMode.MONTH, userId);
 * startActivity(intent);
 * </pre>
 *
 * <h4>Handle Calendar Events</h4>
 * <pre>
 * &#64;Override
 * public void onDayClick(LocalDate date, CalendarDay calendarDay) {
 *     // Show day details or create event
 *     if (calendarDay.hasEvents()) {
 *         showDayEventsDialog(date, calendarDay.getEvents());
 *     } else {
 *         showCreateEventDialog(date);
 *     }
 * }
 *
 * &#64;Override
 * public void onEventClick(LocalEvent event, LocalDate date) {
 *     // Navigate to event details
 *     NavController navController = findNavController();
 *     Bundle args = new Bundle();
 *     args.putLong("event_id", event.getId());
 *     navController.navigate(R.id.eventDetailsFragment, args);
 * }
 * </pre>
 *
 * <h4>Custom Data Provider</h4>
 * <pre>
 * public class CustomCalendarDataProvider implements CalendarDataProvider {
 *
 *     &#64;Override
 *     public CompletableFuture&lt;List&lt;CalendarDay&gt;&gt; loadMonthData(YearMonth month) {
 *         return CompletableFuture.supplyAsync(() -&gt; {
 *             // Custom data loading logic
 *             List&lt;CalendarDay&gt; days = new ArrayList&lt;&gt;();
 *             // ... load and process data
 *             return days;
 *         });
 *     }
 * }
 * </pre>
 *
 * <h3>Performance Optimizations:</h3>
 * <ul>
 *   <li><strong>ViewPager2 Caching</strong> - 3 months kept in memory with lazy loading</li>
 *   <li><strong>Data Provider Caching</strong> - 12 months cache with expiry (10 minutes)</li>
 *   <li><strong>Efficient Updates</strong> - Individual day updates vs full month refresh</li>
 *   <li><strong>Async Loading</strong> - All data operations on background threads</li>
 *   <li><strong>Memory Management</strong> - Automatic cache cleanup and lifecycle-aware components</li>
 * </ul>
 *
 * <h3>Internationalization Support:</h3>
 * <ul>
 *   <li><strong>Localized Strings</strong> - All UI text supports multiple languages</li>
 *   <li><strong>Date Formatting</strong> - Locale-aware date and time formatting</li>
 *   <li><strong>Week Start Day</strong> - Configurable Monday/Sunday week start</li>
 *   <li><strong>EventType Integration</strong> - Uses EventTypeCategory localized names</li>
 * </ul>
 *
 * <h3>Testing Strategy:</h3>
 * <ul>
 *   <li><strong>Unit Tests</strong> - CalendarModule, data providers, and models</li>
 *   <li><strong>Integration Tests</strong> - Service integration and data flow</li>
 *   <li><strong>UI Tests</strong> - Fragment interactions and navigation</li>
 *   <li><strong>Performance Tests</strong> - Memory usage and scrolling performance</li>
 * </ul>
 *
 * <h3>Architecture Benefits:</h3>
 * <ul>
 *   <li><strong>Modularity</strong> - Clean separation of concerns with dependency injection</li>
 *   <li><strong>Testability</strong> - Injectable dependencies and interface-based design</li>
 *   <li><strong>Maintainability</strong> - Clear package structure and comprehensive documentation</li>
 *   <li><strong>Extensibility</strong> - Plugin architecture for custom data providers</li>
 *   <li><strong>Performance</strong> - Optimized for smooth scrolling and memory efficiency</li>
 *   <li><strong>User Experience</strong> - Loading states, error handling, and responsive interactions</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0 - Google Calendar-like implementation with work schedule integration
 * @since Database Version 6
 */
package net.calvuz.qdue.ui.features.calendar;