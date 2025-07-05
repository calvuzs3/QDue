package net.calvuz.qdue.core.backup.models;

import java.util.List;
import java.util.Map;

/**
 * STEP 1: Core Backup System Models
 *
 * Provides data structures for the unified backup system that handles
 * all entities in the QDue application, extending beyond just events.
 */

// ==================== ENTITY BACKUP PACKAGE ====================

/**
 * Generic backup package for any database entity
 */
public class EntityBackupPackage {
    public String entityType;
    public String version;
    public String timestamp;
    public int entityCount;

    // Generic entity data (will be cast to appropriate type during restore)
    public List<?> entities;

    // Entity-specific metadata
    public Map<String, Object> entityMetadata;

    public EntityBackupPackage() {}

    public EntityBackupPackage(String entityType, String version, List<?> entities) {
        this.entityType = entityType;
        this.version = version;
        this.timestamp = java.time.LocalDateTime.now().toString();
        this.entities = entities;
        this.entityCount = entities != null ? entities.size() : 0;
    }
}
