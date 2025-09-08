/**
 * Data Access Objects Package for Calendar Management
 *
 * <p>This package contains Room Data Access Object (DAO) interfaces for calendar and shift
 * management operations in the QDue application. DAOs provide the main API for accessing
 * calendar-related data stored in the local SQLite database through Room persistence library.</p>
 *
 * <h2>Package Structure</h2>
 * <pre>
 * net.calvuz.qdue.data.dao/
 * ├── ShiftDao.java                 # Shift entity operations
 * ├── TeamDao.java                  # Team entity operations
 * └── package-info.java             # This documentation
 * </pre>
 *
 * <h2>Clean Architecture Role</h2>
 *
 * <h3>Data Access Layer</h3>
 * <p>DAOs serve as the data access layer in clean architecture for calendar management:</p>
 * <ul>
 *   <li><strong>Database Abstraction</strong>: Hide SQL complexity from repositories</li>
 *   <li><strong>Type Safety</strong>: Compile-time SQL validation through Room</li>
 *   <li><strong>Performance Optimization</strong>: Efficient query execution for calendar operations</li>
 *   <li><strong>Transaction Management</strong>: Atomic operations support</li>
 *   <li><strong>Calendar-Specific</strong>: Optimized for Google Calendar-like operations</li>
 * </ul>
 *
 * <h3>Repository Integration</h3>
 * <p>DAOs are consumed by repository implementations in the calendar domain:</p>
 * <pre>
 * Calendar Repository Implementation
 *       ↓
 * DAO Interface (this package)
 *       ↓
 * Room Generated Implementation
 *       ↓
 * CalendarDatabase (SQLite)
 * </pre>
 *
 * <h2>DAO Design Principles</h2>
 *
 * <h3>Interface Structure</h3>
 * <ul>
 *   <li><strong>Room Annotations</strong>: {@code @Dao}, {@code @Query}, {@code @Insert}, etc.</li>
 *   <li><strong>Method Categories</strong>: CRUD, queries, batch operations, utilities</li>
 *   <li><strong>Parameter Safety</strong>: Proper null annotations and validation</li>
 *   <li><strong>Return Type Consistency</strong>: Consistent patterns across operations</li>
 *   <li><strong>Calendar Optimization</strong>: Methods designed for calendar view performance</li>
 * </ul>
 *
 * <h3>Query Optimization</h3>
 * <ul>
 *   <li><strong>Index Usage</strong>: Queries designed to leverage entity indexes</li>
 *   <li><strong>Selective Fields</strong>: Minimal data retrieval when possible</li>
 *   <li><strong>Batch Operations</strong>: Efficient multi-record processing</li>
 *   <li><strong>Transaction Boundaries</strong>: Appropriate grouping of operations</li>
 *   <li><strong>Time-based Queries</strong>: Optimized for calendar date/time operations</li>
 * </ul>
 *
 * <h2>Core DAOs</h2>
 *
 * <h3>ShiftDao</h3>
 * <p>Comprehensive data access for shift management in calendar context:</p>
 * <ul>
 *   <li><strong>Basic CRUD</strong>: Insert, update, delete, select operations</li>
 *   <li><strong>Shift Type Queries</strong>: Filter by MORNING, AFTERNOON, NIGHT, CUSTOM types</li>
 *   <li><strong>Time-based Queries</strong>: Find by start time, duration, midnight crossing</li>
 *   <li><strong>User Relevance</strong>: Filter shifts relevant to current user</li>
 *   <li><strong>Break Management</strong>: Query shifts with/without break times</li>
 *   <li><strong>Batch Operations</strong>: Multi-shift insert/update efficiently</li>
 *   <li><strong>Status Management</strong>: Active/inactive shift filtering</li>
 *   <li><strong>Display Ordering</strong>: Maintain shift display order for UI</li>
 *   <li><strong>Statistics</strong>: Comprehensive shift statistics and counts</li>
 * </ul>
 *
 * <h3>TeamDao</h3>
 * <p>Complete data access for work team management:</p>
 * <ul>
 *   <li><strong>Basic CRUD</strong>: Insert, update, delete, select operations</li>
 *   <li><strong>Query Methods</strong>: Find by name, ID, status, patterns</li>
 *   <li><strong>Batch Operations</strong>: Multi-team insert/update efficiently</li>
 *   <li><strong>Status Management</strong>: Active/inactive team filtering</li>
 *   <li><strong>Utility Methods</strong>: Count, existence checks, cleanup</li>
 *   <li><strong>Standard Teams</strong>: Support for QuattroDue A-I team structure</li>
 * </ul>
 *
 * <h2>Operation Categories</h2>
 *
 * <h3>Basic CRUD Operations</h3>
 * <pre>
 * {@code
 * // Insert operations
 * @Insert(onConflict = OnConflictStrategy.ABORT)
 * long insertShift(@NonNull ShiftEntity shift);
 *
 * @Insert(onConflict = OnConflictStrategy.REPLACE)
 * long insertOrReplaceShift(@NonNull ShiftEntity shift);
 *
 * // Update operations
 * @Update
 * int updateShift(@NonNull ShiftEntity shift);
 *
 * // Delete operations (soft delete preferred)
 * @Query("UPDATE shifts SET active = 0, updated_at = :timestamp WHERE id = :shiftId")
 * int markShiftAsInactive(@NonNull String shiftId, long timestamp);
 * }
 * </pre>
 *
 * <h3>Calendar-Specific Query Methods</h3>
 * <pre>
 * {@code
 * // Shift type queries
 * @Query("SELECT * FROM shifts WHERE shift_type = :shiftType AND active = 1 ORDER BY display_order ASC")
 * List<ShiftEntity> getShiftsByType(@NonNull String shiftType);
 *
 * // Time-based queries
 * @Query("SELECT * FROM shifts WHERE crosses_midnight = 1 AND active = 1 ORDER BY name ASC")
 * List<ShiftEntity> getShiftsCrossingMidnight();
 *
 * @Query("SELECT * FROM shifts WHERE start_time = :startTime AND active = 1 ORDER BY name ASC")
 * List<ShiftEntity> getShiftsByStartTime(@NonNull String startTime);
 *
 * // User relevance filtering
 * @Query("SELECT * FROM shifts WHERE is_user_relevant = 1 AND active = 1 ORDER BY display_order ASC")
 * List<ShiftEntity> getUserRelevantShifts();
 * }
 * </pre>
 *
 * <h3>Advanced Calendar Operations</h3>
 * <pre>
 * {@code
 * // Duration-based queries for calendar planning
 * @Query("SELECT * FROM shifts WHERE active = 1 AND " +
 *        "calculated_duration_minutes BETWEEN :minDurationMinutes AND :maxDurationMinutes " +
 *        "ORDER BY name ASC")
 * List<ShiftEntity> getShiftsByDurationRange(long minDurationMinutes, long maxDurationMinutes);
 *
 * // Break time management
 * @Query("SELECT * FROM shifts WHERE has_break_time = 1 AND active = 1 ORDER BY name ASC")
 * List<ShiftEntity> getShiftsWithBreaks();
 *
 * // Statistics for dashboard/reporting
 * @Query("SELECT COUNT(*) as total_shifts, " +
 *        "SUM(CASE WHEN active = 1 THEN 1 ELSE 0 END) as active_shifts, " +
 *        "SUM(CASE WHEN shift_type = 'MORNING' AND active = 1 THEN 1 ELSE 0 END) as morning_shifts " +
 *        "FROM shifts")
 * ShiftStatistics getShiftStatistics();
 * }
 * </pre>
 *
 * <h3>Batch Operations for Performance</h3>
 * <pre>
 * {@code
 * // Batch inserts for initial setup
 * @Insert(onConflict = OnConflictStrategy.ABORT)
 * List<Long> insertShifts(@NonNull List<ShiftEntity> shifts);
 *
 * // Bulk status updates
 * @Query("UPDATE shifts SET active = :active, updated_at = :timestamp WHERE id IN (:ids)")
 * int updateShiftStatusBatch(@NonNull List<String> ids, boolean active, long timestamp);
 *
 * // Color theme updates
 * @Query("UPDATE shifts SET color_hex = :colorHex, updated_at = :timestamp WHERE shift_type = :shiftType")
 * int updateShiftTypeColor(@NonNull String shiftType, @NonNull String colorHex, long timestamp);
 * }
 * </pre>
 *
 * <h2>Calendar Performance Patterns</h2>
 *
 * <h3>Index Utilization for Calendar Operations</h3>
 * <ul>
 *   <li><strong>Primary Key Queries</strong>: Direct shift/team lookup by ID</li>
 *   <li><strong>Time Range Queries</strong>: Use start_time/end_time composite index</li>
 *   <li><strong>Type Filtering</strong>: Leverage shift_type + active index</li>
 *   <li><strong>User Relevance</strong>: Use is_user_relevant + active index</li>
 *   <li><strong>Display Order</strong>: Use display_order + active for UI sorting</li>
 * </ul>
 *
 * <h3>Calendar-Optimized Query Performance</h3>
 * <pre>
 * {@code
 * // Efficient: Uses shift_type + active composite index
 * @Query("SELECT * FROM shifts WHERE shift_type = 'MORNING' AND active = 1")
 * List<ShiftEntity> getMorningShifts();
 *
 * // Efficient: Uses time range index
 * @Query("SELECT * FROM shifts WHERE start_time = :startTime AND active = 1")
 * List<ShiftEntity> getShiftsByStartTime(String startTime);
 *
 * // Efficient: Uses user relevance index
 * @Query("SELECT * FROM shifts WHERE is_user_relevant = 1 AND active = 1")
 * List<ShiftEntity> getUserRelevantShifts();
 *
 * // Less efficient: Full table scan for description search
 * @Query("SELECT * FROM shifts WHERE description LIKE '%:term%'")
 * List<ShiftEntity> searchByDescription(String term);
 * }
 * </pre>
 *
 * <h3>Batch Efficiency for Calendar Operations</h3>
 * <ul>
 *   <li><strong>Single Transaction</strong>: Batch operations in one transaction</li>
 *   <li><strong>Prepared Statements</strong>: Room reuses prepared statements</li>
 *   <li><strong>Bulk Updates</strong>: Single SQL for multiple shift updates</li>
 *   <li><strong>Conflict Resolution</strong>: Efficient handling of shift duplicates</li>
 * </ul>
 *
 * <h2>Repository Usage Patterns</h2>
 *
 * <h3>Calendar Repository Integration</h3>
 * <pre>
 * {@code
 * // In Calendar Repository Implementation
 * public class CalendarRepositoryImpl implements CalendarRepository {
 *     private final ShiftDao shiftDao;
 *     private final TeamDao teamDao;
 *
 *     public CalendarRepositoryImpl(CalendarDatabase database) {
 *         this.shiftDao = database.shiftDao();
 *         this.teamDao = database.teamDao();
 *     }
 *
 *     @Override
 *     public CompletableFuture<OperationResult<List<Shift>>> getActiveShifts() {
 *         return CompletableFuture.supplyAsync(() -> {
 *             try {
 *                 List<ShiftEntity> entities = shiftDao.getActiveShifts();
 *                 List<Shift> shifts = entities.stream()
 *                         .map(this::convertToShift)
 *                         .collect(toList());
 *                 return OperationResult.success(shifts, READ);
 *             } catch (Exception e) {
 *                 return OperationResult.failure(e.getMessage(), READ);
 *             }
 *         });
 *     }
 * }
 * }
 * </pre>
 *
 * <h3>Calendar-Specific Error Handling</h3>
 * <pre>
 * {@code
 * // Calendar repository error handling
 * private Shift getOrCreateShift(String name, String shiftType) {
 *     try {
 *         ShiftEntity entity = shiftDao.getActiveShiftByName(name);
 *
 *         if (entity != null) {
 *             return convertToShift(entity);
 *         }
 *
 *         // Create new shift with default calendar settings
 *         ShiftEntity newEntity = new ShiftEntity(
 *             UUID.randomUUID().toString(), name, shiftType, "08:00", "16:00");
 *         newEntity.setColorHex(getDefaultColorForType(shiftType));
 *         newEntity.setUserRelevant(true);
 *
 *         long id = shiftDao.insertShift(newEntity);
 *         return convertToShift(newEntity);
 *
 *     } catch (SQLiteConstraintException e) {
 *         // Handle unique constraint violations for shift names
 *         Log.w(TAG, "Shift already exists: " + name);
 *         return convertToShift(shiftDao.getActiveShiftByName(name));
 *     } catch (Exception e) {
 *         Log.e(TAG, "Calendar database error: " + e.getMessage());
 *         throw new RuntimeException("Failed to get/create shift", e);
 *     }
 * }
 * }
 * </pre>
 *
 * <h2>Transaction Management for Calendar Operations</h2>
 *
 * <h3>Automatic Transactions</h3>
 * <ul>
 *   <li><strong>Single Operations</strong>: {@code @Insert}, {@code @Update}, {@code @Delete} are atomic</li>
 *   <li><strong>Batch Operations</strong>: Multi-record operations in single transaction</li>
 *   <li><strong>Query Operations</strong>: {@code @Query} operations are consistent reads</li>
 *   <li><strong>Calendar Consistency</strong>: Maintain data consistency for calendar views</li>
 * </ul>
 *
 * <h3>Manual Transactions for Complex Calendar Operations</h3>
 * <pre>
 * {@code
 * // In Repository for complex calendar setup
 * @Transaction
 * public void initializeCalendarWithShiftsAndTeams(CalendarSetupData setupData) {
 *     // Insert shifts
 *     for (ShiftSetupData shiftData : setupData.getWorkShifts()) {
 *         ShiftEntity shift = createShiftEntity(shiftData);
 *         long shiftId = shiftDao.insertShift(shift);
 *
 *         // Set up shift display order
 *         shiftDao.updateShiftDisplayOrder(shift.getId(), shiftData.getDisplayOrder(),
 *                                         System.currentTimeMillis());
 *     }
 *
 *     // Insert teams
 *     for (TeamSetupData teamData : setupData.getTeams()) {
 *         TeamEntity team = createTeamEntity(teamData);
 *         teamDao.insertTeam(team);
 *     }
 * }
 * }
 * </pre>
 *
 * <h2>Testing Strategy for Calendar DAOs</h2>
 *
 * <h3>Calendar DAO Testing</h3>
 * <ul>
 *   <li><strong>Room Testing</strong>: In-memory database for fast calendar tests</li>
 *   <li><strong>CRUD Operations</strong>: Test all calendar-specific operations</li>
 *   <li><strong>Time Logic</strong>: Test midnight crossing and duration calculations</li>
 *   <li><strong>Query Validation</strong>: Verify calendar query results and performance</li>
 *   <li><strong>Constraint Testing</strong>: Test unique constraints and shift validations</li>
 * </ul>
 *
 * <h3>Calendar Integration Testing</h3>
 * <pre>
 * {@code
 * @RunWith(AndroidJUnit4.class)
 * public class CalendarDaoTest {
 *     private CalendarDatabase database;
 *     private ShiftDao shiftDao;
 *     private TeamDao teamDao;
 *
 *     @Before
 *     public void createDb() {
 *         Context context = ApplicationProvider.getApplicationContext();
 *         database = Room.inMemoryDatabaseBuilder(context, CalendarDatabase.class)
 *                 .allowMainThreadQueries()
 *                 .build();
 *         shiftDao = database.shiftDao();
 *         teamDao = database.teamDao();
 *     }
 *
 *     @Test
 *     public void testMorningShiftCreationAndRetrieval() {
 *         ShiftEntity morningShift = new ShiftEntity("test_morning", "Morning", "MORNING", "06:00", "14:00");
 *         morningShift.setColorHex("#4CAF50");
 *         morningShift.setUserRelevant(true);
 *
 *         long id = shiftDao.insertShift(morningShift);
 *         assertTrue("Shift should be inserted", id > 0);
 *
 *         ShiftEntity retrieved = shiftDao.getShiftById(morningShift.getId());
 *         assertNotNull("Shift should be retrievable", retrieved);
 *         assertEquals("Morning", retrieved.getName());
 *         assertEquals("MORNING", retrieved.getWorkScheduleShift());
 *         assertFalse("Morning shift should not cross midnight", retrieved.isCrossesMidnight());
 *     }
 *
 *     @Test
 *     public void testNightShiftMidnightCrossing() {
 *         ShiftEntity nightShift = new ShiftEntity("test_night", "Night", "NIGHT", "22:00", "06:00");
 *         shiftDao.insertShift(nightShift);
 *
 *         List<ShiftEntity> midnightShifts = shiftDao.getShiftsCrossingMidnight();
 *         assertEquals("Should find 1 midnight crossing shift", 1, midnightShifts.size());
 *         assertTrue("Night shift should cross midnight", midnightShifts.get(0).isCrossesMidnight());
 *     }
 *
 *     @After
 *     public void closeDb() {
 *         database.close();
 *     }
 * }
 * }
 * </pre>
 *
 * <h2>Best Practices for Calendar DAOs</h2>
 *
 * <h3>Calendar Query Design</h3>
 * <ul>
 *   <li><strong>Use Time Indexes</strong>: Design queries to use time-based indexes</li>
 *   <li><strong>Filter Early</strong>: Apply active/inactive filtering in WHERE clause</li>
 *   <li><strong>Limit Calendar Results</strong>: Use appropriate LIMIT for large datasets</li>
 *   <li><strong>Batch Calendar Operations</strong>: Group related operations in transactions</li>
 *   <li><strong>Validate Time Logic</strong>: Ensure midnight crossing logic is correct</li>
 * </ul>
 *
 * <h3>Calendar Method Design</h3>
 * <ul>
 *   <li><strong>Clear Naming</strong>: Method names indicate calendar operation and time criteria</li>
 *   <li><strong>Consistent Returns</strong>: Similar calendar operations return similar types</li>
 *   <li><strong>Time Safety</strong>: Validate time format parameters</li>
 *   <li><strong>Calendar Documentation</strong>: Document time logic and business rules</li>
 * </ul>
 *
 * <h3>Calendar Performance</h3>
 * <ul>
 *   <li><strong>Minimize Calendar Queries</strong>: Batch calendar operations when possible</li>
 *   <li><strong>Use Appropriate Time Indexes</strong>: Query patterns should match time indexes</li>
 *   <li><strong>Cache Calendar Data</strong>: Consider caching frequently accessed shifts</li>
 *   <li><strong>Optimize Display Order</strong>: Use display_order for efficient UI sorting</li>
 * </ul>
 *
 * <h2>Future Extensions</h2>
 *
 * <h3>Planned Calendar DAOs</h3>
 * <ul>
 *   <li><strong>ScheduleDao</strong>: Shift-to-date assignments with team linkage</li>
 *   <li><strong>CalendarEventDao</strong>: Integration with calendar events system</li>
 *   <li><strong>ShiftTemplateDao</strong>: Reusable shift template management</li>
 *   <li><strong>CalendarConfigDao</strong>: User calendar preferences and settings</li>
 * </ul>
 *
 * <h3>Advanced Calendar Features</h3>
 * <ul>
 *   <li><strong>Recurring Schedules</strong>: DAO methods for pattern-based scheduling</li>
 *   <li><strong>Calendar Sync</strong>: Integration with external calendar systems</li>
 *   <li><strong>Time Zone Support</strong>: Multi-timezone calendar operations</li>
 *   <li><strong>Calendar Analytics</strong>: Advanced statistics and reporting DAOs</li>
 * </ul>
 *
 * <h2>See Also</h2>
 * <ul>
 *   <li>{@link net.calvuz.qdue.data.entities} - Entity classes used by calendar DAOs</li>
 *   <li>{@link net.calvuz.qdue.data.repositories} - Calendar repository implementations</li>
 *   <li>{@link net.calvuz.qdue.core.db.CalendarDatabase} - Calendar database configuration</li>
 *   <li>{@link net.calvuz.qdue.domain.calendar.models} - Calendar domain models</li>
 *   <li>Room Documentation - Official Android Room guide</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Implementation
 * @since Database Version 7
 */
package net.calvuz.qdue.data.dao;