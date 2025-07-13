package net.calvuz.qdue.smartshifts;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;

import net.calvuz.qdue.smartshifts.data.database.SmartShiftsDatabase;
import net.calvuz.qdue.smartshifts.data.database.DatabaseInitializer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Application class for SmartShifts with Hilt integration
 * Handles app-level initialization and dependency injection setup
 */
@HiltAndroidApp
public class SmartShiftsApplication extends Application {

    private static final String TAG = "SmartShiftsApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize database with localized strings
        // This runs in background thread to avoid blocking UI
        SmartShiftsDatabase.databaseWriteExecutor.execute(() -> {
            SmartShiftsDatabase database = SmartShiftsDatabase.getDatabase(this);
            DatabaseInitializer.initializeWithLocalizedStrings(database, this);
        });
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // Clean up database resources
        SmartShiftsDatabase.closeDatabase();
    }
}
