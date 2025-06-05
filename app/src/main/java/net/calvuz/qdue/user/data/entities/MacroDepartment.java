package net.calvuz.qdue.user.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDate;

/**
 * Entity representing a macro department within an establishment.
 * Flexible structure to support varying organizational hierarchies.
 */
@Entity(tableName = "macro_departments",
        foreignKeys = @ForeignKey(
                entity = Establishment.class,
                parentColumns = "id",
                childColumns = "establishment_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = "establishment_id"),
                @Index(value = {"establishment_id", "name"}, unique = true)
        })
public class MacroDepartment {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @ColumnInfo(name = "establishment_id")
    private long establishmentId;

    @ColumnInfo(name = "name", collate = ColumnInfo.NOCASE)
    private String name;

    @ColumnInfo(name = "code")
    private String code;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "manager_name")
    private String managerName;

    @ColumnInfo(name = "created_at")
    private LocalDate createdAt;

    @ColumnInfo(name = "updated_at")
    private LocalDate updatedAt;

    // Constructors
    public MacroDepartment() {
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
    }

    public MacroDepartment(long establishmentId, String name, String code) {
        this();
        this.establishmentId = establishmentId;
        this.name = name;
        this.code = code;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getEstablishmentId() { return establishmentId; }
    public void setEstablishmentId(long establishmentId) { this.establishmentId = establishmentId; }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        this.updatedAt = LocalDate.now();
    }

    public String getCode() { return code; }
    public void setCode(String code) {
        this.code = code;
        this.updatedAt = LocalDate.now();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDate.now();
    }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) {
        this.managerName = managerName;
        this.updatedAt = LocalDate.now();
    }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public LocalDate getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDate updatedAt) { this.updatedAt = updatedAt; }
}
