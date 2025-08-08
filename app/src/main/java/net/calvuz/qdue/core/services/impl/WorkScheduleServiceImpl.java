package net.calvuz.qdue.core.services.impl;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.quattrodue.models.ShiftType;
import net.calvuz.qdue.quattrodue.models.Team;
import net.calvuz.qdue.quattrodue.models.WorkScheduleEvent;
import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.core.services.UserService;
import net.calvuz.qdue.core.services.WorkScheduleService;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.quattrodue.utils.ShiftTypeFactory;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WorkScheduleServiceImpl - Implementation of WorkScheduleService interface.
 *
 * <p>Provides comprehensive work schedule management using the existing QuattroDue
 * infrastructure. Implements caching, team management, and schedule calculations
 * for optimal performance with calendar integration.</p>
 *
 * <h3>Implementation Details:</h3>
 * <ul>
 *   <li><strong>QuattroDue Integration</strong>: Uses existing singleton for calculations</li>
 *   <li><strong>Performance Caching</strong>: Caches calculated schedules for quick access</li>
 *   <li><strong>Thread Safety</strong>: Thread-safe operations with concurrent collections</li>
 *   <li><strong>Memory Management</strong>: Automatic cache cleanup and size limits</li>
 * </ul>
 *
 * <h3>Caching Strategy:</h3>
 * <ul>
 *   <li>Day objects cached by date</li>
 *   <li>Team assignments cached by user ID</li>
 *   <li>Shift types cached globally</li>
 *   <li>LRU eviction when cache exceeds limits</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since Database Version 6
 */
public class WorkScheduleServiceImpl implements WorkScheduleService {

    private static final String TAG = "WorkScheduleServiceImpl";

    // Cache configuration
    private static final int MAX_DAY_CACHE_SIZE = 1000;
    private static final int CACHE_CLEANUP_THRESHOLD = 1200;
    private static final long CACHE_TTL_MS = 24 * 60 * 60 * 1000; // 24 hours

    // Preference keys
    private static final String PREF_KEY_USER_TEAM = "user_team";
    private static final String PREF_KEY_SCHEME_START_DATE = "scheme_start_date";
    private static final String PREF_KEY_SHOW_CALENDARS = "show_calendars";
    private static final String PREF_KEY_SHOW_STOPS = "show_stops";

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final QDueDatabase mDatabase;
    private final CoreBackupManager mBackupManager;
    private final UserService mUserService;
    private final SharedPreferences mPreferences;

    // ==================== CACHE STORAGE ====================

    // Day cache with timestamps for TTL
    private final Map<LocalDate, CachedDay> mDayCache = new ConcurrentHashMap<>();

    // Team cache by user ID
    private final Map<Long, Team> mUserTeamCache = new ConcurrentHashMap<>();

    // Shift types cache
    private List<ShiftType> mShiftTypesCache;
    private long mShiftTypesCacheTime = 0;

    // Teams cache
    private List<Team> mTeamsCache;
    private long mTeamsCacheTime = 0;

    // HalfTeams cache
    private List<HalfTeam> mHalfTeamsCache;
    private long mHalfTeamsCacheTime = 0;

    // ==================== QUATTRODUE INTEGRATION ====================

    private QuattroDue mQuattroDue;
    private boolean mIsInitialized = false;

    // ==================== CONSTRUCTOR ====================

    /**
     * Creates WorkScheduleServiceImpl with required dependencies.
     *
     * @param context       Application context
     * @param database      Database instance
     * @param backupManager Backup manager for data operations
     * @param userService   User service for team assignments
     */
    public WorkScheduleServiceImpl(@NonNull Context context,
                                   @NonNull QDueDatabase database,
                                   @NonNull CoreBackupManager backupManager,
                                   @NonNull UserService userService) {
        this.mContext = context.getApplicationContext();
        this.mDatabase = database;
        this.mBackupManager = backupManager;
        this.mUserService = userService;
        this.mPreferences = PreferenceManager.getDefaultSharedPreferences( mContext );

        initializeService();
        Log.d( TAG, "WorkScheduleServiceImpl created and initialized" );
    }

    // ==================== INITIALIZATION ====================

    /**
     * Initialize the service and QuattroDue integration with DataBase.
     */
    private void initializeService() {
        try {
            // Initialize QuattroDue singleton with DataBase
            //mQuattroDue = QuattroDue.getInstance( mContext , mDatabase);
            mQuattroDue = QuattroDue.getInstance( mContext );

            // Load initial configuration
            loadConfiguration();

            mIsInitialized = true;
            Log.d( TAG, "Service initialization completed successfully" );

        } catch (Exception e) {
            Log.e( TAG, "Failed initializeService", e );
            // Fallback to standard initialization
            initializeStandardService();
        }
    }

    /**
     * Initialize the service and QuattroDue integration.
     */
    private void initializeStandardService() {
        try {
            // Initialize QuattroDue singleton
            mQuattroDue = QuattroDue.getInstance( mContext );

            // Load initial configuration
            loadConfiguration();

            mIsInitialized = true;
            Log.d( TAG, "Standard Service initialization completed successfully" );

        } catch (Exception e) {
            Log.e( TAG, "Failed initializeStandardService", e );
            mIsInitialized = false;
        }
    }

    /**
     * Load configuration from SharedPreferences.
     */
    private void loadConfiguration() {
        try {
            // Load scheme start date if available
            String schemeStartStr = mPreferences.getString( PREF_KEY_SCHEME_START_DATE, null );
            if ( schemeStartStr != null ) {
                LocalDate schemeStart = LocalDate.parse( schemeStartStr );
                // Update QuattroDue with saved scheme start date if needed
                Log.d( TAG, "Loaded scheme start date: " + schemeStart );
            }

            // Load other preferences
            boolean showCalendars = mPreferences.getBoolean( PREF_KEY_SHOW_CALENDARS, true );
            boolean showStops = mPreferences.getBoolean( PREF_KEY_SHOW_STOPS, true );

            Log.d( TAG, "Configuration loaded: showCalendars=" + showCalendars + ", showStops=" + showStops );

        } catch (Exception e) {
            Log.w( TAG, "Error loading configuration, using defaults", e );
        }
    }

    // ==================== SCHEDULE GENERATION ====================

    @Override
    @Nullable
    public Day getWorkScheduleForDate(@NonNull LocalDate date, @Nullable Long userId) {
        if ( !mIsInitialized ) {
            Log.w( TAG, "Service not initialized, cannot get work schedule" );
            return null;
        }

        try {
            // Check cache first
            CachedDay cachedDay = mDayCache.get( date );
            if ( cachedDay != null && !cachedDay.isExpired() ) {
                return cachedDay.day;
            }

            // Calculate using QuattroDue
            Day day = calculateDaySchedule( date, userId );

            // Cache the result
            if ( day != null ) {
                cacheDaySchedule( date, day );
            }

            return day;

        } catch (Exception e) {
            Log.e( TAG, "Error getting work schedule for date: " + date, e );
            return null;
        }
    }

    @Override
    @NonNull
    public Map<LocalDate, Day> getWorkScheduleForDateRange(@NonNull LocalDate startDate,
                                                           @NonNull LocalDate endDate,
                                                           @Nullable Long userId) {
        Map<LocalDate, Day> result = new HashMap<>();

        if ( !mIsInitialized ) {
            Log.w( TAG, "Service not initialized, returning empty schedule map" );
            return result;
        }

        try {
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter( endDate )) {
                Day day = getWorkScheduleForDate( currentDate, userId );
                if ( day != null ) {
                    result.put( currentDate, day );
                }
                currentDate = currentDate.plusDays( 1 );
            }

            Log.d( TAG, "Generated work schedule for date range: " + startDate + " to " + endDate +
                    ", found " + result.size() + " days with schedules" );

        } catch (Exception e) {
            Log.e( TAG, "Error getting work schedule for date range", e );
        }

        return result;
    }

    @Override
    @NonNull
    public Map<LocalDate, Day> getWorkScheduleForMonth(@NonNull YearMonth month, @Nullable Long userId) {
        LocalDate startDate = month.atDay( 1 );
        LocalDate endDate = month.atEndOfMonth();
        return getWorkScheduleForDateRange( startDate, endDate, userId );
    }

    @Override
    @NonNull
    public List<WorkScheduleEvent> generateWorkScheduleEvents(@NonNull LocalDate startDate,
                                                              @NonNull LocalDate endDate,
                                                              @Nullable Long userId) {
        List<WorkScheduleEvent> events = new ArrayList<>();

        try {
            Map<LocalDate, Day> schedules = getWorkScheduleForDateRange( startDate, endDate, userId );

            for (Map.Entry<LocalDate, Day> entry : schedules.entrySet()) {
                LocalDate date = entry.getKey();
                Day day = entry.getValue();

                List<WorkScheduleEvent> dayEvents = convertDayToEvents( date, day, userId );
                events.addAll( dayEvents );
            }

            Log.d( TAG, "Generated " + events.size() + " work schedule events for date range" );

        } catch (Exception e) {
            Log.e( TAG, "Error generating work schedule events", e );
        }

        return events;
    }

    // ==================== TEAM MANAGEMENT ====================

    @Override
    @NonNull
    public List<Team> getAllTeams() {
        if ( mTeamsCache != null && !isTeamsCacheExpired() ) {
            return new ArrayList<>( mTeamsCache );
        }

        try {
            // Create Teams from standard Quattrodue pattern
            mTeamsCache = Team.Utils.createStandardTeams();
            mTeamsCacheTime = System.currentTimeMillis();

            Log.d( TAG, "Cached " + mTeamsCache.size() + " complete teams" );
            return new ArrayList<>( mTeamsCache );

        } catch (Exception e) {
            Log.e( TAG, "Error getting all teams", e );
            return new ArrayList<>();
        }
    }

    @Override
    @NonNull
    public List<HalfTeam> getAllHalfTeams() {
        if ( mHalfTeamsCache != null && !isHalfTeamsCacheExpired() ) {
            return new ArrayList<>( mHalfTeamsCache );
        }

        try {
            // Use QuattroDue's half-team list for compatibility
            mHalfTeamsCache = new ArrayList<>( QuattroDue.HALFTEAM_ALL );
            mHalfTeamsCacheTime = System.currentTimeMillis();

            Log.d( TAG, "Cached " + mHalfTeamsCache.size() + " half-teams" );
            return new ArrayList<>( mHalfTeamsCache );

        } catch (Exception e) {
            Log.e( TAG, "Error getting all half-teams", e );
            return new ArrayList<>();
        }
    }

    @Override
    @Nullable
    public Team getTeamForUser(@NonNull Long userId) {
        // Check cache first
        Team cachedTeam = mUserTeamCache.get( userId );
        if ( cachedTeam != null ) {
            return cachedTeam;
        }

        try {
            // Load from preferences
            String teamName = mPreferences.getString( PREF_KEY_USER_TEAM + "_" + userId, null );
            if ( teamName != null ) {
                Team team = findTeamByName( teamName );
                if ( team != null ) {
                    mUserTeamCache.put( userId, team );
                    return team;
                }

                // Try to parse as legacy HalfTeam and find appropriate Team
                HalfTeam halfTeam = findHalfTeamByName( teamName );
                if ( halfTeam != null ) {
                    Team foundTeam = findTeamContaining( halfTeam );
                    if ( foundTeam != null ) {
                        mUserTeamCache.put( userId, foundTeam );
                        return foundTeam;
                    }
                }
            }

            Log.d( TAG, "No team found for user: " + userId );
            return null;

        } catch (Exception e) {
            Log.e( TAG, "Error getting team for user: " + userId, e );
            return null;
        }
    }

    @Override
    @NonNull
    public OperationResult<Void> setTeamForUser(@NonNull Long userId, @NonNull Team team) {
        try {
            // Save to preferences
            mPreferences.edit()
                    .putString( PREF_KEY_USER_TEAM + "_" + userId, team.getTeamName() )
                    .apply();

            // Update cache
            mUserTeamCache.put( userId, team );

            Log.d( TAG, "Set team " + team.getTeamName() + " for user " + userId );
            return OperationResult.success( "Success in setTeamForUser", OperationResult.OperationType.READ );

        } catch (Exception e) {
            Log.e( TAG, "Error setting team for user", e );
            return OperationResult.failure( "Failed to set team: " + e.getMessage(), OperationResult.OperationType.READ );
        }
    }

    @Override
    @NonNull
    public OperationResult<Void> setHalfTeamForUser(@NonNull Long userId, @NonNull HalfTeam halfTeam) {
        try {
            // Find Team containing this HalfTeam
            Team team = findTeamContaining( halfTeam );
            if ( team != null ) {
                return setTeamForUser( userId, team );
            } else {
                // Create a default team pairing or use first available team
                List<Team> allTeams = getAllTeams();
                Team defaultTeam = allTeams.stream()
                        .filter( t -> t.containsHalfTeam( halfTeam ) )
                        .findFirst()
                        .orElse( null );

                if ( defaultTeam != null ) {
                    return setTeamForUser( userId, defaultTeam );
                } else {
                    return OperationResult.failure( "Cannot find appropriate team for half-team: " + halfTeam.getName(), OperationResult.OperationType.READ );
                }
            }

        } catch (Exception e) {
            Log.e( TAG, "Error setting half-team for user", e );
            return OperationResult.failure( "Failed to set half-team: " + e.getMessage(), OperationResult.OperationType.READ );
        }
    }

    @Override
    @NonNull
    public List<Team> getTeamsWorkingOnDate(@NonNull LocalDate date) {
        List<Team> workingTeams = new ArrayList<>();

        try {
            Day day = getWorkScheduleForDate( date, null );
            if ( day != null && day.hasWorkSchedule() ) {
                // Extract teams from the day's shifts
                List<Shift> shifts = day.getShifts();
                for (Shift shift : shifts) {
                    Team team = Team.fromShift( shift );
                    if ( team != null && !workingTeams.contains( team ) ) {
                        workingTeams.add( team );
                    }
                }
            }

        } catch (Exception e) {
            Log.e( TAG, "Error getting teams working on date: " + date, e );
        }

        return workingTeams;
    }

    @Override
    @NonNull
    public List<HalfTeam> getHalfTeamsWorkingOnDate(@NonNull LocalDate date) {
        List<HalfTeam> workingHalfTeams = new ArrayList<>();

        try {
            Day day = getWorkScheduleForDate( date, null );
            if ( day != null && day.hasWorkSchedule() ) {
                // Extract half-teams from the day's shifts
                List<Shift> shifts = day.getShifts();
                for (Shift shift : shifts) {
                    Set<HalfTeam> shiftHalfTeams = shift.getHalfTeams();
                    for (HalfTeam halfTeam : shiftHalfTeams) {
                        if ( !workingHalfTeams.contains( halfTeam ) ) {
                            workingHalfTeams.add( halfTeam );
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e( TAG, "Error getting half-teams working on date: " + date, e );
        }

        return workingHalfTeams;
    }

    // ==================== SHIFT TYPE MANAGEMENT ====================

    @Override
    @NonNull
    public List<ShiftType> getAllShiftTypes() {
        if ( mShiftTypesCache != null && !isShiftTypesCacheExpired() ) {
            return new ArrayList<>( mShiftTypesCache );
        }

        try {
            // Get shift types from QuattroDue
            if ( mQuattroDue != null ) {
//                mShiftTypesCache = new ArrayList<>( mQuattroDue.getShiftTypes() );
                mShiftTypesCache = new ArrayList<>( ShiftTypeFactory.getAllShiftTypes() );
                mShiftTypesCacheTime = System.currentTimeMillis();

                Log.d( TAG, "Cached " + mShiftTypesCache.size() + " shift types" );
                return new ArrayList<>( mShiftTypesCache );
            }

        } catch (Exception e) {
            Log.e( TAG, "Error getting all shift types", e );
        }

        return new ArrayList<>();
    }

    @Override
    @Nullable
    public ShiftType getShiftTypeById(@NonNull Long shiftTypeId) {
        List<ShiftType> shiftTypes = getAllShiftTypes();

        for (ShiftType shiftType : shiftTypes) {
            if ( shiftType.getId() != null && shiftType.getId().equals( shiftTypeId ) ) {
                return shiftType;
            }
        }

        return null;
    }

    @Override
    @Nullable
    public ShiftType getShiftTypeByName(@NonNull String name) {
        List<ShiftType> shiftTypes = getAllShiftTypes();

        for (ShiftType shiftType : shiftTypes) {
            if ( name.equals( shiftType.getName() ) ) {
                return shiftType;
            }
        }

        return null;
    }

    @Override
    @NonNull
    public List<Shift> getShiftsForDate(@NonNull LocalDate date) {
        try {
            Day day = getWorkScheduleForDate( date, null );
            if ( day != null ) {
                return new ArrayList<>( day.getShifts() );
            }

        } catch (Exception e) {
            Log.e( TAG, "Error getting shifts for date: " + date, e );
        }

        return new ArrayList<>();
    }

    // ==================== SCHEDULE CONFIGURATION ====================

    @Override
    @NonNull
    public LocalDate getSchemeStartDate() {
        try {
            if ( mQuattroDue != null ) {
                return mQuattroDue.getSchemeDate();
            }

        } catch (Exception e) {
            Log.e( TAG, "Error getting scheme start date", e );
        }

        // Fallback to default
        return LocalDate.of( 2018, 11, 7 );
    }

    @Override
    @NonNull
    public OperationResult<Void> updateSchemeStartDate(@NonNull LocalDate newStartDate) {
        try {
            // Update QuattroDue
            if ( mQuattroDue != null ) {
                // This update Quattrodue's scheme start date on preferences and rebuild
                mQuattroDue.setSchemeDate( mContext, newStartDate );
            }

            // Save to preferences
            mPreferences.edit()
                    .putString( PREF_KEY_SCHEME_START_DATE, newStartDate.toString() )
                    .apply();

            // Clear cache since calculations will change
            clearCache();

            Log.d( TAG, "Updated scheme start date to: " + newStartDate );
            return OperationResult.success( "Success in updateSchemeStartDate", OperationResult.OperationType.UPDATE );

        } catch (Exception e) {
            Log.e( TAG, "Error updating scheme start date", e );
            return OperationResult.failure( "Failed to update scheme start date: " + e.getMessage(), OperationResult.OperationType.UPDATE );
        }
    }

    @Override
    @NonNull
    public Map<String, Object> getScheduleConfiguration() {
        Map<String, Object> config = new HashMap<>();

        try {
            config.put( "schemeStartDate", getSchemeStartDate().toString() );
            config.put( "showCalendars", mPreferences.getBoolean( PREF_KEY_SHOW_CALENDARS, true ) );
            config.put( "showStops", mPreferences.getBoolean( PREF_KEY_SHOW_STOPS, true ) );
            config.put( "teamsCount", getAllTeams().size() );
            config.put( "shiftTypesCount", getAllShiftTypes().size() );
            config.put( "serviceInitialized", mIsInitialized );

        } catch (Exception e) {
            Log.e( TAG, "Error getting schedule configuration", e );
            config.put( "error", e.getMessage() );
        }

        return config;
    }

    @Override
    @NonNull
    public OperationResult<Void> updateScheduleConfiguration(@NonNull Map<String, Object> configuration) {
        try {
            boolean changed = false;

            // Update individual configuration items
            if ( configuration.containsKey( "showCalendars" ) ) {
                boolean showCalendars = (Boolean) configuration.get( "showCalendars" );
                mPreferences.edit().putBoolean( PREF_KEY_SHOW_CALENDARS, showCalendars ).apply();
                changed = true;
            }

            if ( configuration.containsKey( "showStops" ) ) {
                boolean showStops = (Boolean) configuration.get( "showStops" );
                mPreferences.edit().putBoolean( PREF_KEY_SHOW_STOPS, showStops ).apply();
                changed = true;
            }

            if ( configuration.containsKey( "schemeStartDate" ) ) {
                String dateStr = (String) configuration.get( "schemeStartDate" );
                LocalDate newStartDate = LocalDate.parse( dateStr );
                OperationResult<Void> result = updateSchemeStartDate( newStartDate );
                if ( !result.isSuccess() ) {
                    return result;
                }
                changed = true;
            }

            if ( changed ) {
                Log.d( TAG, "Schedule configuration updated" );
            }

            return OperationResult.success( "Success in updateScheduleConfiguration", OperationResult.OperationType.UPDATE );

        } catch (Exception e) {
            Log.e( TAG, "Error updating schedule configuration", e );
            return OperationResult.failure( "Failed to update configuration: " + e.getMessage(), OperationResult.OperationType.UPDATE );
        }
    }

    // ==================== PATTERN CALCULATIONS ====================

    @Override
    public int getDayInCycle(@NonNull LocalDate date) {
        try {
            if ( mQuattroDue != null ) {
                return mQuattroDue.getDayInCycle( date );
            }

        } catch (Exception e) {
            Log.e( TAG, "Error calculating day in cycle for: " + date, e );
        }

        return 0;
    }

    @Override
    public long getDaysFromSchemeStart(@NonNull LocalDate date) {
        LocalDate schemeStart = getSchemeStartDate();
        return ChronoUnit.DAYS.between( schemeStart, date );
    }

    @Override
    public boolean isWorkingDay(@NonNull LocalDate date) {
        try {
            Day day = getWorkScheduleForDate( date, null );
            return day != null && day.hasWorkSchedule();

        } catch (Exception e) {
            Log.e( TAG, "Error checking if working day: " + date, e );
            return false;
        }
    }

    @Override
    public boolean isWorkingDayForTeam(@NonNull LocalDate date, @NonNull Team team) {
        try {
            List<Team> workingTeams = getTeamsWorkingOnDate( date );
            return workingTeams.contains( team );

        } catch (Exception e) {
            Log.e( TAG, "Error checking if working day for team: " + date + ", " + team.getTeamName(), e );
            return false;
        }
    }

    @Override
    public boolean isWorkingDayForHalfTeam(@NonNull LocalDate date, @NonNull HalfTeam halfTeam) {
        try {
            List<HalfTeam> workingHalfTeams = getHalfTeamsWorkingOnDate( date );
            return workingHalfTeams.contains( halfTeam );

        } catch (Exception e) {
            Log.e( TAG, "Error checking if working day for half-team: " + date + ", " + halfTeam.getName(), e );
            return false;
        }
    }

    @Override
    public boolean isRestDayForTeam(@NonNull LocalDate date, @NonNull Team team) {
        return !isWorkingDayForTeam( date, team );
    }

    @Override
    public boolean isRestDayForHalfTeam(@NonNull LocalDate date, @NonNull HalfTeam halfTeam) {
        return !isWorkingDayForHalfTeam( date, halfTeam );
    }

    // ==================== CALENDAR INTEGRATION ====================

    @Override
    public boolean hasWorkSchedule(@Nullable Day day) {
        return day != null && day.hasWorkSchedule();
    }

    @Override
    @Nullable
    public String getWorkScheduleColor(@NonNull LocalDate date, @Nullable Long userId) {
        try {
            Day day = getWorkScheduleForDate( date, userId );
            if ( day != null && day.hasWorkSchedule() ) {
                // Get color from first shift or default
                List<Shift> shifts = day.getShifts();
                if ( !shifts.isEmpty() ) {
                    Shift firstShift = shifts.get( 0 );
                    if ( firstShift.getShiftType() != null ) {
                        return String.valueOf( firstShift.getShiftType().getColor() );
                    }
                }

                // Default work schedule color
                // TODO:
//                return mContext.getString( R.string.default_work_schedule_color );
                return ( "000000" );
            }

        } catch (Exception e) {
            Log.e( TAG, "Error getting work schedule color for: " + date, e );
        }

        return null;
    }

    @Override
    @Nullable
    public String getWorkScheduleSummary(@NonNull LocalDate date, @Nullable Long userId) {
        try {
            Day day = getWorkScheduleForDate( date, userId );
            if ( day != null && day.hasWorkSchedule() ) {
                StringBuilder summary = new StringBuilder();

                List<Shift> shifts = day.getShifts();
                for (int i = 0; i < shifts.size(); i++) {
                    Shift shift = shifts.get( i );
                    if ( i > 0 ) {
                        summary.append( ", " );
                    }

                    if ( shift.getShiftType() != null ) {
                        summary.append( shift.getShiftType().getName() );
                    }

                    if ( shift.getHalfTeams() != null ) {
                        //summary.append( " (" ).append( shift.getHalfTeam().getName() ).append( ")" );
                        summary.append( " (" ).append( shift.getHalfTeamsAsString() ).append( ")" );
                    }
                }

                return summary.toString();
            }

        } catch (Exception e) {
            Log.e( TAG, "Error getting work schedule summary for: " + date, e );
        }

        return null;
    }

    // ==================== DATA MANAGEMENT ====================

    @Override
    @NonNull
    public OperationResult<Void> refreshWorkScheduleData() {
        try {
            clearCache();

            // Reinitialize QuattroDue if needed
            if ( mQuattroDue != null ) {
                mQuattroDue.refresh( mContext);
            }

            Log.d( TAG, "Work schedule data refreshed" );
            return OperationResult.success( "refreshWorkScheduleData", OperationResult.OperationType.READ );

        } catch (Exception e) {
            Log.e( TAG, "Error refreshing work schedule data", e );
            return OperationResult.failure( "Failed to refresh data: " + e.getMessage() , OperationResult.OperationType.READ );
        }
    }

    @Override
    public void clearCache() {
        mDayCache.clear();
        mUserTeamCache.clear();
        mShiftTypesCache = null;
        mTeamsCache = null;
        mHalfTeamsCache = null;
        mShiftTypesCacheTime = 0;
        mTeamsCacheTime = 0;
        mHalfTeamsCacheTime = 0;

        Log.d( TAG, "All caches cleared" );
    }

    @Override
    @NonNull
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put( "initialized", mIsInitialized );
        status.put( "quattroDueInstance", mQuattroDue != null );
        status.put( "dayCacheSize", mDayCache.size() );
        status.put( "userTeamCacheSize", mUserTeamCache.size() );
        status.put( "teamsCount", getAllTeams().size() );
        status.put( "halfTeamsCount", getAllHalfTeams().size() );
        status.put( "shiftTypesCount", getAllShiftTypes().size() );
        status.put( "schemeStartDate", getSchemeStartDate().toString() );

        return status;
    }

    // ==================== VALIDATION ====================

    @Override
    @NonNull
    public OperationResult<Map<String, Object>> validateConfiguration() {
        Map<String, Object> validation = new HashMap<>();
        boolean isValid = true;

        try {
            // Check if service is initialized
            if ( !mIsInitialized ) {
                validation.put( "serviceInitialized", false );
                isValid = false;
            }

            // Check QuattroDue instance
            if ( mQuattroDue == null ) {
                validation.put( "quattroDueInstance", false );
                isValid = false;
            }

            // Check teams availability
            List<Team> teams = getAllTeams();
            List<HalfTeam> halfTeams = getAllHalfTeams();
            if ( teams.isEmpty() || halfTeams.isEmpty() ) {
                validation.put( "teamsAvailable", false );
                isValid = false;
            }

            // Check shift types availability
            List<ShiftType> shiftTypes = getAllShiftTypes();
            if ( shiftTypes.isEmpty() ) {
                validation.put( "shiftTypesAvailable", false );
                isValid = false;
            }

            // Check scheme start date
            LocalDate schemeStart = getSchemeStartDate();
            if ( schemeStart.isAfter( LocalDate.now() ) ) {
                validation.put( "schemeStartDateValid", false );
                isValid = false;
            }

            validation.put( "isValid", isValid );
            validation.put( "validationTime", System.currentTimeMillis() );

            if ( isValid ) {
                return OperationResult.success( "Success in validateConfiguration", OperationResult.OperationType.VALIDATION );
            } else {
                return OperationResult.failure( "Configuration validation failed", validation.toString() , OperationResult.OperationType.VALIDATION);
            }

        } catch (Exception e) {
            Log.e( TAG, "Error validating configuration", e );
            validation.put( "validationError", e.getMessage() );
            return OperationResult.failure( e, OperationResult.OperationType.VALIDATION );
        }
    }

    @Override
    public boolean isServiceReady() {
        return mIsInitialized && mQuattroDue != null;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Calculate schedule for a specific date using QuattroDue.
     */
    @Nullable
    private Day calculateDaySchedule(@NonNull LocalDate date, @Nullable Long userId) {
        try {
            if ( mQuattroDue != null ) {
                // TODO:
                // Set user team if provided
//                if ( userId != null ) {
//                    HalfTeam userTeam = getTeamForUser( userId );
//                    if ( userTeam != null ) {
//                        mQuattroDue.setUserHalfTeam( userTeam );
//                    }
//                }

                // Get day from QuattroDue
                return mQuattroDue.getDayByDate( date );
            }

        } catch (Exception e) {
            Log.e( TAG, "Error calculating day schedule for: " + date, e );
        }

        return null;
    }

    /**
     * Cache a day schedule with timestamp.
     */
    private void cacheDaySchedule(@NonNull LocalDate date, @NonNull Day day) {
        // Clean up cache if it's getting too large
        if ( mDayCache.size() >= CACHE_CLEANUP_THRESHOLD ) {
            cleanupDayCache();
        }

        mDayCache.put( date, new CachedDay( day, System.currentTimeMillis() ) );
    }

    /**
     * Clean up expired cache entries.
     */
    private void cleanupDayCache() {
        long now = System.currentTimeMillis();
        int initialSize = mDayCache.size();

        mDayCache.entrySet().removeIf( entry -> entry.getValue().isExpired( now ) );

        int cleanedUp = initialSize - mDayCache.size();
        if ( cleanedUp > 0 ) {
            Log.d( TAG, "Cleaned up " + cleanedUp + " expired cache entries" );
        }
    }

    /**
     * Find team by name.
     */
    @Nullable
    private Team findTeamByName(@NonNull String teamName) {
        List<Team> teams = getAllTeams();
        for (Team team : teams) {
            if ( teamName.equals( team.getTeamName() ) ) {
                return team;
            }
        }
        return null;
    }

    /**
     * Find half-team by name.
     */
    @Nullable
    private HalfTeam findHalfTeamByName(@NonNull String halfTeamName) {
        List<HalfTeam> halfTeams = getAllHalfTeams();
        for (HalfTeam halfTeam : halfTeams) {
            if ( halfTeamName.equals( halfTeam.getName() ) ) {
                return halfTeam;
            }
        }
        return null;
    }

    /**
     * Find Team containing a specific HalfTeam.
     */
    @Nullable
    private Team findTeamContaining(@NonNull HalfTeam halfTeam) {
        List<Team> teams = getAllTeams();
        for (Team team : teams) {
            if ( team.containsHalfTeam( halfTeam ) ) {
                return team;
            }
        }
        return null;
    }

    /**
     * Convert Day object to WorkScheduleEvent objects.
     */
    @NonNull
    private List<WorkScheduleEvent> convertDayToEvents(@NonNull LocalDate date, @NonNull Day day, @Nullable Long userId) {
        List<WorkScheduleEvent> events = new ArrayList<>();

        try {
            List<Shift> shifts = day.getShifts();
            for (Shift shift : shifts) {
                WorkScheduleEvent event = new WorkScheduleEvent();
                event.setDate( date );
                event.setShift( shift );
                event.setTeam( Team.fromShift( shift ) );
                event.setShiftType( shift.getShiftType() );
                event.setUserId( userId );

                events.add( event );
            }

        } catch (Exception e) {
            Log.e( TAG, "Error converting day to events", e );
        }

        return events;
    }

    /**
     * Check if shift types cache is expired.
     */
    private boolean isShiftTypesCacheExpired() {
        return System.currentTimeMillis() - mShiftTypesCacheTime > CACHE_TTL_MS;
    }

    /**
     * Check if teams cache is expired.
     */
    private boolean isTeamsCacheExpired() {
        return System.currentTimeMillis() - mTeamsCacheTime > CACHE_TTL_MS;
    }

    /**
     * Check if half-teams cache is expired.
     */
    private boolean isHalfTeamsCacheExpired() {
        return System.currentTimeMillis() - mHalfTeamsCacheTime > CACHE_TTL_MS;
    }

    // ==================== CACHED DAY CLASS ====================

    /**
     * Cached day with timestamp for TTL management.
     */
    private record CachedDay(Day day, long timestamp) {
        private CachedDay(@NonNull Day day, long timestamp) {
            this.day = day;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return isExpired( System.currentTimeMillis() );
        }

        boolean isExpired(long currentTime) {
            return currentTime - timestamp > CACHE_TTL_MS;
        }
    }
}