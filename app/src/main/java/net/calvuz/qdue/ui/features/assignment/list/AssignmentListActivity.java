package net.calvuz.qdue.ui.features.assignment.list;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.di.DependencyInjector;
import net.calvuz.qdue.core.di.Injectable;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.data.services.QDueUserService;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.services.UserSchedulePatternService;
import net.calvuz.qdue.domain.qdueuser.models.QDueUser;
import net.calvuz.qdue.ui.features.assignment.wizard.PatternAssignmentWizardActivity;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * AssignmentListActivity - User Schedule Assignment Management
 *
 * <p>Activity for managing user schedule assignments with CRUD operations.
 * Provides list view of user's assignments with create, edit, and delete capabilities.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li><strong>Assignment List</strong>: Chronological view of user assignments</li>
 *   <li><strong>Create Assignment</strong>: FAB to create new assignment via wizard</li>
 *   <li><strong>Edit Assignment</strong>: Edit existing assignments via wizard</li>
 *   <li><strong>Delete Assignment</strong>: Delete with confirmation dialog</li>
 *   <li><strong>Status Display</strong>: Visual status indicators (ACTIVE/PENDING/EXPIRED)</li>
 * </ul>
 *
 * <h3>Navigation:</h3>
 * <ul>
 *   <li><strong>Entry</strong>: Main menu or drawer navigation</li>
 *   <li><strong>Create Flow</strong>: PatternAssignmentWizardActivity for creation</li>
 *   <li><strong>Edit Flow</strong>: PatternAssignmentWizardActivity for editing</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Assignment Management
 * @since Clean Architecture Phase 2
 */
public class AssignmentListActivity extends AppCompatActivity implements Injectable {

    private static final String TAG = "AssignmentListActivity";

    // Request codes for wizard navigation
    public static final int REQUEST_CREATE_ASSIGNMENT = 3001;
    public static final int REQUEST_EDIT_ASSIGNMENT = 3002;

    // ==================== INJECTED DEPENDENCIES ====================

    private UserSchedulePatternService mPatternService;
    private QDueUserService mQDueUserService;
    private ServiceProvider mServiceProvider;

    // ==================== STATE ====================

    private AssignmentListFragment mListFragment;
    private QDueUser mCurrentUser;

    // ==================== LIFECYCLE ====================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Dependency injection
        DependencyInjector.inject(this, this);

//        if (!areDependenciesReady()) {
//            Log.e(TAG, "Dependencies not ready, finishing activity");
//            finish();
//            return;
//        }

        // Initialize current user
        initializeCurrentUser();

        // Set up UI
        setContentView(R.layout.activity_assignment_list);
        setupActionBar();
        setupFragment();

        Log.d(TAG, "AssignmentListActivity created for user: " + mCurrentUser );
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh assignments when returning from wizard
        if (mListFragment != null) {
            mListFragment.refreshAssignments();
        }
    }

    // ==================== DEPENDENCY INJECTION ====================

    @Override
    public void inject(@NonNull ServiceProvider serviceProvider) {
        try {
            this.mServiceProvider = serviceProvider;
            this.mQDueUserService = serviceProvider.getQDueUserService();

            // Get pattern service from CalendarService
            this.mPatternService = serviceProvider.getCalendarService()
                    .getCalendarServiceProvider()
                    .getUserSchedulePatternService();

            Log.d(TAG, "Dependencies injected successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error injecting dependencies", e);
            throw new RuntimeException("Dependency injection failed", e);
        }
    }

    @Override
    public boolean areDependenciesReady() {
        return mServiceProvider != null &&
                mQDueUserService != null &&
                mPatternService != null &&
                mCurrentUser != null;
    }

    // ==================== UI SETUP ====================

    /**
     * Initialize current user ID from UserService.
     */
    private void initializeCurrentUser() {
        try {
            // Get current user ID (assuming UserService provides this)
            // This might need adjustment based on your UserService implementation
            mCurrentUser = getCurrentUser();

            if (mCurrentUser == null) {
                Log.e(TAG, "Current user ID is null");
                finish();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting current user", e);
            finish();
        }
    }

    /**
     * Get current user ID from UserService.
     * Adjust this method based on your UserService implementation.
     */
    @Nullable
    private QDueUser getCurrentUser() {
        try {

            QDueUser _user;

            OperationResult<QDueUser> result =  mQDueUserService.getPrimaryUser().join();
            if (!result.isSuccess()) {
                Log.e(TAG, "Error getting current user: " + result.getErrorMessage());
                return null;
            } else {
                _user = result.getData();
                if (_user == null) {
                    Log.e(TAG, "Current user retrieved is null");
                    return null;
                }
                Log.d(TAG, _user.toString() );
            }

            return _user;
        } catch (Exception e) {
            Log.e(TAG, "Error getting current user ID", e);
            return null;
        }
    }

    /**
     * Setup action bar with back navigation.
     */
    private void setupActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.assignment_list_title);
        }
    }

    /**
     * Setup assignment list fragment.
     */
    private void setupFragment() {
        mListFragment = AssignmentListFragment.newInstance( mCurrentUser );

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, mListFragment)
                .commit();
    }

    // ==================== NAVIGATION ====================

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CREATE_ASSIGNMENT:
                    Log.d(TAG, "Assignment created successfully");
                    if (mListFragment != null) {
                        mListFragment.refreshAssignments();
                    }
                    break;

                case REQUEST_EDIT_ASSIGNMENT:
                    Log.d(TAG, "Assignment edited successfully");
                    if (mListFragment != null) {
                        mListFragment.refreshAssignments();
                    }
                    break;
            }
        }
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * Start create assignment flow.
     */
    public void startCreateAssignment() {
        Intent intent = new Intent(this, PatternAssignmentWizardActivity.class);
        startActivityForResult(intent, REQUEST_CREATE_ASSIGNMENT);
    }

    /**
     * Start edit assignment flow.
     *
     * @param assignmentId Assignment ID to edit
     */
    public void startEditAssignment(@NonNull String assignmentId) {
        Intent intent = new Intent(this, PatternAssignmentWizardActivity.class);
        intent.putExtra("assignment_id", assignmentId);
        intent.putExtra("edit_mode", true);
        startActivityForResult(intent, REQUEST_EDIT_ASSIGNMENT);
    }

    // ==================== GETTERS ====================

    /**
     * Get pattern service for use by fragments.
     */
    @NonNull
    public UserSchedulePatternService getPatternService() {
        return mPatternService;
    }
}