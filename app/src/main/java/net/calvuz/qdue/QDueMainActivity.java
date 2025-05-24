package net.calvuz.qdue;

import android.annotation.SuppressLint;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import net.calvuz.qdue.databinding.ActivityQdueMainBinding;
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.ui.dayslist.DayslistViewFragment;
import net.calvuz.qdue.utils.Log;
import net.calvuz.qdue.utils.TimeChangeReceiver;

/**
 * Activity principale dell'applicazione.
 * Aggiornata per rimuovere i pulsanti di navigazione mese non più necessari
 * con lo scrolling infinito.
 */
public class QDueMainActivity extends AppCompatActivity
        implements DayslistViewFragment.OnQuattroDueHomeFragmentInteractionListener,
        TimeChangeReceiver.TimeChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "QDueMainActivity";
    private static final boolean LOG_ENABLED = true;

    // QD
    private QuattroDue mQD;

    // UI related
    private AppBarConfiguration mAppBarConfiguration;

    // Fragment di riferimento
    private Fragment mCurrentFragment = null;

    // Time Change Receiver
    private TimeChangeReceiver mTimeChangeReceiver;
    private boolean mReceiverRegistered = false;

    // Shared Preferences
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LOG_ENABLED) Log.d(TAG, "onCreate");

        // Inizializza il time change receiver
        mTimeChangeReceiver = new TimeChangeReceiver(this);

        // Registra il listener per le preferenze
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        // QuattroDue
        mQD = QuattroDue.getInstance(this);

        // Inizializza il binding e il layout
        net.calvuz.qdue.databinding.ActivityQdueMainBinding mBinding = ActivityQdueMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        // Configura la toolbar
        setSupportActionBar(mBinding.appBarMain.toolbar);

        // Configura il drawer layout
        DrawerLayout drawer = mBinding.drawerLayout;
        NavigationView navigationView = mBinding.navView;

        // Configurazione del Navigation Controller con il drawer
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_dayslist, R.id.nav_calendar)   // Top level fragment, all other are second level
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

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
        int mID = item.getItemId();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        if (mID == R.id.action_settings) {
            if (LOG_ENABLED) Log.d(TAG, "Menu settings selezionato");
            navController.navigate(R.id.nav_settings);
            return true;
        }
        if (mID == R.id.action_about) {
            if (LOG_ENABLED) Log.d(TAG, "Menu about selezionato");
            navController.navigate(R.id.nav_about);
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

        // Registra il receiver per i cambiamenti di tempo
        registerTimeChangeReceiver();

        // Aggiorna l'interfaccia quando l'activity torna in primo piano
        updateToolbarTitle();
        notifyUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (LOG_ENABLED) Log.d(TAG, "onPause");

        // Annulla la registrazione del receiver
        unregisterTimeChangeReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (LOG_ENABLED) Log.d(TAG, "onDestroy");

        // Assicurati di annullare la registrazione del receiver
        unregisterTimeChangeReceiver();

        // Deregistra il listener
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    /**
     * Registra il BroadcastReceiver per i cambiamenti di tempo.
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerTimeChangeReceiver() {
        if (!mReceiverRegistered && mTimeChangeReceiver != null) {
            try {
                IntentFilter filter = TimeChangeReceiver.createCriticalIntentFilter();
                registerReceiver(mTimeChangeReceiver, filter);
                mReceiverRegistered = true;
                if (LOG_ENABLED) Log.d(TAG, "TimeChangeReceiver registrato");
            } catch (Exception e) {
                if (LOG_ENABLED)
                    Log.e(TAG, "Errore nella registrazione del TimeChangeReceiver: " + e.getMessage());
            }
        }
    }

    /**
     * Annulla la registrazione del BroadcastReceiver.
     */
    private void unregisterTimeChangeReceiver() {
        if (mReceiverRegistered && mTimeChangeReceiver != null) {
            try {
                unregisterReceiver(mTimeChangeReceiver);
                mReceiverRegistered = false;
                if (LOG_ENABLED) Log.d(TAG, "TimeChangeReceiver deregistrato");
            } catch (Exception e) {
                if (LOG_ENABLED)
                    Log.e(TAG, "Errore nella deregistrazione del TimeChangeReceiver: " + e.getMessage());
            }
        }
    }

    // Implementazione dell'interfaccia TimeChangeListener

    @Override
    public void onTimeChanged() {
        if (LOG_ENABLED) Log.d(TAG, "onTimeChanged - aggiornamento interfaccia");

        runOnUiThread(() -> {
            // Aggiorna l'interfaccia sul thread UI
            updateToolbarTitle();
            notifyUpdates();

            // Opzionale: mostra un toast per informare l'utente
            Toast.makeText(this, "Ora di sistema aggiornata", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDateChanged() {
        if (LOG_ENABLED) Log.d(TAG, "onDateChanged - aggiornamento interfaccia");

        runOnUiThread(() -> {
            // Aggiorna l'interfaccia sul thread UI
            updateToolbarTitle();
            notifyUpdates();

            // Opzionale: mostra un toast per informare l'utente
            Toast.makeText(this, "Data di sistema aggiornata", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onTimezoneChanged() {
        if (LOG_ENABLED) Log.d(TAG, "onTimezoneChanged - aggiornamento interfaccia");

        runOnUiThread(() -> {
            // Aggiorna l'interfaccia sul thread UI
            updateToolbarTitle();
            notifyUpdates();

            // Opzionale: mostra un toast per informare l'utente
            Toast.makeText(this, "Fuso orario aggiornato", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onQuattroDueHomeFragmentInteractionListener(long id) {
        if (LOG_ENABLED) Log.d(TAG, "onQuattroDueHomeFragmentInteractionListener: " + id);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (LOG_ENABLED) Log.d(TAG, "Preferenza cambiata: " + key);
    }
}