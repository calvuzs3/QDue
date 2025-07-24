/**
 * Calendar View System
 * <p>
 * Contains the main calendar interface for viewing shift schedules
 * in a traditional calendar grid format. Implements a 7x6 grid
 * (42 days) with shift indicators and interactive features.
 *
 * <h2>Components</h2>
 * <ul>
 *   <li><strong>SmartShiftsCalendarFragment</strong> - Main calendar view</li>
 *   <li><strong>CalendarViewModel</strong> - Calendar business logic</li>
 *   <li><strong>CalendarAdapter</strong> - 42-day grid adapter</li>
 *   <li><strong>ShiftLegendAdapter</strong> - Color legend adapter</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Monthly calendar view with shift indicators</li>
 *   <li>Color-coded shift types</li>
 *   <li>Interactive day selection</li>
 *   <li>Shift legend with type descriptions</li>
 *   <li>Month navigation (previous/next)</li>
 * </ul>
 *
 * <h2>Display Logic</h2>
 * <ul>
 *   <li>Current month highlighting</li>
 *   <li>Today indicator</li>
 *   <li>Multiple shift support per day</li>
 *   <li>Responsive grid sizing</li>
 * </ul>
 *
 * @author SmartShifts Team
 * @since Phase 3 - UI Foundation
 */
package net.calvuz.qdue.smartshifts.ui.calendar;