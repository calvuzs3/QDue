package net.calvuz.qdue.smartshifts.di;

import android.content.Context;
import android.content.SharedPreferences;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

import net.calvuz.qdue.smartshifts.di.qualifiers.BackgroundExecutor;
import net.calvuz.qdue.smartshifts.di.qualifiers.SystemPreferences;
import net.calvuz.qdue.smartshifts.di.qualifiers.UserPreferences;
import net.calvuz.qdue.smartshifts.di.qualifiers.MainThreadExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

/**
 * Hilt module for providing application-level dependencies
 * Manages preferences, executors, and other app-wide components
 */
@Module
@InstallIn(SingletonComponent.class)
public class ApplicationModule {

    private static final String USER_PREFERENCES_NAME = "smartshifts_user_prefs";
    private static final String SYSTEM_PREFERENCES_NAME = "smartshifts_system_prefs";

    /**
     * Provide user preferences
     */
    @Provides
    @Singleton
    @UserPreferences
    public SharedPreferences provideUserPreferences(@ApplicationContext Context context) {
        return context.getSharedPreferences(USER_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Provide system preferences
     */
    @Provides
    @Singleton
    @SystemPreferences
    public SharedPreferences provideSystemPreferences(@ApplicationContext Context context) {
        return context.getSharedPreferences(SYSTEM_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Provide background executor for database operations
     */
    @Provides
    @Singleton
    @BackgroundExecutor
    public Executor provideBackgroundExecutor() {
        return Executors.newFixedThreadPool(4);
    }

    /**
     * Provide main thread executor for UI updates
     */
    @Provides
    @Singleton
    @net.calvuz.qdue.smartshifts.di.qualifiers.MainThreadExecutor
    public Executor provideMainThreadExecutor() {
        return new MainThreadExecutor();
    }

    /**
     * Main thread executor implementation
     */
    private static class MainThreadExecutor implements Executor {
        private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            mainHandler.post(command);
        }
    }
}