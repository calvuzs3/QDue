package net.calvuz.qdue.core.backup.models;

import net.calvuz.qdue.user.data.entities.Establishment;
import net.calvuz.qdue.user.data.entities.MacroDepartment;
import net.calvuz.qdue.user.data.entities.SubDepartment;

import java.util.List;
import java.util.Map;

/**
 * STEP 1: Core Backup System Models
 *
 * Provides data structures for the unified backup system that handles
 * all entities in the QDue application, extending beyond just events.
 */

// ==================== SPECIALIZED ENTITY BACKUPS ====================

/**
 * Organization-specific backup with hierarchical data
 */
public class OrganizationBackupPackage {
    public String version;
    public String timestamp;

    public List<Establishment> establishments;
    public List<MacroDepartment> macroDepartments;
    public List<SubDepartment> subDepartments;

    public OrganizationBackupMetadata organizationMetadata;

    public OrganizationBackupPackage(List<Establishment> establishments,
                                     List<MacroDepartment> macroDepartments,
                                     List<SubDepartment> subDepartments) {
        this.version = "1.0";
        this.timestamp = java.time.LocalDateTime.now().toString();
        this.establishments = establishments;
        this.macroDepartments = macroDepartments;
        this.subDepartments = subDepartments;
        this.organizationMetadata = new OrganizationBackupMetadata(
                establishments, macroDepartments, subDepartments);
    }

    public static class OrganizationBackupMetadata {
        public int totalEstablishments;
        public int totalMacroDepartments;
        public int totalSubDepartments;
        public Map<String, Integer> departmentsByEstablishment;
        public Map<String, Integer> subDepartmentsByMacroDepartment;

        public OrganizationBackupMetadata(List<Establishment> establishments,
                                          List<MacroDepartment> macroDepartments,
                                          List<SubDepartment> subDepartments) {
            this.totalEstablishments = establishments != null ? establishments.size() : 0;
            this.totalMacroDepartments = macroDepartments != null ? macroDepartments.size() : 0;
            this.totalSubDepartments = subDepartments != null ? subDepartments.size() : 0;

            this.departmentsByEstablishment = new java.util.HashMap<>();
            this.subDepartmentsByMacroDepartment = new java.util.HashMap<>();

            // Count departments by establishment
            if (macroDepartments != null) {
                for (MacroDepartment dept : macroDepartments) {
                    String estId = String.valueOf(dept.getEstablishmentId());
                    departmentsByEstablishment.put(estId,
                            departmentsByEstablishment.getOrDefault(estId, 0) + 1);
                }
            }

            // Count sub-departments by macro department
            if (subDepartments != null) {
                for (SubDepartment subDept : subDepartments) {
                    String macroId = String.valueOf(subDept.getMacroDepartmentId());
                    subDepartmentsByMacroDepartment.put(macroId,
                            subDepartmentsByMacroDepartment.getOrDefault(macroId, 0) + 1);
                }
            }
        }
    }
}
