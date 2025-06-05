package net.calvuz.qdue.user.data.models;

import net.calvuz.qdue.user.data.entities.User;

/**
 * Google authentication data model.
 * Used for handling Google Sign-In integration.
 */
public class GoogleAuthData {

    private String googleId;
    private String email;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private String locale;
    private boolean emailVerified;

    // Constructors
    public GoogleAuthData() {
    }

    public GoogleAuthData(String googleId, String email, String firstName,
                          String lastName, String profileImageUrl) {
        this.googleId = googleId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profileImageUrl = profileImageUrl;
        this.emailVerified = true; // Google emails are typically verified
    }

    // Utility methods
    public User createUser() {
        User user = new User(firstName, lastName, email);
        user.setGoogleId(googleId);
        user.setAuthProvider("google");
        user.setProfileImageUrl(profileImageUrl);
        user.setProfileImageSource("google");
        return user;
    }

    public void updateUser(User user) {
        if (user == null) return;

        // Update basic info if not manually changed
        if (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
            user.setFirstName(firstName);
        }
        if (user.getLastName() == null || user.getLastName().trim().isEmpty()) {
            user.setLastName(lastName);
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            user.setEmail(email);
        }

        // Always update Google-specific data
        user.setGoogleId(googleId);
        if ("google".equals(user.getAuthProvider()) && profileImageUrl != null) {
            user.setProfileImageUrl(profileImageUrl);
            user.setProfileImageSource("google");
        }
    }

    public boolean isValid() {
        return googleId != null && !googleId.trim().isEmpty() &&
                email != null && !email.trim().isEmpty();
    }

    // Getters and Setters
    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}