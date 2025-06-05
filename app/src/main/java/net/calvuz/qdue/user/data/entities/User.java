package net.calvuz.qdue.user.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDate;

/**
 * Main User entity with support for Google authentication and comprehensive profile data.
 * Single user per device with 1:1 relationship to establishment.
 */
@Entity(tableName = "users",
        foreignKeys = {
                @ForeignKey(
                        entity = Establishment.class,
                        parentColumns = "id",
                        childColumns = "establishment_id",
                        onDelete = ForeignKey.SET_NULL
                ),
                @ForeignKey(
                        entity = MacroDepartment.class,
                        parentColumns = "id",
                        childColumns = "macro_department_id",
                        onDelete = ForeignKey.SET_NULL
                ),
                @ForeignKey(
                        entity = SubDepartment.class,
                        parentColumns = "id",
                        childColumns = "sub_department_id",
                        onDelete = ForeignKey.SET_NULL
                )
        },
        indices = {
                @Index(value = "email", unique = true),
                @Index(value = "employee_id", unique = true),
                @Index(value = "establishment_id"),
                @Index(value = "macro_department_id"),
                @Index(value = "sub_department_id")
        })
public class User {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    // Authentication fields
    @ColumnInfo(name = "google_id")
    private String googleId; // Google user ID for authentication

    @ColumnInfo(name = "auth_provider")
    private String authProvider; // "google", "manual", "future_oauth"

    // Personal information
    @ColumnInfo(name = "first_name")
    private String firstName;

    @ColumnInfo(name = "last_name")
    private String lastName;

    @ColumnInfo(name = "nickname")
    private String nickname;

    @ColumnInfo(name = "email")
    private String email;

    @ColumnInfo(name = "profile_image_url")
    private String profileImageUrl; // URL or local path

    @ColumnInfo(name = "profile_image_source")
    private String profileImageSource; // "google", "local", "none"

    // Professional information
    @ColumnInfo(name = "employee_id")
    private String employeeId; // Matricola

    @ColumnInfo(name = "job_title")
    private String jobTitle; // Ruolo/Posizione

    @ColumnInfo(name = "job_level")
    private String jobLevel; // Livello/Grado

    @ColumnInfo(name = "hire_date")
    private LocalDate hireDate; // Data assunzione

    @ColumnInfo(name = "phone_work")
    private String phoneWork;

    @ColumnInfo(name = "phone_personal")
    private String phonePersonal;

    // Organizational relationships
    @ColumnInfo(name = "establishment_id")
    private Long establishmentId; // Nullable for flexibility

    @ColumnInfo(name = "macro_department_id")
    private Long macroDepartmentId; // Nullable

    @ColumnInfo(name = "sub_department_id")
    private Long subDepartmentId; // Nullable - for flexible hierarchy

    @ColumnInfo(name = "team_name")
    private String teamName; // Optional team/squad identifier

    // Status and metadata
    @ColumnInfo(name = "is_active")
    private boolean isActive;

    @ColumnInfo(name = "last_login")
    private LocalDate lastLogin;

    @ColumnInfo(name = "profile_completed")
    private boolean profileCompleted; // Track if user finished setup

    @ColumnInfo(name = "created_at")
    private LocalDate createdAt;

    @ColumnInfo(name = "updated_at")
    private LocalDate updatedAt;

    // Constructors
    public User() {
        this.isActive = true;
        this.profileCompleted = false;
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
        this.authProvider = "manual";
    }

    public User(String firstName, String lastName, String email) {
        this();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    // Utility methods
    public String getFullName() {
        if (firstName == null && lastName == null) return null;
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }

    public String getDisplayName() {
        if (nickname != null && !nickname.trim().isEmpty()) {
            return nickname;
        }
        return getFullName();
    }

    public boolean hasGoogleAuth() {
        return "google".equals(authProvider) && googleId != null;
    }

    public boolean hasProfileImage() {
        return profileImageUrl != null && !profileImageUrl.trim().isEmpty();
    }

    public void updateLastLogin() {
        this.lastLogin = LocalDate.now();
        this.updatedAt = LocalDate.now();
    }

    public void markProfileCompleted() {
        this.profileCompleted = true;
        this.updatedAt = LocalDate.now();
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) {
        this.googleId = googleId;
        this.updatedAt = LocalDate.now();
    }

    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
        this.updatedAt = LocalDate.now();
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
        this.updatedAt = LocalDate.now();
    }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.updatedAt = LocalDate.now();
    }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) {
        this.nickname = nickname;
        this.updatedAt = LocalDate.now();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        this.email = email;
        this.updatedAt = LocalDate.now();
    }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
        this.updatedAt = LocalDate.now();
    }

    public String getProfileImageSource() { return profileImageSource; }
    public void setProfileImageSource(String profileImageSource) {
        this.profileImageSource = profileImageSource;
        this.updatedAt = LocalDate.now();
    }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
        this.updatedAt = LocalDate.now();
    }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
        this.updatedAt = LocalDate.now();
    }

    public String getJobLevel() { return jobLevel; }
    public void setJobLevel(String jobLevel) {
        this.jobLevel = jobLevel;
        this.updatedAt = LocalDate.now();
    }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
        this.updatedAt = LocalDate.now();
    }

    public String getPhoneWork() { return phoneWork; }
    public void setPhoneWork(String phoneWork) {
        this.phoneWork = phoneWork;
        this.updatedAt = LocalDate.now();
    }

    public String getPhonePersonal() { return phonePersonal; }
    public void setPhonePersonal(String phonePersonal) {
        this.phonePersonal = phonePersonal;
        this.updatedAt = LocalDate.now();
    }

    public Long getEstablishmentId() { return establishmentId; }
    public void setEstablishmentId(Long establishmentId) {
        this.establishmentId = establishmentId;
        this.updatedAt = LocalDate.now();
    }

    public Long getMacroDepartmentId() { return macroDepartmentId; }
    public void setMacroDepartmentId(Long macroDepartmentId) {
        this.macroDepartmentId = macroDepartmentId;
        this.updatedAt = LocalDate.now();
    }

    public Long getSubDepartmentId() { return subDepartmentId; }
    public void setSubDepartmentId(Long subDepartmentId) {
        this.subDepartmentId = subDepartmentId;
        this.updatedAt = LocalDate.now();
    }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) {
        this.teamName = teamName;
        this.updatedAt = LocalDate.now();
    }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) {
        isActive = active;
        this.updatedAt = LocalDate.now();
    }

    public LocalDate getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDate lastLogin) { this.lastLogin = lastLogin; }

    public boolean isProfileCompleted() { return profileCompleted; }
    public void setProfileCompleted(boolean profileCompleted) { this.profileCompleted = profileCompleted; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public LocalDate getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDate updatedAt) { this.updatedAt = updatedAt; }
}