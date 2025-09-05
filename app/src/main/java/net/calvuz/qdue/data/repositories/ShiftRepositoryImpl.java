package net.calvuz.qdue.data.repositories;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.core.db.CalendarDatabase;
import net.calvuz.qdue.data.dao.ShiftDao;
import net.calvuz.qdue.data.entities.ShiftEntity;
import net.calvuz.qdue.domain.calendar.models.Shift;
import net.calvuz.qdue.domain.calendar.repositories.ShiftRepository;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * ShiftRepositoryImpl - Clean Architecture Data Layer Implementation
 *
 * <p>Comprehensive repository implementation providing shift template management and operations
 * following clean architecture principles. Bridges between domain models and data persistence
 * while maintaining complete separation of concerns and dependency injection compliance.</p>
 *
 * <h3>Core Responsibilities:</h3>
 * <ul>
 *   <li><strong>Domain Model Conversion</strong>: Convert between ShiftEntity and Shift domain models</li>
 *   <li><strong>Data Persistence</strong>: CRUD operations via CalendarDatabase and ShiftDao</li>
 *   <li><strong>System Defaults</strong>: Manage pre-populated standard shift templates</li>
 *   <li><strong>Internationalization</strong>: Localized shift names and descriptions</li>
 *   <li><strong>Async Operations</strong>: CompletableFuture-based background processing</li>
 *   <li><strong>Error Handling</strong>: Comprehensive error management with OperationResult pattern</li>
 * </ul>
 *
 * <h3>Design Patterns:</h3>
 * <ul>
 *   <li><strong>Repository Pattern</strong>: Clean separation between domain and data</li>
 *   <li><strong>Dependency Injection</strong>: Constructor-based DI following project standards</li>
 *   <li><strong>Async Processing</strong>: Background thread execution with ExecutorService</li>
 *   <li><strong>Caching Strategy</strong>: In-memory cache for system default shifts</li>
 *   <li><strong>Factory Methods</strong>: Standard shift template creation</li>
 * </ul>
 *
 * <h3>System Integration:</h3>
 * <ul>
 *   <li><strong>CalendarDatabase</strong>: Room database integration for persistence</li>
 *   <li><strong>LocaleManager</strong>: Internationalization for shift names and descriptions</li>
 *   <li><strong>CoreBackupManager</strong>: Automatic backup integration for data changes</li>
 *   <li><strong>OperationResult</strong>: Consistent error handling across all operations</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Implementation
 * @since Database Version 7
 */
public class ShiftRepositoryImpl implements ShiftRepository {

    private static final String TAG = "ShiftRepositoryImpl";

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final CalendarDatabase mDatabase;
    private final ShiftDao mShiftDao;
    private final CoreBackupManager mBackupManager;
    private final LocaleManager mLocaleManager;
    private final ExecutorService mExecutorService;

    // ==================== CACHING ====================

    private final Map<String, Shift> mSystemShiftsCache = new ConcurrentHashMap<>();
    private volatile boolean mSystemShiftsCacheInitialized = false;

    // ==================== CONSTANTS ====================

    private static final String SYSTEM_MORNING_SHIFT_ID = "shift_morning";
    private static final String SYSTEM_AFTERNOON_SHIFT_ID = "shift_afternoon";
    private static final String SYSTEM_NIGHT_SHIFT_ID = "shift_night";
    private static final String SYSTEM_QUATTRODUE_SHIFT_ID = "shift_quattrodue";

    // ==================== CONSTRUCTOR FOR DEPENDENCY INJECTION ====================

    /**
     * Constructor for dependency injection following clean architecture principles.
     *
     * @param context Application context for resource access
     * @param database CalendarDatabase instance for data operations
     * @param backupManager CoreBackupManager for automatic backup integration
     */
    public ShiftRepositoryImpl(@NonNull Context context,
                               @NonNull CalendarDatabase database,
                               @NonNull CoreBackupManager backupManager) {
        this.mContext = context.getApplicationContext();
        this.mDatabase = database;
        this.mShiftDao = database.shiftDao();
        this.mBackupManager = backupManager;
        this.mLocaleManager = new LocaleManager(mContext);
        this.mExecutorService = Executors.newFixedThreadPool(3);

        Log.d(TAG, "ShiftRepositoryImpl initialized via dependency injection");
    }

    // ==================== SHIFT CRUD OPERATIONS ====================

    @NonNull
    @Override
    public CompletableFuture<Shift> getShiftById(@NonNull String shiftId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting shift by ID: " + shiftId);

                // Check system cache first for performance
                if (mSystemShiftsCache.containsKey(shiftId)) {
                    Log.d(TAG, "Returning cached system shift: " + shiftId);
                    return mSystemShiftsCache.get(shiftId);
                }

                // Query database
                ShiftEntity entity = mShiftDao.getShiftById(shiftId);
                if (entity == null) {
                    Log.w(TAG, "Shift not found with ID: " + shiftId);
                    return null;
                }

                Shift domainShift = convertToDomainModel(entity);
                Log.d(TAG, "Successfully retrieved shift: " + shiftId);
                return domainShift;

            } catch (Exception e) {
                Log.e(TAG, "Error getting shift by ID: " + shiftId, e);
                return null;
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Shift> getShiftByName(@NonNull String shiftName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting shift by name: " + shiftName);

                ShiftEntity entity = mShiftDao.getActiveShiftByName(shiftName);
                if (entity == null) {
                    Log.w(TAG, "Shift not found with name: " + shiftName);
                    return null;
                }

                Shift domainShift = convertToDomainModel(entity);
                Log.d(TAG, "Successfully retrieved shift by name: " + shiftName);
                return domainShift;

            } catch (Exception e) {
                Log.e(TAG, "Error getting shift by name: " + shiftName, e);
                return null;
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Shift>> getAllShifts() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting all shifts");

                List<ShiftEntity> entities = mShiftDao.getActiveShifts();
                List<Shift> domainShifts = entities.stream()
                        .map(this::convertToDomainModel)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                Log.d(TAG, "Successfully retrieved " + domainShifts.size() + " shifts");
                return domainShifts;

            } catch (Exception e) {
                Log.e(TAG, "Error getting all shifts", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Shift>> getShiftsByType(@NonNull Shift.ShiftType shiftType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting shifts by type: " + shiftType);

                String entityShiftType = convertToEntityShiftType(shiftType);
                List<ShiftEntity> entities = mShiftDao.getShiftsByType(entityShiftType);
                List<Shift> domainShifts = entities.stream()
                        .map(this::convertToDomainModel)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                Log.d(TAG, "Successfully retrieved " + domainShifts.size() + " shifts of type: " + shiftType);
                return domainShifts;

            } catch (Exception e) {
                Log.e(TAG, "Error getting shifts by type: " + shiftType, e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Shift> saveShift(@NonNull Shift shift) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Saving shift: " + shift.getDisplayName());

                // Convert to entity
                ShiftEntity entity = convertToEntity(shift);
                if (entity == null) {
                    throw new IllegalArgumentException("Cannot convert shift to entity");
                }

                // Save to database
                boolean isUpdate = mShiftDao.shiftExistsById(entity.getId());
                if (isUpdate) {
                    mShiftDao.updateShift(entity);
                    Log.d(TAG, "Updated existing shift: " + shift.getId());
                } else {
                    mShiftDao.insertShift(entity);
                    Log.d(TAG, "Inserted new shift: " + shift.getId());
                }

                // Trigger auto backup
                mBackupManager.performAutoBackup("shifts", isUpdate ? "update" : "create");

                // Clear cache if system shift updated
                if (isSystemShift(shift.getId())) {
                    mSystemShiftsCache.remove(shift.getId());
                }

                Shift savedShift = convertToDomainModel(entity);
                Log.d(TAG, "Successfully saved shift: " + shift.getDisplayName());
                return savedShift;

            } catch (Exception e) {
                Log.e(TAG, "Error saving shift: " + shift.getDisplayName(), e);
                throw new RuntimeException("Failed to save shift", e);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> deleteShift(@NonNull String shiftId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Deleting shift: " + shiftId);

                // Prevent deletion of system shifts
                if (isSystemShift(shiftId)) {
                    Log.w(TAG, "Cannot delete system shift: " + shiftId);
                    return false;
                }

                // Soft delete by marking inactive
                long timestamp = System.currentTimeMillis();
                int affectedRows = mShiftDao.markShiftAsInactive(shiftId, timestamp);

                if (affectedRows > 0) {
                    mBackupManager.performAutoBackup("shifts", "delete");
                    Log.d(TAG, "Successfully deleted shift: " + shiftId);
                    return true;
                } else {
                    Log.w(TAG, "No shift found to delete: " + shiftId);
                    return false;
                }

            } catch (Exception e) {
                Log.e(TAG, "Error deleting shift: " + shiftId, e);
                return false;
            }
        }, mExecutorService);
    }

    // ==================== PREDEFINED SHIFTS ====================

    @NonNull
    @Override
    public CompletableFuture<List<Shift>> getSystemDefaultShifts() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting system default shifts");

                if (!mSystemShiftsCacheInitialized) {
                    initializeSystemShiftsCache();
                }

                List<Shift> systemShifts = new ArrayList<>(mSystemShiftsCache.values());
                Log.d(TAG, "Retrieved " + systemShifts.size() + " system default shifts");
                return systemShifts;

            } catch (Exception e) {
                Log.e(TAG, "Error getting system default shifts", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Shift> getMorningShift() {
        return getSystemShift(SYSTEM_MORNING_SHIFT_ID, "Morning Shift", "06:00", "14:00", "#4CAF50");
    }

    @NonNull
    @Override
    public CompletableFuture<Shift> getAfternoonShift() {
        return getSystemShift(SYSTEM_AFTERNOON_SHIFT_ID, "Afternoon Shift", "14:00", "22:00", "#FF9800");
    }

    @NonNull
    @Override
    public CompletableFuture<Shift> getNightShift() {
        return getSystemShift(SYSTEM_NIGHT_SHIFT_ID, "Night Shift", "22:00", "06:00", "#3F51B5");
    }

    @NonNull
    @Override
    public CompletableFuture<Shift> getQuattroDueCycleShift() {
        return getSystemShift(SYSTEM_QUATTRODUE_SHIFT_ID, "QuattroDue Cycle", "06:00", "18:00", "#9C27B0");
    }

    // ==================== SHIFT DISCOVERY AND FILTERING ====================

    @NonNull
    @Override
    public CompletableFuture<List<Shift>> getShiftsByTimeRange(@NonNull LocalTime startTime, @NonNull LocalTime endTime) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting shifts by time range: " + startTime + " - " + endTime);

                String startTimeStr = startTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                List<ShiftEntity> entities = mShiftDao.getShiftsByStartTime(startTimeStr);

                // Filter by end time as well
                List<Shift> filteredShifts = entities.stream()
                        .map(this::convertToDomainModel)
                        .filter(java.util.Objects::nonNull)
                        .filter(shift -> {
                            LocalTime shiftEndTime = shift.getEndTime();
                            return shiftEndTime.equals(endTime) ||
                                    (shiftEndTime.isAfter(startTime) && shiftEndTime.isBefore(endTime.plusHours(1)));
                        })
                        .collect(Collectors.toList());

                Log.d(TAG, "Found " + filteredShifts.size() + " shifts in time range");
                return filteredShifts;

            } catch (Exception e) {
                Log.e(TAG, "Error getting shifts by time range", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Shift>> getShiftsByDurationRange(@NonNull Duration minDuration, @NonNull Duration maxDuration) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting shifts by duration range: " + minDuration + " - " + maxDuration);

                long minMinutes = minDuration.toMinutes();
                long maxMinutes = maxDuration.toMinutes();

                List<ShiftEntity> entities = mShiftDao.getShiftsByDurationRange(minMinutes, maxMinutes);
                List<Shift> domainShifts = entities.stream()
                        .map(this::convertToDomainModel)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                Log.d(TAG, "Found " + domainShifts.size() + " shifts in duration range");
                return domainShifts;

            } catch (Exception e) {
                Log.e(TAG, "Error getting shifts by duration range", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Shift>> getMidnightCrossingShifts() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting midnight crossing shifts");

                List<ShiftEntity> entities = mShiftDao.getShiftsCrossingMidnight();
                List<Shift> domainShifts = entities.stream()
                        .map(this::convertToDomainModel)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                Log.d(TAG, "Found " + domainShifts.size() + " midnight crossing shifts");
                return domainShifts;

            } catch (Exception e) {
                Log.e(TAG, "Error getting midnight crossing shifts", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Shift>> getShiftsWithBreaks() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting shifts with breaks");

                List<ShiftEntity> entities = mShiftDao.getShiftsWithBreaks();
                List<Shift> domainShifts = entities.stream()
                        .map(this::convertToDomainModel)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                Log.d(TAG, "Found " + domainShifts.size() + " shifts with breaks");
                return domainShifts;

            } catch (Exception e) {
                Log.e(TAG, "Error getting shifts with breaks", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Shift>> getShiftsByColor(@NonNull String colorHex) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting shifts by color: " + colorHex);

                List<ShiftEntity> allEntities = mShiftDao.getActiveShifts();
                List<Shift> filteredShifts = allEntities.stream()
                        .filter(entity -> colorHex.equals(entity.getColorHex()))
                        .map(this::convertToDomainModel)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                Log.d(TAG, "Found " + filteredShifts.size() + " shifts with color: " + colorHex);
                return filteredShifts;

            } catch (Exception e) {
                Log.e(TAG, "Error getting shifts by color", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Shift>> searchShiftsByName(@NonNull String namePattern) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Searching shifts by name pattern: " + namePattern);

                // Convert pattern to SQL LIKE pattern
                String sqlPattern = namePattern.replace("*", "%").replace("?", "_");
                if (!sqlPattern.contains("%")) {
                    sqlPattern = "%" + sqlPattern + "%";
                }

                List<ShiftEntity> entities = mShiftDao.findShiftsByNamePattern(sqlPattern);
                List<Shift> domainShifts = entities.stream()
                        .map(this::convertToDomainModel)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                Log.d(TAG, "Found " + domainShifts.size() + " shifts matching pattern: " + namePattern);
                return domainShifts;

            } catch (Exception e) {
                Log.e(TAG, "Error searching shifts by name pattern", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    // ==================== SHIFT VALIDATION ====================

    @NonNull
    @Override
    public CompletableFuture<ValidationResult> validateShift(@NonNull Shift shift) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Validating shift: " + shift.getDisplayName());

                List<String> errors = new ArrayList<>();
                List<String> warnings = new ArrayList<>();

                // Validate basic properties
                if (shift.getName() == null || shift.getName().trim().isEmpty()) {
                    errors.add("Shift name cannot be empty");
                }

                if (shift.getStartTime() == null) {
                    errors.add("Start time cannot be null");
                }

                if (shift.getEndTime() == null) {
                    errors.add("End time cannot be null");
                }

                // Validate timing logic
                if (shift.getStartTime() != null && shift.getEndTime() != null) {
                    if (shift.getStartTime().equals(shift.getEndTime())) {
                        errors.add("Start time and end time cannot be the same");
                    }

                    Duration totalDuration = shift.getTotalDuration();
                    if (totalDuration.toMinutes() < 30) {
                        warnings.add("Shift duration is very short (less than 30 minutes)");
                    }
                    if (totalDuration.toMinutes() > 16 * 60) {
                        warnings.add("Shift duration is very long (more than 16 hours)");
                    }
                }

                // Validate break time
                if (shift.hasBreakTime() && shift.getBreakTimeDuration() != null) {
                    Duration breakDuration = shift.getBreakTimeDuration();
                    Duration totalDuration = shift.getTotalDuration();

                    if (breakDuration.toMinutes() >= totalDuration.toMinutes()) {
                        errors.add("Break time cannot be longer than total shift duration");
                    }
                }

                // Validate color
                if (shift.getColorHex() != null && !isValidColorHex(shift.getColorHex())) {
                    warnings.add("Invalid color format, should be #RRGGBB");
                }

                boolean isValid = errors.isEmpty();
                Log.d(TAG, "Shift validation completed - Valid: " + isValid + ", Errors: " + errors.size() + ", Warnings: " + warnings.size());

                return new ValidationResult(isValid, errors, warnings);

            } catch (Exception e) {
                Log.e(TAG, "Error validating shift", e);
                List<String> errors = new ArrayList<>();
                errors.add("Validation error: " + e.getMessage());
                return new ValidationResult(false, errors, new ArrayList<>());
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> shiftExists(@NonNull String shiftId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return mShiftDao.shiftExistsById(shiftId);
            } catch (Exception e) {
                Log.e(TAG, "Error checking if shift exists: " + shiftId, e);
                return false;
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> isShiftNameAvailable(@NonNull String shiftName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return !mShiftDao.activeShiftExistsByName(shiftName);
            } catch (Exception e) {
                Log.e(TAG, "Error checking if shift name is available: " + shiftName, e);
                return false;
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Shift>> findConflictingShifts(@NonNull Shift shift) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Finding conflicting shifts for: " + shift.getDisplayName());

                List<ShiftEntity> allEntities = mShiftDao.getActiveShifts();
                List<Shift> conflictingShifts = new ArrayList<>();

                for (ShiftEntity entity : allEntities) {
                    // Skip self
                    if (entity.getId().equals(shift.getId())) {
                        continue;
                    }

                    Shift otherShift = convertToDomainModel(entity);
                    if (otherShift == null) {
                        continue;
                    }

                    // Check for time conflicts
                    if (hasTimeConflict(shift, otherShift)) {
                        conflictingShifts.add(otherShift);
                    }
                }

                Log.d(TAG, "Found " + conflictingShifts.size() + " conflicting shifts");
                return conflictingShifts;

            } catch (Exception e) {
                Log.e(TAG, "Error finding conflicting shifts", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    // ==================== BUSINESS OPERATIONS ====================

    @NonNull
    @Override
    public CompletableFuture<List<Shift>> getReplacementShiftsFor(@NonNull String originalShiftId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting replacement shifts for: " + originalShiftId);

                // Get the original shift
                Shift originalShift = getShiftById(originalShiftId).get();
                if (originalShift == null) {
                    return new ArrayList<>();
                }

                // Get all shifts and filter suitable replacements
                List<ShiftEntity> allEntities = mShiftDao.getActiveShifts();
                List<Shift> replacementShifts = new ArrayList<>();

                for (ShiftEntity entity : allEntities) {
                    if (entity.getId().equals(originalShiftId)) {
                        continue; // Skip original
                    }

                    Shift candidate = convertToDomainModel(entity);
                    if (candidate != null && isSuitableReplacement(originalShift, candidate)) {
                        replacementShifts.add(candidate);
                    }
                }

                Log.d(TAG, "Found " + replacementShifts.size() + " replacement shifts");
                return replacementShifts;

            } catch (Exception e) {
                Log.e(TAG, "Error getting replacement shifts", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Shift>> getCompatibleShifts(@NonNull LocalTime requiredStartTime,
                                                              @NonNull LocalTime requiredEndTime,
                                                              boolean allowTimeVariance) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting compatible shifts for time: " + requiredStartTime + " - " + requiredEndTime);

                List<ShiftEntity> allEntities = mShiftDao.getActiveShifts();
                List<Shift> compatibleShifts = new ArrayList<>();

                Duration variance = allowTimeVariance ? Duration.ofMinutes(30) : Duration.ZERO;

                for (ShiftEntity entity : allEntities) {
                    Shift shift = convertToDomainModel(entity);
                    if (shift == null) {
                        continue;
                    }

                    if (isTimeCompatible(shift, requiredStartTime, requiredEndTime, variance)) {
                        compatibleShifts.add(shift);
                    }
                }

                Log.d(TAG, "Found " + compatibleShifts.size() + " compatible shifts");
                return compatibleShifts;

            } catch (Exception e) {
                Log.e(TAG, "Error getting compatible shifts", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Shift> createShiftTemplate(@NonNull String name,
                                                        @NonNull LocalTime startTime,
                                                        @NonNull LocalTime endTime,
                                                        @Nullable Shift.ShiftType shiftType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Creating shift template: " + name);

                // Create temporary shift (not persisted)
                Shift.Builder builder = Shift.builder(name)
                        .setStartTime(startTime)
                        .setEndTime(endTime)
                        .setColorHex("#2196F3") // Default blue
                        .setUserRelevant(true);

                if (shiftType != null) {
                    builder.setShiftType(shiftType);
                }

                Shift template = builder.build();
                Log.d(TAG, "Created shift template: " + name);
                return template;

            } catch (Exception e) {
                Log.e(TAG, "Error creating shift template", e);
                throw new RuntimeException("Failed to create shift template", e);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Shift> duplicateShift(@NonNull String sourceShiftId,
                                                   @NonNull String newName,
                                                   @Nullable ShiftModifications modifications) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Duplicating shift: " + sourceShiftId + " as " + newName);

                // Get source shift
                Shift sourceShift = getShiftById(sourceShiftId).get();
                if (sourceShift == null) {
                    throw new IllegalArgumentException("Source shift not found: " + sourceShiftId);
                }

                // Create builder from source
                Shift.Builder builder = Shift.builder(newName)
                        .setDescription(sourceShift.getDescription())
                        .setStartTime(sourceShift.getStartTime())
                        .setEndTime(sourceShift.getEndTime())
                        .setColorHex(sourceShift.getColorHex())
                        .setUserRelevant(sourceShift.isUserRelevant())
                        .setHasBreakTime(sourceShift.hasBreakTime())
                        .setBreakTimeIncluded(sourceShift.isBreakTimeIncluded())
                        .setShiftType(sourceShift.getShiftType());

                if (sourceShift.getBreakTimeDuration() != null) {
                    builder.setBreakTimeDuration(sourceShift.getBreakTimeDuration());
                }

                // Apply modifications if provided
                if (modifications != null) {
                    if (modifications.newStartTime() != null) {
                        builder.setStartTime( modifications.newStartTime() );
                    }
                    if (modifications.newEndTime() != null) {
                        builder.setEndTime( modifications.newEndTime() );
                    }
                    if (modifications.newColorHex() != null) {
                        builder.setColorHex( modifications.newColorHex() );
                    }
                    if (modifications.newBreakDuration() != null) {
                        builder.setBreakTimeDuration( modifications.newBreakDuration() );
                    }
                    if (modifications.newShiftType() != null) {
                        builder.setShiftType( modifications.newShiftType() );
                    }
                }

                Shift duplicatedShift = builder.build();

                // Save the duplicated shift
                Shift savedShift = saveShift(duplicatedShift).get();
                Log.d(TAG, "Successfully duplicated shift: " + newName);
                return savedShift;

            } catch (Exception e) {
                Log.e(TAG, "Error duplicating shift", e);
                throw new RuntimeException("Failed to duplicate shift", e);
            }
        }, mExecutorService);
    }

    // ==================== STATISTICS AND REPORTING ====================

    @NonNull
    @Override
    public CompletableFuture<ShiftStatistics> getShiftStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting shift statistics");

                ShiftDao.ShiftStatistics dbStats = mShiftDao.getShiftStatistics();
                List<ShiftEntity> allEntities = mShiftDao.getActiveShifts();

                // Calculate additional statistics
                int midnightCrossingCount = 0;
                long totalDurationMinutes = 0;
                ShiftEntity longestShift = null;
                ShiftEntity shortestShift = null;
                long longestDuration = 0;
                long shortestDuration = Long.MAX_VALUE;

                for (ShiftEntity entity : allEntities) {
                    if (entity.isCrossesMidnight()) {
                        midnightCrossingCount++;
                    }

                    long duration = entity.getTotalDurationMinutes();
                    totalDurationMinutes += duration;

                    if (duration > longestDuration) {
                        longestDuration = duration;
                        longestShift = entity;
                    }

                    if (duration < shortestDuration) {
                        shortestDuration = duration;
                        shortestShift = entity;
                    }
                }

                long averageDuration = allEntities.isEmpty() ? 0 : totalDurationMinutes / allEntities.size();

                // TODO: Implement most used shift tracking
                // For now, use the first shift as placeholder
                ShiftEntity mostUsedEntity = allEntities.isEmpty() ? null : allEntities.get(0);

                ShiftStatistics statistics = new ShiftStatistics(
                        dbStats.total_shifts,
                        dbStats.total_shifts, // Assuming all are system shifts for now
                        dbStats.custom_shifts,
                        dbStats.shifts_with_breaks,
                        midnightCrossingCount,
                        averageDuration,
                        mostUsedEntity != null ? convertToDomainModel(mostUsedEntity) : null,
                        longestShift != null ? convertToDomainModel(longestShift) : null,
                        shortestShift != null ? convertToDomainModel(shortestShift) : null
                );

                Log.d(TAG, "Calculated shift statistics successfully");
                return statistics;

            } catch (Exception e) {
                Log.e(TAG, "Error getting shift statistics", e);
                // Return empty statistics on error
                return new ShiftStatistics(0, 0, 0, 0, 0, 0, null, null, null);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Shift>> getMostUsedShifts(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting most used shifts (limit: " + limit + ")");

                // TODO: Implement usage tracking
                // For now, return first N shifts ordered by name
                List<ShiftEntity> entities = mShiftDao.getActiveShifts();
                List<Shift> shifts = entities.stream()
                        .limit(limit)
                        .map(this::convertToDomainModel)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                Log.d(TAG, "Retrieved " + shifts.size() + " most used shifts");
                return shifts;

            } catch (Exception e) {
                Log.e(TAG, "Error getting most used shifts", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Shift>> getShiftsByUsageFrequency(boolean ascending) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting shifts by usage frequency (ascending: " + ascending + ")");

                // TODO: Implement usage frequency tracking
                // For now, return all shifts ordered by name
                List<ShiftEntity> entities = mShiftDao.getActiveShifts();
                List<Shift> shifts = entities.stream()
                        .map(this::convertToDomainModel)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                // Simple sort by name for now
                shifts.sort((a, b) -> ascending ?
                        a.getName().compareToIgnoreCase(b.getName()) :
                        b.getName().compareToIgnoreCase(a.getName()));

                Log.d(TAG, "Retrieved " + shifts.size() + " shifts by usage frequency");
                return shifts;

            } catch (Exception e) {
                Log.e(TAG, "Error getting shifts by usage frequency", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    // ==================== BULK OPERATIONS ====================

    @NonNull
    @Override
    public CompletableFuture<Integer> saveShifts(@NonNull List<Shift> shifts) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Saving " + shifts.size() + " shifts");

                int successCount = 0;
                List<ShiftEntity> entities = new ArrayList<>();

                // Convert all shifts to entities
                for (Shift shift : shifts) {
                    ShiftEntity entity = convertToEntity(shift);
                    if (entity != null) {
                        entities.add(entity);
                    }
                }

                // Batch insert/update
                List<Long> results = mShiftDao.insertShifts(entities);
                successCount = results.size();

                if (successCount > 0) {
                    mBackupManager.performAutoBackup("shifts", "bulk_create");
                }

                Log.d(TAG, "Successfully saved " + successCount + " out of " + shifts.size() + " shifts");
                return successCount;

            } catch (Exception e) {
                Log.e(TAG, "Error saving shifts in bulk", e);
                return 0;
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Integer> deleteShifts(@NonNull List<String> shiftIds) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Deleting " + shiftIds.size() + " shifts");

                int successCount = 0;
                long timestamp = System.currentTimeMillis();

                for (String shiftId : shiftIds) {
                    // Skip system shifts
                    if (isSystemShift(shiftId)) {
                        Log.w(TAG, "Skipping system shift: " + shiftId);
                        continue;
                    }

                    int affectedRows = mShiftDao.markShiftAsInactive(shiftId, timestamp);
                    if (affectedRows > 0) {
                        successCount++;
                    }
                }

                if (successCount > 0) {
                    mBackupManager.performAutoBackup("shifts", "bulk_delete");
                }

                Log.d(TAG, "Successfully deleted " + successCount + " out of " + shiftIds.size() + " shifts");
                return successCount;

            } catch (Exception e) {
                Log.e(TAG, "Error deleting shifts in bulk", e);
                return 0;
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<ImportResult> importShifts(@NonNull List<Shift> shifts, boolean overwriteExisting) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Importing " + shifts.size() + " shifts (overwrite: " + overwriteExisting + ")");

                int totalShifts = shifts.size();
                int successfulImports = 0;
                int skippedShifts = 0;
                int failedImports = 0;
                List<String> errors = new ArrayList<>();

                for (Shift shift : shifts) {
                    try {
                        boolean exists = mShiftDao.shiftExistsById(shift.getId());

                        if (exists && !overwriteExisting) {
                            skippedShifts++;
                            Log.d(TAG, "Skipped existing shift: " + shift.getId());
                            continue;
                        }

                        ShiftEntity entity = convertToEntity(shift);
                        if (entity != null) {
                            if (exists) {
                                mShiftDao.updateShift(entity);
                            } else {
                                mShiftDao.insertShift(entity);
                            }
                            successfulImports++;
                            Log.d(TAG, "Imported shift: " + shift.getId());
                        } else {
                            failedImports++;
                            errors.add("Failed to convert shift: " + shift.getId());
                        }

                    } catch (Exception e) {
                        failedImports++;
                        errors.add("Error importing shift " + shift.getId() + ": " + e.getMessage());
                        Log.e(TAG, "Error importing shift: " + shift.getId(), e);
                    }
                }

                if (successfulImports > 0) {
                    mBackupManager.performAutoBackup("shifts", "import");
                }

                ImportResult result = new ImportResult(totalShifts, successfulImports, skippedShifts, failedImports, errors);
                Log.d(TAG, "Import completed - Success: " + successfulImports + ", Skipped: " + skippedShifts + ", Failed: " + failedImports);
                return result;

            } catch (Exception e) {
                Log.e(TAG, "Error during shift import", e);
                List<String> errors = new ArrayList<>();
                errors.add("Import failed: " + e.getMessage());
                return new ImportResult(shifts.size(), 0, 0, shifts.size(), errors);
            }
        }, mExecutorService);
    }

    // ==================== DOMAIN MODEL CONVERSION ====================

    /**
     * Convert ShiftEntity to domain Shift model.
     */
    @Nullable
    private Shift convertToDomainModel(@NonNull ShiftEntity entity) {
        try {
            LocalTime startTime = entity.getStartTimeAsLocalTime();
            LocalTime endTime = entity.getEndTimeAsLocalTime();

            if (startTime == null || endTime == null) {
                Log.w(TAG, "Invalid time data in shift entity: " + entity.getId());
                return null;
            }

            Shift.Builder builder = Shift.builder(entity.getName())
                    .setId(entity.getId())
                    .setDescription(entity.getDescription())
                    .setStartTime(startTime)
                    .setEndTime(endTime)
                    .setColorHex(entity.getColorHex())
                    .setUserRelevant(entity.isUserRelevant())
                    .setHasBreakTime(entity.isHasBreakTime())
                    .setBreakTimeIncluded(entity.isBreakTimeIncluded());

            if (entity.isHasBreakTime()) {
                builder.setBreakTimeDuration(entity.getBreakTimeDuration());
            }

            // Convert entity shift type to domain shift type
            Shift.ShiftType domainShiftType = convertToDomainShiftType(entity.getShiftType());
            if (domainShiftType != null) {
                builder.setShiftType(domainShiftType);
            }

            return builder.build();

        } catch (Exception e) {
            Log.e(TAG, "Error converting entity to domain model: " + entity.getId(), e);
            return null;
        }
    }

    /**
     * Convert domain Shift to ShiftEntity.
     */
    @Nullable
    private ShiftEntity convertToEntity(@NonNull Shift shift) {
        try {
            ShiftEntity entity = new ShiftEntity();
            entity.setId(shift.getId());
            entity.setName(shift.getName());
            entity.setDescription(shift.getDescription());
            entity.setShiftType(convertToEntityShiftType(shift.getShiftType()));
            entity.setStartTime(shift.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            entity.setEndTime(shift.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            entity.setColorHex(shift.getColorHex());
            entity.setUserRelevant(shift.isUserRelevant());
            entity.setHasBreakTime(shift.hasBreakTime());
            entity.setBreakTimeIncluded(shift.isBreakTimeIncluded());

            if (shift.getBreakTimeDuration() != null) {
                entity.setBreakTimeDuration(shift.getBreakTimeDuration());
            }

            entity.setActive(true);
            entity.setUpdatedAt(System.currentTimeMillis());

            return entity;

        } catch (Exception e) {
            Log.e(TAG, "Error converting domain model to entity: " + shift.getId(), e);
            return null;
        }
    }

    /**
     * Convert entity shift type to domain shift type.
     */
    @Nullable
    private Shift.ShiftType convertToDomainShiftType(@NonNull String entityShiftType) {
        switch (entityShiftType.toUpperCase()) {
            case "CYCLE_42":
            case "QUATTRODUE":
                return Shift.ShiftType.CYCLE_42;
            case "DAILY":
                return Shift.ShiftType.DAILY;
            default:
                Log.w(TAG, "Unknown entity shift type: " + entityShiftType);
                return Shift.ShiftType.DAILY; // Default fallback
        }
    }

    /**
     * Convert domain shift type to entity shift type.
     */
    @NonNull
    private String convertToEntityShiftType(@NonNull Shift.ShiftType shiftType) {
        switch (shiftType) {
            case CYCLE_42:
                return "QUATTRODUE";
            case DAILY:
                return "DAILY";
            default:
                return "DAILY";
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Get system shift with localized name.
     */
    @NonNull
    private CompletableFuture<Shift> getSystemShift(@NonNull String shiftId,
                                                    @NonNull String defaultName,
                                                    @NonNull String startTime,
                                                    @NonNull String endTime,
                                                    @NonNull String colorHex) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check cache first
                if (mSystemShiftsCache.containsKey(shiftId)) {
                    return mSystemShiftsCache.get(shiftId);
                }

                // Try to get from database
                ShiftEntity entity = mShiftDao.getShiftById(shiftId);
                Shift shift;

                if (entity != null) {
                    shift = convertToDomainModel(entity);
                } else {
                    // Create default system shift
                    String localizedName = mLocaleManager.getShiftName(mContext, shiftId.replace("shift_", "").toUpperCase());
                    String localizedDesc = mLocaleManager.getShiftDescription(mContext, shiftId.replace("shift_", "").toUpperCase());

                    shift = Shift.builder(localizedName != null ? localizedName : defaultName)
                            .setId(shiftId)
                            .setDescription(localizedDesc)
                            .setStartTime(LocalTime.parse(startTime))
                            .setEndTime(LocalTime.parse(endTime))
                            .setColorHex(colorHex)
                            .setUserRelevant(true)
                            .setHasBreakTime(true)
                            .setBreakTimeDuration(Duration.ofMinutes(30))
                            .setShiftType(Shift.ShiftType.CYCLE_42)
                            .build();
                }

                // Cache the result
                if (shift != null) {
                    mSystemShiftsCache.put(shiftId, shift);
                }

                return shift;

            } catch (Exception e) {
                Log.e(TAG, "Error getting system shift: " + shiftId, e);
                return null;
            }
        }, mExecutorService);
    }

    /**
     * Initialize system shifts cache.
     */
    private void initializeSystemShiftsCache() {
        if (mSystemShiftsCacheInitialized) {
            return;
        }

        try {
            // Load all system shifts
            getMorningShift().get();
            getAfternoonShift().get();
            getNightShift().get();
            getQuattroDueCycleShift().get();

            mSystemShiftsCacheInitialized = true;
            Log.d(TAG, "System shifts cache initialized");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing system shifts cache", e);
        }
    }

    /**
     * Check if shift ID is a system shift.
     */
    private boolean isSystemShift(@NonNull String shiftId) {
        return SYSTEM_MORNING_SHIFT_ID.equals(shiftId) ||
                SYSTEM_AFTERNOON_SHIFT_ID.equals(shiftId) ||
                SYSTEM_NIGHT_SHIFT_ID.equals(shiftId) ||
                SYSTEM_QUATTRODUE_SHIFT_ID.equals(shiftId);
    }

    /**
     * Check for time conflicts between shifts.
     */
    private boolean hasTimeConflict(@NonNull Shift shift1, @NonNull Shift shift2) {
        LocalTime start1 = shift1.getStartTime();
        LocalTime end1 = shift1.getEndTime();
        LocalTime start2 = shift2.getStartTime();
        LocalTime end2 = shift2.getEndTime();

        // Simple overlap check (can be enhanced for midnight crossing)
        return (start1.isBefore(end2) && end1.isAfter(start2)) ||
                (start2.isBefore(end1) && end2.isAfter(start1));
    }

    /**
     * Check if a shift is suitable as replacement for another.
     */
    private boolean isSuitableReplacement(@NonNull Shift original, @NonNull Shift candidate) {
        // Similar duration (within 2 hours)
        Duration originalDuration = original.getTotalDuration();
        Duration candidateDuration = candidate.getTotalDuration();
        long durationDiff = Math.abs(originalDuration.toMinutes() - candidateDuration.toMinutes());

        return durationDiff <= 120; // 2 hours tolerance
    }

    /**
     * Check if shift is time compatible with requirements.
     */
    private boolean isTimeCompatible(@NonNull Shift shift,
                                     @NonNull LocalTime requiredStartTime,
                                     @NonNull LocalTime requiredEndTime,
                                     @NonNull Duration variance) {
        LocalTime shiftStart = shift.getStartTime();
        LocalTime shiftEnd = shift.getEndTime();

        LocalTime earliestStart = requiredStartTime.minus(variance);
        LocalTime latestStart = requiredStartTime.plus(variance);
        LocalTime earliestEnd = requiredEndTime.minus(variance);
        LocalTime latestEnd = requiredEndTime.plus(variance);

        return (shiftStart.equals(requiredStartTime) ||
                (shiftStart.isAfter(earliestStart) && shiftStart.isBefore(latestStart))) &&
                (shiftEnd.equals(requiredEndTime) ||
                        (shiftEnd.isAfter(earliestEnd) && shiftEnd.isBefore(latestEnd)));
    }

    /**
     * Validate color hex format.
     */
    private boolean isValidColorHex(@NonNull String colorHex) {
        return colorHex.matches("^#[0-9A-Fa-f]{6}$");
    }
}