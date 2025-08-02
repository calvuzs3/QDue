package net.calvuz.qdue.ui.features.calendar.models;

/**
 * CalendarLoadingState - Represents loading state for calendar data.
 */
public enum CalendarLoadingState {
    /**
     * Initial state - no data loaded yet.
     */
    IDLE,

    /**
     * Data is being loaded.
     */
    LOADING,

    /**
     * Data loaded successfully.
     */
    LOADED,

    /**
     * Error occurred during loading.
     */
    ERROR,

    /**
     * Data is being refreshed.
     */
    REFRESHING;

    /**
     * Check if currently in a loading state.
     */
    public boolean isLoading() {
        return this == LOADING || this == REFRESHING;
    }

    /**
     * Check if data is available.
     */
    public boolean hasData() {
        return this == LOADED || this == REFRESHING;
    }

    /**
     * Check if in error state.
     */
    public boolean hasError() {
        return this == ERROR;
    }
}
