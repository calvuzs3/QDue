package net.calvuz.qdue.core.backup.models;

import net.calvuz.qdue.user.data.entities.User;

import java.util.List;
import java.util.Map;

/**
 * STEP 1: Core Backup System Models
 * <p>
 * Provides data structures for the unified backup system that handles
 * all entities in the QDue application, extending beyond just events.
 */

// ==================== SPECIALIZED ENTITY BACKUPS ====================

/**
 * Users-specific backup with organizational context
 */
public class UsersBackupPackage extends EntityBackupPackage {
    public List<User> users;
    public UsersBackupMetadata usersMetadata;

    public UsersBackupPackage(List<User> users) {
        super("users", "1.0", users);
        this.users = users;
        this.usersMetadata = new UsersBackupMetadata(users);
    }

    public static class UsersBackupMetadata {
        public int totalUsers;
        public int activeUsers;
        public int inactiveUsers;
        public Map<String, Integer> usersByAuthProvider;
        public int completeProfiles;
        public int incompleteProfiles;

        public UsersBackupMetadata(List<User> users) {
            if (users != null) {
                this.totalUsers = users.size();
                this.usersByAuthProvider = new java.util.HashMap<>();

                for (User user : users) {
                    // Count active/inactive
                    if (user.isActive()) {
                        activeUsers++;
                    } else {
                        inactiveUsers++;
                    }

                    // Count by auth provider
                    String provider = user.getAuthProvider() != null ? user.getAuthProvider() : "UNKNOWN";
                    usersByAuthProvider.put(provider, usersByAuthProvider.getOrDefault(provider, 0) + 1);

                    // Count profile completion
                    if (user.isProfileCompleted()) {
                        completeProfiles++;
                    } else {
                        incompleteProfiles++;
                    }
                }
            }
        }
    }
}
