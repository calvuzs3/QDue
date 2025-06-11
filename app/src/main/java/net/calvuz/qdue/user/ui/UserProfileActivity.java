package net.calvuz.qdue.user.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.calvuz.qdue.R;
import net.calvuz.qdue.databinding.ActivityUserProfileBinding;
import net.calvuz.qdue.user.data.entities.Establishment;
import net.calvuz.qdue.user.data.entities.MacroDepartment;
import net.calvuz.qdue.user.data.entities.SubDepartment;
import net.calvuz.qdue.user.data.entities.User;
import net.calvuz.qdue.user.data.models.GoogleAuthData;
import net.calvuz.qdue.user.data.models.UserWithOrganization;
import net.calvuz.qdue.user.data.repository.OrganizationRepository;
import net.calvuz.qdue.user.manager.UserManager;
import net.calvuz.qdue.user.services.GoogleAuthService;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Activity for user profile management.
 * Handles user data input, Google authentication, and organizational selection.
 */
public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfileActivity";
    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_CAMERA_PERMISSION = 1002;

    // View binding
    private ActivityUserProfileBinding binding;

    // Core components
    private UserManager userManager;
    private OrganizationRepository organizationRepository;
    private GoogleAuthService googleAuthService;

    // Current data
    private User currentUser;
    private UserWithOrganization userWithOrganization;
    private List<Establishment> establishments = new ArrayList<>();
    private List<MacroDepartment> macroDepartments = new ArrayList<>();
    private List<SubDepartment> subDepartments = new ArrayList<>();

    // UI state
    private boolean isDataLoaded = false;
    private boolean hasUnsavedChanges = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeComponents();
        setupToolbar();
        setupUI();
        loadUserData();
    }

    /**
     * Initialize core components and managers.
     */
    private void initializeComponents() {
        userManager = UserManager.getInstance(this);
        organizationRepository = OrganizationRepository.getInstance(this);
        googleAuthService = GoogleAuthService.getInstance(this);
    }

    /**
     * Setup toolbar with back navigation.
     */
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    /**
     * Setup UI components and listeners.
     */
    private void setupUI() {
        setupTextChangeListeners();
        setupClickListeners();
        setupDropdownAdapters();
    }

    /**
     * Setup text change listeners to track unsaved changes.
     */
    private void setupTextChangeListeners() {
        TextWatcher changeWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isDataLoaded) {
                    hasUnsavedChanges = true;
                    updateSaveButtonState();
                }
            }
        };

        // Apply to all input fields
        binding.etFirstName.addTextChangedListener(changeWatcher);
        binding.etLastName.addTextChangedListener(changeWatcher);
        binding.etNickname.addTextChangedListener(changeWatcher);
        binding.etEmail.addTextChangedListener(changeWatcher);
        binding.etEmployeeId.addTextChangedListener(changeWatcher);
        binding.etJobTitle.addTextChangedListener(changeWatcher);
        binding.etJobLevel.addTextChangedListener(changeWatcher);
        binding.etPhoneWork.addTextChangedListener(changeWatcher);
        binding.etPhonePersonal.addTextChangedListener(changeWatcher);
        binding.etTeamName.addTextChangedListener(changeWatcher);
    }

    /**
     * Setup click listeners for buttons and interactive elements.
     */
    private void setupClickListeners() {
        // Google Sign-In
        binding.btnGoogleSignin.setOnClickListener(v -> handleGoogleSignIn());

        // Save Profile
        binding.btnSaveProfile.setOnClickListener(v -> saveProfile());

        // Reset Changes
        binding.btnResetProfile.setOnClickListener(v -> resetChanges());

        // Change Profile Image
        binding.fabChangeImage.setOnClickListener(v -> changeProfileImage());

        // Hire Date Picker
        binding.etHireDate.setOnClickListener(v -> showDatePicker());

        // Organizational dropdowns
        binding.etEstablishment.setOnItemClickListener((parent, view, position, id) -> {
            onEstablishmentSelected(position);
        });

        binding.etMacroDepartment.setOnItemClickListener((parent, view, position, id) -> {
            onMacroDepartmentSelected(position);
        });
    }

    /**
     * Setup dropdown adapters for organizational data.
     */
    private void setupDropdownAdapters() {
        loadOrganizationalData();
    }

    /**
     * Load current user data and populate fields.
     */
    private void loadUserData() {
        showLoading(true);

        userManager.getCurrentUser(new UserManager.OnCurrentUserListener() {
            @Override
            public void onSuccess(UserWithOrganization userWithOrg) {
                runOnUiThread(() -> {
                    userWithOrganization = userWithOrg;
                    currentUser = userWithOrg != null ? userWithOrg.user : null;

                    if (currentUser != null) {
                        populateUserData();
                    } else {
                        createNewUser();
                    }

                    isDataLoaded = true;
                    showLoading(false);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error loading user data: " + e.getMessage());
                    createNewUser();
                    isDataLoaded = true;
                    showLoading(false);
                });
            }
        });
    }

    /**
     * Load organizational data (establishments, departments).
     */
    private void loadOrganizationalData() {
        organizationRepository.getAllEstablishments(new OrganizationRepository.OnEstablishmentListListener() {
            @Override
            public void onSuccess(List<Establishment> loadedEstablishments) {
                runOnUiThread(() -> {
                    establishments = loadedEstablishments;
                    updateEstablishmentDropdown();
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading establishments: " + e.getMessage());
                // Continue with empty list
            }
        });
    }

    /**
     * Create new user instance for first-time setup.
     */
    private void createNewUser() {
        currentUser = new User();
        populateUserData();
    }

    /**
     * Populate UI fields with user data.
     */
    private void populateUserData() {
        if (currentUser == null) return;

        // Personal information
        setTextSafely(binding.etFirstName, currentUser.getFirstName());
        setTextSafely(binding.etLastName, currentUser.getLastName());
        setTextSafely(binding.etNickname, currentUser.getNickname());
        setTextSafely(binding.etEmail, currentUser.getEmail());

        // Professional information
        setTextSafely(binding.etEmployeeId, currentUser.getEmployeeId());
        setTextSafely(binding.etJobTitle, currentUser.getJobTitle());
        setTextSafely(binding.etJobLevel, currentUser.getJobLevel());

        if (currentUser.getHireDate() != null) {
            binding.etHireDate.setText(currentUser.getHireDate().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        // Contact information
        setTextSafely(binding.etPhoneWork, currentUser.getPhoneWork());
        setTextSafely(binding.etPhonePersonal, currentUser.getPhonePersonal());
        setTextSafely(binding.etTeamName, currentUser.getTeamName());

        // Update profile image
        updateProfileImage();

        // Update authentication status
        updateAuthenticationStatus();

        // Update header info
        updateHeaderInfo();

        // Load organizational data based on current user
        loadUserOrganizationalData();

        hasUnsavedChanges = false;
        updateSaveButtonState();
    }

    /**
     * Safely set text to avoid null pointer exceptions.
     */
    private void setTextSafely(TextInputEditText editText, String text) {
        if (editText != null) {
            editText.setText(text != null ? text : "");
        }
    }

    /**
     * Safely set text to avoid null pointer exceptions.
     */
    private void setTextSafely(AutoCompleteTextView editText, String text) {
        if (editText != null) {
            editText.setText(text != null ? text : "");
        }
    }

    /**
     * Update profile image from user data.
     */
    private void updateProfileImage() {
        if (currentUser == null) return;

        if (currentUser.hasProfileImage()) {
            Glide.with(this)
                    .load(currentUser.getProfileImageUrl())
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_person_placeholder)
                    .error(R.drawable.ic_person_placeholder)
                    .into(binding.ivProfileImage);
        } else {
            binding.ivProfileImage.setImageResource(R.drawable.ic_person_placeholder);
        }
    }

    /**
     * Update authentication status display.
     */
    private void updateAuthenticationStatus() {
        if (currentUser == null) return;

        if (currentUser.hasGoogleAuth()) {
            binding.layoutAuthStatus.setVisibility(View.VISIBLE);
            binding.btnGoogleSignin.setText(R.string.google_account_connected);
            binding.btnGoogleSignin.setEnabled(false);
            binding.ivAuthStatus.setImageResource(R.drawable.ic_check_circle);
            binding.tvAuthStatus.setText(R.string.authenticated_with_google);
        } else {
            binding.layoutAuthStatus.setVisibility(View.GONE);
            binding.btnGoogleSignin.setText(R.string.sign_in_with_google);
            binding.btnGoogleSignin.setEnabled(true);
        }
    }

    /**
     * Update header information (name and image).
     */
    private void updateHeaderInfo() {
        if (currentUser == null) return;

        String displayName = currentUser.getDisplayName();
        if (displayName != null && !displayName.trim().isEmpty()) {
            binding.tvUserName.setText(displayName);
        } else {
            binding.tvUserName.setText(R.string.user_name_placeholder);
        }
    }

    /**
     * Load organizational data specific to current user.
     */
    private void loadUserOrganizationalData() {
        if (currentUser == null || userWithOrganization == null) return;

        // Set establishment if user has one
        if (userWithOrganization.establishment != null) {
            binding.etEstablishment.setText(userWithOrganization.establishment.getName());
            loadMacroDepartments(userWithOrganization.establishment.getId());
        }

        // Set macro department if user has one
        if (userWithOrganization.macroDepartment != null) {
            binding.etMacroDepartment.setText(userWithOrganization.macroDepartment.getName());
            loadSubDepartments(userWithOrganization.macroDepartment.getId());
        }

        // Set sub department if user has one
        if (userWithOrganization.subDepartment != null) {
            binding.etSubDepartment.setText(userWithOrganization.subDepartment.getName());
        }
    }

    /**
     * Handle Google Sign-In button click.
     */
    private void handleGoogleSignIn() {
        if (currentUser != null && currentUser.hasGoogleAuth()) {
            // Already authenticated, show options
            showGoogleAccountOptions();
        } else {
            // Start Google Sign-In flow
            googleAuthService.signIn(this);
        }
    }

    /**
     * Show options for already connected Google account.
     */
    private void showGoogleAccountOptions() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.google_account_options)
                .setMessage(R.string.google_account_connected_message)
                .setPositiveButton(R.string.disconnect, (dialog, which) -> disconnectGoogleAccount())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Disconnect Google account.
     */
    private void disconnectGoogleAccount() {
        googleAuthService.signOut(new GoogleAuthService.OnSignOutListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    if (currentUser != null) {
                        currentUser.setGoogleId(null);
                        currentUser.setAuthProvider("manual");
                        currentUser.setProfileImageSource("none");
                        // Keep profile image URL but change source
                        updateAuthenticationStatus();
                        hasUnsavedChanges = true;
                        updateSaveButtonState();

                        Snackbar.make(binding.getRoot(),
                                R.string.google_account_disconnected,
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error disconnecting Google account: " + e.getMessage());
                    Toast.makeText(UserProfileActivity.this,
                            R.string.error_disconnecting_google,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Save user profile data.
     */
    private void saveProfile() {
        if (!validateInput()) {
            return;
        }

        showLoading(true);

        // Update user object with form data
        updateUserFromForm();

        userManager.updateUserProfile(currentUser, new UserManager.OnProfileUpdateListener() {
            @Override
            public void onSuccess(User user, boolean profileCompleted) {
                runOnUiThread(() -> {
                    currentUser = user;
                    hasUnsavedChanges = false;
                    updateSaveButtonState();
                    showLoading(false);

                    String message = profileCompleted ?
                            getString(R.string.profile_completed_successfully) :
                            getString(R.string.profile_saved_successfully);

                    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();

                    // Update header info
                    updateHeaderInfo();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error saving profile: " + e.getMessage());
                    showLoading(false);

                    Toast.makeText(UserProfileActivity.this,
                            R.string.error_saving_profile,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Validate user input before saving.
     */
    private boolean validateInput() {
        boolean isValid = true;

        // Clear previous errors
        clearFieldErrors();

        // Date validation
        String hireDateText = getTextFromField(binding.etHireDate);
        if (!hireDateText.isEmpty()) {
            try {
                LocalDate.parse(hireDateText, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException e) {
                setFieldError(binding.tilHireDate, R.string.error_date_invalid);
                isValid = false;
            }
        }

        return isValid;

        /*// Required fields validation
        if (isEmpty(binding.etFirstName)) {
            setFieldError(binding.tilFirstName, R.string.error_first_name_required);
            isValid = false;
        }

        if (isEmpty(binding.etLastName)) {
            setFieldError(binding.tilLastName, R.string.error_last_name_required);
            isValid = false;
        }

        if (isEmpty(binding.etEmail)) {
            setFieldError(binding.tilEmail, R.string.error_email_required);
            isValid = false;
        } else if (!isValidEmail(getTextFromField(binding.etEmail))) {
            setFieldError(binding.tilEmail, R.string.error_email_invalid);
            isValid = false;
        }

        if (isEmpty(binding.etEstablishment)) {
            setFieldError( binding.tilEstablishment, R.string.error_establishment_required);
            isValid = false;
        }

        if (isEmpty(binding.etMacroDepartment)) {
            setFieldError(binding.tilMacroDepartment, R.string.error_macro_department_required);
            isValid = false;
        }



        return isValid;*/
    }

    /**
     * Update user object with data from form fields.
     */
    private void updateUserFromForm() {
        if (currentUser == null) return;

        // Personal information
        currentUser.setFirstName(getTextFromField(binding.etFirstName));
        currentUser.setLastName(getTextFromField(binding.etLastName));
        currentUser.setNickname(getTextFromField(binding.etNickname));
        currentUser.setEmail(getTextFromField(binding.etEmail));

        // Professional information
        currentUser.setEmployeeId(getTextFromField(binding.etEmployeeId));
        currentUser.setJobTitle(getTextFromField(binding.etJobTitle));
        currentUser.setJobLevel(getTextFromField(binding.etJobLevel));

        // Hire date
        String hireDateText = getTextFromField(binding.etHireDate);
        if (!hireDateText.isEmpty()) {
            try {
                LocalDate hireDate = LocalDate.parse(hireDateText,
                        DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                currentUser.setHireDate(hireDate);
            } catch (DateTimeParseException e) {
                // Error already handled in validation
            }
        }

        // Contact information
        currentUser.setPhoneWork(getTextFromField(binding.etPhoneWork));
        currentUser.setPhonePersonal(getTextFromField(binding.etPhonePersonal));
        currentUser.setTeamName(getTextFromField(binding.etTeamName));

        // Organizational information (IDs will be set by dropdown selections)
    }

    /**
     * Reset form to original data.
     */
    private void resetChanges() {
        if (!hasUnsavedChanges) {
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reset_changes)
                .setMessage(R.string.reset_changes_confirmation)
                .setPositiveButton(R.string.reset, (dialog, which) -> {
                    populateUserData();
                    hasUnsavedChanges = false;
                    updateSaveButtonState();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Handle profile image change.
     */
    private void changeProfileImage() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.change_profile_image)
                .setItems(new String[]{
                        getString(R.string.choose_from_gallery),
                        getString(R.string.remove_image)
                }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openImagePicker();
                            break;
                        case 1:
                            removeProfileImage();
                            break;
                    }
                })
                .show();
    }

    /**
     * Open image picker to select profile image.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    /**
     * Remove current profile image.
     */
    private void removeProfileImage() {
        if (currentUser != null) {
            currentUser.setProfileImageUrl(null);
            currentUser.setProfileImageSource("none");
            updateProfileImage();
            hasUnsavedChanges = true;
            updateSaveButtonState();
        }
    }

    /**
     * Show date picker for hire date.
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        // Set current date if hire date is already set
        if (currentUser != null && currentUser.getHireDate() != null) {
            LocalDate hireDate = currentUser.getHireDate();
            calendar.set(hireDate.getYear(), hireDate.getMonthValue() - 1, hireDate.getDayOfMonth());
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    LocalDate selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    String formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    binding.etHireDate.setText(formattedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    /**
     * Handle establishment selection.
     */
    private void onEstablishmentSelected(int position) {
        if (position >= 0 && position < establishments.size()) {
            Establishment selected = establishments.get(position);
            if (currentUser != null) {
                currentUser.setEstablishmentId(selected.getId());
                // Clear dependent selections
                currentUser.setMacroDepartmentId(null);
                currentUser.setSubDepartmentId(null);
                binding.etMacroDepartment.setText("");
                binding.etSubDepartment.setText("");

                // Load macro departments for selected establishment
                loadMacroDepartments(selected.getId());

                hasUnsavedChanges = true;
                updateSaveButtonState();
            }
        }
    }

    /**
     * Handle macro department selection.
     */
    private void onMacroDepartmentSelected(int position) {
        if (position >= 0 && position < macroDepartments.size()) {
            MacroDepartment selected = macroDepartments.get(position);
            if (currentUser != null) {
                currentUser.setMacroDepartmentId(selected.getId());
                // Clear dependent selection
                currentUser.setSubDepartmentId(null);
                binding.etSubDepartment.setText("");

                // Load sub departments for selected macro department
                loadSubDepartments(selected.getId());

                hasUnsavedChanges = true;
                updateSaveButtonState();
            }
        }
    }

    /**
     * Load macro departments for selected establishment.
     */
    private void loadMacroDepartments(long establishmentId) {
        organizationRepository.getMacroDepartmentsByEstablishment(establishmentId,
                new OrganizationRepository.OnMacroDepartmentListListener() {
                    @Override
                    public void onSuccess(List<MacroDepartment> departments) {
                        runOnUiThread(() -> {
                            macroDepartments = departments;
                            updateMacroDepartmentDropdown();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error loading macro departments: " + e.getMessage());
                    }
                });
    }

    /**
     * Load sub departments for selected macro department.
     */
    private void loadSubDepartments(long macroDepartmentId) {
        organizationRepository.getSubDepartmentsByMacroDepartment(macroDepartmentId,
                new OrganizationRepository.OnSubDepartmentListListener() {
                    @Override
                    public void onSuccess(List<SubDepartment> departments) {
                        runOnUiThread(() -> {
                            subDepartments = departments;
                            updateSubDepartmentDropdown();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error loading sub departments: " + e.getMessage());
                    }
                });
    }

    /**
     * Update establishment dropdown adapter.
     */
    private void updateEstablishmentDropdown() {
        List<String> names = new ArrayList<>();
        for (Establishment establishment : establishments) {
            names.add(establishment.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names);
        ((AutoCompleteTextView) binding.etEstablishment).setAdapter(adapter);
    }

    /**
     * Update macro department dropdown adapter.
     */
    private void updateMacroDepartmentDropdown() {
        List<String> names = new ArrayList<>();
        for (MacroDepartment department : macroDepartments) {
            names.add(department.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names);
        ((AutoCompleteTextView) binding.etMacroDepartment).setAdapter(adapter);
    }

    /**
     * Update sub department dropdown adapter.
     */
    private void updateSubDepartmentDropdown() {
        List<String> names = new ArrayList<>();
        for (SubDepartment department : subDepartments) {
            names.add(department.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names);
        ((AutoCompleteTextView) binding.etSubDepartment).setAdapter(adapter);
    }

    /**
     * Update save button state based on changes and validation.
     */
    private void updateSaveButtonState() {
        binding.btnSaveProfile.setEnabled(hasUnsavedChanges);
        binding.btnResetProfile.setEnabled(hasUnsavedChanges);
    }

    /**
     * Show/hide loading indicator.
     */
    private void showLoading(boolean show) {
        // You can implement a progress dialog or overlay here
        binding.btnSaveProfile.setEnabled(!show && hasUnsavedChanges);
    }

    // Helper methods
    private boolean isEmpty(TextInputEditText editText) {
        return getTextFromField(editText).trim().isEmpty();
    }
    private boolean isEmpty(AutoCompleteTextView editText) {
        return getTextFromField(editText).trim().isEmpty();
    }

    private String getTextFromField(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }
    private String getTextFromField(AutoCompleteTextView editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void setFieldError(TextInputLayout layout, int messageResId) {
        layout.setError(getString(messageResId));
    }

    private void setFieldError(AutoCompleteTextView layout, int messageResId) {
        layout.setError(getString(messageResId));
    }

    private void clearFieldErrors() {
        binding.tilFirstName.setError(null);
        binding.tilLastName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilEstablishment.setError(null);
        binding.tilMacroDepartment.setError(null);
        binding.tilHireDate.setError(null);
    }

    // Activity result handling
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GoogleAuthService.RC_SIGN_IN) {
            handleGoogleSignInResult(data);
        } else if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            handleImagePickResult(data);
        }
    }

    /**
     * Handle Google Sign-In result.
     */
    private void handleGoogleSignInResult(Intent data) {
        googleAuthService.handleSignInResult(data, new GoogleAuthService.OnGoogleAuthListener() {
            @Override
            public void onSuccess(GoogleAuthData authData) {
                runOnUiThread(() -> {
                    // Update user with Google data
                    if (currentUser != null) {
                        authData.updateUser(currentUser);
                        populateUserData();
                        hasUnsavedChanges = true;
                        updateSaveButtonState();

                        Snackbar.make(binding.getRoot(),
                                R.string.google_signin_successful,
                                Snackbar.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Google Sign-In failed: " + e.getMessage());
                    Toast.makeText(UserProfileActivity.this,
                            R.string.google_signin_failed,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Handle image pick result.
     */
    private void handleImagePickResult(Intent data) {
        Uri imageUri = data.getData();
        if (imageUri != null && currentUser != null) {
            // For simplicity, store the URI as string
            // In production, you'd want to copy the image to app storage
            currentUser.setProfileImageUrl(imageUri.toString());
            currentUser.setProfileImageSource("local");
            updateProfileImage();
            hasUnsavedChanges = true;
            updateSaveButtonState();
        }
    }

    // Menu and navigation handling
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.unsaved_changes)
                    .setMessage(R.string.unsaved_changes_message)
                    .setPositiveButton(R.string.save, (dialog, which) -> saveProfile())
                    .setNegativeButton(R.string.discard, (dialog, which) -> super.onBackPressed())
                    .setNeutralButton(R.string.cancel, null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) {
            binding = null;
        }
    }
}