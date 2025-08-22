package net.calvuz.qdue.user.ui;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import net.calvuz.qdue.smartshifts.ui.main.SmartshiftsActivity;

public class UserProfileLauncher  {

    /**
     * Launch the activity associated with this launcher.
     *
     * @param fromActivity The activity launching the target activity.
     */
    public static void launch(AppCompatActivity fromActivity) {
        Intent intent = new Intent(fromActivity, UserProfileActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        fromActivity.startActivity(intent);
    }

    private UserProfileLauncher() {
        throw new UnsupportedOperationException( "This class cannot be instantiated" );
    }

    /**
     * Check if SmartShifts is available and properly configured
     */
    public static boolean isAvailable() {
        // Add any availability checks here
        // For example, check if database is initialized
        return true;
    }

    /**
     * Launch SmartShifts with specific configuration
     */
    public static void launchWithConfig(AppCompatActivity fromActivity, String config) {
        Intent intent = new Intent( fromActivity, SmartshiftsActivity.class );
        intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        intent.putExtra( "config", config );
        fromActivity.startActivity( intent );
    }


}
