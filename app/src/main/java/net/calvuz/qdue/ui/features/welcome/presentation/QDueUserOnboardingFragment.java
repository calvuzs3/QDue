package net.calvuz.qdue.ui.features.welcome.presentation;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.di.Injectable;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.data.services.QDueUserService;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.welcome.interfaces.WelcomeInterface;

/**
 * QDueUserOnboardingFragment - Minimal Single-Step User Onboarding
 *
 * <p>Simple onboarding fragment for QDueUser creation with minimal friction.
 * Designed for single-step user creation with optional fields and real-time validation.</p>
 *
 * <h3>Fragment Features:</h3>
 * <ul>
 *   <li><strong>Minimal Fields</strong>: Only nickname and email (both optional)</li>
 *   <li><strong>Real-time Validation</strong>: Email format validation as user types</li>
 *   <li><strong>Skip Option</strong>: Allow completion with empty fields</li>
 *   <li><strong>Clean Architecture</strong>: Uses QDueUserService for business logic</li>
 * </ul>
 *
 * <h3>UI/UX Design:</h3>
 * <ul>
 *   <li><strong>Single Step</strong>: All fields on one screen</li>
 *   <li><strong>Optional Indicators</strong>: Clear indication that fields are optional</li>
 *   <li><strong>Progress Feedback</strong>: Loading states and success/error feedback</li>
 *   <li><strong>Accessibility</strong>: Proper content descriptions and focus handling</li>
 * </ul>
 *
 * <h3>Integration Points:</h3>
 * <ul>
 *   <li><strong>ServiceProvider</strong>: Dependency injection for QDueUserService</li>
 *   <li><strong>Navigation</strong>: Can be embedded in larger onboarding flow</li>
 *   <li><strong>Callbacks</strong>: OnboardingListener for parent communication</li>
 *   <li><strong>Lifecycle</strong>: Proper cleanup and state management</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Minimal User Onboarding Fragment
 * @since Clean Architecture Phase 3
 */
public class QDueUserOnboardingFragment extends Fragment implements Injectable {

    private static final String TAG = "QDueUserOnboardingFragment";

    // ==================== VIEW COMPONENTS ====================

    private EditText mNicknameEditText;
    private EditText mEmailEditText;
    private TextView mEmailErrorTextView;

    // ==================== DEPENDENCY INJECTION ====================

    private ServiceProvider mServiceProvider;
    private QDueUserService mQDueUserService;

    // ==================== COMMUNICATION INTERFACES ====================

    private WelcomeInterface mWelcomeInterface;

    // ==================== VALIDATION STATE ====================

    private boolean mIsEmailValid = true;
    private String mCurrentNickname = "";
    private String mCurrentEmail = "";

    // ==================== FRAGMENT LIFECYCLE ====================

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        Log.d( TAG, "QDueUserOnboardingFragment created" );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d( TAG, "Creating QDueUserOnboardingFragment view" );
        return inflater.inflate( R.layout.fragment_qdueuser_onboarding, container, false );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated( view, savedInstanceState );

        initializeViews( view );
        loadExistingData(); // Load data and set text before listeners

        setupValidation();  // Apply listeners

    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data from interface in case it changed
//        loadExistingData();
    }

    // ==================== COMMUNICATION INTERFACE SETUP ====================

    /**
     * Set WelcomeInterface for communication with parent Activity
     */
    public void setWelcomeInterface(WelcomeInterface welcomeInterface) {
        Log.d( TAG, "Setting WelcomeInterface" );
        this.mWelcomeInterface = welcomeInterface;

        // Load existing data if available
        if (isAdded()) {
            loadExistingData();
        }
    }

    // ==================== DEPENDENCY INJECTION ====================

    @Override
    public void inject(@NonNull ServiceProvider serviceProvider) {
        Log.d( TAG, "Injecting dependencies into QDueUserOnboardingFragment" );

        this.mServiceProvider = serviceProvider;
        try {
            this.mQDueUserService = serviceProvider.getQDueUserService();
        } catch (Exception e) {
            // Continue without service - basic validation will still work
            Log.w( TAG, "Could not obtain QDueUserService: " + e.getMessage() );
        }
        Log.d( TAG, "✅ Dependencies injected successfully" );
    }

    // =============== DEPENDENCY INJECTION & COMMUNICATION INTERFACE CHECK ===============

    /**
     * Check if dependencies are injected and ready
     */
    @Override
    public boolean areDependenciesReady() {
        return mServiceProvider != null && mWelcomeInterface != null;
    }

    // ==================== VIEW INITIALIZATION ====================

    @SuppressLint ("SetTextI18n")
    private void initializeViews(@NonNull View view) {
        mNicknameEditText = view.findViewById( R.id.et_nickname );
        mEmailEditText = view.findViewById( R.id.et_email );
        mEmailErrorTextView = view.findViewById( R.id.tv_email_error );
        TextView descriptionTextView = view.findViewById( R.id.tv_description );

        // Set initial UI state
        mEmailErrorTextView.setVisibility( View.GONE );

        // Set placeholder hints to indicate optional fields
        mNicknameEditText.setHint( "Nickname (optional)" );
        mEmailEditText.setHint( "Email (optional)" );

        // Set description text
        descriptionTextView.setText(
                "Help us personalize your experience by providing some basic information. " +
                        "Both fields are optional - you can skip this step and complete your profile later."
        );
    }

    // ==================== VALIDATION SETUP ====================

    private void setupValidation() {
        // Real-time email validation
        mEmailEditText.addTextChangedListener( new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mCurrentEmail = s.toString().trim();
                validateEmail();
                saveCurrentData();
            }
        } );

        // Nickname change tracking
        mNicknameEditText.addTextChangedListener( new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mCurrentNickname = s.toString().trim();
                saveCurrentData();
            }
        } );
    }

    private void validateEmail() {
        // First try service validation if available
        if (mQDueUserService != null) {
            try {
                OperationResult<Boolean> validationResult = mQDueUserService.isEmailValid( mCurrentEmail );

                if (validationResult.isSuccess()) {
                    assert validationResult.getData() != null;
                    mIsEmailValid = validationResult.getData();
                } else {
                    // Fall back to basic validation
                    mIsEmailValid = isEmailValidBasic( mCurrentEmail );
                }
            } catch (Exception e) {
                Log.w( TAG, "Service validation failed, using basic validation: " + e.getMessage() );
                mIsEmailValid = isEmailValidBasic( mCurrentEmail );
            }
        } else {
            // Use interface validation if available
            if (mWelcomeInterface != null) {
                mIsEmailValid = mWelcomeInterface.validateQDueUserData( mCurrentNickname, mCurrentEmail );
            } else {
                // Fall back to basic validation
                mIsEmailValid = isEmailValidBasic( mCurrentEmail );
            }
        }

        // Update UI based on validation result
        updateValidationUI();
    }

    private boolean isEmailValidBasic(String email) {
        if (email == null || email.trim().isEmpty()) {
            return true; // Empty email is valid (optional field)
        }

        // Basic email validation pattern
        return android.util.Patterns.EMAIL_ADDRESS.matcher( email.trim() ).matches();
    }

    private void updateValidationUI() {
        if (mIsEmailValid || mCurrentEmail.isEmpty()) {
            mEmailErrorTextView.setVisibility( View.GONE );
        } else {
            mEmailErrorTextView.setText( "Invalid email format" );
            mEmailErrorTextView.setVisibility( View.VISIBLE );
        }
    }

    // ==================== DATA MANAGEMENT ====================

    private void loadExistingData() {
        if (mWelcomeInterface == null) return;

        try {
            String existingNickname = mWelcomeInterface.getQDueUserNickname();
            String existingEmail = mWelcomeInterface.getQDueUserEmail();

            Log.d( TAG, "Loading existing data: nickname='" + existingNickname + "', email='" + existingEmail + "'" );

            // Update UI without triggering listeners
            mNicknameEditText.removeTextChangedListener( null );
            mEmailEditText.removeTextChangedListener( null );

            mNicknameEditText.setText( existingNickname );
            mEmailEditText.setText( existingEmail );

            mCurrentNickname = existingNickname;
            mCurrentEmail = existingEmail;

            // Re-setup listeners
            setupValidation();

            // Validate loaded data
            validateEmail();
        } catch (Exception e) {
            Log.e( TAG, "Error loading existing data", e );
        }
    }

    private void saveCurrentData() {
        if (mWelcomeInterface == null) return;

        try {
            //Log.d( TAG, "Saving current data: nickname='" + mCurrentNickname + "', email='" + mCurrentEmail + "'" );

            mWelcomeInterface.setQDueUserNickname( mCurrentNickname );
            mWelcomeInterface.setQDueUserEmail( mCurrentEmail );
        } catch (Exception e) {
            Log.e( TAG, "Error saving current data", e );
        }
    }

    // ==================== CLEANUP ====================

    @Override
    public void onDestroyView() {
        // Save data one final time
        saveCurrentData();

        // Clear references
        mWelcomeInterface = null;
        mQDueUserService = null;
        mServiceProvider = null;

        super.onDestroyView();
    }
}