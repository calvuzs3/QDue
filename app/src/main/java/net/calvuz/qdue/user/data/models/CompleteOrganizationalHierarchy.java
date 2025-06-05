package net.calvuz.qdue.user.data.models;

import androidx.room.Embedded;
import androidx.room.Relation;

import net.calvuz.qdue.user.data.entities.Establishment;
import net.calvuz.qdue.user.data.entities.MacroDepartment;

import java.util.List;

/**
 * Complete organizational hierarchy model.
 * Used for full organizational structure display and management.
 */
public class CompleteOrganizationalHierarchy {

    @Embedded
    public Establishment establishment;

    @Relation(
            entity = MacroDepartment.class,
            parentColumn = "id",
            entityColumn = "establishment_id"
    )
    public List<MacroDepartmentWithSubDepartments> macroDepartments;

    // Constructors
    public CompleteOrganizationalHierarchy() {}

    public CompleteOrganizationalHierarchy(Establishment establishment,
                                           List<MacroDepartmentWithSubDepartments> macroDepartments) {
        this.establishment = establishment;
        this.macroDepartments = macroDepartments;
    }

    // Utility methods
    public int getTotalDepartmentCount() {
        if (macroDepartments == null) return 0;

        int total = macroDepartments.size();
        for (MacroDepartmentWithSubDepartments macro : macroDepartments) {
            total += macro.getSubDepartmentCount();
        }
        return total;
    }

    public int getMacroDepartmentCount() {
        return macroDepartments != null ? macroDepartments.size() : 0;
    }

    public int getSubDepartmentCount() {
        if (macroDepartments == null) return 0;

        int total = 0;
        for (MacroDepartmentWithSubDepartments macro : macroDepartments) {
            total += macro.getSubDepartmentCount();
        }
        return total;
    }

    public boolean hasComplexHierarchy() {
        return getSubDepartmentCount() > 0;
    }
}
