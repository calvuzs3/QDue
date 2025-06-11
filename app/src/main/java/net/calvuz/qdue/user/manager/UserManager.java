package net.calvuz.qdue.user.manager;

import android.content.Context;

import net.calvuz.qdue.user.data.entities.User;
import net.calvuz.qdue.user.data.models.GoogleAuthData;
import net.calvuz.qdue.user.data.models.UserWithOrganization;
import net.calvuz.qdue.user.data.repository.UserRepository;
import net.calvuz.qdue.user.data.repository.OrganizationRepository;
import net.calvuz.qdue.user.services.GoogleAuthService;
import net.calvuz.qdue.utils.Log;

/**
 * Main manager class for user operations.
 * Coordinates between repositories and services.
 */
public class UserManager {

    private static final String TAG = "UserManager";

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final GoogleAuthService googleAuthService;
    private static volatile UserManager INSTANCE;

    private UserManager(Context context) {
        userRepository = UserRepository.getInstance(context);
        organizationRepository = OrganizationRepository.getInstance(context);
        googleAuthService = GoogleAuthService.getInstance(context);
    }

    public static UserManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (UserManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new UserManager(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Get current active user with full organizational info.
     */
    public void getCurrentUser(OnCurrentUserListener listener) {
        userRepository.getActiveUser(new UserRepository.OnUserWithOrganizationListener() {
            @Override
            public void onSuccess(UserWithOrganization userWithOrganization) {
                if (listener != null) {
                    listener.onSuccess(userWithOrganization);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error getting current user: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * Authenticate user with Google and handle account creation/linking.
     */
    public void authenticateWithGoogle(GoogleAuthData googleData, OnAuthenticationListener listener) {
        userRepository.authenticateWithGoogle(googleData, new UserRepository.OnUserOperationListener() {
            @Override
            public void onSuccess(User user) {
                Log.d(TAG, "Google authentication successful for: " + user.getEmail());
                if (listener != null) {
                    listener.onSuccess(user, !user.isProfileCompleted());
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Google authentication failed: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * Update user profile and mark as completed if all required fields are filled.
     */
    public void updateUserProfile(User user, OnProfileUpdateListener listener) {
        // Check if profile is now complete
        boolean isComplete = isProfileComplete(user);
        user.markProfileCompleted(); // Try do it anyway
        if (isComplete && !user.isProfileCompleted()) {
            user.markProfileCompleted();
        }

        userRepository.updateUser(user, new UserRepository.OnUserOperationListener() {
            @Override
            public void onSuccess(User updatedUser) {
                Log.d(TAG, "User profile updated: " + updatedUser.getId());
                if (listener != null) {
                    listener.onSuccess(updatedUser, isComplete);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error updating user profile: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * Check if user profile has all required information.
     */
    private boolean isProfileComplete(User user) {
        return user.getFirstName() != null && !user.getFirstName().trim().isEmpty() &&
                user.getLastName() != null && !user.getLastName().trim().isEmpty() &&
                user.getEmail() != null && !user.getEmail().trim().isEmpty() &&
                user.getEstablishmentId() != null &&
                user.getMacroDepartmentId() != null;
    }

    /**
     * Get repositories for direct access when needed.
     */
    public UserRepository getUserRepository() {
        return userRepository;
    }

    public OrganizationRepository getOrganizationRepository() {
        return organizationRepository;
    }

    public GoogleAuthService getGoogleAuthService() {
        return googleAuthService;
    }

    /**
     * Cleanup resources.
     */
    public void shutdown() {
        userRepository.shutdown();
        organizationRepository.shutdown();
    }

    // Callback interfaces
    public interface OnCurrentUserListener {
        void onSuccess(UserWithOrganization userWithOrganization);
        void onError(Exception e);
    }

    public interface OnAuthenticationListener {
        void onSuccess(User user, boolean needsProfileCompletion);
        void onError(Exception e);
    }

    public interface OnProfileUpdateListener {
        void onSuccess(User user, boolean profileCompleted);
        void onError(Exception e);
    }
}