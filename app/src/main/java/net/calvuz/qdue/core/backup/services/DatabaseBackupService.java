package net.calvuz.qdue.core.backup.services;

import android.content.Context;

import net.calvuz.qdue.core.backup.models.EntityBackupPackage;
import net.calvuz.qdue.core.backup.models.EventsBackupPackage;
import net.calvuz.qdue.core.backup.models.UsersBackupPackage;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.events.dao.EventDao;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.user.data.dao.EstablishmentDao;
import net.calvuz.qdue.user.data.dao.MacroDepartmentDao;
import net.calvuz.qdue.user.data.dao.SubDepartmentDao;
import net.calvuz.qdue.user.data.dao.UserDao;
import net.calvuz.qdue.user.data.entities.Establishment;
import net.calvuz.qdue.user.data.entities.MacroDepartment;
import net.calvuz.qdue.user.data.entities.SubDepartment;
import net.calvuz.qdue.user.data.entities.User;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * STEP 1: Database Backup Service
 *
 * Handles backup and restore operations for all database entities.
 * Provides unified interface for backing up Events, Users, and Organization data.
 *
 * This service replaces direct DAO access for backup operations and ensures
 * consistent handling of all database entities.
 */
public class DatabaseBackupService {

    private static final String TAG = "DatabaseBackupService";

    private final Context mContext;
    private final QDueDatabase mDatabase;

    // DAOs for all entities
    private final EventDao mEventDao;
    private final UserDao mUserDao;
    private final EstablishmentDao mEstablishmentDao;
    private final MacroDepartmentDao mMacroDepartmentDao;
    private final SubDepartmentDao mSubDepartmentDao;

    // ==================== CONSTRUCTOR ====================

    public DatabaseBackupService(Context context, QDueDatabase database) {
        this.mContext = context.getApplicationContext();
        this.mDatabase = database;

        // Initialize all DAOs
        this.mEventDao = mDatabase.eventDao();
        this.mUserDao = mDatabase.userDao();
        this.mEstablishmentDao = mDatabase.establishmentDao();
        this.mMacroDepartmentDao = mDatabase.macroDepartmentDao();
        this.mSubDepartmentDao = mDatabase.subDepartmentDao();

        Log.d(TAG, "DatabaseBackupService initialized");
    }

    // ==================== ENTITY BACKUP CREATION ====================

    /**
     * Create backup for specific entity type
     */
    public EntityBackupPackage createEntityBackup(String entityType) {
        Log.d(TAG, "Creating backup for entity: " + entityType);

        switch (entityType.toLowerCase()) {
            case "events":
                return createEventsBackup();
            case "users":
                return createUsersBackup();
            case "establishments":
                return createEstablishmentsBackup();
            case "macro_departments":
                return createMacroDepartmentsBackup();
            case "sub_departments":
                return createSubDepartmentsBackup();
            default:
                Log.w(TAG, "Unknown entity type: " + entityType);
                return createEmptyBackup(entityType);
        }
    }

    /**
     * Create events backup with calendar-specific metadata
     */
    private EntityBackupPackage createEventsBackup() {
        try {
            List<LocalEvent> events = mEventDao.getAllEvents();
            Log.d(TAG, "Creating backup for " + events.size() + " events");

            // Use specialized EventsBackupPackage for enhanced features
            return new EventsBackupPackage(events);

        } catch (Exception e) {
            Log.e(TAG, "Failed to create events backup", e);
            return createEmptyBackup("events");
        }
    }

    /**
     * Create users backup with organizational context
     */
    private EntityBackupPackage createUsersBackup() {
        try {
            List<User> users = mUserDao.getAllUsers();
            Log.d(TAG, "Creating backup for " + users.size() + " users");

            // Use specialized UsersBackupPackage for enhanced features
            return new UsersBackupPackage(users);

        } catch (Exception e) {
            Log.e(TAG, "Failed to create users backup", e);
            return createEmptyBackup("users");
        }
    }

    /**
     * Create establishments backup
     */
    private EntityBackupPackage createEstablishmentsBackup() {
        try {
            List<Establishment> establishments = mEstablishmentDao.getAllEstablishments();
            Log.d(TAG, "Creating backup for " + establishments.size() + " establishments");

            EntityBackupPackage backup = new EntityBackupPackage("establishments", "1.0", establishments);

            // Add establishment-specific metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("totalEstablishments", establishments.size());
            if (!establishments.isEmpty()) {
                // Find establishment with most departments
                Map<Long, Integer> departmentCounts = new HashMap<>();
                List<MacroDepartment> allDepartments = mMacroDepartmentDao.getAllMacroDepartments();
                for (MacroDepartment dept : allDepartments) {
                    departmentCounts.put(dept.getEstablishmentId(),
                            departmentCounts.getOrDefault(dept.getEstablishmentId(), 0) + 1);
                }
                metadata.put("departmentCounts", departmentCounts);
            }
            backup.entityMetadata = metadata;

            return backup;

        } catch (Exception e) {
            Log.e(TAG, "Failed to create establishments backup", e);
            return createEmptyBackup("establishments");
        }
    }

    /**
     * Create macro departments backup
     */
    private EntityBackupPackage createMacroDepartmentsBackup() {
        try {
            List<MacroDepartment> macroDepartments = mMacroDepartmentDao.getAllMacroDepartments();
            Log.d(TAG, "Creating backup for " + macroDepartments.size() + " macro departments");

            EntityBackupPackage backup = new EntityBackupPackage("macro_departments", "1.0", macroDepartments);

            // Add macro department-specific metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("totalMacroDepartments", macroDepartments.size());

            // Group by establishment
            Map<Long, Integer> byEstablishment = new HashMap<>();
            for (MacroDepartment dept : macroDepartments) {
                byEstablishment.put(dept.getEstablishmentId(),
                        byEstablishment.getOrDefault(dept.getEstablishmentId(), 0) + 1);
            }
            metadata.put("departmentsByEstablishment", byEstablishment);
            backup.entityMetadata = metadata;

            return backup;

        } catch (Exception e) {
            Log.e(TAG, "Failed to create macro departments backup", e);
            return createEmptyBackup("macro_departments");
        }
    }

    /**
     * Create sub departments backup
     */
    private EntityBackupPackage createSubDepartmentsBackup() {
        try {
            List<SubDepartment> subDepartments = mSubDepartmentDao.getAllSubDepartments();
            Log.d(TAG, "Creating backup for " + subDepartments.size() + " sub departments");

            EntityBackupPackage backup = new EntityBackupPackage("sub_departments", "1.0", subDepartments);

            // Add sub department-specific metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("totalSubDepartments", subDepartments.size());

            // Group by macro department
            Map<Long, Integer> byMacroDepartment = new HashMap<>();
            for (SubDepartment subDept : subDepartments) {
                byMacroDepartment.put(subDept.getMacroDepartmentId(),
                        byMacroDepartment.getOrDefault(subDept.getMacroDepartmentId(), 0) + 1);
            }
            metadata.put("subDepartmentsByMacroDepartment", byMacroDepartment);
            backup.entityMetadata = metadata;

            return backup;

        } catch (Exception e) {
            Log.e(TAG, "Failed to create sub departments backup", e);
            return createEmptyBackup("sub_departments");
        }
    }

    // ==================== ENTITY RESTORE OPERATIONS ====================

    /**
     * Restore entity backup to database
     */
    public int restoreEntityBackup(EntityBackupPackage backup, boolean replaceAll) {
        if (backup == null || backup.entities == null) {
            Log.w(TAG, "Cannot restore null backup");
            return 0;
        }

        Log.d(TAG, "Restoring " + backup.entityType + " backup (" + backup.entityCount + " entities)");

        switch (backup.entityType.toLowerCase()) {
            case "events":
                return restoreEventsBackup(backup, replaceAll);
            case "users":
                return restoreUsersBackup(backup, replaceAll);
            case "establishments":
                return restoreEstablishmentsBackup(backup, replaceAll);
            case "macro_departments":
                return restoreMacroDepartmentsBackup(backup, replaceAll);
            case "sub_departments":
                return restoreSubDepartmentsBackup(backup, replaceAll);
            default:
                Log.w(TAG, "Unknown entity type for restore: " + backup.entityType);
                return 0;
        }
    }

    /**
     * Restore events from backup
     */
    @SuppressWarnings("unchecked")
    private int restoreEventsBackup(EntityBackupPackage backup, boolean replaceAll) {
        try {
            // Clear existing events if replace mode
            if (replaceAll) {
                mEventDao.deleteAllLocalEvents();
                Log.d(TAG, "Cleared all existing events for replace mode");
            }

            // Handle specialized EventsBackupPackage
            List<LocalEvent> events;
            if (backup instanceof EventsBackupPackage) {
                events = ((EventsBackupPackage) backup).events;
            } else {
                // Generic entity backup - need to cast
                events = (List<LocalEvent>) backup.entities;
            }

            if (events == null || events.isEmpty()) {
                Log.w(TAG, "No events to restore");
                return 0;
            }

            int restored = 0;
            for (LocalEvent event : events) {
                try {
                    // Check for duplicates if not in replace mode
                    if (!replaceAll && mEventDao.getEventById(event.getId()) != null) {
                        Log.d(TAG, "Skipping duplicate event: " + event.getId());
                        continue;
                    }

                    // Set restore timestamp
                    event.setLastUpdated(LocalDateTime.now());

                    // Insert event
                    mEventDao.insertEvent(event);
                    restored++;

                } catch (Exception e) {
                    Log.e(TAG, "Failed to restore event: " + event.getId(), e);
                }
            }

            Log.d(TAG, "Restored " + restored + " events");
            return restored;

        } catch (Exception e) {
            Log.e(TAG, "Failed to restore events backup", e);
            return 0;
        }
    }

    /**
     * Restore users from backup
     */
    @SuppressWarnings("unchecked")
    private int restoreUsersBackup(EntityBackupPackage backup, boolean replaceAll) {
        try {
            // Clear existing users if replace mode
            if (replaceAll) {
                mUserDao.deleteAllUsers();
                Log.d(TAG, "Cleared all existing users for replace mode");
            }

            // Handle specialized UsersBackupPackage
            List<User> users;
            if (backup instanceof UsersBackupPackage) {
                users = ((UsersBackupPackage) backup).users;
            } else {
                // Generic entity backup - need to cast
                users = (List<User>) backup.entities;
            }

            if (users == null || users.isEmpty()) {
                Log.w(TAG, "No users to restore");
                return 0;
            }

            int restored = 0;
            for (User user : users) {
                try {
                    // Check for duplicates if not in replace mode
                    if (!replaceAll && mUserDao.getUserById(user.getId()) != null) {
                        Log.d(TAG, "Skipping duplicate user: " + user.getId());
                        continue;
                    }

                    // Insert user
                    mUserDao.insertUser(user);
                    restored++;

                } catch (Exception e) {
                    Log.e(TAG, "Failed to restore user: " + user.getId(), e);
                }
            }

            Log.d(TAG, "Restored " + restored + " users");
            return restored;

        } catch (Exception e) {
            Log.e(TAG, "Failed to restore users backup", e);
            return 0;
        }
    }

    /**
     * Restore establishments from backup
     */
    @SuppressWarnings("unchecked")
    private int restoreEstablishmentsBackup(EntityBackupPackage backup, boolean replaceAll) {
        try {
            // Clear existing establishments if replace mode
            if (replaceAll) {
                mEstablishmentDao.deleteAllEstablishments();
                Log.d(TAG, "Cleared all existing establishments for replace mode");
            }

            List<Establishment> establishments = (List<Establishment>) backup.entities;
            if (establishments == null || establishments.isEmpty()) {
                Log.w(TAG, "No establishments to restore");
                return 0;
            }

            int restored = 0;
            for (Establishment establishment : establishments) {
                try {
                    // Check for duplicates if not in replace mode
                    if (!replaceAll && mEstablishmentDao.getEstablishmentById(establishment.getId()) != null) {
                        Log.d(TAG, "Skipping duplicate establishment: " + establishment.getId());
                        continue;
                    }

                    // Insert establishment
                    mEstablishmentDao.insertEstablishment(establishment);
                    restored++;

                } catch (Exception e) {
                    Log.e(TAG, "Failed to restore establishment: " + establishment.getId(), e);
                }
            }

            Log.d(TAG, "Restored " + restored + " establishments");
            return restored;

        } catch (Exception e) {
            Log.e(TAG, "Failed to restore establishments backup", e);
            return 0;
        }
    }

    /**
     * Restore macro departments from backup
     */
    @SuppressWarnings("unchecked")
    private int restoreMacroDepartmentsBackup(EntityBackupPackage backup, boolean replaceAll) {
        try {
            // Clear existing macro departments if replace mode
            if (replaceAll) {
                mMacroDepartmentDao.deleteAllMacroDepartments();
                Log.d(TAG, "Cleared all existing macro departments for replace mode");
            }

            List<MacroDepartment> macroDepartments = (List<MacroDepartment>) backup.entities;
            if (macroDepartments == null || macroDepartments.isEmpty()) {
                Log.w(TAG, "No macro departments to restore");
                return 0;
            }

            int restored = 0;
            for (MacroDepartment macroDepartment : macroDepartments) {
                try {
                    // Check for duplicates if not in replace mode
                    if (!replaceAll && mMacroDepartmentDao.getMacroDepartmentById(macroDepartment.getId()) != null) {
                        Log.d(TAG, "Skipping duplicate macro department: " + macroDepartment.getId());
                        continue;
                    }

                    // Insert macro department
                    mMacroDepartmentDao.insertMacroDepartment(macroDepartment);
                    restored++;

                } catch (Exception e) {
                    Log.e(TAG, "Failed to restore macro department: " + macroDepartment.getId(), e);
                }
            }

            Log.d(TAG, "Restored " + restored + " macro departments");
            return restored;

        } catch (Exception e) {
            Log.e(TAG, "Failed to restore macro departments backup", e);
            return 0;
        }
    }

    /**
     * Restore sub departments from backup
     */
    @SuppressWarnings("unchecked")
    private int restoreSubDepartmentsBackup(EntityBackupPackage backup, boolean replaceAll) {
        try {
            // Clear existing sub departments if replace mode
            if (replaceAll) {
                mSubDepartmentDao.deleteAllSubDepartments();
                Log.d(TAG, "Cleared all existing sub departments for replace mode");
            }

            List<SubDepartment> subDepartments = (List<SubDepartment>) backup.entities;
            if (subDepartments == null || subDepartments.isEmpty()) {
                Log.w(TAG, "No sub departments to restore");
                return 0;
            }

            int restored = 0;
            for (SubDepartment subDepartment : subDepartments) {
                try {
                    // Check for duplicates if not in replace mode
                    if (!replaceAll && mSubDepartmentDao.getSubDepartmentById(subDepartment.getId()) != null) {
                        Log.d(TAG, "Skipping duplicate sub department: " + subDepartment.getId());
                        continue;
                    }

                    // Insert sub department
                    mSubDepartmentDao.insertSubDepartment(subDepartment);
                    restored++;

                } catch (Exception e) {
                    Log.e(TAG, "Failed to restore sub department: " + subDepartment.getId(), e);
                }
            }

            Log.d(TAG, "Restored " + restored + " sub departments");
            return restored;

        } catch (Exception e) {
            Log.e(TAG, "Failed to restore sub departments backup", e);
            return 0;
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get entity count for specific type
     */
    public int getEntityCount(String entityType) {
        try {
            switch (entityType.toLowerCase()) {
                case "events":
                    return mEventDao.getEventsCount();
                case "users":
                    return mUserDao.getAllUsers().size();
                case "establishments":
                    return mEstablishmentDao.getEstablishmentCount();
                case "macro_departments":
                    return mMacroDepartmentDao.getAllMacroDepartments().size();
                case "sub_departments":
                    return mSubDepartmentDao.getAllSubDepartments().size();
                default:
                    return 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get entity count for " + entityType, e);
            return 0;
        }
    }

    /**
     * Check if entity type exists in database
     */
    public boolean hasEntities(String entityType) {
        return getEntityCount(entityType) > 0;
    }

    /**
     * Get all supported entity types
     */
    public List<String> getSupportedEntityTypes() {
        List<String> types = new ArrayList<>();
        types.add("events");
        types.add("users");
        types.add("establishments");
        types.add("macro_departments");
        types.add("sub_departments");
        return types;
    }

    /**
     * Validate entity backup before restore
     */
    public boolean validateEntityBackup(EntityBackupPackage backup) {
        if (backup == null) {
            Log.w(TAG, "Backup is null");
            return false;
        }

        if (backup.entityType == null || backup.entityType.trim().isEmpty()) {
            Log.w(TAG, "Backup entity type is null or empty");
            return false;
        }

        if (!getSupportedEntityTypes().contains(backup.entityType.toLowerCase())) {
            Log.w(TAG, "Unsupported entity type: " + backup.entityType);
            return false;
        }

        if (backup.entities == null) {
            Log.w(TAG, "Backup entities list is null");
            return false;
        }

        // Validate entity count matches
        if (backup.entityCount != backup.entities.size()) {
            Log.w(TAG, "Entity count mismatch: declared=" + backup.entityCount +
                    ", actual=" + backup.entities.size());
            return false;
        }

        Log.d(TAG, "Entity backup validation passed for " + backup.entityType);
        return true;
    }

    /**
     * Create empty backup for unsupported entity types
     */
    private EntityBackupPackage createEmptyBackup(String entityType) {
        Log.w(TAG, "Creating empty backup for " + entityType);
        return new EntityBackupPackage(entityType, "1.0", new ArrayList<>());
    }

    /**
     * Get database health information
     */
    public DatabaseHealthInfo getDatabaseHealth() {
        DatabaseHealthInfo health = new DatabaseHealthInfo();

        try {
            // Count all entities
            health.eventsCount = getEntityCount("events");
            health.usersCount = getEntityCount("users");
            health.establishmentsCount = getEntityCount("establishments");
            health.macroDepartmentsCount = getEntityCount("macro_departments");
            health.subDepartmentsCount = getEntityCount("sub_departments");

            // Calculate total
            health.totalEntities = health.eventsCount + health.usersCount +
                    health.establishmentsCount + health.macroDepartmentsCount +
                    health.subDepartmentsCount;

            // Check database accessibility
            health.databaseAccessible = true;
            health.lastCheckTime = System.currentTimeMillis();

            // Validate referential integrity
            health.referentialIntegrityValid = validateReferentialIntegrity();

            Log.d(TAG, "Database health check completed: " + health.totalEntities + " total entities");

        } catch (Exception e) {
            Log.e(TAG, "Database health check failed", e);
            health.databaseAccessible = false;
            health.errorMessage = e.getMessage();
        }

        return health;
    }

    /**
     * Validate referential integrity across entities
     */
    private boolean validateReferentialIntegrity() {
        try {
            // Check macro departments reference valid establishments
            List<MacroDepartment> macroDepartments = mMacroDepartmentDao.getAllMacroDepartments();
            List<Establishment> establishments = mEstablishmentDao.getAllEstablishments();

            Map<Long, Boolean> establishmentIds = new HashMap<>();
            for (Establishment est : establishments) {
                establishmentIds.put(est.getId(), true);
            }

            for (MacroDepartment macro : macroDepartments) {
                if (!establishmentIds.containsKey(macro.getEstablishmentId())) {
                    Log.w(TAG, "Referential integrity violation: MacroDepartment " +
                            macro.getId() + " references non-existent Establishment " +
                            macro.getEstablishmentId());
                    return false;
                }
            }

            // Check sub departments reference valid macro departments
            List<SubDepartment> subDepartments = mSubDepartmentDao.getAllSubDepartments();

            Map<Long, Boolean> macroDepartmentIds = new HashMap<>();
            for (MacroDepartment macro : macroDepartments) {
                macroDepartmentIds.put(macro.getId(), true);
            }

            for (SubDepartment sub : subDepartments) {
                if (!macroDepartmentIds.containsKey(sub.getMacroDepartmentId())) {
                    Log.w(TAG, "Referential integrity violation: SubDepartment " +
                            sub.getId() + " references non-existent MacroDepartment " +
                            sub.getMacroDepartmentId());
                    return false;
                }
            }

            Log.d(TAG, "Referential integrity validation passed");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Referential integrity validation failed", e);
            return false;
        }
    }

    // ==================== INNER CLASSES ====================

    /**
     * Database health information
     */
    public static class DatabaseHealthInfo {
        public boolean databaseAccessible;
        public boolean referentialIntegrityValid;
        public int totalEntities;
        public long lastCheckTime;
        public String errorMessage;

        // Entity counts
        public int eventsCount;
        public int usersCount;
        public int establishmentsCount;
        public int macroDepartmentsCount;
        public int subDepartmentsCount;

        public boolean isHealthy() {
            return databaseAccessible && referentialIntegrityValid && errorMessage == null;
        }

        public String getHealthSummary() {
            if (isHealthy()) {
                return "Database is healthy with " + totalEntities + " total entities";
            } else {
                return "Database issues detected: " + (errorMessage != null ? errorMessage : "Unknown error");
            }
        }
    }
}