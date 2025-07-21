package net.calvuz.qdue.smartshifts.ui.setup;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import net.calvuz.qdue.smartshifts.domain.usecases.AssignPatternUseCase;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for setup wizard
 * Manages setup flow and data collection
 */
@HiltViewModel
public class SetupWizardViewModel extends ViewModel {

    private final AssignPatternUseCase assignPatternUseCase;

    private final MutableLiveData<Boolean> isSetupComplete = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> canProceed = new MutableLiveData<>(true);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Setup data
    private String selectedPatternId;
    private LocalDate selectedStartDate;
    private boolean isCustomPattern = false;

    // Current user ID - in real app would come from authentication
    private static final String CURRENT_USER_ID = "user_default";

    @Inject
    public SetupWizardViewModel(AssignPatternUseCase assignPatternUseCase) {
        this.assignPatternUseCase = assignPatternUseCase;
    }

    /**
     * Set selected pattern
     */
    public void setSelectedPattern(String patternId, boolean isCustom) {
        this.selectedPatternId = patternId;
        this.isCustomPattern = isCustom;
        updateCanProceed();
    }

    /**
     * Set selected start date
     */
    public void setSelectedStartDate(LocalDate startDate) {
        this.selectedStartDate = startDate;
        updateCanProceed();
    }

    /**
     * Validate current step
     */
    public boolean validateCurrentStep(int stepIndex) {
        switch (stepIndex) {
            case 0: // Welcome step
                return true;
            case 1: // Pattern selection
                return selectedPatternId != null;
            case 2: // Start date
                return selectedStartDate != null;
            case 3: // Confirmation
                return true;
            default:
                return false;
        }
    }

    /**
     * Complete setup process
     */
    public void completeSetup() {
        if (selectedPatternId == null || selectedStartDate == null) {
            errorMessage.setValue("Configurazione incompleta");
            return;
        }

        CompletableFuture<AssignPatternUseCase.AssignmentResult> future =
                assignPatternUseCase.assignPatternToUser(CURRENT_USER_ID, selectedPatternId, selectedStartDate);

        future.thenAccept(result -> {
            if (result.success) {
                isSetupComplete.postValue(true);
            } else {
                errorMessage.postValue(result.message);
            }
        }).exceptionally(throwable -> {
            errorMessage.postValue("Errore durante la configurazione: " + throwable.getMessage());
            return null;
        });
    }

    /**
     * Update can proceed state
     */
    private void updateCanProceed() {
        canProceed.setValue(true);
    }

    // Getters
    public LiveData<Boolean> isSetupComplete() {
        return isSetupComplete;
    }

    public LiveData<Boolean> canProceed() {
        return canProceed;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }

    public String getSelectedPatternId() {
        return selectedPatternId;
    }

    public LocalDate getSelectedStartDate() {
        return selectedStartDate;
    }

    public boolean isCustomPattern() {
        return isCustomPattern;
    }
}