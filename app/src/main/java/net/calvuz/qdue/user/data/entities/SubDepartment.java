package net.calvuz.qdue.user.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDate;

/**
 * Entity representing a sub-department (optional level in hierarchy).
 * Can be null/absent for flatter organizational structures.
 */
@Entity(tableName = "sub_departments",
        foreignKeys = @ForeignKey(
                entity = MacroDepartment.class,
                parentColumns = "id",
                childColumns = "macro_department_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = "macro_department_id"),
                @Index(value = {"macro_department_id", "name"}, unique = true)
        })
public class SubDepartment {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @ColumnInfo(name = "macro_department_id")
    private long macroDepartmentId;

    @ColumnInfo(name = "name", collate = ColumnInfo.NOCASE)
    private String name;

    @ColumnInfo(name = "code")
    private String code;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "supervisor_name")
    private String supervisorName;

    @ColumnInfo(name = "created_at")
    private LocalDate createdAt;

    @ColumnInfo(name = "updated_at")
    private LocalDate updatedAt;

    // Constructors
    public SubDepartment() {
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
    }

    public SubDepartment(long macroDepartmentId, String name, String code) {
        this();
        this.macroDepartmentId = macroDepartmentId;
        this.name = name;
        this.code = code;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMacroDepartmentId() { return macroDepartmentId; }
    public void setMacroDepartmentId(long macroDepartmentId) { this.macroDepartmentId = macroDepartmentId; }

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

    public String getSupervisorName() { return supervisorName; }
    public void setSupervisorName(String supervisorName) {
        this.supervisorName = supervisorName;
        this.updatedAt = LocalDate.now();
    }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public LocalDate getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDate updatedAt) { this.updatedAt = updatedAt; }
}
