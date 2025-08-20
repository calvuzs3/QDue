package net.calvuz.qdue.data.repositories;

import android.content.Context;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.core.db.CalendarDatabase;
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

    private final Context mContext;
    private final CalendarDatabase mDatabase;
    private final RecurrenceRuleDao mRecurrenceRuleDao;
    private final CoreBackupManager mBackupManager;
    private final LocaleManager mLocaleManager;
    private final ExecutorService mExecutorService;

    // ==================== CACHING ====================

    private final ConcurrentHashMap<String, RecurrenceRule> mStandardRulesCache = new ConcurrentHashMap<>();
    private volatile boolean mStandardRulesCacheInitialized = false;

    // ==================== CONSTANTS ====================

    private static final String STANDARD_QUATTRODUE_RULE_ID = "quattrodue_standard_cycle";
    private static final String STANDARD_WEEKDAYS_RULE_ID = "weekdays_only";
    private static final String STANDARD_DAILY_RULE_ID = "daily_pattern";

    // ==================== CONSTRUCTOR FOR DEPENDENCY INJECTION ====================

    /**
     * Constructor for dependency injection following clean architecture principles.
     *
     * @param context Application context for resource access
     * @param database CalendarDatabase instance for data operations
     * @param backupManager CoreBackupManager for automatic backup integration
     */
    public RecurrenceRuleRepositoryImpl(@NonNull Context context,
                                        @NonNull CalendarDatabase database,
                                        @NonNull CoreBackupManager backupManager) {
        this.mContext = context.getApplicationContext();
        this.mDatabase = database;
        this.mRecurrenceRuleDao = database.recurrenceRuleDao();
        this.mBackupManager = backupManager;
        this.mLocaleManager = new LocaleManager(mContext);
        this.mExecutorService = Executors.newFixedThreadPool(3);

        // Initialize standard recurrence rules if needed
        initializeStandardRulesIfNeeded();

        Log.d(TAG, "RecurrenceRuleRepositoryImpl initialized via dependency injection");
    }

    // ==================== RECURRENCE RULE CRUD OPERATIONS ====================

    @NonNull
    @Override
    public CompletableFuture<RecurrenceRule> getRecurrenceRuleById(@NonNull String ruleId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting recurrence rule by ID: " + ruleId);

                // Check standard rules cache first
                if (mStandardRulesCache.containsKey(ruleId)) {
                    Log.d(TAG, "Returning cached standard rule: " + ruleId);
                    return mStandardRulesCache.get(ruleId);
                }

                // Query database
                RecurrenceRuleEntity entity = mRecurrenceRuleDao.getRecurrenceRuleById(ruleId);
                if (entity == null) {
                    Log.w(TAG, "Recurrence rule not found with ID: " + ruleId);
                    return null;
                }

                RecurrenceRule domainRule = entity.toDomainModel();
                Log.d(TAG, "Successfully retrieved recurrence rule: " + ruleId);
                return domainRule;

            } catch (Exception e) {
                Log.e(TAG, "Error getting recurrence rule by ID: " + ruleId, e);
                return null;
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<RecurrenceRule>> getAllActiveRecurrenceRules() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting all active recurrence rules");

                List<RecurrenceRuleEntity> entities = mRecurrenceRuleDao.getAllActiveRecurrenceRules();
                List<RecurrenceRule> domainRules = entities.stream()
                        .map(RecurrenceRuleEntity::toDomainModel)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                Log.d(TAG, "Successfully retrieved " + domainRules.size() + " active recurrence rules");
                return domainRules;

            } catch (Exception e) {
                Log.e(TAG, "Error getting all active recurrence rules", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<RecurrenceRule>> getRecurrenceRulesByFrequency(@NonNull RecurrenceRule.Frequency frequency) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting recurrence rules by frequency: " + frequency);

                String frequencyString = frequency.name();
                List<RecurrenceRuleEntity> entities = mRecurrenceRuleDao.getRecurrenceRulesByFrequency(frequencyString);
                List<RecurrenceRule> domainRules = entities.stream()
                        .map(RecurrenceRuleEntity::toDomainModel)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                Log.d(TAG, "Successfully retrieved " + domainRules.size() + " rules with frequency: " + frequency);
                return domainRules;

            } catch (Exception e) {
                Log.e(TAG, "Error getting recurrence rules by frequency: " + frequency, e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<RecurrenceRule> saveRecurrenceRule(@NonNull RecurrenceRule recurrenceRule) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Saving recurrence rule: " + recurrenceRule.getName());

                // Convert to entity
                RecurrenceRuleEntity entity = RecurrenceRuleEntity.fromDomainModel(recurrenceRule);
                if (entity == null) {
                    throw new IllegalArgumentException("Cannot convert recurrence rule to entity");
                }

                // Check if rule exists
                RecurrenceRuleEntity existingEntity = mRecurrenceRuleDao.getRecurrenceRuleById(recurrenceRule.getId());
                boolean isUpdate = existingEntity != null;

                if (isUpdate) {
                    // Preserve creation time for updates
                    entity.setCreatedAt(existingEntity.getCreatedAt());
                    entity.setUpdatedAt(System.currentTimeMillis());
                    mRecurrenceRuleDao.updateRecurrenceRule(entity);
                    Log.d(TAG, "Updated existing recurrence rule: " + recurrenceRule.getId());
                } else {
                    // Ensure we have an ID for new rules
                    if (recurrenceRule.getId() == null || recurrenceRule.getId().isEmpty()) {
                        entity.setId(UUID.randomUUID().toString());
                    }
                    mRecurrenceRuleDao.insertRecurrenceRule(entity);
                    Log.d(TAG, "Inserted new recurrence rule: " + entity.getId());
                }

                // Trigger auto backup
                mBackupManager.performAutoBackup("recurrence_rules", isUpdate ? "update" : "create");

                // Clear cache if standard rule updated
                if (isStandardRule(entity.getId())) {
                    mStandardRulesCache.remove(entity.getId());
                    mStandardRulesCacheInitialized = false; // Force refresh
                }

                RecurrenceRule savedRule = entity.toDomainModel();
                Log.d(TAG, "Successfully saved recurrence rule: " + recurrenceRule.getName());
                return savedRule;

            } catch (Exception e) {
                Log.e(TAG, "Error saving recurrence rule: " + recurrenceRule.getName(), e);
                throw new RuntimeException("Failed to save recurrence rule", e);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> deleteRecurrenceRule(@NonNull String ruleId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Deleting recurrence rule: " + ruleId);

                // Prevent deletion of standard rules
                if (isStandardRule(ruleId)) {
                    Log.w(TAG, "Cannot delete standard recurrence rule: " + ruleId);
                    return false;
                }

                // Check if rule is in use before deletion
                boolean inUse = isRecurrenceRuleInUse(ruleId).get();
                if (inUse) {
                    Log.w(TAG, "Cannot delete recurrence rule in use: " + ruleId);
                    // Soft delete by marking inactive instead
                    long timestamp = System.currentTimeMillis();
                    int affectedRows = mRecurrenceRuleDao.deactivateRecurrenceRule(ruleId, timestamp);
                    if (affectedRows > 0) {
                        mBackupManager.performAutoBackup("recurrence_rules", "deactivate");
                        Log.d(TAG, "Deactivated recurrence rule in use: " + ruleId);
                        return true;
                    }
                    return false;
                } else {
                    // Hard delete if not in use
                    int affectedRows = mRecurrenceRuleDao.deleteRecurrenceRuleById(ruleId);
                    if (affectedRows > 0) {
                        mBackupManager.performAutoBackup("recurrence_rules", "delete");
                        Log.d(TAG, "Successfully deleted recurrence rule: " + ruleId);
                        return true;
                    } else {
                        Log.w(TAG, "No recurrence rule found to delete: " + ruleId);
                        return false;
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error deleting recurrence rule: " + ruleId, e);
                return false;
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> isRecurrenceRuleInUse(@NonNull String ruleId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Checking if recurrence rule is in use: " + ruleId);

                // This would need to check UserScheduleAssignmentDao or other entities
                // that reference this recurrence rule. For now, implement basic logic.

                // Standard rules are always considered "in use"
                if (isStandardRule(ruleId)) {
                    return true;
                }

                // TODO: Implement actual usage check by querying UserScheduleAssignmentDao
                // For now, return false (not in use) for non-standard rules
                Log.d(TAG, "Recurrence rule usage check completed for: " + ruleId);
                return false;

            } catch (Exception e) {
                Log.e(TAG, "Error checking recurrence rule usage: " + ruleId, e);
                return false; // Assume not in use on error
            }
        }, mExecutorService);
    }

    // ==================== ADDITIONAL BUSINESS OPERATIONS ====================

    /**
     * Get standard QuattroDue recurrence rules.
     *
     * @return CompletableFuture with List of standard recurrence rules
     */
    @NonNull
    public CompletableFuture<List<RecurrenceRule>> getStandardRecurrenceRules() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting standard recurrence rules");

                if (!mStandardRulesCacheInitialized) {
                    initializeStandardRulesCache();
                }

                List<RecurrenceRule> standardRules = new ArrayList<>(mStandardRulesCache.values());
                Log.d(TAG, "Retrieved " + standardRules.size() + " standard recurrence rules");
                return standardRules;

            } catch (Exception e) {
                Log.e(TAG, "Error getting standard recurrence rules", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    /**
     * Get QuattroDue cycle recurrence rules specifically.
     *
     * @return CompletableFuture with List of QuattroDue cycle rules
     */
    @NonNull
    public CompletableFuture<List<RecurrenceRule>> getQuattroDueRecurrenceRules() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting QuattroDue recurrence rules");

                List<RecurrenceRuleEntity> entities = mRecurrenceRuleDao.getQuattroDueRecurrenceRules();
                List<RecurrenceRule> domainRules = entities.stream()
                        .map(RecurrenceRuleEntity::toDomainModel)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                Log.d(TAG, "Retrieved " + domainRules.size() + " QuattroDue recurrence rules");
                return domainRules;

            } catch (Exception e) {
                Log.e(TAG, "Error getting QuattroDue recurrence rules", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    /**
     * Get recurrence rules active for a specific date.
     *
     * @param date Target date
     * @return CompletableFuture with List of active rules for the date
     */
    @NonNull
    public CompletableFuture<List<RecurrenceRule>> getActiveRecurrenceRulesForDate(@NonNull LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting active recurrence rules for date: " + date);

                String dateString = date.toString();
                List<RecurrenceRuleEntity> entities = mRecurrenceRuleDao.getActiveRecurrenceRulesForDate(dateString);
                List<RecurrenceRule> domainRules = entities.stream()
                        .map(RecurrenceRuleEntity::toDomainModel)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                Log.d(TAG, "Retrieved " + domainRules.size() + " active rules for date: " + date);
                return domainRules;

            } catch (Exception e) {
                Log.e(TAG, "Error getting active recurrence rules for date: " + date, e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    /**
     * Get recurrence rules statistics.
     *
     * @return CompletableFuture with RecurrenceRuleStatistics
     */
    @NonNull
    public CompletableFuture<RecurrenceRuleStatistics> getRecurrenceRuleStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting recurrence rule statistics");

                RecurrenceRuleDao.RecurrenceRuleStatistics dbStats = mRecurrenceRuleDao.getRecurrenceRuleStatistics();
                if (dbStats == null) {
                    return new RecurrenceRuleStatistics(0, 0, 0, 0, 0);
                }

                RecurrenceRuleStatistics statistics = new RecurrenceRuleStatistics(
                        dbStats.total_rules,
                        dbStats.active_rules,
                        dbStats.quattrodue_rules,
                        dbStats.weekly_rules,
                        dbStats.daily_rules
                );

                Log.d(TAG, "Calculated recurrence rule statistics successfully");
                return statistics;

            } catch (Exception e) {
                Log.e(TAG, "Error getting recurrence rule statistics", e);
                return new RecurrenceRuleStatistics(0, 0, 0, 0, 0);
            }
        }, mExecutorService);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Initialize standard recurrence rules if they don't exist in database.
     */
    private void initializeStandardRulesIfNeeded() {
        CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "Checking if standard recurrence rules need initialization");

                // Check if standard rules exist
                if (mRecurrenceRuleDao.getRecurrenceRuleById(STANDARD_QUATTRODUE_RULE_ID) == null) {
                    createStandardQuattroDueRule();
                }

                if (mRecurrenceRuleDao.getRecurrenceRuleById(STANDARD_WEEKDAYS_RULE_ID) == null) {
                    createStandardWeekdaysRule();
                }

                if (mRecurrenceRuleDao.getRecurrenceRuleById(STANDARD_DAILY_RULE_ID) == null) {
                    createStandardDailyRule();
                }

                Log.d(TAG, "Standard recurrence rules initialization completed");

            } catch (Exception e) {
                Log.e(TAG, "Error initializing standard recurrence rules", e);
            }
        }, mExecutorService);
    }

    /**
     * Initialize standard rules cache.
     */
    private void initializeStandardRulesCache() {
        try {
            Log.d(TAG, "Initializing standard recurrence rules cache");

            // Load all standard rules
            String[] standardRuleIds = {STANDARD_QUATTRODUE_RULE_ID, STANDARD_WEEKDAYS_RULE_ID, STANDARD_DAILY_RULE_ID};

            for (String ruleId : standardRuleIds) {
                RecurrenceRuleEntity entity = mRecurrenceRuleDao.getRecurrenceRuleById(ruleId);
                if (entity != null) {
                    RecurrenceRule rule = entity.toDomainModel();
                    mStandardRulesCache.put(ruleId, rule);
                }
            }

            mStandardRulesCacheInitialized = true;
            Log.d(TAG, "Standard recurrence rules cache initialized with " + mStandardRulesCache.size() + " rules");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing standard rules cache", e);
        }
    }

    /**
     * Create standard QuattroDue recurrence rule.
     */
    private void createStandardQuattroDueRule() {
        try {
            String quattroDueName = mLocaleManager.getRecurrenceRuleName(mContext, "QUATTRODUE_CYCLE");
            String quattroDueDesc = mLocaleManager.getRecurrenceRuleDescription(mContext, "QUATTRODUE_CYCLE");

            RecurrenceRule quattroDueRule = RecurrenceRule.builder()
                    .id(STANDARD_QUATTRODUE_RULE_ID)
                    .name(quattroDueName != null ? quattroDueName : "Ciclo QuattroDue Standard")
                    .description(quattroDueDesc != null ? quattroDueDesc : "Pattern QuattroDue 4-2: 4 giorni lavoro, 2 giorni riposo")
                    .frequency(RecurrenceRule.Frequency.QUATTRODUE_CYCLE)
                    .interval(1)
                    .startDate(LocalDate.now())
                    .endType(RecurrenceRule.EndType.NEVER)
                    .cycleLength(18)
                    .workDays(4)
                    .restDays(2)
                    .active(true)
                    .build();

            RecurrenceRuleEntity entity = RecurrenceRuleEntity.fromDomainModel(quattroDueRule);
            mRecurrenceRuleDao.insertRecurrenceRule(entity);
            Log.d(TAG, "Created standard QuattroDue recurrence rule");

        } catch (Exception e) {
            Log.e(TAG, "Error creating standard QuattroDue rule", e);
        }
    }

    /**
     * Create standard weekdays recurrence rule.
     */
    private void createStandardWeekdaysRule() {
        try {
            String weekdaysName = LocaleManager.getRecurrenceRuleName(mContext, "WEEKDAYS");
            String weekdaysDesc = LocaleManager.getRecurrenceRuleDescription(mContext, "WEEKDAYS");

            List<DayOfWeek> weekdays = List.of(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            );
            List<RecurrenceRule.WeekStart> weekdays2 = List.of(
                    RecurrenceRule.WeekStart.MONDAY,
                    RecurrenceRule.WeekStart.THURSDAY,
                    RecurrenceRule.WeekStart.WEDNESDAY,
                    RecurrenceRule.WeekStart.THURSDAY,
                    RecurrenceRule.WeekStart.FRIDAY
            );

                    RecurrenceRule weekdaysRule = RecurrenceRule.builder()
                    .id(STANDARD_WEEKDAYS_RULE_ID)
                    .name(weekdaysName != null ? weekdaysName : "Solo Giorni Feriali")
                    .description(weekdaysDesc != null ? weekdaysDesc : "Dal lunedì al venerdì, esclusi weekend")
                    .frequency(RecurrenceRule.Frequency.WEEKLY)
                    .interval(1)
                    .startDate(LocalDate.now())
                    .endType(RecurrenceRule.EndType.NEVER)
                    .byDay(weekdays)
                    .weekStart( RecurrenceRule.WeekStart.MONDAY)
                    .active(true)
                    .build();

            RecurrenceRuleEntity entity = RecurrenceRuleEntity.fromDomainModel(weekdaysRule);
            mRecurrenceRuleDao.insertRecurrenceRule(entity);
            Log.d(TAG, "Created standard weekdays recurrence rule");

        } catch (Exception e) {
            Log.e(TAG, "Error creating standard weekdays rule", e);
        }
    }

    /**
     * Create standard daily recurrence rule.
     */
    private void createStandardDailyRule() {
        try {
            RecurrenceRule dailyRule = RecurrenceRule.builder()
                    .id(STANDARD_DAILY_RULE_ID)
                    .name("Pattern Giornaliero")
                    .description("Ogni giorno senza eccezioni")
                    .frequency(RecurrenceRule.Frequency.DAILY)
                    .interval(1)
                    .startDate(LocalDate.now())
                    .endType(RecurrenceRule.EndType.NEVER)
                    .active(true)
                    .build();

            RecurrenceRuleEntity entity = RecurrenceRuleEntity.fromDomainModel(dailyRule);
            mRecurrenceRuleDao.insertRecurrenceRule(entity);
            Log.d(TAG, "Created standard daily recurrence rule");

        } catch (Exception e) {
            Log.e(TAG, "Error creating standard daily rule", e);
        }
    }

    /**
     * Check if recurrence rule ID is a standard rule.
     */
    private boolean isStandardRule(@NonNull String ruleId) {
        return STANDARD_QUATTRODUE_RULE_ID.equals(ruleId) ||
                STANDARD_WEEKDAYS_RULE_ID.equals(ruleId) ||
                STANDARD_DAILY_RULE_ID.equals(ruleId);
    }

    // ==================== INNER CLASSES ====================

    /**
     * Recurrence rule statistics data class.
     */
    public static class RecurrenceRuleStatistics {
        public final int totalRules;
        public final int activeRules;
        public final int quattroDueRules;
        public final int weeklyRules;
        public final int dailyRules;

        public RecurrenceRuleStatistics(int totalRules, int activeRules, int quattroDueRules,
                                        int weeklyRules, int dailyRules) {
            this.totalRules = totalRules;
            this.activeRules = activeRules;
            this.quattroDueRules = quattroDueRules;
            this.weeklyRules = weeklyRules;
            this.dailyRules = dailyRules;
        }

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