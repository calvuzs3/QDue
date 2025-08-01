package net.calvuz.qdue.core.domain.quattrodue.models;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ✅ DOMAIN MODEL: Work Schedule Template
 *
 * <p>Represents a work schedule template that defines recurring patterns for
 * work shift assignments. Acts as the "recurrence rule" equivalent for work
 * schedules, similar to Google Calendar recurring events.</p>
 *
 * <p>Template characteristics:</p>
 * <ul>
 *   <li>Defines cyclical patterns (e.g., 18-day rotation)</li>
 *   <li>Contains daily patterns with shift assignments</li>
 *   <li>Supports both predefined and user-defined templates</li>
 *   <li>Persisted to database for reuse</li>
 *   <li>Used by WorkScheduleProvider implementations</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 5
 */
public class WorkScheduleTemplate {

    // ==================== CORE IDENTIFICATION ====================

    private Long id;                              // Database primary key
    private String name;                          // Template display name
    private String description;                   // Template description
    private WorkScheduleType type;                // Template type (FIXED_4_2, CUSTOM, etc.)

    // ==================== TEMPLATE CONFIGURATION ====================

    private int cycleDays;                        // Days in one complete cycle
    private List<WorkSchedulePattern> patterns;   // Daily patterns for the cycle
    private boolean isActive;                     // Template activation status
    private boolean isUserDefined;                // User-created vs system-predefined

    // ==================== METADATA ====================

    private LocalDate createdAt;                  // Creation date
    private LocalDateTime lastModified;           // Last modification timestamp
    private String createdBy;                     // User who created this template
    private int usageCount;                       // How many times this template is used

    // ==================== VALIDATION SETTINGS ====================

    private boolean requiresTeamAssignment;       // Whether team assignment is mandatory
    private int minTeamsPerShift;                 // Minimum teams required per shift
    private int maxTeamsPerShift;                 // Maximum teams allowed per shift
    private List<String> supportedTeams;          // Teams that can use this template

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor for framework use.
     */
    public WorkScheduleTemplate() {
        this.patterns = new ArrayList<>();
        this.supportedTeams = new ArrayList<>();
        this.isActive = true;
        this.isUserDefined = false;
        this.createdAt = LocalDate.now();
        this.lastModified = LocalDateTime.now();
        this.usageCount = 0;
        this.minTeamsPerShift = 1;
        this.maxTeamsPerShift = 10;
    }

    /**
     * Constructor for creating a basic template.
     *
     * @param name      Template name
     * @param type      Template type
     * @param cycleDays Number of days in cycle
     */
    public WorkScheduleTemplate(String name, WorkScheduleType type, int cycleDays) {
        this();
        this.name = name;
        this.type = type;
        this.cycleDays = cycleDays;
    }

    /**
     * Constructor for creating a complete custom template.
     *
     * @param name        Template name
     * @param description Template description
     * @param cycleDays   Number of days in cycle
     * @param patterns    Daily patterns for the cycle
     */
    public WorkScheduleTemplate(String name, String description, int cycleDays,
                                List<WorkSchedulePattern> patterns) {
        this();
        this.name = name;
        this.description = description;
        this.type = WorkScheduleType.CUSTOM;
        this.cycleDays = cycleDays;
        this.patterns = patterns != null ? new ArrayList<>( patterns ) : new ArrayList<>();
        this.isUserDefined = true;
    }

    // ==================== PATTERN MANAGEMENT ====================

    /**
     * Gets the pattern for a specific day in the cycle.
     *
     * @param cycleDay Day in cycle (0-based)
     * @return Pattern for the day, or null if not found
     */
    public WorkSchedulePattern getPatternForDay(int cycleDay) {
        if (patterns == null || patterns.isEmpty()) {
            return null;
        }

        if (cycleDay < 0 || cycleDay >= patterns.size()) {
            return null;
        }

        return patterns.get( cycleDay );
    }

    /**
     * Sets the pattern for a specific day in the cycle.
     *
     * @param cycleDay Day in cycle (0-based)
     * @param pattern  Pattern to set
     */
    public void setPatternForDay(int cycleDay, WorkSchedulePattern pattern) {
        if (patterns == null) {
            patterns = new ArrayList<>();
        }

        // Ensure list is large enough
        while (patterns.size() <= cycleDay) {
            patterns.add( null );
        }

        patterns.set( cycleDay, pattern );
        updateLastModified();
    }

    /**
     * Adds a pattern to the end of the cycle.
     *
     * @param pattern Pattern to add
     */
    public void addPattern(WorkSchedulePattern pattern) {
        if (patterns == null) {
            patterns = new ArrayList<>();
        }

        patterns.add( pattern );
        updateLastModified();
    }

    /**
     * Removes all patterns and resets the cycle.
     */
    public void clearPatterns() {
        if (patterns != null) {
            patterns.clear();
        }
        updateLastModified();
    }

    // ==================== BUSINESS LOGIC METHODS ====================

    /**
     * Validates the template configuration.
     *
     * @return Validation result with any errors or warnings
     */
    public TemplateValidationResult validate() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Basic validation
        if (name == null || name.trim().isEmpty()) {
            errors.add( "Template name is required" );
        }

        if (type == null) {
            errors.add( "Template type is required" );
        }

        if (cycleDays <= 0) {
            errors.add( "Cycle days must be positive" );
        }

        if (cycleDays > 365) {
            warnings.add( "Cycle length is very long (" + cycleDays + " days)" );
        }

        // Pattern validation
        if (patterns == null || patterns.isEmpty()) {
            if (type == WorkScheduleType.CUSTOM) {
                errors.add( "Custom templates require at least one pattern" );
            }
        } else {
            if (patterns.size() < cycleDays) {
                errors.add( "Not enough patterns for cycle length: " + patterns.size() + "/" + cycleDays );
            }

            // Validate each pattern
            for (int i = 0; i < patterns.size(); i++) {
                WorkSchedulePattern pattern = patterns.get( i );
                if (pattern != null && !pattern.isValid()) {
                    errors.add( "Invalid pattern at day " + (i + 1) );
                }
            }
        }

        // Team validation
        if (requiresTeamAssignment && (supportedTeams == null || supportedTeams.isEmpty())) {
            warnings.add( "Template requires team assignment but no supported teams defined" );
        }

        if (minTeamsPerShift > maxTeamsPerShift) {
            errors.add( "Minimum teams per shift cannot exceed maximum" );
        }

        return new TemplateValidationResult( errors.isEmpty(), errors, warnings );
    }

    /**
     * Checks if this template is compatible with a specific team.
     *
     * @param teamName Team name to check
     * @return true if team can use this template
     */
    public boolean supportsTeam(String teamName) {
        if (!requiresTeamAssignment) {
            return true;
        }

        return supportedTeams != null &&
                supportedTeams.stream().anyMatch( team -> team.equalsIgnoreCase( teamName ) );
    }

    /**
     * Gets the total number of work shifts defined in this template.
     *
     * @return Total work shifts across all patterns
     */
    public int getTotalWorkShifts() {
        if (patterns == null) {
            return 0;
        }

        return patterns.stream()
                .filter( Objects::nonNull )
                .mapToInt( WorkSchedulePattern::getWorkShiftCount )
                .sum();
    }

    /**
     * Gets the total number of rest periods defined in this template.
     *
     * @return Total rest periods across all patterns
     */
    public int getTotalRestPeriods() {
        if (patterns == null) {
            return 0;
        }

        return patterns.stream()
                .filter( Objects::nonNull )
                .mapToInt( WorkSchedulePattern::getRestPeriodCount )
                .sum();
    }

    /**
     * Creates a copy of this template for customization.
     *
     * @param newName Name for the copied template
     * @return New template instance
     */
    public WorkScheduleTemplate createCopy(String newName) {
        WorkScheduleTemplate copy = new WorkScheduleTemplate();
        copy.name = newName != null ? newName : (this.name + " - Copy");
        copy.description = this.description;
        copy.type = WorkScheduleType.CUSTOM; // Copies are always custom
        copy.cycleDays = this.cycleDays;
        copy.isUserDefined = true;
        copy.requiresTeamAssignment = this.requiresTeamAssignment;
        copy.minTeamsPerShift = this.minTeamsPerShift;
        copy.maxTeamsPerShift = this.maxTeamsPerShift;

        // Deep copy patterns
        if (this.patterns != null) {
            copy.patterns = new ArrayList<>();
            for (WorkSchedulePattern pattern : this.patterns) {
                if (pattern != null) {
                    copy.patterns.add( pattern.createCopy() );
                } else {
                    copy.patterns.add( null );
                }
            }
        }

        // Copy supported teams
        if (this.supportedTeams != null) {
            copy.supportedTeams = new ArrayList<>( this.supportedTeams );
        }

        return copy;
    }

    /**
     * Updates the last modified timestamp.
     */
    private void updateLastModified() {
        this.lastModified = LocalDateTime.now();
    }

    /**
     * Increments the usage count for statistics.
     */
    public void incrementUsageCount() {
        this.usageCount++;
        updateLastModified();
    }

    // ==================== GETTERS AND SETTERS ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        updateLastModified();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        updateLastModified();
    }

    public WorkScheduleType getType() {
        return type;
    }

    public void setType(WorkScheduleType type) {
        this.type = type;
        updateLastModified();
    }

    public int getCycleDays() {
        return cycleDays;
    }

    public void setCycleDays(int cycleDays) {
        this.cycleDays = cycleDays;
        updateLastModified();
    }

    public List<WorkSchedulePattern> getPatterns() {
        return patterns != null ? new ArrayList<>( patterns ) : new ArrayList<>();
    }

    public void setPatterns(List<WorkSchedulePattern> patterns) {
        this.patterns = patterns != null ? new ArrayList<>( patterns ) : new ArrayList<>();
        updateLastModified();
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
        updateLastModified();
    }

    public boolean isUserDefined() {
        return isUserDefined;
    }

    public void setUserDefined(boolean userDefined) {
        this.isUserDefined = userDefined;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public boolean requiresTeamAssignment() {
        return requiresTeamAssignment;
    }

    public void setRequiresTeamAssignment(boolean requiresTeamAssignment) {
        this.requiresTeamAssignment = requiresTeamAssignment;
        updateLastModified();
    }

    public int getMinTeamsPerShift() {
        return minTeamsPerShift;
    }

    public void setMinTeamsPerShift(int minTeamsPerShift) {
        this.minTeamsPerShift = minTeamsPerShift;
        updateLastModified();
    }

    public int getMaxTeamsPerShift() {
        return maxTeamsPerShift;
    }

    public void setMaxTeamsPerShift(int maxTeamsPerShift) {
        this.maxTeamsPerShift = maxTeamsPerShift;
        updateLastModified();
    }

    public List<String> getSupportedTeams() {
        return supportedTeams != null ? new ArrayList<>( supportedTeams ) : new ArrayList<>();
    }

    public void setSupportedTeams(List<String> supportedTeams) {
        this.supportedTeams = supportedTeams != null ? new ArrayList<>( supportedTeams ) : new ArrayList<>();
        updateLastModified();
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkScheduleTemplate that = (WorkScheduleTemplate) o;

        if (id != null && that.id != null) {
            return Objects.equals( id, that.id );
        }

        return Objects.equals( name, that.name ) &&
                Objects.equals( type, that.type ) &&
                cycleDays == that.cycleDays;
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash( id );
        }
        return Objects.hash( name, type, cycleDays );
    }

    @NonNull
    @Override
    public String toString() {
        return "WorkScheduleTemplate{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", cycleDays=" + cycleDays +
                ", patternsCount=" + (patterns != null ? patterns.size() : 0) +
                ", isActive=" + isActive +
                ", isUserDefined=" + isUserDefined +
                '}';
    }

    // ==================== VALIDATION RESULT CLASS ====================

    /**
     * Result of template validation.
     */
    public record TemplateValidationResult(boolean valid, List<String> errors,
                                           List<String> warnings) {
        public TemplateValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors != null ? new ArrayList<>( errors ) : new ArrayList<>();
            this.warnings = warnings != null ? new ArrayList<>( warnings ) : new ArrayList<>();
        }

        @Override
        public List<String> errors() {
            return new ArrayList<>( errors );
        }

        @Override
        public List<String> warnings() {
            return new ArrayList<>( warnings );
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public String getFormattedErrors() {
            return errors.isEmpty() ? "" : String.join( "; ", errors );
        }

        public String getFormattedWarnings() {
            return warnings.isEmpty() ? "" : String.join( "; ", warnings );
        }
    }
}