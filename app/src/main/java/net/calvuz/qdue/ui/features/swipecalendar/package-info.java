/**
 * Swipe Calendar Feature Module for QDue Android Application.
 *
 * <p>This package provides a simplified month-to-month calendar view with horizontal
 * swipe navigation, as an alternative to the infinite scrolling CalendarViewFragment.
 * Designed for users who prefer discrete month navigation with clear boundaries.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Finite Range Navigation</strong>: January 1900 to December 2100 (2400 months)</li>
 *   <li><strong>Swipe Boundaries</strong>: Automatic swipe blocking at range limits</li>
 *   <li><strong>State Persistence</strong>: Remembers user's last viewed month position</li>
 *   <li><strong>"Go to Today"</strong>: Quick navigation to current month with smooth animation</li>
 *   <li><strong>Memory Efficient</strong>: ViewPager2 manages only 3 pages in memory</li>
 * </ul>
 *
 * <h3>Architecture Components:</h3>
 *
 * <h4>{@link net.calvuz.qdue.ui.features.swipecalendar.presentation.SwipeCalendarFragment}</h4>
 * <ul>
 *   <li><strong>Main Fragment</strong>: Host container with ViewPager2 and month navigation</li>
 *   <li><strong>Dependency Injection</strong>: Uses SwipeCalendarModule for component management</li>
 *   <li><strong>State Management</strong>: Integrates with SwipeCalendarStateManager</li>
 *   <li><strong>Event Handling</strong>: Basic day click events and navigation controls</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.ui.features.swipecalendar.adapters.MonthPagerAdapter}</h4>
 * <ul>
 *   <li><strong>ViewPager2 Adapter</strong>: Manages 2400 month pages with finite bounds</li>
 *   <li><strong>Position Mapping</strong>: Converts adapter positions to YearMonth objects</li>
 *   <li><strong>ViewHolder Recycling</strong>: Efficient memory management for month views</li>
 *   <li><strong>Data Integration</strong>: Coordinates with CalendarDataProvider for events</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.ui.features.swipecalendar.adapters.SwipeCalendarDayAdapter}</h4>
 * <ul>
 *   <li><strong>Month Grid Adapter</strong>: 7x6 grid layout for individual month display</li>
 *   <li><strong>Day Cell Management</strong>: Reuses existing item_calendar_day.xml layout</li>
 *   <li><strong>Event Integration</strong>: Shows events and work schedule data per day</li>
 *   <li><strong>Click Handling</strong>: Forwards day clicks to parent fragment</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.ui.features.swipecalendar.components.SwipeCalendarStateManager}</h4>
 * <ul>
 *   <li><strong>State Persistence</strong>: SharedPreferences integration for position memory</li>
 *   <li><strong>Initial Position Logic</strong>: "Today" for first use, saved position on return</li>
 *   <li><strong>Thread Safety</strong>: Atomic operations for concurrent access protection</li>
 *   <li><strong>Cleanup Support</strong>: State reset and memory management</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.ui.features.swipecalendar.di.SwipeCalendarModule}</h4>
 * <ul>
 *   <li><strong>Dependency Injection</strong>: Following established ServiceProvider patterns</li>
 *   <li><strong>Component Lifecycle</strong>: Manages adapter and state manager instances</li>
 *   <li><strong>Service Integration</strong>: Coordinates with EventsService and UserService</li>
 *   <li><strong>Memory Management</strong>: Proper cleanup and resource disposal</li>
 * </ul>
 *
 * <h3>Date Range Strategy:</h3>
 * <pre>
 * Start Date: January 1900 (Position 0)
 * End Date: December 2100 (Position 2399)
 * Total Months: 2400
 * Current Month: Calculated from today's date
 * Memory Usage: ~3 MonthViewHolders active (ViewPager2 default)
 * </pre>
 *
 * <h3>Position Calculation:</h3>
 * <pre>
 * {@code
 * // Convert YearMonth to adapter position
 * YearMonth baseMonth = YearMonth.of(1900, 1);
 * int position = (int) ChronoUnit.MONTHS.between(baseMonth, targetMonth);
 *
 * // Convert adapter position to YearMonth
 * YearMonth targetMonth = baseMonth.plusMonths(position);
 * }
 * </pre>
 *
 * <h3>State Management Strategy:</h3>
 * <ul>
 *   <li><strong>First Launch</strong>: Navigate to current month (today)</li>
 *   <li><strong>Return from Background</strong>: Restore last viewed month position</li>
 *   <li><strong>Session Memory</strong>: Track position changes during active session</li>
 *   <li><strong>Cleanup</strong>: Clear state on app uninstall or data reset</li>
 * </ul>
 *
 * <h3>Performance Considerations:</h3>
 * <ul>
 *   <li><strong>Memory Footprint</strong>: Maximum 3 month views in memory</li>
 *   <li><strong>Smooth Navigation</strong>: Hardware-accelerated ViewPager2 transitions</li>
 *   <li><strong>Data Loading</strong>: Lazy loading of events per visible month</li>
 *   <li><strong>Range Validation</strong>: Efficient bounds checking for swipe limits</li>
 * </ul>
 *
 * <h3>Integration Points:</h3>
 * <ul>
 *   <li><strong>CalendarDataProvider</strong>: Reuses existing calendar data infrastructure</li>
 *   <li><strong>EventsService</strong>: Integrates with events loading and management</li>
 *   <li><strong>UserService</strong>: User preferences and team-specific data</li>
 *   <li><strong>WorkScheduleService</strong>: Quattrodue pattern integration</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since Database Version 6
 */
package net.calvuz.qdue.ui.features.swipecalendar;