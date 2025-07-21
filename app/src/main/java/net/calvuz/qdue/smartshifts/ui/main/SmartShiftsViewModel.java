package net.calvuz.qdue.smartshifts.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import net.calvuz.qdue.smartshifts.domain.usecases.GetUserShiftsUseCase;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for SmartShifts main activity
 * Manages app-level state and navigation
 */
@HiltViewModel
public class SmartShiftsViewModel extends ViewModel {

    private final GetUserShiftsUseCase getUserShiftsUseCase;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Current user ID - in real app this would come from authentication
    private static final String CURRENT_USER_ID = "user_default";

    @Inject
    public SmartShiftsViewModel(GetUserShiftsUseCase getUserShiftsUseCase) {
        this.getUserShiftsUseCase = getUserShiftsUseCase;
    }

    /**
     * Check if user has active assignment
     */
    public LiveData<Boolean> hasActiveAssignment() {
        return getUserShiftsUseCase.hasActiveAssignment(CURRENT_USER_ID);
    }

    /**
     * Check if this is first time setup
     */
    public void checkFirstTimeSetup() {
        // This will trigger the observer in the activity
        // If user has no assignment, setup wizard will be shown
    }

    /**
     * Get loading state
     */
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    /**
     * Set loading state
     */
    public void setLoading(boolean loading) {
        isLoading.setValue(loading);
    }

    /**
     * Get error message
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Set error message
     */
    public void setErrorMessage(String message) {
        errorMessage.setValue(message);
    }

    /**
     * Clear error message
     */
    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }

    /**
     * Get current user ID
     */
    public String getCurrentUserId() {
        return CURRENT_USER_ID;
    }
}