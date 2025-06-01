package net.calvuz.qdue;

import static net.calvuz.qdue.QDue.Debug.DEBUG_ACTIVITY;
import static net.calvuz.qdue.QDue.Debug.DEBUG_COLORS;

import android.annotation.SuppressLint;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import net.calvuz.qdue.databinding.ActivityQdueMainBinding;
import net.calvuz.qdue.ui.dayslist.DayslistViewFragment;
import net.calvuz.qdue.utils.Log;
import net.calvuz.qdue.utils.ThemeManager;
import net.calvuz.qdue.utils.ThemeUtils;
import net.calvuz.qdue.utils.TimeChangeReceiver;

/**
 * Activity principale con gestione automatica portrait/landscape.
 * Android gestisce automaticamente il cambio layout attraverso i resource qualifiers.
 */
public class QDueMainActivity extends AppCompatActivity
        implements
        TimeChangeReceiver.TimeChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "QDueMainActivity";

    // UI related
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityQdueMainBinding binding;
    private NavController navController;

    // Navigation components (uno dei due sarà null in base al layout)
    private BottomNavigationView bottomNavigation;
    private NavigationView sidebarNavigation;

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
        if (DEBUG_COLORS) testThemeColors();

        Log.d( TAG, String.valueOf(QDue.getContext() == getApplicationContext()));

        // Inizializza il time change receiver
        mTimeChangeReceiver = new TimeChangeReceiver(this);

        // Registra il listener per le preferenze
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        // Inizializza il binding e il layout
        binding = ActivityQdueMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configura la toolbar (se presente nel layout)
        if (!(binding.toolbar == null)) setSupportActionBar(binding.toolbar);

        // Setup navigation con controllo sicuro
        setupNavigationSafely();

        // Tieni traccia del fragment corrente
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            mCurrentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
            if (DEBUG_ACTIVITY)
                Log.d(TAG, "Fragment changed: " + (mCurrentFragment != null ? mCurrentFragment.getClass().getSimpleName() : "null"));
        });
    }

    /**
     * Setup sicuro della navigazione con controllo degli errori.
     */
    private void setupNavigationSafely() {
        try {
            // Metodo 1: Trova il NavHostFragment direttamente
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_content_main);

            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
                if (DEBUG_ACTIVITY) Log.d(TAG, "NavController trovato tramite NavHostFragment");
            } else {
                // Metodo 2: Fallback con Navigation.findNavController
                navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                if (DEBUG_ACTIVITY)
                    Log.d(TAG, "NavController trovato tramite Navigation.findNavController");
            }

            // Setup navigation components
            setupNavigationComponents();

        } catch (IllegalStateException e) {
            Log.e(TAG, "Errore setup NavController: " + e.getMessage());

            // Retry dopo un piccolo delay
            findViewById(R.id.nav_host_fragment_content_main).post(() -> {
                try {
                    navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                    setupNavigationComponents();
                    if (DEBUG_ACTIVITY) Log.d(TAG, "NavController trovato al secondo tentativo");
                } catch (Exception retryException) {
                    Log.e(TAG, "Impossibile trovare NavController anche al secondo tentativo: " + retryException.getMessage());
                }
            });
        }
    }

    /**
     * Setup dei componenti di navigazione una volta che il NavController è disponibile.
     */
    private void setupNavigationComponents() {
        if (navController == null) {
            Log.e(TAG, "NavController è null, impossibile configurare navigazione");
            return;
        }

        // Cerca i componenti di navigazione (solo uno esisterà in base al layout)
        bottomNavigation = findViewById(R.id.bottom_navigation);
        sidebarNavigation = findViewById(R.id.sidebar_navigation);

        // Determina quale tipo di navigazione è disponibile
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        if (DEBUG_ACTIVITY) {
            Log.d(TAG, "Orientamento: " + (isLandscape ? "Landscape" : "Portrait"));
            Log.d(TAG, "Bottom Navigation disponibile: " + (bottomNavigation != null));
            Log.d(TAG, "Sidebar Navigation disponibile: " + (sidebarNavigation != null));
        }

        // Setup navigation in base ai componenti disponibili
        if (bottomNavigation != null) {
            setupBottomNavigation();
        }

        if (sidebarNavigation != null) {
            setupSidebarNavigation();
        }

        // Configura ActionBar
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_dayslist, R.id.nav_calendar)
                .build();

        if (binding.toolbar != null) {
            NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        }
    }

    /**
     * Setup bottom navigation (portrait).
     */
    private void setupBottomNavigation() {
        if (DEBUG_ACTIVITY) Log.d(TAG, "Configurando Bottom Navigation");

        try {
            // Setup bottom navigation con NavController
            NavigationUI.setupWithNavController(bottomNavigation, navController);

            // Gestisci click personalizzati se necessario
            bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();

                try {
                    if (id == R.id.nav_settings) {
                        navController.navigate(R.id.nav_settings);
                        return true;
                    } else if (id == R.id.nav_about) {
                        navController.navigate(R.id.nav_about);
                        return true;
                    }

                    // Per gli altri item, usa il comportamento standard
                    return NavigationUI.onNavDestinationSelected(item, navController);
                } catch (Exception e) {
                    Log.e(TAG, "Errore durante navigazione: " + e.getMessage());
                    return false;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Errore setup bottom navigation: " + e.getMessage());
        }
    }

    /**
     * Setup sidebar navigation (landscape).
     */
    private void setupSidebarNavigation() {
        if (DEBUG_ACTIVITY) Log.d(TAG, "Configurando Sidebar Navigation");

        try {
            // Setup sidebar navigation con NavController
            NavigationUI.setupWithNavController(sidebarNavigation, navController);

            // Gestisci click personalizzati
            sidebarNavigation.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();

                try {
                    if (id == R.id.nav_settings) {
                        navController.navigate(R.id.nav_settings);
                        return true;
                    } else if (id == R.id.nav_about) {
                        navController.navigate(R.id.nav_about);
                        return true;
                    }

                    // Per gli altri item, usa il comportamento standard
                    return NavigationUI.onNavDestinationSelected(item, navController);
                } catch (Exception e) {
                    Log.e(TAG, "Errore durante navigazione sidebar: " + e.getMessage());
                    return false;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Errore setup sidebar navigation: " + e.getMessage());
        }
    }

    /**
     * Aggiorna il titolo della toolbar.
     */
    private void updateToolbarTitle() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }
    }

    /**
     * Notifica il fragment di aggiornare i dati.
     */
    private void notifyUpdates() {
        if (DEBUG_ACTIVITY) Log.d(TAG, "notifyUpdates");

        try {
            // Cerca il fragment attivo
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
            if (fragment != null) {
                // Se è un NavHostFragment, cerca il fragment figlio attivo
                if (fragment instanceof NavHostFragment) {
                    Fragment childFragment = ((NavHostFragment) fragment).getChildFragmentManager().getPrimaryNavigationFragment();

                    if (childFragment instanceof DayslistViewFragment) {
                        ((DayslistViewFragment) childFragment).notifyUpdates();
                    }
                }
            } else {
                Log.e(TAG, "Fragment non trovato");
            }
        } catch (Exception e) {
            Log.e(TAG, "Errore durante notifyUpdates: " + e.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Solo per portrait (quando non c'è sidebar), in landscape le opzioni sono nella sidebar
        if (sidebarNavigation == null) {
            getMenuInflater().inflate(R.menu.main, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (navController == null) {
            return super.onOptionsItemSelected(item);
        }

        int mID = item.getItemId();

        try {
            if (mID == R.id.action_settings) {
                navController.navigate(R.id.nav_settings);
                return true;
            }
            if (mID == R.id.action_about) {
                navController.navigate(R.id.nav_about);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Errore durante navigazione menu: " + e.getMessage());
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (navController != null && mAppBarConfiguration != null) {
            return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                    || super.onSupportNavigateUp();
        }
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG_ACTIVITY) Log.d(TAG, "onResume");

        // Registra il receiver per i cambiamenti di tempo
        registerTimeChangeReceiver();

        // Aggiorna l'interfaccia quando l'activity torna in primo piano
        notifyUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG_ACTIVITY) Log.d(TAG, "onPause");

        // Annulla la registrazione del receiver
        unregisterTimeChangeReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DEBUG_ACTIVITY) Log.d(TAG, "onDestroy");

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
            notifyUpdates();
            Toast.makeText(this, "Ora di sistema aggiornata", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDateChanged() {
        Log.d(TAG, "onDateChanged - aggiornamento interfaccia");

        runOnUiThread(() -> {
            notifyUpdates();
            Toast.makeText(this, "Data di sistema aggiornata", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onTimezoneChanged() {
        Log.d(TAG, "onTimezoneChanged - aggiornamento interfaccia");

        runOnUiThread(() -> {
            notifyUpdates();
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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (DEBUG_ACTIVITY) Log.d(TAG, "Preferenza cambiata: " + key);
    }
}