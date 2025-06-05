package net.calvuz.qdue.user.data.models;

import androidx.room.Embedded;
import androidx.room.Relation;

import net.calvuz.qdue.user.data.entities.Establishment;
import net.calvuz.qdue.user.data.entities.MacroDepartment;

import java.util.List;

/**
 * Composite model for Establishment with all its departments.
 * Used for organizational structure management.
 */
public class EstablishmentWithDepartments {

    @Embedded
    public Establishment establishment;

    @Relation(
            parentColumn = "id",
            entityColumn = "establishment_id"
    )
    public List<MacroDepartment> macroDepartments;

    // Constructors
    public EstablishmentWithDepartments() {}

    public EstablishmentWithDepartments(Establishment establishment, List<MacroDepartment> macroDepartments) {
        this.establishment = establishment;
        this.macroDepartments = macroDepartments;
    }

    // Utility methods
    public int getDepartmentCount() {
        return macroDepartments != null ? macroDepartments.size() : 0;
    }

    public boolean hasDepartments() {
        return macroDepartments != null && !macroDepartments.isEmpty();
    }
}