package net.calvuz.qdue.user.data.models;

import androidx.room.Embedded;
import androidx.room.Relation;

import net.calvuz.qdue.user.data.entities.MacroDepartment;
import net.calvuz.qdue.user.data.entities.SubDepartment;

import java.util.List;

/**
 * Composite model for MacroDepartment with its sub-departments.
 * Used for department hierarchy management.
 */
public class MacroDepartmentWithSubDepartments {

    @Embedded
    public MacroDepartment macroDepartment;

    @Relation(
            parentColumn = "id",
            entityColumn = "macro_department_id"
    )
    public List<SubDepartment> subDepartments;

    // Constructors
    public MacroDepartmentWithSubDepartments() {}

    public MacroDepartmentWithSubDepartments(MacroDepartment macroDepartment, List<SubDepartment> subDepartments) {
        this.macroDepartment = macroDepartment;
        this.subDepartments = subDepartments;
    }

    // Utility methods
    public int getSubDepartmentCount() {
        return subDepartments != null ? subDepartments.size() : 0;
    }

    public boolean hasSubDepartments() {
        return subDepartments != null && !subDepartments.isEmpty();
    }
}
