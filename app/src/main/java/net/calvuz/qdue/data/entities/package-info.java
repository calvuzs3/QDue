/**
 * Data Entities Package
 *
 * <p>This package contains RoomDB entity classes for data persistence in the QDue application.
 * All entities follow Room conventions and are designed to map cleanly to domain models
 * while providing efficient database operations.</p>
 *
 * <h2>Package Structure</h2>
 * <pre>
 * net.calvuz.qdue.data.entities/
 * ├── TeamEntity.java               # Work team entity
 * └── package-info.java             # This documentation
 * </pre>
 *
 * <h2>Clean Architecture Role</h2>
 *
 * <h3>Data Layer Responsibilities</h3>
 * <p>Entities in this package serve the data persistence layer:</p>
 * <ul>
 *   <li><strong>Database Mapping</strong>: Direct representation of database tables</li>
 *   <li><strong>Room Integration</strong>: Uses Room annotations for ORM functionality</li>
 *   <li><strong>Data Validation</strong>: Entity-level validation for database constraints</li>
 *   <li><strong>Performance Optimization</strong>: Indexed fields for efficient queries</li>
 * </ul>
 *
 * <h3>Separation from Domain</h3>
 * <p>These entities are separate from domain models:</p>
 * <ul>
 *   <li><strong>Framework Dependent</strong>: Uses Room/Android-specific annotations</li>
 *   <li><strong>Persistence Focused</strong>: Optimized for database operations</li>
 *   <li><strong>Repository Converted</strong>: Mapped to domain models by repositories</li>
 *   <li><strong>Technology Specific</strong>: Tied to SQLite/Room implementation</li>
 * </ul>
 *
 * <h2>Entity Design Principles</h2>
 *
 * <h3>Naming Conventions</h3>
 * <ul>
 *   <li><strong>Entity Classes</strong>: Suffix with "Entity" (e.g., {@code TeamEntity})</li>
 *   <li><strong>Table Names</strong>: Lowercase plural (e.g., "teams")</li>
 *   <li><strong>Column Names</strong>: Snake_case (e.g., "display_name")</li>
 *   <li><strong>Primary Keys</strong>: "id" field with auto-generation</li>
 * </ul>
 *
 * <h3>Common Patterns</h3>
 * <ul>
 *   <li><strong>Timestamps</strong>: created_at and updated_at for audit trails</li>
 *   <li><strong>Soft Deletes</strong>: active boolean field instead of hard deletion</li>
 *   <li><strong>Indexes</strong>: Strategic indexing for performance</li>
 *   <li><strong>Unique Constraints</strong>: Business-level uniqueness enforcement</li>
 * </ul>
 *
 * <h2>Core Entities</h2>
 *
 * <h3>TeamEntity</h3>
 * <p>Represents work teams in the schedule system:</p>
 * <ul>
 *   <li><strong>Table</strong>: "teams"</li>
 *   <li><strong>Business Key</strong>: Unique team name</li>
 *   <li><strong>Features</strong>: Display names, descriptions, active status</li>
 *   <li><strong>Extensible</strong>: Ready for additional team properties</li>
 *   <li><strong>Optimized</strong>: Indexes on name and active status</li>
 * </ul>
 *
 * <h2>Database Schema Design</h2>
 *
 * <h3>Table Structure</h3>
 * <pre>
 * teams:
 * ├── id (INTEGER PRIMARY KEY AUTOINCREMENT)
 * ├── name (TEXT NOT NULL UNIQUE)
 * ├── display_name (TEXT)
 * ├── description (TEXT)
 * ├── active (INTEGER DEFAULT 1)
 * ├── created_at (INTEGER)
 * ├── updated_at (INTEGER)
 * ├── color_hex (TEXT)
 * └── sort_order (INTEGER DEFAULT 0)
 *
 * Indexes:
 * ├── UNIQUE INDEX ix_teams_name ON teams(name)
 * ├── INDEX ix_teams_active ON teams(active)
 * └── INDEX ix_teams_name_active ON teams(name, active)
 * </pre>
 *
 * <h3>Performance Considerations</h3>
 * <ul>
 *   <li><strong>Primary Queries</strong>: By name (unique index) and active status</li>
 *   <li><strong>Composite Index</strong>: name + active for common filtered queries</li>
 *   <li><strong>Sort Optimization</strong>: sort_order field for custom ordering</li>
 *   <li><strong>Soft Deletes</strong>: active field for logical deletion</li>
 * </ul>
 *
 * <h2>Entity Usage Patterns</h2>
 *
 * <h3>Basic Entity Operations</h3>
 * <pre>
 * {@code
 * // Create new entity
 * TeamEntity team = new TeamEntity("TeamA");
 * team.setDisplayName("Team Alpha");
 * team.setDescription("Primary morning shift team");
 *
 * // Insert via DAO
 * long id = teamDao.insertTeam(team);
 *
 * // Query by name
 * TeamEntity found = teamDao.getTeamByName("TeamA");
 *
 * // Update entity
 * found.setDescription("Updated description");
 * teamDao.updateTeam(found);
 *
 * // Soft delete
 * found.markAsInactive();
 * teamDao.updateTeam(found);
 * }
 * </pre>
 *
 * <h3>Validation and Safety</h3>
 * <pre>
 * {@code
 * // Validate before database operations
 * try {
 *     team.validate();
 *     long id = teamDao.insertTeam(team);
 * } catch (IllegalStateException e) {
 *     // Handle validation errors
 *     Log.e(TAG, "Invalid team data: " + e.getMessage());
 * }
 *
 * // Safe updates with timestamp tracking
 * team.setDisplayName("New Name");
 * team.touch(); // Updates updated_at timestamp
 * teamDao.updateTeam(team);
 * }
 * </pre>
 *
 * <h2>Repository Integration</h2>
 *
 * <h3>Entity to Domain Mapping</h3>
 * <p>Repositories handle conversion between entities and domain models:</p>
 * <pre>
 * {@code
 * // In Repository Implementation
 * private Team convertEntityToTeam(TeamEntity entity) {
 *     return Team.builder(entity.getName())
 *             .displayName(entity.getEffectiveDisplayName())
 *             .description(entity.getDescription())
 *             .active(entity.isActive())
 *             .build();
 * }
 *
 * private TeamEntity convertTeamToEntity(Team team) {
 *     return new TeamEntity(
 *             team.getName(),
 *             team.getDisplayName(),
 *             team.getDescription(),
 *             team.isActive()
 *     );
 * }
 * }
 * </pre>
 *
 * <h3>Repository Methods</h3>
 * <pre>
 * {@code
 * // Repository returns domain objects
 * public CompletableFuture<OperationResult<List<Team>>> getAllTeams() {
 *     return CompletableFuture.supplyAsync(() -> {
 *         List<TeamEntity> entities = teamDao.getActiveTeams();
 *         List<Team> teams = entities.stream()
 *                 .map(this::convertEntityToTeam)
 *                 .collect(toList());
 *         return OperationResult.success(teams, READ);
 *     });
 * }
 * }
 * </pre>
 *
 * <h2>Migration and Versioning</h2>
 *
 * <h3>Schema Evolution</h3>
 * <p>When adding fields to entities:</p>
 * <ol>
 *   <li><strong>Add Field</strong>: Add new field with appropriate default</li>
 *   <li><strong>Update Entity</strong>: Add getter/setter methods</li>
 *   <li><strong>Database Migration</strong>: Create Room migration script</li>
 *   <li><strong>DAO Updates</strong>: Add queries using new field if needed</li>
 *   <li><strong>Repository Updates</strong>: Update mapping logic</li>
 * </ol>
 *
 * <h3>Backward Compatibility</h3>
 * <ul>
 *   <li><strong>Nullable Fields</strong>: New fields should be nullable or have defaults</li>
 *   <li><strong>Conservative Changes</strong>: Avoid breaking existing queries</li>
 *   <li><strong>Migration Testing</strong>: Test upgrades from previous versions</li>
 * </ul>
 *
 * <h2>Testing Strategy</h2>
 *
 * <h3>Entity Testing</h3>
 * <ul>
 *   <li><strong>Validation Tests</strong>: Test entity validation rules</li>
 *   <li><strong>Mapping Tests</strong>: Test Room annotations and mapping</li>
 *   <li><strong>Constraint Tests</strong>: Test unique constraints and indexes</li>
 *   <li><strong>Migration Tests</strong>: Test database schema migrations</li>
 * </ul>
 *
 * <h3>Integration Testing</h3>
 * <ul>
 *   <li><strong>DAO Integration</strong>: Test entities with DAO operations</li>
 *   <li><strong>Repository Integration</strong>: Test entity-domain conversion</li>
 *   <li><strong>Performance Testing</strong>: Verify index effectiveness</li>
 * </ul>
 *
 * <h2>Best Practices</h2>
 *
 * <h3>Entity Design</h3>
 * <ul>
 *   <li><strong>Immutable When Possible</strong>: Minimize mutable state</li>
 *   <li><strong>Validation</strong>: Include entity-level validation methods</li>
 *   <li><strong>Defensive Programming</strong>: Null-safe getters and setters</li>
 *   <li><strong>Meaningful Defaults</strong>: Provide sensible default values</li>
 * </ul>
 *
 * <h3>Performance</h3>
 * <ul>
 *   <li><strong>Strategic Indexing</strong>: Index frequently queried fields</li>
 *   <li><strong>Composite Indexes</strong>: For common multi-field queries</li>
 *   <li><strong>Soft Deletes</strong>: Preserve data for audit trails</li>
 *   <li><strong>Timestamp Tracking</strong>: For change auditing</li>
 * </ul>
 *
 * <h3>Maintainability</h3>
 * <ul>
 *   <li><strong>Clear Naming</strong>: Self-documenting field and method names</li>
 *   <li><strong>Comprehensive Documentation</strong>: Document business rules</li>
 *   <li><strong>Version Control</strong>: Track schema changes carefully</li>
 *   <li><strong>Future Planning</strong>: Design for expected enhancements</li>
 * </ul>
 *
 * <h2>Future Extensions</h2>
 *
 * <h3>Planned Entities</h3>
 * <ul>
 *   <li><strong>UserTeamEntity</strong>: Many-to-many user-team relationships</li>
 *   <li><strong>ShiftTemplateEntity</strong>: Reusable shift templates</li>
 *   <li><strong>ScheduleConfigEntity</strong>: System configuration settings</li>
 *   <li><strong>AuditLogEntity</strong>: Change tracking and history</li>
 * </ul>
 *
 * <h3>Advanced Features</h3>
 * <ul>
 *   <li><strong>Full-Text Search</strong>: FTS integration for team search</li>
 *   <li><strong>Relationship Mapping</strong>: Complex entity relationships</li>
 *   <li><strong>Caching Strategy</strong>: Entity-level caching integration</li>
 *   <li><strong>Synchronization</strong>: Multi-device data sync support</li>
 * </ul>
 *
 * <h2>See Also</h2>
 * <ul>
 *   <li>{@link net.calvuz.qdue.data.dao} - Data Access Objects for entity operations</li>
 *   <li>{@link net.calvuz.qdue.domain.calendar.models} - Domain models</li>
 *   <li>{@link net.calvuz.qdue.data.repositories} - Repository implementations</li>
 *   <li>{@link net.calvuz.qdue.core.db.QDueDatabase} - Database configuration</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Initial Implementation
 * @since Database Version 6
 */
package net.calvuz.qdue.data.entities;