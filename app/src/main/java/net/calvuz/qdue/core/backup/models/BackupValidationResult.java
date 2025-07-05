package net.calvuz.qdue.core.backup.models;

import java.util.List;

/**
 * STEP 1: Core Backup System Models
 *
 * Provides data structures for the unified backup system that handles
 * all entities in the QDue application, extending beyond just events.
 */
// ==================== BACKUP VALIDATION ====================

/**
 * Backup validation result
 */
public class BackupValidationResult {
    public boolean isValid;
    public List<String> errors;
    public List<String> warnings;
    public BackupIntegrityCheck integrityCheck;

    public BackupValidationResult() {
        this.errors = new java.util.ArrayList<>();
        this.warnings = new java.util.ArrayList<>();
        this.integrityCheck = new BackupIntegrityCheck();
    }

    public void addError(String error) {
        this.errors.add(error);
        this.isValid = false;
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public static class BackupIntegrityCheck {
        public boolean checksumValid;
        public boolean structureValid;
        public boolean dataTypesValid;
        public boolean relationshipsValid;
        public String checksumValue;
        public long validationTimestamp;

        public BackupIntegrityCheck() {
            this.validationTimestamp = System.currentTimeMillis();
        }
    }
}

