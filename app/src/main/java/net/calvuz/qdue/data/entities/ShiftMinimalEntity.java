package net.calvuz.qdue.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ShiftMinimalEntity - Minimal shift data for UI selections and dropdowns.
 *
 * <p>This class contains only the essential shift information needed
 * for UI components like dropdowns, spinners, and selection lists.
 * Used to optimize database queries when full ShiftEntity data is not needed.</p>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Implementation
 * @since Database Version 7
 */
public record ShiftMinimalEntity(@NonNull String id, @NonNull String name, @Nullable String description, int display_order) {

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for minimal shift data.
     *
     * @param id            Shift ID
     * @param name          Shift name
     * @param description   Shift description (optional)
     * @param display_order Display order
     */
    public ShiftMinimalEntity {
    }

    // ==================== GETTERS ====================

    // ==================== UTILITY METHODS ====================

    /**
     * Get display text for UI components.
     *
     * @return Formatted display text
     */
    @NonNull
    public String getDisplayText() {
        if (description != null && !description.trim().isEmpty()) {
            return name + " - " + description;
        }
        return name;
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ShiftMinimalEntity that = (ShiftMinimalEntity) obj;
        return id.equals( that.id );
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    @NonNull
    public String toString() {
        return "ShiftMinimalEntity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}