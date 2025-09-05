package net.calvuz.qdue.domain.common.enums;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;

/**
 * Common Status enum for assignments.
 */
public enum Status {
    ACTIVE( "active", "currently_active_assignment" ),
    PENDING( "pending", "future_assignment_not_yet_active" ),
    EXPIRED( "expired", "past_assignment_no_longer_active" ),
    SUSPENDED( "suspended", "temporarily_suspended" ),
    CANCELLED( "cancelled", "cancelled_assignment" );

    private final String displayNameKey;
    private final String descriptionKey;

    Status(String displayNameKey, String descriptionKey) {
        this.displayNameKey = displayNameKey;
        this.descriptionKey = descriptionKey;
    }

    public String getDisplayNameKey() {
        return displayNameKey;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public static Status computeStatus(
            @NonNull LocalDate startDate,
            @Nullable LocalDate endDate,
            boolean active
            )
    {
        // Basic date validation
        if (endDate != null && endDate.isBefore( startDate )) {
            throw new IllegalArgumentException( "End date cannot be before start date" );
        }

        // Administrative override: if not active, it's cancelled
        if (!active) {
            return Status.CANCELLED;
        }

        LocalDate today = LocalDate.now();

        // Date-based status computation
        // Other states must be set manually
        if (startDate.isAfter( today )) {
            return Status.PENDING;  // Future assignment
        } else if (endDate != null && endDate.isBefore( today )) {
            return Status.EXPIRED;  // Past assignment
        } else {
            return Status.ACTIVE;   // Current assignment
        }
    }
}
