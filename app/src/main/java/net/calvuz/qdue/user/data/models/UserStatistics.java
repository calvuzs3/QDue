package net.calvuz.qdue.user.data.models;

/**
 * User statistics and summary model.
 * Used for dashboard and reporting functionality.
 */
public class UserStatistics {

    private int totalUsers;
    private int activeUsers;
    private int completedProfiles;
    private int googleAuthUsers;
    private int establishmentsCount;
    private int macroDepartmentsCount;
    private int subDepartmentsCount;

    // Constructors
    public UserStatistics() {}

    public UserStatistics(int totalUsers, int activeUsers, int completedProfiles,
                          int googleAuthUsers, int establishmentsCount,
                          int macroDepartmentsCount, int subDepartmentsCount) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.completedProfiles = completedProfiles;
        this.googleAuthUsers = googleAuthUsers;
        this.establishmentsCount = establishmentsCount;
        this.macroDepartmentsCount = macroDepartmentsCount;
        this.subDepartmentsCount = subDepartmentsCount;
    }

    // Utility methods
    public double getProfileCompletionRate() {
        return totalUsers > 0 ? (double) completedProfiles / totalUsers * 100 : 0;
    }

    public double getActiveUserRate() {
        return totalUsers > 0 ? (double) activeUsers / totalUsers * 100 : 0;
    }

    public double getGoogleAuthRate() {
        return totalUsers > 0 ? (double) googleAuthUsers / totalUsers * 100 : 0;
    }

    public boolean hasOrganizationalStructure() {
        return establishmentsCount > 0 && macroDepartmentsCount > 0;
    }

    // Getters and Setters
    public int getTotalUsers() { return totalUsers; }
    public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }

    public int getActiveUsers() { return activeUsers; }
    public void setActiveUsers(int activeUsers) { this.activeUsers = activeUsers; }

    public int getCompletedProfiles() { return completedProfiles; }
    public void setCompletedProfiles(int completedProfiles) { this.completedProfiles = completedProfiles; }

    public int getGoogleAuthUsers() { return googleAuthUsers; }
    public void setGoogleAuthUsers(int googleAuthUsers) { this.googleAuthUsers = googleAuthUsers; }

    public int getEstablishmentsCount() { return establishmentsCount; }
    public void setEstablishmentsCount(int establishmentsCount) { this.establishmentsCount = establishmentsCount; }

    public int getMacroDepartmentsCount() { return macroDepartmentsCount; }
    public void setMacroDepartmentsCount(int macroDepartmentsCount) { this.macroDepartmentsCount = macroDepartmentsCount; }

    public int getSubDepartmentsCount() { return subDepartmentsCount; }
    public void setSubDepartmentsCount(int subDepartmentsCount) { this.subDepartmentsCount = subDepartmentsCount; }
}