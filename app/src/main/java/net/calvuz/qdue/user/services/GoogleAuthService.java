package net.calvuz.qdue.user.services;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import net.calvuz.qdue.BuildConfig;
import net.calvuz.qdue.user.data.models.GoogleAuthData;
import net.calvuz.qdue.utils.Log;

/**
 * Service for handling Google Sign-In authentication.
 * Provides abstraction layer for Google authentication operations.
 */
public class GoogleAuthService {

    private static final String TAG = "GoogleAuthService";
    public static final int RC_SIGN_IN = 9001;

    private final Context context;
    private final GoogleSignInClient googleSignInClient;
    private static volatile GoogleAuthService INSTANCE;

    private GoogleAuthService(Context context) {
        this.context = context.getApplicationContext();

        // Configure Google Sign-In options
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestIdToken(BuildConfig.GOOGLE_CLIENT_ID) // Add to strings.xml
                .build();

        googleSignInClient = GoogleSignIn.getClient(context, gso);
    }

    public static GoogleAuthService getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (GoogleAuthService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GoogleAuthService(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Start Google Sign-In flow.
     */
    public void signIn(Activity activity) {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /**
     * Handle Google Sign-In result from onActivityResult.
     */
    public void handleSignInResult(Intent data, OnGoogleAuthListener listener) {
        try {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            GoogleSignInAccount account = task.getResult(ApiException.class);

            if (account != null) {
                GoogleAuthData authData = createAuthDataFromAccount(account);
                if (listener != null) {
                    listener.onSuccess(authData);
                }
                Log.d(TAG, "Google Sign-In successful: " + account.getEmail());
            } else {
                if (listener != null) {
                    listener.onError(new Exception("Google account is null"));
                }
            }
        } catch (ApiException e) {
            Log.e(TAG, "Google Sign-In failed: " + e.getStatusCode());
            if (listener != null) {
                listener.onError(e);
            }
        }
    }

    /**
     * Check if user is already signed in to Google.
     */
    public void checkExistingSignIn(OnGoogleAuthListener listener) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account != null) {
            GoogleAuthData authData = createAuthDataFromAccount(account);
            if (listener != null) {
                listener.onSuccess(authData);
            }
            Log.d(TAG, "Existing Google Sign-In found: " + account.getEmail());
        } else {
            if (listener != null) {
                listener.onError(new Exception("No existing Google Sign-In"));
            }
        }
    }

    /**
     * Sign out from Google.
     */
    public void signOut(OnSignOutListener listener) {
        googleSignInClient.signOut()
                .addOnCompleteListener(task -> {
                    if (listener != null) {
                        if (task.isSuccessful()) {
                            listener.onSuccess();
                            Log.d(TAG, "Google Sign-Out successful");
                        } else {
                            listener.onError(task.getException());
                            Log.e(TAG, "Google Sign-Out failed");
                        }
                    }
                });
    }

    /**
     * Revoke Google access (complete disconnect).
     */
    public void revokeAccess(OnSignOutListener listener) {
        googleSignInClient.revokeAccess()
                .addOnCompleteListener(task -> {
                    if (listener != null) {
                        if (task.isSuccessful()) {
                            listener.onSuccess();
                            Log.d(TAG, "Google access revoked");
                        } else {
                            listener.onError(task.getException());
                            Log.e(TAG, "Google access revocation failed");
                        }
                    }
                });
    }

    /**
     * Create GoogleAuthData from GoogleSignInAccount.
     */
    private GoogleAuthData createAuthDataFromAccount(GoogleSignInAccount account) {
        GoogleAuthData authData = new GoogleAuthData();
        authData.setGoogleId(account.getId());
        authData.setEmail(account.getEmail());
        authData.setFirstName(account.getGivenName());
        authData.setLastName(account.getFamilyName());

        if (account.getPhotoUrl() != null) {
            authData.setProfileImageUrl(account.getPhotoUrl().toString());
        }

        authData.setEmailVerified(true); // Google accounts are verified
        return authData;
    }

    /**
     * Check if Google Play Services are available.
     */
    public boolean isGooglePlayServicesAvailable() {
        // You can add GoogleApiAvailability check here if needed
        return true;
    }

    // Callback interfaces
    public interface OnGoogleAuthListener {
        void onSuccess(GoogleAuthData authData);
        void onError(Exception e);
    }

    public interface OnSignOutListener {
        void onSuccess();
        void onError(Exception e);
    }
}