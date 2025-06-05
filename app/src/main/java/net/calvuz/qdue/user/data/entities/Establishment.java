package net.calvuz.qdue.user.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDate;

/**
 * Entity representing a company/establishment where users work.
 * Supports variable organizational structures.
 */
@Entity(tableName = "establishments",
        indices = {@Index(value = "name", unique = true)})
public class Establishment {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @ColumnInfo(name = "name", collate = ColumnInfo.NOCASE)
    private String name;

    @ColumnInfo(name = "code")
    private String code; // Company code/identifier

    @ColumnInfo(name = "address")
    private String address;

    @ColumnInfo(name = "phone")
    private String phone;

    @ColumnInfo(name = "email")
    private String email;

    @ColumnInfo(name = "created_at")
    private LocalDate createdAt;

    @ColumnInfo(name = "updated_at")
    private LocalDate updatedAt;

    // Constructors
    public Establishment() {
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
    }

    public Establishment(String name, String code) {
        this();
        this.name = name;
        this.code = code;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

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

    public String getAddress() { return address; }
    public void setAddress(String address) {
        this.address = address;
        this.updatedAt = LocalDate.now();
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) {
        this.phone = phone;
        this.updatedAt = LocalDate.now();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        this.email = email;
        this.updatedAt = LocalDate.now();
    }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public LocalDate getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDate updatedAt) { this.updatedAt = updatedAt; }
}
