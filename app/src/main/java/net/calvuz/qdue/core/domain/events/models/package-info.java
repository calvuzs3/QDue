/**
 * Events models package providing domain entities for calendar event management.
 *
 * <p>This package contains all domain models related to calendar events, including
 * the enhanced EventType system with drawable icon support, event priorities,
 * and specialized event entities for different use cases.</p>
 *
 * <h3>Core Models:</h3>
 *
 * <h4>{@link net.calvuz.qdue.core.domain.events.models.EventType} - Enhanced Event Classification</h4>
 * <ul>
 *   <li><strong>Display Properties</strong> - Localized names, colors, emojis, and drawable icons</li>
 *   <li><strong>Icon Support</strong> - Drawable resources following ic_rounded_*_24 naming convention</li>
 *   <li><strong>Category System</strong> - Integration with EventTypeCategory for logical grouping ✅ NEW</li>
 *   <li><strong>Business Logic</strong> - Simplified methods using category-based logic</li>
 *   <li><strong>Visual Optimization</strong> - Text color calculation based on background for readability</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.core.domain.events.models.EventTypeCategory} - Event Categorization System ✅ NEW</h4>
 * <ul>
 *   <li><strong>Internationalized</strong> - Localized category names and descriptions</li>
 *   <li><strong>Business Logic</strong> - Category-based rules for approval, scheduling, and UI behavior</li>
 *   <li><strong>UI Integration</strong> - Icons, filtering, and grouping support</li>
 *   <li><strong>Priority System</strong> - Weighted priorities for display ordering</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.core.domain.events.models.LocalEvent} - Primary Event Entity</h4>
 * <ul>
 *   <li><strong>Room Entity</strong> - Database persistence with full CRUD operations</li>
 *   <li><strong>CalendarEvent Interface</strong> - Unified contract for different event sources</li>
 *   <li><strong>Metadata Support</strong> - Custom properties and external integration data</li>
 *   <li><strong>Enhanced Fields</strong> - Custom icon overrides and display hints (Database v6)</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.core.domain.events.models.EventPriority} - Event Priority System</h4>
 * <ul>
 *   <li><strong>Priority Levels</strong> - LOW, NORMAL, HIGH, URGENT with visual indicators</li>
 *   <li><strong>Color Coding</strong> - Consistent color scheme for priority visualization</li>
 * </ul>
 *
 * <h4>{@link net.calvuz.qdue.core.domain.events.models.TurnException} - Schedule Exceptions</h4>
 * <ul>
 *   <li><strong>Work Schedule Integration</strong> - Modifications to standard work patterns</li>
 *   <li><strong>User Association</strong> - Foreign key relationships with user management</li>
 * </ul>
 *
 * <h3>EventTypeCategory System (Database Version 6):</h3>
 *
 * <h4>Category-Based Business Logic ✅ SIMPLIFIED</h4>
 * <pre>
 * EventType.VACATION.getCategory()          → EventTypeCategory.ABSENCE
 * EventType.VACATION.isWorkAbsence()        → category.isWorkAbsence() → true
 * EventType.STOP_PLANNED.getCategory()      → EventTypeCategory.PRODUCTION
 * EventType.STOP_PLANNED.isProductionStop() → category-based logic → true
 * </pre>
 *
 * <h4>Category Properties</h4>
 * <pre>
 * EventTypeCategory.ABSENCE.getDisplayName()           → "Assenze" (localized)
 * EventTypeCategory.ABSENCE.getDescription()           → "Eventi di assenza..." (localized)
 * EventTypeCategory.ABSENCE.isWorkAbsence()            → true
 * EventTypeCategory.ABSENCE.typicallyRequiresApproval() → true
 * EventTypeCategory.ABSENCE.getPriorityWeight()        → 1 (highest priority)
 * </pre>
 *
 * <h3>EventType Categories:</h3>
 * <ul>
 *   <li><strong>GENERAL</strong> - General events, miscellaneous activities (Priority: 6)</li>
 *   <li><strong>ABSENCE</strong> - Work absences affecting attendance (Priority: 1 - Highest)</li>
 *   <li><strong>WORK_ADJUSTMENT</strong> - Schedule modifications, overtime, shift swaps (Priority: 3)</li>
 *   <li><strong>PRODUCTION</strong> - Production stops, maintenance, emergencies (Priority: 2)</li>
 *   <li><strong>ORGANIZATIONAL</strong> - Meetings, training, company events (Priority: 5)</li>
 *   <li><strong>COMPLIANCE</strong> - Safety drills, audits, regulatory events (Priority: 4)</li>
 * </ul>
 *
 * <h3>Database Integration:</h3>
 *
 * <h4>EventType Storage</h4>
 * <ul>
 *   <li>Stored as enum name string via EventsTypeConverters</li>
 *   <li>Icon resources resolved at runtime from enum definition</li>
 *   <li>Future customization support via event_type_customizations table</li>
 * </ul>
 *
 * <h4>Performance Optimization</h4>
 * <ul>
 *   <li>Indexed by event_type for efficient filtering</li>
 *   <li>Custom icon lookups optimized with sparse indices</li>
 *   <li>Category-based queries supported with computed properties</li>
 * </ul>
 *
 * <h3>Icon Naming Convention:</h3>
 * <pre>
 * Pattern: ic_rounded_&lt;descriptive_name&gt;_24
 * Examples:
 * - ic_rounded_beach_access_24   (vacation)
 * - ic_rounded_local_hospital_24 (sick leave)
 * - ic_rounded_emergency_24      (emergency events)
 * - ic_rounded_build_24          (maintenance)
 * </pre>
 *
 * <h3>Migration Path:</h3>
 * <ul>
 *   <li><strong>Database v5 → v6</strong> - Adds icon customization support</li>
 *   <li><strong>Backward Compatible</strong> - Existing EventType usage unchanged</li>
 *   <li><strong>Enhanced Features</strong> - New icon and category methods available</li>
 *   <li><strong>Future Ready</strong> - Prepared for user customization features</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 *
 * <h4>Creating Events with Categories ✅ ENHANCED</h4>
 * <pre>
 * LocalEvent event = new LocalEvent.Builder()
 *     .title("Team Meeting")
 *     .eventType(EventType.MEETING)
 *     .build();
 *
 * // Icon and category automatically resolved from EventType
 * int iconRes = event.getEventType().getIconRes(); // ic_rounded_groups_24
 * EventTypeCategory category = event.getEventType().getCategory(); // ORGANIZATIONAL
 * </pre>
 *
 * <h4>Category-Based Filtering</h4>
 * <pre>
 * // Get all absence events
 * EventType[] absenceTypes = EventType.getEventTypesByCategory(EventTypeCategory.ABSENCE);
 *
 * // Filter events by category
 * boolean isWorkRelated = eventType.getCategory().isWorkScheduleModification();
 * boolean needsApproval = eventType.getCategory().typicallyRequiresApproval();
 * </pre>
 *
 * <h4>UI Integration with Categories</h4>
 * <pre>
 * // Set category icon in UI
 * ImageView categoryIcon = findViewById(R.id.category_icon);
 * categoryIcon.setImageResource(eventType.getCategory().getIconRes());
 *
 * // Display category name
 * TextView categoryName = findViewById(R.id.category_name);
 * categoryName.setText(eventType.getCategory().getDisplayName()); // Localized
 *
 * // Set event type icon
 * ImageView eventIcon = findViewById(R.id.event_icon);
 * eventIcon.setImageResource(eventType.getIconRes());
 * </pre>
 *
 * <h4>Business Logic with Categories ✅ SIMPLIFIED</h4>
 * <pre>
 * // Simplified logic using categories
 * if (eventType.getCategory().isWorkAbsence()) {
 *     // Handle all absence types uniformly
 *     absenceService.processAbsence(event);
 * }
 *
 * if (eventType.getCategory().isProductionRelated()) {
 *     // Handle all production events
 *     productionService.logProductionEvent(event);
 * }
 *
 * if (eventType.getCategory().typicallyRequiresApproval()) {
 *     // Send for approval based on category rules
 *     approvalService.submitForApproval(event);
 * }
 * </pre>
 *
 * <h3>Architecture Benefits:</h3>
 * <ul>
 *   <li><strong>Consistency</strong> - Unified icon, color, and category system across the app</li>
 *   <li><strong>Simplicity</strong> - Category-based business logic reduces code complexity ✅ NEW</li>
 *   <li><strong>Maintainability</strong> - Centralized EventType and Category definitions</li>
 *   <li><strong>Extensibility</strong> - Easy to add new event types with automatic category integration</li>
 *   <li><strong>Performance</strong> - Optimized database queries and UI rendering with category indices</li>
 *   <li><strong>Internationalization</strong> - Localized display names and descriptions for global deployment ✅ NEW</li>
 *   <li><strong>UI Organization</strong> - Category-based filtering, grouping, and priority ordering ✅ NEW</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 2.1 - Enhanced EventType with EventTypeCategory integration
 * @since Database Version 6
 */
package net.calvuz.qdue.core.domain.events.models;