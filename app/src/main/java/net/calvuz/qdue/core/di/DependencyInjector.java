package net.calvuz.qdue.core.di;

import android.content.Context;

import net.calvuz.qdue.core.di.impl.ServiceProviderImpl;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * Dependency injection helper for activities and fragments
 */
public class DependencyInjector {

    private static final String TAG = "DependencyInjector";

    /**
     * Inject dependencies into an Injectable component
     */
    public static void inject(Injectable injectable, Context context) {
        if (injectable == null) {
            Log.w(TAG, "Injectable component is null, skipping injection");
            return;
        }

        try {
            ServiceProvider serviceProvider = ServiceProviderImpl.getInstance(context);
            serviceProvider.initializeServices();
            injectable.inject(serviceProvider);

            Log.d(TAG, "✅ Dependencies injected successfully into " +
                    injectable.getClass().getSimpleName());

        } catch (Exception e) {
            Log.e(TAG, "❌ Error injecting dependencies: " + e.getMessage());
            throw new RuntimeException("Dependency injection failed", e);
        }
    }

    /**
     * Check if dependencies are properly injected
     */
    public static boolean verifyInjection(Injectable injectable, Context context) {
        if (injectable == null) {
            return false;
        }

        try {
            ServiceProvider serviceProvider = ServiceProviderImpl.getInstance(context);
            return serviceProvider.areServicesReady() && injectable.areDependenciesReady();
        } catch (Exception e) {
            Log.e(TAG, "Error verifying injection: " + e.getMessage());
            return false;
        }
    }
}
