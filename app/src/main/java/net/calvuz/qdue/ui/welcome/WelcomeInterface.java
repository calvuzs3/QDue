package net.calvuz.qdue.ui.welcome;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

import net.calvuz.qdue.QDue;

public interface WelcomeInterface {


    /**
     * Get SharedPreferences instance
     * @return SharedPreferences instance
     */
    abstract public SharedPreferences getPreferences();

    /**
     * Set selected team preference
     * @param teamIndex Selected team ID
     */
    abstract void setSelectedTeam(int teamIndex);

    /**
     * Get selected team preference
     * @return Selected team ID or default (1)
     */
    abstract public int getSelectedTeam();

    /**
     * Set view mode preference
     * @param viewMode View mode to set
     */
    abstract public void setViewMode( String viewMode);

    /**
     * Get view mode preference
     * @return View mode or default ("dayslist")
     */
    abstract public String getViewMode();

    /**
     * Set dynamic colors enabled preference
     * @param enabled True if enabled, false otherwise
     */
    abstract public void setDynamicColorsEnabled( boolean enabled);

    /**
     * Get dynamic colors enabled preference
     * @return True if enabled, false otherwise
     */
    abstract public boolean isDynamicColorsEnabled();

    /**
     * Set welcome as completed
     */
    abstract public void setWelcomeCompleted();

    /**
     * Check if welcome was completed
     * @return True if completed, false otherwise
     */
    public static boolean isWelcomeCompleted(AppCompatActivity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(QDue.Settings.QD_PREF_NAME, MODE_PRIVATE);
        return prefs.getBoolean(QDue.Settings.QD_KEY_WELCOME_COMPLETED, false);
    }
}
