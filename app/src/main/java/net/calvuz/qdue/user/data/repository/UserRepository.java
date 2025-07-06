package net.calvuz.qdue.user.data.repository;

import android.content.Context;

import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.user.data.dao.UserDao;
import net.calvuz.qdue.user.data.entities.User;
import net.calvuz.qdue.user.data.models.GoogleAuthData;
import net.calvuz.qdue.user.data.models.UserWithOrganization;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for user data operations.
 * Provides abstraction layer between UI and database.
 */
public class UserRepository {

    private static final String TAG = "UserRepository";

    private final UserDao userDao;
    private final ExecutorService executorService;
    private static volatile UserRepository INSTANCE;

    private UserRepository(Context context) {
        QDueDatabase database = QDueDatabase.getInstance(context);
        userDao = database.userDao();
        executorService = Executors.newFixedThreadPool(4);
    }

    public static UserRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (UserRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new UserRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    // User CRUD operations
    public void insertUser(User user, OnUserOperationListener listener) {
        executorService.execute(() -> {
            try {
                long id = userDao.insertUser(user);
                user.setId(id);
                if (listener != null) {
                    listener.onSuccess(user);
                }
                Log.d(TAG, "User inserted with ID: " + id);
            } catch (Exception e) {
                Log.e(TAG, "Error inserting user: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    public void updateUser(User user, OnUserOperationListener listener) {
        executorService.execute(() -> {
            try {
                userDao.updateUser(user);
                if (listener != null) {
                    listener.onSuccess(user);
                }
                Log.d(TAG, "User updated: " + user.getId());
            } catch (Exception e) {
                Log.e(TAG, "Error updating user: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    public void getActiveUser(OnUserWithOrganizationListener listener) {
        executorService.execute(() -> {
            try {
                UserWithOrganization userWithOrg = userDao.getActiveUserWithOrganization();
                if (listener != null) {
                    listener.onSuccess(userWithOrg);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting active user: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    public void getUserByEmail(String email, OnUserListener listener) {
        executorService.execute(() -> {
            try {
                User user = userDao.getUserByEmail(email);
                if (listener != null) {
                    listener.onSuccess(user);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting user by email: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    public void getUserByGoogleId(String googleId, OnUserListener listener) {
        executorService.execute(() -> {
            try {
                User user = userDao.getUserByGoogleId(googleId);
                if (listener != null) {
                    listener.onSuccess(user);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting user by Google ID: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    // Google Authentication operations
    public void authenticateWithGoogle(GoogleAuthData googleData, OnUserOperationListener listener) {
        executorService.execute(() -> {
            try {
                // Check if user already exists
                User existingUser = userDao.getUserByGoogleId(googleData.getGoogleId());

                if (existingUser != null) {
                    // Update existing user with latest Google data
                    googleData.updateUser(existingUser);
                    existingUser.updateLastLogin();
                    userDao.updateUser(existingUser);

                    if (listener != null) {
                        listener.onSuccess(existingUser);
                    }
                    Log.d(TAG, "Existing Google user updated: " + existingUser.getEmail());
                } else {
                    // Check if user exists with same email but different auth method
                    User emailUser = userDao.getUserByEmail(googleData.getEmail());

                    if (emailUser != null) {
                        // Merge accounts - add Google auth to existing user
                        googleData.updateUser(emailUser);
                        emailUser.updateLastLogin();
                        userDao.updateUser(emailUser);

                        if (listener != null) {
                            listener.onSuccess(emailUser);
                        }
                        Log.d(TAG, "Email user merged with Google auth: " + emailUser.getEmail());
                    } else {
                        // Create new user from Google data
                        User newUser = googleData.createUser();
                        newUser.updateLastLogin();
                        long id = userDao.insertUser(newUser);
                        newUser.setId(id);

                        if (listener != null) {
                            listener.onSuccess(newUser);
                        }
                        Log.d(TAG, "New Google user created: " + newUser.getEmail());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error authenticating with Google: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    // Profile management
    public void markProfileCompleted(long userId, OnUserOperationListener listener) {
        executorService.execute(() -> {
            try {
                userDao.markProfileCompleted(userId, LocalDateTime.now());
                User user = userDao.getUserById(userId);
                if (listener != null) {
                    listener.onSuccess(user);
                }
                Log.d(TAG, "Profile marked as completed for user: " + userId);
            } catch (Exception e) {
                Log.e(TAG, "Error marking profile completed: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    public void updateLastLogin(long userId) {
        executorService.execute(() -> {
            try {
                userDao.updateLastLogin(userId, LocalDateTime.now());
                Log.d(TAG, "Last login updated for user: " + userId);
            } catch (Exception e) {
                Log.e(TAG, "Error updating last login: " + e.getMessage());
            }
        });
    }

    // Search and filtering
    public void searchUsers(String query, OnUserListListener listener) {
        executorService.execute(() -> {
            try {
                List<User> users = userDao.searchActiveUsers(query);
                if (listener != null) {
                    listener.onSuccess(users);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error searching users: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    // Validation methods
    public void checkEmailExists(String email, OnValidationListener listener) {
        executorService.execute(() -> {
            try {
                boolean exists = userDao.existsByEmail(email);
                if (listener != null) {
                    listener.onValidationResult(exists);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking email existence: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    public void checkEmployeeIdExists(String employeeId, OnValidationListener listener) {
        executorService.execute(() -> {
            try {
                boolean exists = userDao.existsByEmployeeId(employeeId);
                if (listener != null) {
                    listener.onValidationResult(exists);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking employee ID existence: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    // Cleanup operations
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    // Callback interfaces
    public interface OnUserListener {
        void onSuccess(User user);
        void onError(Exception e);
    }

    public interface OnUserOperationListener {
        void onSuccess(User user);
        void onError(Exception e);
    }

    public interface OnUserWithOrganizationListener {
        void onSuccess(UserWithOrganization userWithOrganization);
        void onError(Exception e);
    }

    public interface OnUserListListener {
        void onSuccess(List<User> users);
        void onError(Exception e);
    }

    public interface OnValidationListener {
        void onValidationResult(boolean exists);
        void onError(Exception e);
    }
}