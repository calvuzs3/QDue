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
import androidx.core.view.GravityCompat;
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
import net.calvuz.qdue.utils.ThemeManager;
import net.calvuz.qdue.utils.ThemeUtils;
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
    // Configura il drawer layout
    DrawerLayout drawer;
    NavigationView navigationView;
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

        // Test
        testThemeColors();

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
        drawer = mBinding.drawerLayout;
        navigationView = mBinding.navView;

        // Configurazione del Navigation Controller con il drawer
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_dayslist, R.id.nav_calendar)   // Top level fragment, all other are second level
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        navController.setGraph(R.navigation.mobile_navigation);

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Intercetta solo nav_settings per chiudere il drawer
        navigationView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

// Chiudi il drawer prima di navigare
            drawer.closeDrawer(GravityCompat.START);

            if (id == R.id.nav_settings) {
                // Naviga verso Settings con un piccolo delay per permettere al drawer di chiudersi
                new android.os.Handler().postDelayed(() -> {
                    navController.navigate(R.id.nav_settings);
                }, 120);

                return true; // Consuma l'evento
            }

            if (id == R.id.nav_about) {


                // Naviga verso About con un piccolo delay per permettere al drawer di chiudersi
                new android.os.Handler().postDelayed(() -> {
                    navController.navigate(R.id.nav_about);
                }, 120);

                return true; // Consuma l'evento
            }

            // Per tutti gli altri item, usa il comportamento standard di NavigationUI
            return NavigationUI.onNavDestinationSelected(item, navController);
        });

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

        if (getSupportActionBar() != null) {
            // Imposta il titolo dell'app
            getSupportActionBar().setTitle(R.string.app_name);
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
            }
        } else {
            Log.e(TAG, "Fragment non trovato");
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
            navController.navigate(R.id.nav_settings);
            return true;
        }
        if (mID == R.id.action_about) {
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
            } catch (Exception e) {
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
            } catch (Exception e) {
                Log.e(TAG, "Errore nella deregistrazione del TimeChangeReceiver: " + e.getMessage());
            }
        }
    }

    // Implementazione dell'interfaccia TimeChangeListener

    @Override
    public void onTimeChanged() {
        Log.d(TAG, "onTimeChanged - aggiornamento interfaccia");

        runOnUiThread(() -> {
            // Aggiorna l'interfaccia sul thread UI
            notifyUpdates();

            // Opzionale: mostra un toast per informare l'utente
            Toast.makeText(this, "Ora di sistema aggiornata", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDateChanged() {
        Log.d(TAG, "onDateChanged - aggiornamento interfaccia");

        runOnUiThread(() -> {
            // Aggiorna l'interfaccia sul thread UI
            notifyUpdates();

            // Opzionale: mostra un toast per informare l'utente
            Toast.makeText(this, "Data di sistema aggiornata", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onTimezoneChanged() {
        Log.d(TAG, "onTimezoneChanged - aggiornamento interfaccia");

        runOnUiThread(() -> {
            // Aggiorna l'interfaccia sul thread UI
            notifyUpdates();

            // Opzionale: mostra un toast per informare l'utente
            Toast.makeText(this, "Fuso orario aggiornato", Toast.LENGTH_SHORT).show();
        });
    }

    private void testThemeColors() {
        // Test dei colori dinamici
        int surfaceColor = ThemeUtils.getDynamicSurfaceColor(this);
        int onSurfaceColor = ThemeUtils.getDynamicOnSurfaceColor(this);
        int primaryColor = ThemeUtils.getDynamicPrimaryColor(this);

        // Test colori specifici app
        int todayBg = ThemeUtils.getTodayBackgroundColor(this);
        int userShiftBg = ThemeUtils.getUserShiftBackgroundColor(this);
        int sundayText = ThemeUtils.getSundayTextColor(this);

        Log.d("ThemeTest", "=== COLORI DINAMICI ===");
        Log.d("ThemeTest", "Surface: " + Integer.toHexString(surfaceColor));
        Log.d("ThemeTest", "OnSurface: " + Integer.toHexString(onSurfaceColor));
        Log.d("ThemeTest", "Primary: " + Integer.toHexString(primaryColor));

        Log.d("ThemeTest", "=== COLORI APP ===");
        Log.d("ThemeTest", "Today BG: " + Integer.toHexString(todayBg));
        Log.d("ThemeTest", "User Shift BG: " + Integer.toHexString(userShiftBg));
        Log.d("ThemeTest", "Sunday Text: " + Integer.toHexString(sundayText));

        // Test se il tema scuro è attivo
        ThemeManager themeManager = ThemeManager.getInstance(this);
        Log.d("ThemeTest", "Is Dark Mode: " + themeManager.isDarkMode());

        // Confronto con Material Design
        int materialSurface = ThemeUtils.getMaterialSurfaceColor(this);
        int yourSurface = getColor(R.color.surface);

        Log.d("ThemeTest", "=== CONFRONTO ===");
        Log.d("ThemeTest", "Material Surface: " + Integer.toHexString(materialSurface));
        Log.d("ThemeTest", "Your Surface: " + Integer.toHexString(yourSurface));

        if (materialSurface != yourSurface) {
            Log.d("ThemeTest", "✅ I tuoi colori sono diversi da Material Design (corretto!)");
        } else {
            Log.w("ThemeTest", "⚠️ I colori sono uguali a Material Design");
        }
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