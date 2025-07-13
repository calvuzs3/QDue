package net.calvuz.qdue.smartshifts.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing team contacts for shift swapping
 * Allows users to store contacts of team members for coordination
 */
@Entity(
        tableName = "team_contacts",
        indices = {
                @Index(value = {"user_id", "is_active"}),
                @Index(value = {"shift_pattern_id"}),
                @Index(value = {"android_contact_id"})
        }
)
public class TeamContact {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;                    // UUID generated

    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;                // owner of the contact

    @NonNull
    @ColumnInfo(name = "contact_name")
    public String contactName;

    @Nullable
    @ColumnInfo(name = "phone_number")
    public String phoneNumber;

    @Nullable
    @ColumnInfo(name = "email")
    public String email;

    @Nullable
    @ColumnInfo(name = "shift_pattern_id")
    public String shiftPatternId;        // shared pattern (optional)

    @Nullable
    @ColumnInfo(name = "notes")
    public String notes;                 // notes for shift swapping

    @Nullable
    @ColumnInfo(name = "android_contact_id")
    public String androidContactId;      // link to phone contact

    @ColumnInfo(name = "is_active")
    public boolean isActive;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    // Default constructor for Room
    public TeamContact() {}

    // Constructor for creating contacts
    public TeamContact(@NonNull String id, @NonNull String userId,
                       @NonNull String contactName, @Nullable String phoneNumber,
                       @Nullable String email, @Nullable String notes) {
        this.id = id;
        this.userId = userId;
        this.contactName = contactName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.notes = notes;
        this.isActive = true;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Check if contact is linked to phone contacts
     */
    public boolean isLinkedToAndroidContact() {
        return androidContactId != null && !androidContactId.isEmpty();
    }

    /**
     * Check if contact has phone number
     */
    public boolean hasPhoneNumber() {
        return phoneNumber != null && !phoneNumber.trim().isEmpty();
    }

    /**
     * Check if contact has email
     */
    public boolean hasEmail() {
        return email != null && !email.trim().isEmpty();
    }

    /**
     * Get display name with additional info
     */
    public String getDisplayName() {
        if (hasPhoneNumber()) {
            return contactName + " (" + phoneNumber + ")";
        }
        return contactName;
    }

    @Override
    public String toString() {
        return "TeamContact{" +
                "id='" + id + '\'' +
                ", contactName='" + contactName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", email='" + email + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}