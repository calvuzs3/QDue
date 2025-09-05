package net.calvuz.qdue.data.repositories;

import static net.calvuz.qdue.QDue.Defaults.DEFAULT_DAILY_RULE_ID;
import static net.calvuz.qdue.QDue.Defaults.DEFAULT_DAILY_RULE_NAME;
import static net.calvuz.qdue.QDue.Defaults.DEFAULT_DAILY_RULE_DESCRIPTION;
import static net.calvuz.qdue.QDue.Defaults.DEFAULT_QD_RRULE_DESCRIPTION;
import static net.calvuz.qdue.QDue.Defaults.DEFAULT_QD_RRULE_ID;
import static net.calvuz.qdue.QDue.Defaults.DEFAULT_QD_RRULE_NAME;
import static net.calvuz.qdue.QDue.Defaults.DEFAULT_WEEKDAYS_RRULE_ID;
import static net.calvuz.qdue.QDue.Defaults.DEFAULT_WEEKDAYS_RRULE_NAME;
import static net.calvuz.qdue.QDue.Defaults.DEFAULT_WEEKDAYS_RRULE_DESCRIPTION;

import android.content.Context;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.db.CalendarDatabase;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.dao.RecurrenceRuleDao;
import net.calvuz.qdue.data.entities.RecurrenceRuleEntity;
import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.domain.calendar.repositories.RecurrenceRuleRepository;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * RecurrenceRuleRepositoryImpl - Clean Architecture Data Layer Implementation
 *
 * <p>Repository implementation for managing recurrence rules and schedule patterns
 * following clean architecture principles. Provides data persistence and business logic
 * for Google Calendar-style RRULE patterns and QuattroDue cycle management.</p>
 *
 * <h3>Core Responsibilities:</h3>
 * <ul>
 *   <li><strong>Recurrence Rule CRUD</strong>: Create, read, update, delete recurrence patterns</li>
 *   <li><strong>Pattern Management</strong>: Handle DAILY, WEEKLY, MONTHLY, YEARLY, QUATTRODUE_CYCLE patterns</li>
 *   <li><strong>Domain Model Conversion</strong>: Convert between RecurrenceRuleEntity and RecurrenceRule</li>
 *   <li><strong>Usage Tracking</strong>: Monitor which rules are in use by schedule assignments</li>
 *   <li><strong>Standard Patterns</strong>: Initialize and manage standard QuattroDue recurrence patterns</li>
 * </ul>
 *
 * <h3>Design Patterns:</h3>
 * <ul>
 *   <li><strong>Repository Pattern</strong>: Clean separation between domain and data</li>
 *   <li><strong>Dependency Injection</strong>: Constructor-based DI following project standards</li>
 *   <li><strong>Async Processing</strong>: Background thread execution with ExecutorService</li>
 *   <li><strong>Caching Strategy</strong>: In-memory cache for frequently accessed standard patterns</li>
 * </ul>
 *
 * <h3>Recurrence Pattern Support:</h3>
 * <ul>
 *   <li><strong>DAILY</strong>: Every N days patterns</li>
 *   <li><strong>WEEKLY</strong>: Weekly patterns with specific days (BYDAY)</li>
 *   <li><strong>MONTHLY</strong>: Monthly patterns with day-of-month support</li>
 *   <li><strong>YEARLY</strong>: Annual recurrence patterns</li>
 *   <li><strong>QUATTRODUE_CYCLE</strong>: Custom 4-2 work pattern (4 work days, 2 rest days)</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Implementation
 * @since Database Version 7
 */
public class RecurrenceRuleRepositoryImpl implements RecurrenceRuleRepository {

    private static final String TAG = "RecurrenceRuleRepositoryImpl";

    // ==================== DEPENDENCIES ====================

    private final RecurrenceRuleDao mRecurrenceRuleDao;
    private final CoreBackupManager mBackupManager;
    private final ExecutorService mExecutorService;

    // ==================== CACHING ====================

    private final ConcurrentHashMap<String, RecurrenceRule> mStandardRulesCache = new ConcurrentHashMap<>();
    private volatile boolean mStandardRulesCacheInitialized = false;

    // ==================== CONSTRUCTOR FOR DEPENDENCY INJECTION ====================

    /**
     * Constructor for dependency injection following clean architecture principles.
     *
     * @param context       Application context for resource access
     * @param database      CalendarDatabase instance for data operations
     * @param backupManager CoreBackupManager for automatic backup integration
     */
    public RecurrenceRuleRepositoryImpl(@NonNull Context context,
                                        @NonNull CalendarDatabase database,
                                        @NonNull CoreBackupManager backupManager) {
        this.mRecurrenceRuleDao = database.recurrenceRuleDao();
        this.mBackupManager = backupManager;
        this.mExecutorService = Executors.newSingleThreadExecutor(); // .newFixedThreadPool( 3 );

        // Initialize standard recurrence rules if needed
        initializeStandardRulesIfNeeded();

        Log.d( TAG, "RecurrenceRuleRepositoryImpl initialized" );
    }

    // ==================== RECURRENCE RULE CRUD OPERATIONS ====================

    @NonNull
    @Override
    public CompletableFuture<RecurrenceRule> saveRecurrenceRule(@NonNull RecurrenceRule recurrenceRule) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                // Convert to entity
                RecurrenceRuleEntity entity = RecurrenceRuleEntity.fromDomainModel( recurrenceRule );

                // Check if rule exists
                RecurrenceRuleEntity existingEntity = mRecurrenceRuleDao.getRecurrenceRuleById( recurrenceRule.getId() );
                boolean isUpdate = existingEntity != null;

                if (isUpdate) {
                    // Preserve creation time for updates
                    entity.setCreatedAt( existingEntity.getCreatedAt() );
                    entity.setUpdatedAt( System.currentTimeMillis() );
                    mRecurrenceRuleDao.updateRecurrenceRule( entity );
                    Log.d( TAG, "Updated existing recurrence rule: " + recurrenceRule.getId() );
                } else {
                    // Ensure we have an ID for new rules
                    if (recurrenceRule.getId().isEmpty()) {
                        entity.setId( UUID.randomUUID().toString() );
                    }
                    mRecurrenceRuleDao.insertRecurrenceRule( entity );
                    Log.d( TAG, "Inserted new recurrence rule: " + entity.getId() );
                }

                // Trigger auto backup
                mBackupManager.performAutoBackup( "recurrence_rules", isUpdate ? "update" : "create" );

                // Clear cache if standard rule updated
                if (isDefaultRule( entity.getId() )) {
                    mStandardRulesCache.remove( entity.getId() );
                    mStandardRulesCacheInitialized = false; // Force refresh
                }

                RecurrenceRule savedRule = entity.toDomainModel();
                Log.d( TAG, "Successfully saved recurrence rule: " + recurrenceRule.getName() );
                return savedRule;
            } catch (Exception e) {
                Log.e( TAG, "Error saving recurrence rule: " + recurrenceRule.getName(), e );
                throw new RuntimeException( "Failed to save recurrence rule", e );
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> insertRecurrenceRule(@NonNull RecurrenceRule recurrenceRule) {
        try {
            // Convert to entity
            RecurrenceRuleEntity entity = RecurrenceRuleEntity.fromDomainModel( recurrenceRule );

            // Ensure we have an ID for new rules
            if (recurrenceRule.getId().isEmpty()) {
                entity.setId( UUID.randomUUID().toString() );
            }

            long result = mRecurrenceRuleDao.insertRecurrenceRule( entity );

            if (result > 0) {
                Log.d( TAG, "Inserted new recurrence rule: " + entity.getId() );
                return CompletableFuture.completedFuture( true );
            } else {
                throw new Exception( "Failed to insert recurrence rule" );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error inserting recurrence rule: " + recurrenceRule.getName(), e );
            return CompletableFuture.completedFuture( false );
        }
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> updateRecurrenceRule(@NonNull RecurrenceRule recurrenceRule) {
        try {
            // Convert to entity
            RecurrenceRuleEntity entity = RecurrenceRuleEntity.fromDomainModel( recurrenceRule );

            // Check if rule exists
            RecurrenceRuleEntity existingEntity = mRecurrenceRuleDao.getRecurrenceRuleById( recurrenceRule.getId() );
            boolean isUpdate = existingEntity != null;
            if (isUpdate) {
                // Preserve creation time for updates
                entity.setCreatedAt( existingEntity.getCreatedAt() );
                entity.setUpdatedAt( System.currentTimeMillis() );
            } else {
                throw new Exception( "Failed to update recurrence rule (not found)" );
            }

            // Update
            int result = mRecurrenceRuleDao.updateRecurrenceRule( entity );

            if (result == 1) {
                Log.d( TAG, "Inserted new recurrence rule: " + entity.getId() );
                return CompletableFuture.completedFuture( true );
            } else {
                throw new Exception( "Failed to update recurrence rule (db error)" );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error updating recurrence rule: " + recurrenceRule.getName(), e );
            return CompletableFuture.completedFuture( false );
        }
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> deleteRecurrenceRule(@NonNull String ruleId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                // Prevent deletion of standard rules
                if (isDefaultRule( ruleId )) {
                    Log.w( TAG, "Cannot delete standard recurrence rule: " + ruleId );
                    return false;
                }

                // Check if rule is in use before deletion
                boolean inUse = isRecurrenceRuleInUse( ruleId ).get();
                if (inUse) {
                    Log.w( TAG, "Cannot delete recurrence rule in use: " + ruleId );
                    // Soft delete by marking inactive instead
                    long timestamp = System.currentTimeMillis();
                    int affectedRows = mRecurrenceRuleDao.deactivateRecurrenceRule( ruleId, timestamp );
                    if (affectedRows > 0) {
                        mBackupManager.performAutoBackup( "recurrence_rules", "deactivate" );
                        Log.d( TAG, "Deactivated recurrence rule in use: " + ruleId );
                        return true;
                    }
                    return false;
                } else {
                    // Hard delete if not in use
                    int affectedRows = mRecurrenceRuleDao.deleteRecurrenceRuleById( ruleId );
                    if (affectedRows > 0) {
                        mBackupManager.performAutoBackup( "recurrence_rules", "delete" );
                        Log.i( TAG, "Successfully deleted recurrence rule: " + ruleId );
                        return true;
                    } else {
                        Log.w( TAG, "No recurrence rule found to delete: " + ruleId );
                        return false;
                    }
                }
            } catch (Exception e) {
                Log.e( TAG, "Error deleting recurrence rule: " + ruleId, e );
                return false;
            }
        }, mExecutorService );
    }

    // ==================== RECURRENCE RULE QUERY OPERATIONS ====================

    /**
     * Get active RecurrenceRules created by specific user.
     *
     * @param userId User ID to filter by
     * @return CompletableFuture with user's active patterns
     */
    @NonNull
    @Override
    public CompletableFuture<OperationResult<List<RecurrenceRule>>> getActiveUserRecurrenceRules(@NonNull String userId) {
        // Unused userID
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<RecurrenceRuleEntity> entities = mRecurrenceRuleDao.getActiveUserRecurrenceRules();
                List<RecurrenceRule> domainRules = entities.stream()
                        .map( RecurrenceRuleEntity::toDomainModel )
                        .filter( java.util.Objects::nonNull )
                        .collect( Collectors.toList() );

                Log.v( TAG, "Successfully retrieved " + domainRules.size() + " active recurrence rules" );
                return OperationResult.success( domainRules,
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting all active recurrence rules", e );
                return OperationResult.failure( "Error getting all active user recurrence rules",
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    /**
     * Get RecurrenceRule by name with fallback alternatives.
     *
     * @param name Primary name to search
     * @return OperationResult that supports .or() chaining
     */
    @NonNull
    @Override
    public CompletableFuture<OperationResult<RecurrenceRule>> getRecurrenceRuleByName(@NonNull String name) {

        return CompletableFuture.supplyAsync( () -> {
            try {
                RecurrenceRuleEntity entity = mRecurrenceRuleDao.getRecurrenceRuleByName( name );
                if (entity == null) {
                    Log.w( TAG, "No recurrence Rule found by name: " + name );
                    return OperationResult.success( null, OperationResult.OperationType.READ );
                }
                RecurrenceRule domainRule = entity.toDomainModel();

                Log.d( TAG, "Successfully retrieved recurrence rule by name: " + name );
                return OperationResult.success( domainRule, OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting recurrence rule by name: " + name, e );
                return OperationResult.failure( "Error getting recurrence rule by name: " + name,
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<List<RecurrenceRule>> getAllRecurrenceRules() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<RecurrenceRuleEntity> entities = mRecurrenceRuleDao.getAllRecurrenceRules();

                List<RecurrenceRule> domainRules = entities.stream()
                        .map( RecurrenceRuleEntity::toDomainModel )
                        .filter( java.util.Objects::nonNull )
                        .collect( Collectors.toList() );

                Log.d( TAG, "Successfully retrieved " + domainRules.size() + " recurrence rules" );
                return domainRules;
            } catch (Exception e) {
                Log.e( TAG, "Error getting all recurrence rules", e );
                return new ArrayList<>();
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<RecurrenceRule> getRecurrenceRuleById(@NonNull String ruleId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                // Check standard rules cache first
                if (mStandardRulesCache.containsKey( ruleId )) {
                    Log.d( TAG, "Returning cached standard rule: " + ruleId );
                    return mStandardRulesCache.get( ruleId );
                }

                // Query database
                RecurrenceRuleEntity entity = mRecurrenceRuleDao.getRecurrenceRuleById( ruleId );
                if (entity == null) {
                    Log.w( TAG, "Recurrence rule not found with ID: " + ruleId );
                    return null;
                }

                RecurrenceRule domainRule = entity.toDomainModel();
                Log.v( TAG, "Successfully retrieved recurrence rule: " + ruleId );
                return domainRule;
            } catch (Exception e) {
                Log.e( TAG, "Error getting recurrence rule by ID: " + ruleId, e );
                return null;
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<List<RecurrenceRule>> getAllActiveRecurrenceRules() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<RecurrenceRuleEntity> entities = mRecurrenceRuleDao.getAllActiveRecurrenceRules();
                List<RecurrenceRule> domainRules = entities.stream()
                        .map( RecurrenceRuleEntity::toDomainModel )
                        .filter( java.util.Objects::nonNull )
                        .collect( Collectors.toList() );

                Log.v( TAG, "Successfully retrieved " + domainRules.size() + " active recurrence rules" );
                return domainRules;
            } catch (Exception e) {
                Log.e( TAG, "Error getting all active recurrence rules", e );
                return new ArrayList<>();
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<List<RecurrenceRule>> getRecurrenceRulesByFrequency(@NonNull RecurrenceRule.Frequency frequency) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                String frequencyString = frequency.name();
                List<RecurrenceRuleEntity> entities = mRecurrenceRuleDao.getRecurrenceRulesByFrequency( frequencyString );
                List<RecurrenceRule> domainRules = entities.stream()
                        .map( RecurrenceRuleEntity::toDomainModel )
                        .filter( java.util.Objects::nonNull )
                        .collect( Collectors.toList() );

                Log.v( TAG, "Successfully retrieved " + domainRules.size() + " rules with frequency: " + frequency );
                return domainRules;
            } catch (Exception e) {
                Log.e( TAG, "Error getting recurrence rules by frequency: " + frequency, e );
                return new ArrayList<>();
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> isRecurrenceRuleInUse(@NonNull String ruleId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                // This would need to check UserScheduleAssignmentDao or other entities
                // that reference this recurrence rule. For now, implement basic logic.

                // Standard rules are always considered "in use"
                if (isDefaultRule( ruleId )) {
                    return true;
                }

                // TODO: Implement actual usage check by querying UserScheduleAssignmentDao
                // For now, return false (not in use) for non-standard rules
                Log.d( TAG, "Recurrence rule usage check completed for: " + ruleId );
                return false;
            } catch (Exception e) {
                Log.e( TAG, "Error checking recurrence rule usage: " + ruleId, e );
                return false; // Assume not in use on error
            }
        }, mExecutorService );
    }

    // ==================== ADDITIONAL BUSINESS OPERATIONS ====================

    /**
     * Get standard QuattroDue recurrence rules.
     *
     * @return CompletableFuture with List of standard recurrence rules
     */
    @NonNull
    public CompletableFuture<List<RecurrenceRule>> getStandardRecurrenceRules() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                if (!mStandardRulesCacheInitialized) {
                    initializeStandardRulesCache();
                }

                List<RecurrenceRule> standardRules = new ArrayList<>( mStandardRulesCache.values() );
                Log.v( TAG, "Retrieved " + standardRules.size() + " standard recurrence rules" );
                return standardRules;
            } catch (Exception e) {
                Log.e( TAG, "Error getting standard recurrence rules", e );
                return new ArrayList<>();
            }
        }, mExecutorService );
    }

    /**
     * Get QuattroDue cycle recurrence rules specifically.
     *
     * @return CompletableFuture with List of QuattroDue cycle rules
     */
    @NonNull
    public CompletableFuture<List<RecurrenceRule>> getQuattroDueRecurrenceRules() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<RecurrenceRuleEntity> entities = mRecurrenceRuleDao.getQuattroDueRecurrenceRules();
                List<RecurrenceRule> domainRules = entities.stream()
                        .map( RecurrenceRuleEntity::toDomainModel )
                        .filter( java.util.Objects::nonNull )
                        .collect( Collectors.toList() );

                Log.v( TAG, "Retrieved " + domainRules.size() + " QuattroDue recurrence rules" );
                return domainRules;
            } catch (Exception e) {
                Log.e( TAG, "Error getting QuattroDue recurrence rules", e );
                return new ArrayList<>();
            }
        }, mExecutorService );
    }

    /**
     * Get recurrence rules active for a specific date.
     *
     * @param date Target date
     * @return CompletableFuture with List of active rules for the date
     */
    @NonNull
    public CompletableFuture<List<RecurrenceRule>> getActiveRecurrenceRulesForDate(@NonNull LocalDate date) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                String dateString = date.toString();
                List<RecurrenceRuleEntity> entities = mRecurrenceRuleDao.getActiveRecurrenceRulesForDate( dateString );
                List<RecurrenceRule> domainRules = entities.stream()
                        .map( RecurrenceRuleEntity::toDomainModel )
                        .filter( java.util.Objects::nonNull )
                        .collect( Collectors.toList() );

                Log.v( TAG, "Retrieved " + domainRules.size() + " active rules for date: " + date );
                return domainRules;
            } catch (Exception e) {
                Log.e( TAG, "Error getting active recurrence rules for date: " + date, e );
                return new ArrayList<>();
            }
        }, mExecutorService );
    }

    /**
     * Get recurrence rules statistics.
     *
     * @return CompletableFuture with RecurrenceRuleStatistics
     */
    @NonNull
    public CompletableFuture<RecurrenceRuleStatistics> getRecurrenceRuleStatistics() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                RecurrenceRuleDao.RecurrenceRuleStatistics dbStats = mRecurrenceRuleDao.getRecurrenceRuleStatistics();
                if (dbStats == null) {
                    return new RecurrenceRuleStatistics( 0, 0, 0, 0, 0 );
                }

                RecurrenceRuleStatistics statistics = new RecurrenceRuleStatistics(
                        dbStats.total_rules,
                        dbStats.active_rules,
                        dbStats.quattrodue_rules,
                        dbStats.weekly_rules,
                        dbStats.daily_rules
                );

                Log.v( TAG, "Calculated recurrence rule statistics successfully" );
                return statistics;
            } catch (Exception e) {
                Log.e( TAG, "Error getting recurrence rule statistics", e );
                return new RecurrenceRuleStatistics( 0, 0, 0, 0, 0 );
            }
        }, mExecutorService );
    }

    // ==================== HELPER METHODS ====================

    /**
     * Initialize standard recurrence rules if they don't exist in database.
     */
    private void initializeStandardRulesIfNeeded() {
        CompletableFuture.runAsync( () -> {
            try {
                // Check if standard rules exist
                if (mRecurrenceRuleDao.getRecurrenceRuleById( DEFAULT_QD_RRULE_ID ) == null) {
                    createStandardQuattroDueRule();
                }

                if (mRecurrenceRuleDao.getRecurrenceRuleById( DEFAULT_WEEKDAYS_RRULE_ID ) == null) {
                    createStandardWeekdaysRule();
                }

                if (mRecurrenceRuleDao.getRecurrenceRuleById( DEFAULT_DAILY_RULE_ID ) == null) {
                    createStandardDailyRule();
                }

                Log.d( TAG, "Standard recurrence rules initialization completed" );
            } catch (Exception e) {
                Log.e( TAG, "Error initializing standard recurrence rules", e );
            }
        }, mExecutorService );
    }

    /**
     * Initialize standard rules cache.
     */
    private void initializeStandardRulesCache() {
        try {
            // Load all standard rules
            String[] standardRuleIds = {DEFAULT_QD_RRULE_ID, DEFAULT_WEEKDAYS_RRULE_ID, DEFAULT_DAILY_RULE_ID};

            for (String ruleId : standardRuleIds) {
                RecurrenceRuleEntity entity = mRecurrenceRuleDao.getRecurrenceRuleById( ruleId );
                if (entity != null) {
                    RecurrenceRule rule = entity.toDomainModel();
                    mStandardRulesCache.put( ruleId, rule );
                }
            }

            mStandardRulesCacheInitialized = true;
            Log.d( TAG, "Standard recurrence rules cache initialized with " + mStandardRulesCache.size() + " rules" );
        } catch (Exception e) {
            Log.e( TAG, "Error initializing standard rules cache", e );
        }
    }

    /**
     * Create standard QuattroDue recurrence rule.
     */
    private void createStandardQuattroDueRule() {
        try {
            RecurrenceRule quattroDueRule = RecurrenceRule.builder()
                    .id( DEFAULT_QD_RRULE_ID )
                    .name( DEFAULT_QD_RRULE_NAME )
                    .description( DEFAULT_QD_RRULE_DESCRIPTION )
                    .frequency( RecurrenceRule.Frequency.QUATTRODUE_CYCLE )
                    .interval( 1 )
                    .startDate( LocalDate.now() )
                    .endType( RecurrenceRule.EndType.NEVER )
                    .cycleLength( 18 )
                    .workDays( 4 )
                    .restDays( 2 )
                    .active( true )
                    .build();

            RecurrenceRuleEntity entity = RecurrenceRuleEntity.fromDomainModel( quattroDueRule );
            long result = mRecurrenceRuleDao.insertRecurrenceRule( entity );
            if (result > 0) {
                Log.d( TAG, "Created standard QuattroDue recurrence rule" );
            } else {
                throw new IllegalStateException( "Failed to create standard QuattroDue rule" );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error creating standard QuattroDue rule", e );
        }
    }

    /**
     * Create standard weekdays recurrence rule.
     */
    private void createStandardWeekdaysRule() {
        try {
            List<DayOfWeek> weekdays = List.of(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            );

            RecurrenceRule weekdaysRule = RecurrenceRule.builder()
                    .id( DEFAULT_WEEKDAYS_RRULE_ID )
                    .name( DEFAULT_WEEKDAYS_RRULE_NAME )
                    .description(DEFAULT_WEEKDAYS_RRULE_DESCRIPTION )
                    .frequency( RecurrenceRule.Frequency.WEEKLY )
                    .interval( 1 )
                    .startDate( LocalDate.now() )
                    .endType( RecurrenceRule.EndType.NEVER )
                    .byDay( weekdays )
                    .weekStart( RecurrenceRule.WeekStart.MONDAY )
                    .active( true )
                    .build();

            RecurrenceRuleEntity entity = RecurrenceRuleEntity.fromDomainModel( weekdaysRule );
            long result = mRecurrenceRuleDao.insertRecurrenceRule( entity );
            if (result > 0 ) {
                Log.d( TAG, "Created standard weekdays recurrence rule" );
            } else {
                throw new IllegalStateException( "Failed to create standard weekdays rule" );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error creating standard weekdays rule", e );
        }
    }

    /**
     * Create standard daily recurrence rule.
     */
    private void createStandardDailyRule() {
        try {
            RecurrenceRule dailyRule = RecurrenceRule.builder()
                    .id( DEFAULT_DAILY_RULE_ID )
                    .name( DEFAULT_DAILY_RULE_NAME )
                    .description( DEFAULT_DAILY_RULE_DESCRIPTION )
                    .frequency( RecurrenceRule.Frequency.DAILY )
                    .interval( 1 )
                    .startDate( LocalDate.now() )
                    .endType( RecurrenceRule.EndType.NEVER )
                    .active( true )
                    .build();

            RecurrenceRuleEntity entity = RecurrenceRuleEntity.fromDomainModel( dailyRule );
            long result = mRecurrenceRuleDao.insertRecurrenceRule( entity );
            if (result > 0) {
                Log.d( TAG, "Created standard daily recurrence rule" );
            } else {
                throw new IllegalStateException( "Failed to create standard daily rule" );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error creating standard daily rule", e );
        }
    }

    /**
     * Check if recurrence rule ID is a standard rule.
     */
    private boolean isDefaultRule(@NonNull String ruleId) {
        return DEFAULT_QD_RRULE_ID.equals( ruleId ) ||
                DEFAULT_WEEKDAYS_RRULE_ID.equals( ruleId ) ||
                DEFAULT_DAILY_RULE_ID.equals( ruleId );
    }

    // ==================== INNER CLASSES ====================

    /**
         * Recurrence rule statistics data class.
         */
        public record RecurrenceRuleStatistics(int totalRules, int activeRules, int quattroDueRules,
                                               int weeklyRules, int dailyRules) {

        public double getActiveRulePercentage() {
                return totalRules > 0 ? (double) activeRules / totalRules * 100.0 : 0.0;
            }

            public boolean hasActiveRules() {
                return activeRules > 0;
            }

            @NonNull
            @Override
            public String toString() {
                return "RecurrenceRuleStatistics{" +
                        "totalRules=" + totalRules +
                        ", activeRules=" + activeRules +
                        ", quattroDueRules=" + quattroDueRules +
                        ", weeklyRules=" + weeklyRules +
                        ", dailyRules=" + dailyRules +
                        '}';
            }
        }
}