package net.calvuz.qdue;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;

import androidx.core.view.GravityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import net.calvuz.qdue.databinding.ActivityQdueMainBinding;
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.ui.dayslist.DayslistViewFragment;
import net.calvuz.qdue.ui.other.DialogAbout;
import net.calvuz.qdue.ui.settings.SettingsActivity;
import net.calvuz.qdue.utils.Log;

/**
 * Activity principale dell'applicazione.
 * Aggiornata per rimuovere i pulsanti di navigazione mese non più necessari
 * con lo scrolling infinito.
 */
public class QDueMainActivity extends AppCompatActivity
        implements DayslistViewFragment.OnQuattroDueHomeFragmentInteractionListener {

    private static final String TAG = "QDueMainActivity";
    private static final boolean LOG_ENABLED = true;

    // UI related
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityQdueMainBinding mBinding;

    // App Engine reference
    private QuattroDue mQD;

    // Fragment di riferimento
    private Fragment mCurrentFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LOG_ENABLED) Log.d(TAG, "onCreate");

        // Inizializza QuattroDue
        mQD = QuattroDue.getInstance(getApplicationContext());

        // Inizializza il binding e il layout
        mBinding = ActivityQdueMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        // Configura la toolbar
        setSupportActionBar(mBinding.appBarMain.toolbar);

        // Configura il drawer layout
        DrawerLayout drawer = mBinding.drawerLayout;
        NavigationView navigationView = mBinding.navView;

        // Configurazione del Navigation Controller con il drawer
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_settings)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Imposta un listener personalizzato invece di setupWithNavController
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_settings) {
                // Lancia l'activity Settings
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
            } else if (id == R.id.nav_about) {
                // Mostra il dialog fragment About
                DialogAbout aboutDialog = new DialogAbout();
                aboutDialog.show(getSupportFragmentManager(), "AboutDialogFragment");
            } else {
                // Usa il Navigation Controller per le altre destinazioni
                drawer.closeDrawer(GravityCompat.START);
                return NavigationUI.onNavDestinationSelected(item, navController);
            }

            // Chiudi il drawer dopo la selezione
            drawer.closeDrawer(GravityCompat.START);
            return true;
        });

        // Aggiorna il titolo della toolbar con il nome dell'app
        updateToolbarTitle();

        // Tieni traccia del fragment corrente
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            mCurrentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
            if (LOG_ENABLED)
                Log.d(TAG, "Fragment changed: " + (mCurrentFragment != null ? mCurrentFragment.getClass().getSimpleName() : "null"));
        });
    }

    /**
     * Aggiorna il titolo della toolbar.
     * Con lo scrolling infinito, mostriamo il nome dell'app invece del mese corrente.
     */
    private void updateToolbarTitle() {
        if (LOG_ENABLED) Log.d(TAG, "updateToolbarTitle");

        if (getSupportActionBar() != null) {
            // Imposta il titolo dell'app
            getSupportActionBar().setTitle(R.string.app_name);
            if (LOG_ENABLED) Log.d(TAG, "Titolo toolbar aggiornato con nome app");
        } else {
            if (LOG_ENABLED) Log.e(TAG, "supportActionBar è null");
        }
    }

    /**
     * Notifica il fragment di aggiornare i dati.
     */
    private void notifyUpdates() {
        if (LOG_ENABLED) Log.d(TAG, "notifyUpdates");

        // Cerca il fragment attivo
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        if (fragment != null) {
            // Se è un NavHostFragment, cerca il fragment figlio attivo
            Fragment childFragment = ((androidx.navigation.fragment.NavHostFragment) fragment).getChildFragmentManager().getPrimaryNavigationFragment();

            if (childFragment instanceof DayslistViewFragment) {
                ((DayslistViewFragment) childFragment).notifyUpdates();
                if (LOG_ENABLED) Log.d(TAG, "DayslistViewFragment.notifyUpdates chiamato");
            } else {
                if (LOG_ENABLED) Log.d(TAG, "Fragment attivo non è DayslistViewFragment: " +
                        (childFragment != null ? childFragment.getClass().getSimpleName() : "null"));
            }
        } else {
            if (LOG_ENABLED) Log.e(TAG, "Fragment non trovato");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent mSettingsIntent;
        int mID = item.getItemId();

        if (mID == R.id.action_settings) {
            if (LOG_ENABLED) Log.d(TAG, "Menu settings selezionato");
            mSettingsIntent = new Intent(QDueMainActivity.this, SettingsActivity.class);
            startActivity(mSettingsIntent);
            return true;
        }
        if (mID == R.id.action_about) {
            if (LOG_ENABLED) Log.d(TAG, "Menu about selezionato");
            DialogFragment newFragment = new DialogAbout();
            newFragment.setCancelable(true);
            newFragment.show(getSupportFragmentManager(), "about");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (LOG_ENABLED) Log.d(TAG, "onResume");

        // Aggiorna l'interfaccia quando l'activity torna in primo piano
        updateToolbarTitle();
        notifyUpdates();
    }

    @Override
    public void onQuattroDueHomeFragmentInteractionListener(long id) {
        // Implementazione dell'interfaccia
        if (LOG_ENABLED) Log.d(TAG, "onQuattroDueHomeFragmentInteractionListener: " + id);
    }
}