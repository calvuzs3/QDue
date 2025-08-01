/**
 * Events domain - Core calendar event management and work schedule exceptions.
 *
 * <p>This package handles all calendar events including regular appointments,
 * work schedule events, and schedule exceptions (modifications, overtime).
 * It provides the central event management system with metadata-driven
 * classification and merging capabilities.</p>
 *
 * <h3>Key Components:</h3>
 *
 * <h4>Domain Models:</h4>
 * <ul>
 *   <li><strong>Event</strong> - Base calendar event with common properties</li>
 *   <li><strong>WorkScheduleEvent</strong> - Work schedule specific events</li>
 *   <li><strong>EventMetadata</strong> - Classification and relationship metadata</li>
 *   <li><strong>EventType</strong> - Event type enumeration (NORMAL, WORK_SHIFT, OVERTIME, EXCEPTION)</li>
 *   <li><strong>EventRecurrence</strong> - Standard recurrence rule processing</li>
 * </ul>
 *
 * <h4>Provider Pattern:</h4>
 * <ul>
 *   <li><strong>WorkScheduleExceptionsProvider</strong> - Manages work schedule modifications</li>
 *   <li>Handles overtime, shift changes, and schedule exceptions</li>
 *   <li>Provides CRUD operations for schedule modifications</li>
 * </ul>
 *
 * <h4>Services:</h4>
 * <ul>
 *   <li><strong>EventService</strong> - Core event CRUD operations</li>
 *   <li><strong>EventMergeService</strong> - Merges base schedules with exceptions</li>
 *   <li><strong>RecurrenceService</strong> - Processes recurrence rules</li>
 * </ul>
 *
 * <h3>Event Classification System:</h3>
 * <pre>
 * EventClass.NORMAL         → Regular calendar appointments
 * EventClass.WORK_SHIFT     → Generated from work schedule templates
 * EventClass.OVERTIME       → Additional work shifts (not in base schedule)
 * EventClass.EXCEPTION      → Modifications to existing work shifts
 * </pre>
 *
 * <h3>Event Metadata:</h3>
 * <ul>
 *   <li><strong>modifiesWorkShift</strong> - Indicates if event modifies a work shift</li>
 *   <li><strong>originalShiftId</strong> - Reference to the original shift being modified</li>
 *   <li><strong>workScheduleId</strong> - Reference to the work schedule template</li>
 * </ul>
 *
 * <h3>Integration with Work Schedules:</h3>
 * <pre>
 * WorkScheduleProvider (quattrodue) → generates base schedule events
 *                     ↓
 * EventMergeService → combines with exceptions from this domain
 *                     ↓
 * Final merged events → displayed in UI
 * </pre>
 *
 * <h3>Exception Handling:</h3>
 * <ul>
 *   <li>LegacyShift time changes (start/end time modifications)</li>
 *   <li>LegacyShift cancellations (removing scheduled shifts)</li>
 *   <li>Additional shifts (overtime, extra work)</li>
 *   <li>LegacyShift swaps (between team members)</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since API Level 21
 */
package net.calvuz.qdue.core.domain.events;
