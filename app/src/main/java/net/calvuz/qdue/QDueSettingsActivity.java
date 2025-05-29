package net.calvuz.qdue;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import net.calvuz.qdue.ui.settings.SettingsFragment;

/**
 * Activity dedicata per le impostazioni con ActionBar nativa.
 */
public class QDueSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qdue_settings);

        // Configura ActionBar con pulsante indietro
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.menu_title_settings);
        }

        // Carica il SettingsFragment
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Pulsante indietro premuto
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}