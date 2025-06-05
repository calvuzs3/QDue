package net.calvuz.qdue.user.data.models;

import androidx.room.Embedded;
import androidx.room.Relation;

import net.calvuz.qdue.user.data.entities.Establishment;
import net.calvuz.qdue.user.data.entities.MacroDepartment;
import net.calvuz.qdue.user.data.entities.SubDepartment;
import net.calvuz.qdue.user.data.entities.User;

import java.util.List;

/**
 * Composite model containing User with complete organizational hierarchy.
 * Used for detailed user profile display and management.
 */
public class UserWithOrganization {

    @Embedded
    public User user;

    @Relation(
            parentColumn = "establishment_id",
            entityColumn = "id"
    )
    public Establishment establishment;

    @Relation(
            parentColumn = "macro_department_id",
            entityColumn = "id"
    )
    public MacroDepartment macroDepartment;

    @Relation(
            parentColumn = "sub_department_id",
            entityColumn = "id"
    )
    public SubDepartment subDepartment;

    // Constructors
    public UserWithOrganization() {}

    public UserWithOrganization(User user, Establishment establishment,
                                MacroDepartment macroDepartment, SubDepartment subDepartment) {
        this.user = user;
        this.establishment = establishment;
        this.macroDepartment = macroDepartment;
        this.subDepartment = subDepartment;
    }

    // Utility methods
    public String getFullOrganizationalPath() {
        StringBuilder path = new StringBuilder();

        if (establishment != null) {
            path.append(establishment.getName());
        }

        if (macroDepartment != null) {
            if (path.length() > 0) path.append(" → ");
            path.append(macroDepartment.getName());
        }

        if (subDepartment != null) {
            if (path.length() > 0) path.append(" → ");
            path.append(subDepartment.getName());
        }

        if (user != null && user.getTeamName() != null && !user.getTeamName().trim().isEmpty()) {
            if (path.length() > 0) path.append(" → ");
            path.append(user.getTeamName());
        }

        return path.toString();
    }

    public boolean hasCompleteOrganization() {
        return establishment != null && macroDepartment != null;
    }

    public boolean hasSubDepartment() {
        return subDepartment != null;
    }

    public String getOrganizationalLevel() {
        if (subDepartment != null) return "Sub-Department";
        if (macroDepartment != null) return "Macro-Department";
        if (establishment != null) return "Establishment";
        return "None";
    }
}






