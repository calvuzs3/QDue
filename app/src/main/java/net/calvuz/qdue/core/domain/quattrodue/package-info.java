
/**
 * Quattrodue domain - Work schedule provider system with template management.
 *
 * <p>This package implements the work schedule generation system, providing
 * both predefined 4-2 pattern schedules and custom user-defined schemas.
 * It generates volatile schedule events that are merged with persistent
 * exceptions in the events domain.</p>
 *
 * <h3>Core Concepts:</h3>
 *
 * <h4>"Quattrodue" (4-2) Pattern:</h4>
 * <ul>
 *   <li>4 consecutive work days followed by 2 rest days</li>
 *   <li>Continuous cycle with no breaks</li>
 *   <li>Team-based rotation system</li>
 *   <li>Predefined shift types with colors and timing</li>
 * </ul>
 *
 * <h4>Provider Pattern Architecture:</h4>
 * <ul>
 *   <li><strong>WorkScheduleProvider</strong> - Interface for schedule generation</li>
 *   <li><strong>FixedScheduleProvider</strong> - Implements 4-2 predefined schemas</li>
 *   <li><strong>CustomScheduleProvider</strong> - Handles user-defined schemas</li>
 * </ul>
 *
 * <h3>Domain Models:</h3>
 *
 * <h4>Schedule Templates:</h4>
 * <ul>
 *   <li><strong>WorkScheduleTemplate</strong> - Base template definition</li>
 *   <li><strong>CustomScheduleSchema</strong> - User-created custom patterns</li>
 *   <li><strong>LegacyShiftType</strong> - LegacyShift definitions with colors, names, and times</li>
 *   <li><strong>Team</strong> - Team definitions for schedule rotation</li>
 * </ul>
 *
 * <h4>Generation Rules:</h4>
 * <ul>
 *   <li><strong>RecurrenceRule</strong> - Volatile recurrence rule generation</li>
 *   <li>Pattern-based generation (4 work + 2 rest cycles)</li>
 *   <li>Team rotation logic</li>
 *   <li>Date range scheduling</li>
 * </ul>
 *
 * <h3>Services:</h3>
 * <ul>
 *   <li><strong>ScheduleGenerationService</strong> - Generates schedule events from templates</li>
 *   <li><strong>TemplateService</strong> - Manages schedule templates and schemas</li>
 *   <li><strong>WorkScheduleManager</strong> - High-level orchestration of schedule operations</li>
 * </ul>
 *
 * <h3>Schedule Generation Flow:</h3>
 * <pre>
 * 1. User selects template (4-2 or custom)
 * 2. WorkScheduleProvider generates volatile events for date range
 * 3. Events include team, shift type, colors, and timing
 * 4. Generated events sent to EventMergeService
 * 5. Merged with exceptions from events domain
 * 6. Final schedule displayed in UI
 * </pre>
 *
 * <h3>Template Types:</h3>
 * <ul>
 *   <li><strong>Fixed Templates</strong>:
 *     <ul>
 *       <li>Standard 4-2 pattern</li>
 *       <li>Multiple shift types (morning, afternoon, night)</li>
 *       <li>Team-based rotation</li>
 *       <li>Predefined colors and timing</li>
 *     </ul>
 *   </li>
 *   <li><strong>Custom Templates</strong>:
 *     <ul>
 *       <li>User-defined work/rest patterns</li>
 *       <li>Custom shift definitions</li>
 *       <li>Flexible team configurations</li>
 *       <li>Personalized colors and naming</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>Integration Points:</h3>
 * <ul>
 *   <li>Reads user preferences from preferences domain</li>
 *   <li>Provides volatile events to events domain</li>
 *   <li>Configures team assignments and shift preferences</li>
 *   <li>Supplies color schemes for UI visualization</li>
 * </ul>
 *
 * <h3>Extensibility:</h3>
 * <ul>
 *   <li>New WorkScheduleProvider implementations for different patterns</li>
 *   <li>Additional shift types and team configurations</li>
 *   <li>Complex recurrence rules and exceptions</li>
 *   <li>Integration with external work schedule systems</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since API Level 21
 */
package net.calvuz.qdue.core.domain.quattrodue;