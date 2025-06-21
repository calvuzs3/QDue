package net.calvuz.qdue.ui.shared;

import static net.calvuz.qdue.QDue.Debug.DEBUG_BASEFRAGMENT;
import static net.calvuz.qdue.quattrodue.Costants.QD_MAX_CACHE_SIZE;
import static net.calvuz.qdue.quattrodue.Costants.QD_MONTHS_CACHE_RADIUS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.QDueMainActivity;
import net.calvuz.qdue.events.data.database.EventsDatabase;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.utils.CalendarDataManager;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base fragment that implements common logic for DayslistViewFragment and CalendarViewFragment.
 * <p>
 * This class provides:
 * - Infinite scrolling with intelligent caching
 * - Unified data management through CalendarDataManager
 * - Layout manager agnostic operations (works with LinearLayoutManager and GridLayoutManager)
 * - Thread-safe operations with proper scroll callback handling
 * - Memory management with automatic cache cleanup
 * <p>
 * Subclasses must implement abstract methods to define specific behavior for:
 * - View initialization
 * - Data conversion
 * - Adapter setup
 * - FAB visibility logic
 *
 * @author calvuzs3
 */
public abstract class BaseFragment extends Fragment
        implements NotifyUpdatesInterface {

    // TAG
    private static final String TAG = "BaseFragment";

    // MOCK waiting for a stable integration
    // NEW: Events data support
    protected Map<LocalDate, List<LocalEvent>> mEventsCache = new ConcurrentHashMap<>();
    protected final AtomicBoolean mIsLoadingEvents = new AtomicBoolean(false);
    protected EventsDatabase mEventsDatabase;

    // DEBUG it is for initial scrolling bug. now resolved
    protected boolean mIsInitialScrolling = false;

    // Members
    protected CalendarDataManager mDataManager;
    protected RecyclerView mRecyclerView;
    protected GridLayoutManager mGridLayoutManager;
    protected FloatingActionButton mFabGoToToday;

    // ==================== INFINITE SCROLLING CACHE ==============

    // Cache of view items for infinite scrolling
    protected List<SharedViewModels.ViewItem> mItemsCache;

    // Current center position in cache
    protected int mCurrentCenterPosition;

    // Current date cursor
    protected LocalDate mCurrentDate;

    // ==================== CONCURRENCY CONTROL ===================

    // Atomic flag to prevent concurrent cache updates
    protected final AtomicBoolean mIsUpdatingCache = new AtomicBoolean(false);

    // Atomic flag for pending top load operations
    protected final AtomicBoolean mIsPendingTopLoad = new AtomicBoolean(false);

    // Atomic flag for pending bottom load operations
    protected final AtomicBoolean mIsPendingBottomLoad = new AtomicBoolean(false);

    // ==================== ASYNC OPERATION HANDLERS ==============

    // Main thread handler for UI operations
    protected Handler mMainHandler;

    // Background handler for data operations
    protected Handler mBackgroundHandler;

    // ==================== POSITION TRACKING =====================

    // Position of today in the cache (-1 if not found)
    protected int mTodayPosition = -1;

    // ==================== LOADING INDICATORS ====================

    // Flag indicating top loader is currently showing
    protected boolean mShowingTopLoader = false;

    // Flag indicating bottom loader is currently showing
    protected boolean mShowingBottomLoader = false;

    // ==================== SCROLL VELOCITY CONTROL ===============

    // Last scroll timestamp for throttling
    protected long mLastScrollTime = 0;

    // Current scroll velocity for performance optimization
    protected int mScrollVelocity = 0;

    // Maximum scroll velocity before ignoring updates
    protected static final int MAX_SCROLL_VELOCITY = 25;

    // Delay before processing operations after scroll settles
    protected static final long SCROLL_SETTLE_DELAY = 150;

    // ==================== ENHANCED CONSTANTS ====================

    // Number of months to load in a single operation
    private static final int MONTHS_PER_LOAD = 3;

    // Preload trigger zone (in calendar weeks)
    private static final int PRELOAD_TRIGGER_WEEKS = 6; // ~1.5 months

    // Maximum number of months to keep in cache
    private static final int MAX_CACHED_MONTHS = 18; // ~1.5 years

    // ==================== INTERFACES INTERACTION ================

    //Reference to the communication interface
    protected FragmentCommunicationInterface communicationInterface;

    // ============================================================

    /**
     * Initialize fragment components and handlers.
     * Sets up async handlers and data manager.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String mTAG = "onCreate: ";
        Log.v(mTAG, mTAG + getClass().getSimpleName());

        // Initialize handlers for async operations
        mMainHandler = new Handler(Looper.getMainLooper());
        mBackgroundHandler = new Handler(Looper.getMainLooper());

        // Initialize centralized data manager
        mDataManager = CalendarDataManager.getInstance();

        // MOCK
        // Initialize events database
        mEventsDatabase = EventsDatabase.getInstance(requireContext());
        // Start initial events load
        loadEventsForCurrentPeriod();
    }

    /**
     * Complete view setup after inflation.
     * Coordinates view finding, RecyclerView setup, and infinite scrolling initialization.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final String mTAG = "onViewCreated: ";
        Log.v(TAG, mTAG + "called.");

        // Subclasses must implement findViews() to initialize their specific views
        findViews(view);

        // Configure the RecyclerView
        setupRecyclerView();

        // Initialize infinite scrolling
        if (getActivity() != null) {
            setupInfiniteScrolling();
        } else {
            Log.e(TAG, mTAG + "Activity is null, cannot initialize infinite scrolling");
        }

        // Configure the Floating Action Button
        setupFAB();
    }

    // ==================== COMMUNICATION INTERFACE ===============

    /**
     * Get reference to communication interface during attachment.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        final String mTAG = "onAttach: ";

        try {
            // Get reference to activity's communication interface
            communicationInterface = (FragmentCommunicationInterface) context;
            Log.v(TAG, mTAG + "Communication interface attached");
        } catch (ClassCastException e) {
            Log.e(TAG, mTAG + "Error in attaching Communication Interface");
            throw new ClassCastException(context.toString()
                    + " must implement FragmentCommunicationInterface");
        }
    }

    /**
     * Clear reference when fragment is detached.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        communicationInterface = null;
        Log.v(TAG, "onDetach: Communication interface detached");
    }

    // ==================== COMMUNICATION HELPER METHODS ==========

    /**
     * Request navigation to another destination.
     */
    protected void navigateTo(int destinationId) {
        navigateTo(destinationId, null);
    }

    /**
     * Request navigation with data bundle.
     */
    protected void navigateTo(int destinationId, Bundle data) {
        if (communicationInterface != null) {
            communicationInterface.onFragmentNavigationRequested(destinationId, data);
        }
    }

    // ==================== RECYCLER ===========================

    /**
     * Setup RecyclerView with unified GridLayoutManager approach.
     * Both DayslistViewFragment (1 column) and CalendarViewFragment (7 columns) will use GridLayoutManager.
     * This unifies scroll handling, infinite loading, and sticky header logic.
     */
    protected void setupRecyclerView() {
        final String mTAG = "setupRecyclerView: ";
        Log.v(TAG, mTAG + "called.");

        if (mRecyclerView == null) {
            Log.e(TAG, mTAG + "RecyclerView is null - subclass must implement findViews()");
            return;
        }

        // Get column count from subclass
        int columnCount = getGridColumnCount();

        // Create unified GridLayoutManager
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), columnCount);

        // Configure span size for headers and loading items
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position < mItemsCache.size()) {

                    SharedViewModels.ViewItem item = mItemsCache.get(position);
                    Log.v(TAG, mTAG + "called mItemsCache.get( " + position + " )");

                    // Headers and loading items always span full width
                    if (item instanceof SharedViewModels.MonthHeader ||
                            item instanceof SharedViewModels.LoadingItem) {
                        return columnCount; // Full width
                    }
                }
                return 1; // Regular items take 1 column
            }
        });

        mRecyclerView.setLayoutManager(gridLayoutManager);

        // Store reference for position calculations
        mGridLayoutManager = gridLayoutManager;

        // Set mLayoutManager to null since we're using GridLayoutManager
//        mLayoutManager = null;

        // Apply common optimizations
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(null);

        Log.d(TAG, mTAG + "Configured GridLayoutManager with " + columnCount + " columns");
    }

    /**
     * Abstract method for subclasses to specify column count.
     * DayslistViewFragment returns 1, CalendarViewFragment returns 7.
     */
    protected abstract int getGridColumnCount();

    // ============================================================

    /**
     * Unified scroll listener for both fragment types.
     * Handles infinite scrolling and sticky header updates.
     */
    private class UnifiedGridScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            long currentTime = System.currentTimeMillis();

            // Calculate scroll velocity for performance optimization
            if (mLastScrollTime > 0) {
                long timeDiff = currentTime - mLastScrollTime;
                if (timeDiff > 0) {
                    mScrollVelocity = (int) (Math.abs(dy) / timeDiff * 16); // Normalized to 60fps
                }
            }

            // Throttling to avoid too frequent updates
            if (currentTime - mLastScrollTime < 100) return;
            mLastScrollTime = currentTime;

            // Skip updates if scrolling too fast
            if (mScrollVelocity > MAX_SCROLL_VELOCITY) return;

            // Get visible positions using GridLayoutManager
            int firstVisible = getFirstVisiblePosition();
            int lastVisible = getLastVisiblePosition();

            if (firstVisible == RecyclerView.NO_POSITION) return;

            // Handle infinite scrolling
            mMainHandler.post(() -> handleUnifiedScrollBasedLoading(firstVisible, lastVisible, dy));

            // Update FAB visibility
            updateFabVisibility(firstVisible, lastVisible);

            // Update sticky header in toolbar
            updateStickyHeader(firstVisible, lastVisible);
        }

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                mScrollVelocity = 0;

                // Process pending operations after scroll stops
                mMainHandler.postDelayed(() -> {
                    if (mScrollVelocity == 0) {
                        processPendingOperations();
                        scheduleCleanupIfNeeded();
                    }
                }, SCROLL_SETTLE_DELAY);
            }
        }
    }

    /**
     * Unified scroll-based loading logic that works for both 1-column and 7-column grids.
     */
    private void handleUnifiedScrollBasedLoading(int firstVisible, int lastVisible, int scrollDirection) {
        final String mTAG = "handleUnifiedScrollBasedLoading: ";
        Log.v(TAG, mTAG + "called.");

        // Conservative trigger zones
        int loadTriggerZone = getLoadTriggerZone();

        // Load upward (previous months) when near top
        if (firstVisible <= loadTriggerZone && scrollDirection <= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingTopLoad.get() && !mShowingTopLoader) {
            Log.v(TAG, mTAG + "Triggering top load at position: " + firstVisible);

            mMainHandler.postDelayed(() -> {
                if (!mIsUpdatingCache.get() && !mIsPendingTopLoad.get()) {
                    triggerTopLoad();
                }
            }, 100);
        }

        // Load downward (next months) when near bottom
        if (lastVisible >= mItemsCache.size() - loadTriggerZone && scrollDirection >= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingBottomLoad.get() && !mShowingBottomLoader) {
            Log.v(TAG, mTAG + "Triggering bottom load at position: " + lastVisible);

            mMainHandler.postDelayed(() -> {
                if (!mIsUpdatingCache.get() && !mIsPendingBottomLoad.get()) {
                    triggerBottomLoad();
                }
            }, 100);
        }
    }

    /**
     * Get load trigger zone based on column count.
     * More conservative for calendar view due to grid density.
     */
    private int getLoadTriggerZone() {
        int columnCount = getGridColumnCount();
        return columnCount == 1 ? 10 : 21; // 10 items for list, 21 items (~3 weeks) for calendar
    }

    /**
     * Update sticky header in toolbar based on currently visible month.
     */
    private void updateStickyHeader(int firstVisible, int lastVisible) {

        // Find the month header that's currently most visible
        LocalDate currentMonth = findCurrentVisibleMonth(firstVisible, lastVisible);

        if (currentMonth != null) {
            updateToolbarTitle(currentMonth);
        }
    }

    /**
     * Find which month is currently most visible in the viewport.
     */
    private LocalDate findCurrentVisibleMonth(int firstVisible, int lastVisible) {
        // Find the first month header in the visible range
        for (int i = firstVisible; i <= lastVisible && i < mItemsCache.size(); i++) {
            SharedViewModels.ViewItem item = mItemsCache.get(i);

            if (item instanceof SharedViewModels.MonthHeader) {
                return ((SharedViewModels.MonthHeader) item).monthDate;
            }

            if (item instanceof SharedViewModels.DayItem) {
                return ((SharedViewModels.DayItem) item).monthDate;
            }
        }

        // Fallback: search backwards from first visible
        for (int i = firstVisible - 1; i >= 0; i--) {
            SharedViewModels.ViewItem item = mItemsCache.get(i);

            if (item instanceof SharedViewModels.MonthHeader) {
                return ((SharedViewModels.MonthHeader) item).monthDate;
            }
        }

        return null;
    }

    /**
     * Update toolbar title with formatted month name.
     * Shows only month name for current year, "Month Year" for other years.
     */
    private void updateToolbarTitle(LocalDate monthDate) {
        final String mTAG = "updateToolbarTitle: ";
        Log.v(TAG, mTAG + "called.");

        if (getActivity() == null) return;

        try {
            LocalDate today = LocalDate.now();
            String formattedTitle;

            if (monthDate.getYear() == today.getYear()) {
                // Current year: show only month name
                formattedTitle = monthDate.format(DateTimeFormatter.ofPattern("MMMM", QDue.getLocale()));
            } else {
                // Different year: show month and year
                formattedTitle = monthDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", QDue.getLocale()));
            }

            // Update toolbar title - this will be implemented in the activity
            if (communicationInterface != null) {
                Bundle data = new Bundle();
                data.putString("title", formattedTitle);
                communicationInterface.onFragmentCustomAction("update_toolbar_title", data);
            }

        } catch (Exception e) {
            Log.e(TAG, mTAG + "updateToolbarTitle: Error updating toolbar title: " + e.getMessage());
        }
    }

    /**
     * Infinite scrolling
     * FIXED: Setup infinite scrolling with proper initial positioning
     */
    protected void setupInfiniteScrolling() {
        final String mTAG = ":setupInfiniteScrolling: ";
        Log.v(TAG, mTAG + "called.");

        try {
            // Reset all control flags
            resetControlFlags();

            // Initialize cache
            mCurrentDate = QDue.getQuattrodue().getCursorDate();
            mItemsCache = new ArrayList<>();

            // Pre-load months in cache around current date
            mDataManager.preloadMonthsAround(mCurrentDate, QD_MONTHS_CACHE_RADIUS);

            // Generate initial months for display
            // Here the items are added to mItemsCache
            for (int i = -QD_MONTHS_CACHE_RADIUS; i <= QD_MONTHS_CACHE_RADIUS; i++) {
                LocalDate monthDate = mCurrentDate.plusMonths(i);
                addMonthToCache(monthDate);
            }

            setupAdapter();

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error during infinite scrolling setup: " + e.getMessage());
        }

        try {
            // NUOVO: Verifica prerequisites
            if (!areComponentsReady()) {
                Log.w(TAG, mTAG + "Components not ready, deferring infinite scroll setup");
                scheduleInfiniteScrollingSetup();
                return;
            }

            // 1. Calcola la posizione di today
            mTodayPosition = SharedViewModels.DataConverter.findTodayPosition(mItemsCache);

            if (DEBUG_BASEFRAGMENT) {
                Log.d(TAG, mTAG + "Today position calculated: " + mTodayPosition);
                Log.d(TAG, mTAG + "Cache size: " + (mItemsCache != null ? mItemsCache.size() : "null"));
                Log.d(TAG, mTAG + "Adapter items: " + (getFragmentAdapter() != null ? getFragmentAdapter().getItemCount() : "null"));
            }

            // 2. Forza sincronizzazione cache-adapter
            ensureAdapterSyncWithCache();

            // 3. Scroll ritardato con verifica
            scheduleVerifiedScrollToToday();

            // 4. Setup infinite scroll listeners
            setupScrollListeners();

            if (DEBUG_BASEFRAGMENT) {
                Log.d(TAG, mTAG + "Infinite scroll setup completed: " + mItemsCache.size() +
                        " elements, today at position: " + mTodayPosition);
            }

        } catch (Exception e) {
            Log.e("TAG", mTAG + "Error in setupInfiniteScrolling: " + e.getMessage());
        }
    }

    // ===== 2. NUOVO: Verifica Prerequisites =====
    private boolean areComponentsReady() {
        String mTAG = "areComponentsReady: ";

        // Here every fragment implements its own adapter, so we ask for it
        if (getFragmentAdapter() == null) {
            Log.w(TAG, mTAG + "getFragmentAdapter() is null");
            return false;
        }

        if (mItemsCache == null) {
            Log.w(TAG, mTAG + "mmItemsCache is null");
            return false;
        }

        if (mRecyclerView == null) {
            Log.w(TAG, mTAG + "mRecyclerView is null");
            return false;
        }

        if (mGridLayoutManager == null) {
            Log.w(TAG, mTAG + "mGridLayoutManager is null");
            return false;
        }

        return true;
    }

    // ===== NUOVO METODO: Sincronizzazione Forzata =====
    private void ensureAdapterSyncWithCache() {
        final String mTAG = "ensureAdapterSyncWithCache: ";

        try {
            // NULL CHECKS aggiunti
            if (getFragmentAdapter() == null) {
                Log.e(TAG, mTAG + "getFragmentAdapter() is null, cannot sync");
                return;
            }

            if (mItemsCache == null) {
                Log.e(TAG, mTAG + "mCachedItems is null, cannot sync");
                return;
            }

            // Verifica se adapter è vuoto ma cache ha dati
            if (getFragmentAdapter().getItemCount() == 0 && !mItemsCache.isEmpty()) {

                if (DEBUG_BASEFRAGMENT) {
                    Log.d(TAG, mTAG + "Adapter empty but cache has " + mItemsCache.size() +
                            " items. Forcing sync...");
                }

                // Popola immediatamente the adapter con tutti i dati della cache
                getFragmentAdapter().setItems(new ArrayList<>(mItemsCache));
                getFragmentAdapter().notifyDataSetChanged();

                if (DEBUG_BASEFRAGMENT) {
                    Log.d(TAG, mTAG + "Adapter populated: " + getFragmentAdapter().getItemCount() + " items");
                }

            } else if (getFragmentAdapter().getItemCount() != mItemsCache.size()) {

                // Cache e adapter non sincronizzati - forza riallineamento
                if (DEBUG_BASEFRAGMENT) {
                    Log.w(TAG, mTAG + "Cache-Adapter mismatch. Cache: " + mItemsCache.size() +
                            ", Adapter: " + getFragmentAdapter().getItemCount() + ". Re-syncing...");
                }

                getFragmentAdapter().setItems(new ArrayList<>(mItemsCache));
                getFragmentAdapter().notifyDataSetChanged();
            } else {
                if (DEBUG_BASEFRAGMENT) {
                    Log.d(TAG, mTAG + "Cache and adapter already in sync: " + mItemsCache.size() + " items");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error in cache synchronization: " + e.getMessage());
        }
    }

    // ===== 3. NUOVO: Setup Ritardato =====
    private void scheduleInfiniteScrollingSetup() {
        final String mTAG = "scheduleInfiniteScrollingSetup: ";

        // Retry ogni 100ms fino a quando i componenti sono pronti
        mRecyclerView.postDelayed(new Runnable() {
            private int attempts = 0;
            private final int MAX_ATTEMPTS = 50; // 5 secondi max

            @Override
            public void run() {
                attempts++;

                if (areComponentsReady()) {
                    if (DEBUG_BASEFRAGMENT) {
                        Log.d(TAG, mTAG + "Components ready after " + attempts + " attempts");
                    }
                    setupInfiniteScrolling();
                } else if (attempts < MAX_ATTEMPTS) {
                    if (DEBUG_BASEFRAGMENT) {
                        Log.d(TAG, mTAG + "Retry " + attempts + "/" + MAX_ATTEMPTS);
                    }
                    mRecyclerView.postDelayed(this, 100);
                } else {
                    Log.e(TAG, mTAG + "Failed to setup infinite scrolling after " + MAX_ATTEMPTS + " attempts");
                }
            }
        }, 100);
    }

    // ===== METODO MODIFICATO: Scroll con Verifica =====
    private void scheduleVerifiedScrollToToday() {
        final String mTAG = "scheduleVerifiedScrollToToday: -";

        if (mTodayPosition < 0) {
            if (DEBUG_BASEFRAGMENT) {
                Log.w(TAG, mTAG + "Today position not found (" + mTodayPosition + ")");
                // DEBUG: Prova a forzare il caricamento del mese corrente
                tryLoadCurrentMonth();
            }
            return;
        }

        // NULL CHECK aggiunto
        if (mRecyclerView == null) {
            Log.e(TAG, mTAG + "mRecyclerView is null, cannot schedule scroll");
            return;
        }

        // Verifica che the adapter abbia abbastanza elementi
        if (getFragmentAdapter().getItemCount() > mTodayPosition) {
            // Dati pronti - scroll immediato ma con un piccolo delay per layout
            mRecyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    performVerifiedScrollToToday();
                }
            }, 100);

        } else {
            // Adapter non ancora pronto - usa retry con timeout
            scheduleRetryScrollToToday(0);
        }
    }

    // ===== 6. NUOVO: Tentativo di Caricamento Mese Corrente =====
    private void tryLoadCurrentMonth() {
        final String mTAG = "tryLoadCurrentMonth: ";

        try {
            if (DEBUG_BASEFRAGMENT) {
                Log.d(TAG, mTAG + "Attempting to load current month to find today");
            }

            // Forza il caricamento del mese corrente se non è in cache
            LocalDate today = LocalDate.now();
            LocalDate currentMonth = today.withDayOfMonth(1);

            // Chiama il metodo che carica un mese specifico
            // NOTA: Questo metodo potrebbe variare nel tuo DataManager
            if (mDataManager != null) {

                // Sostituisci questo con il metodo corretto del tuo DataManager
                // mDataManager.ensureMonthLoaded(currentMonth);
                mDataManager.getMonthDays(currentMonth);

                // Dopo il caricamento, riprova a trovare today
                mRecyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        recheckTodayPosition();
                    }
                }, 200);
            }

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error trying to load current month: " + e.getMessage());
        }
    }

    // ===== 7. NUOVO: Ricontrolla Posizione Today =====
    private void recheckTodayPosition() {
        final String mTAG = "recheckTodayPosition: ";

        try {
            // Aggiorna la cache
            // but it's a pain in the ass
            //mItemsCache = mDataManager.getCachedItems();

            if (mItemsCache != null && !mItemsCache.isEmpty()) {
                // Ricalcola posizione today
                mTodayPosition = SharedViewModels.DataConverter.findTodayPosition(mItemsCache);

                if (DEBUG_BASEFRAGMENT) {
                    Log.d(TAG, mTAG + "Rechecked today position: " + mTodayPosition +
                            " (cache size: " + mItemsCache.size() + ")");
                }

                if (mTodayPosition >= 0) {
                    // Today trovato - riprova il setup
                    ensureAdapterSyncWithCache();
                    scheduleVerifiedScrollToToday();
                }
            } else {
                Log.e(TAG, mTAG + "Error in mItemsCache");
            }

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error rechecking today position: " + e.getMessage());
        }
    }

    // ===== NUOVO METODO: Retry con Timeout =====
    private void scheduleRetryScrollToToday(int attemptCount) {
        final String mTAG = "scheduleRetryScrollToToday: ";
        final int MAX_ATTEMPTS = 20; // 20 * 50ms = 1 secondo max

        if (attemptCount >= MAX_ATTEMPTS) {
            Log.e(TAG, mTAG + "Failed to scroll to today after " + MAX_ATTEMPTS + " attempts");
            return;
        }

        mRecyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getFragmentAdapter().getItemCount() > mTodayPosition) {
                    if (DEBUG_BASEFRAGMENT) {
                        Log.d(TAG, mTAG + "Adapter ready after " + (attemptCount + 1) + " attempts");
                    }
                    performVerifiedScrollToToday();
                } else {
                    if (DEBUG_BASEFRAGMENT) {
                        Log.d(TAG, mTAG + "Retry " + (attemptCount + 1) + "/" + MAX_ATTEMPTS +
                                " - Adapter: " + getFragmentAdapter().getItemCount() + ", Need: " + (mTodayPosition + 1));
                    }
                    scheduleRetryScrollToToday(attemptCount + 1);
                }
            }
        }, 50); // Retry ogni 50ms
    }

    // ===== METODO MODIFICATO: Scroll Verificato =====
    private void performVerifiedScrollToToday() {
        final String mTAG = "performVerifiedScrollToToday: ";

        try {
            // Verifica finale prima dello scroll
            if (mTodayPosition < 0 || mTodayPosition >= getFragmentAdapter().getItemCount()) {
                Log.e(TAG, mTAG + "Invalid today position: " + mTodayPosition +
                        " (adapter size: " + getFragmentAdapter().getItemCount() + ")");
                return;
            }

            if (DEBUG_BASEFRAGMENT) {
                Log.d(TAG, mTAG + "Performing verified scroll to position: " + mTodayPosition);
            }

            // AGGIUNTO: Flag per disabilitare temporaneamente infinite loading
            mIsInitialScrolling = true;

            if (mGridLayoutManager instanceof GridLayoutManager) {
                GridLayoutManager gridManager = (GridLayoutManager) mGridLayoutManager;

                // Calcola posizione ottimale per centrare today
                int optimalPosition = Math.max(0, mTodayPosition - ( 4 * getGridColumnCount() )); // 4 righe sopra
                gridManager.scrollToPositionWithOffset(optimalPosition, 20);

                if (DEBUG_BASEFRAGMENT) {
                    Log.d(TAG, mTAG + "Using GridLayoutManager positioning for today: " + mTodayPosition +
                            " (scroll to: " + optimalPosition + ")");
                }

            } else {
                // Fallback per LinearLayoutManager
                mRecyclerView.scrollToPosition(Math.max(0, mTodayPosition - 3));

                if (DEBUG_BASEFRAGMENT) {
                    Log.e(TAG, mTAG + "Using fallback mRecyclerView positioning for today: " + mTodayPosition);
                }
            }

            // Riabilita infinite loading dopo un delay
            mRecyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mIsInitialScrolling = false;
                    if (DEBUG_BASEFRAGMENT) {
                        Log.d(TAG, mTAG + "Initial scroll completed, infinite loading re-enabled");
                    }
                }
            }, 500); // 500ms di "grazia"

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error performing verified scroll: " + e.getMessage());
            mIsInitialScrolling = false; // Assicurati di riabilitare anche in caso di errore
        }
    }

    // ============================================================

    /**
     * NEW: Setup scroll listeners separately for better organization
     * DEBUG: set to protected instead of private
     */
    protected void setupScrollListeners() {
        if (mRecyclerView != null) {
            mRecyclerView.addOnScrollListener(new DebuggingScrollListener());
            mRecyclerView.clearOnScrollListeners();
            mRecyclerView.addOnScrollListener(new UnifiedGridScrollListener());
        }
    }

// ===================================================================
// DEBUGGING: IDENTIFICARE IL COLPEVOLE
// ===================================================================

    /**
     * DEBUG: Add logging to identify what's causing the scroll reset
     */
    public class DebuggingScrollListener extends RecyclerView.OnScrollListener {

        private int mLastPosition = -1;

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            int currentPosition = getCurrentVisiblePosition();

            if (currentPosition != mLastPosition && currentPosition != RecyclerView.NO_POSITION) {
                Log.w("SCROLL_DEBUG", "Position changed from " + mLastPosition + " to " + currentPosition +
                        " dx=" + dx + " dy=" + dy + " thread=" + Thread.currentThread().getName());

                // Print stack trace to see who triggered the scroll
                Log.w("SCROLL_DEBUG", "Stack trace:" + new Exception("Scroll trigger"));

                mLastPosition = currentPosition;
            }
        }
    }

    /**
     * Updated position methods to work with GridLayoutManager.
     */
    protected int getFirstVisiblePosition() {
        if (mGridLayoutManager != null) {
            return mGridLayoutManager.findFirstVisibleItemPosition();
        }
        return RecyclerView.NO_POSITION;
    }

    protected int getLastVisiblePosition() {
        if (mGridLayoutManager != null) {
            return mGridLayoutManager.findLastVisibleItemPosition();
        }
        return RecyclerView.NO_POSITION;
    }

    public void scrollToToday() {
        final String mTAG = "scrollToToday: ";

        if (mTodayPosition >= 0 && mTodayPosition < mItemsCache.size()) {
            Log.v(TAG, mTAG + "Scrolling to today, position: " + mTodayPosition);
            if (mGridLayoutManager != null) {
                mRecyclerView.smoothScrollToPosition(mTodayPosition);
            }
        } else {
            // Rebuild cache centered on today
            Log.v(TAG, mTAG + "Today not in cache, rebuilding");
            mCurrentDate = LocalDate.now().withDayOfMonth(1);
            setupInfiniteScrolling();
        }
    }

    /**
     * Maintain scroll position for GridLayoutManager.
     *
     * @param itemsAdded number of items added at the beginning
     */

    private void maintainScrollPosition(int itemsAdded) {
        if (mGridLayoutManager != null) {
            try {
                mGridLayoutManager.scrollToPositionWithOffset(itemsAdded, 0);
                if (DEBUG_BASEFRAGMENT) {
                    Log.d(TAG, "Maintained GridLayoutManager position");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error maintaining scroll position: " + e.getMessage());
            }
        }
    }

    // ======================================================================

    // ================================= UI =================================

    /**
     * Enhanced FAB setup that works with both integrated and separate FAB modes.
     * Add this method to BaseFragment.java
     */
    protected void setupFAB() {
        if (getActivity() instanceof QDueMainActivity) {
            mFabGoToToday = ((QDueMainActivity) getActivity()).getFabGoToToday();

            if (mFabGoToToday != null) {
                mFabGoToToday.setOnClickListener(v -> scrollToToday());
            } else {
                // Riprova dopo che l'Activity ha finito l'inizializzazione
                mRecyclerView.post(() -> retrySetupFAB());
            }
        }
    }

    private void retrySetupFAB() {
        if (mFabGoToToday == null && getActivity() instanceof QDueMainActivity) {
            mFabGoToToday = ((QDueMainActivity) getActivity()).getFabGoToToday();
            if (mFabGoToToday != null) {
                mFabGoToToday.setOnClickListener(v -> scrollToToday());
            }
        }
    }

    /**
     * Enhanced FAB visibility update that respects integrated mode.
     * Update this method in BaseFragment.java
     */
    protected void updateFabVisibility(int firstVisible, int lastVisible) {
        if (mFabGoToToday == null) {
            Log.v(TAG, "updateFabVisibility: FAB is null, skipping");
            return;
        }

        // Logica di visibilità standard
        boolean showFab = true;
        if (mTodayPosition >= 0) {
            showFab = !(firstVisible <= mTodayPosition && lastVisible >= mTodayPosition);
        }

        // Applica visibilità
        if (showFab && mFabGoToToday.getVisibility() != View.VISIBLE) {
            mFabGoToToday.show();
            Log.v(TAG, "updateFabVisibility: Showing FAB");
        } else if (!showFab && mFabGoToToday.getVisibility() == View.VISIBLE) {
            mFabGoToToday.hide();
            Log.v(TAG, "updateFabVisibility: Hiding FAB");
        }
    }

    /**
     * Execute top load operation (loading previous months).
     * <p>
     * This method is thread-safe and posts all adapter operations to the next frame
     * to avoid conflicts with scroll callbacks.
     * FIXED: Execute top load operation with proper error handling and debugging
     * ENHANCED: Execute top load operation - loads multiple months at once
     * ENHANCED: Better today position updates during cache operations
     */
    protected void executeTopLoad() {
        final String mTAG = "executeTopLoad: ";

        if (!mIsPendingTopLoad.compareAndSet(true, false)) {
            Log.w(TAG, mTAG + "No pending top load - aborting");
            hideTopLoader();
            return;
        }

        if (!mIsUpdatingCache.compareAndSet(false, true)) {
            Log.w(TAG, mTAG + "Cache update in progress - aborting");
            mIsPendingTopLoad.set(true);
            return;
        }

        Log.d(TAG, "Starting multi-month top load execution");

        try {
            LocalDate firstMonth = findFirstMonthInCache();
            if (firstMonth == null) {
                Log.e(TAG, mTAG + "Cannot find first month in cache");
                mIsUpdatingCache.set(false);
                hideTopLoader();
                return;
            }

            List<SharedViewModels.ViewItem> allNewItems = new ArrayList<>();

            for (int i = 1; i <= MONTHS_PER_LOAD; i++) {
                LocalDate monthToLoad = firstMonth.minusMonths(i);
                Log.d(TAG, mTAG + "Loading previous month " + i + ": " + monthToLoad);

                List<SharedViewModels.ViewItem> monthItems = generateMonthItems(monthToLoad);
                if (!monthItems.isEmpty()) {
                    allNewItems.addAll(0, monthItems);
                }
            }

            if (allNewItems.isEmpty()) {
                Log.w(TAG, mTAG + "No items generated for any previous months");
                mIsUpdatingCache.set(false);
                hideTopLoader();
                return;
            }

            Log.d(TAG, mTAG + "Generated " + allNewItems.size() + " items for " + MONTHS_PER_LOAD + " previous months");

            mMainHandler.post(() -> {
                try {
                    if (mItemsCache == null) {
                        Log.e(TAG, mTAG + "Items cache is null during UI update");
                        return;
                    }

                    mItemsCache.addAll(0, allNewItems);
                    Log.d(TAG, mTAG + "Added items to cache. New size: " + mItemsCache.size());

                    // ENHANCED: Better today position tracking
                    if (mTodayPosition >= 0) {
                        mTodayPosition += allNewItems.size();
                    } else {
                        // Today position was lost - try to find it again
                        mTodayPosition = SharedViewModels.DataConverter.findTodayPosition(mItemsCache);
                        Log.d(TAG, mTAG + "Recomputed today position: " + mTodayPosition);
                    }

                    mCurrentCenterPosition += allNewItems.size();

                    mMainHandler.post(() -> {
                        try {
                            if (getFragmentAdapter() != null) {
                                getFragmentAdapter().notifyItemRangeInserted(0, allNewItems.size());
                                Log.d(TAG, mTAG + "Notified adapter of " + allNewItems.size() + " inserted items");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, mTAG + "Error notifying adapter: " + e.getMessage());
                        }
                    });

                    maintainScrollPosition(allNewItems.size());

                    mMainHandler.postDelayed(() -> {
                        hideTopLoader();
                        Log.d(TAG, mTAG + "Multi-month top load completed successfully");
                        schedulePreloadingCheck();
                    }, 100);

                } catch (Exception e) {
                    Log.e(TAG, mTAG + "Error during UI update: " + e.getMessage());
                    hideTopLoader();
                } finally {
                    mIsUpdatingCache.set(false);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error loading previous months: " + e.getMessage());
            mIsUpdatingCache.set(false);
            hideTopLoader();
        }
    }

    /**
     * Execute bottom load operation (loading next months).
     * <p>
     * This method is thread-safe and posts all adapter operations to the next frame
     * to avoid conflicts with scroll callbacks.
     * FIXED: Execute bottom load operation with proper error handling and debugging
     * ENHANCED: Execute bottom load operation - loads multiple months at once
     * ENHANCED: Better today position updates during bottom loading
     */
    protected void executeBottomLoad() {
        final String mTAG = "executeBottomLoad: ";

        if (!mIsPendingBottomLoad.compareAndSet(true, false)) {
            Log.w(TAG, "No pending bottom load - aborting");
            hideBottomLoader();
            return;
        }

        if (!mIsUpdatingCache.compareAndSet(false, true)) {
            Log.w(TAG, "Cache update in progress - aborting");
            mIsPendingBottomLoad.set(true);
            return;
        }

        Log.d(TAG, "Starting multi-month bottom load execution");

        try {
            LocalDate lastMonth = findLastMonthInCache();
            if (lastMonth == null) {
                Log.e(TAG, "Cannot find last month in cache");
                mIsUpdatingCache.set(false);
                hideBottomLoader();
                return;
            }

            List<SharedViewModels.ViewItem> allNewItems = new ArrayList<>();

            for (int i = 1; i <= MONTHS_PER_LOAD; i++) {
                LocalDate monthToLoad = lastMonth.plusMonths(i);
                Log.d(TAG, "Loading next month " + i + ": " + monthToLoad);

                List<SharedViewModels.ViewItem> monthItems = generateMonthItems(monthToLoad);
                if (!monthItems.isEmpty()) {
                    allNewItems.addAll(monthItems);
                }
            }

            if (allNewItems.isEmpty()) {
                Log.w(TAG, "No items generated for any next months");
                mIsUpdatingCache.set(false);
                hideBottomLoader();
                return;
            }

            Log.d(TAG, "Generated " + allNewItems.size() + " items for " + MONTHS_PER_LOAD + " next months");

            mMainHandler.post(() -> {
                try {
                    if (mItemsCache == null) {
                        Log.e(TAG, "Items cache is null during UI update");
                        return;
                    }

                    int insertPos = mItemsCache.size();
                    mItemsCache.addAll(allNewItems);
                    Log.d(TAG, "Added items to cache. New size: " + mItemsCache.size());

                    // ENHANCED: Better today position tracking
                    if (mTodayPosition < 0) {
                        // Today position not set - try to find it in the expanded cache
                        mTodayPosition = SharedViewModels.DataConverter.findTodayPosition(mItemsCache);
                        Log.d(TAG, "Found today position in expanded cache: " + mTodayPosition);
                    }

                    mMainHandler.post(() -> {
                        try {
                            if (getFragmentAdapter() != null) {
                                getFragmentAdapter().notifyItemRangeInserted(insertPos, allNewItems.size());
                                Log.d(TAG, "Notified adapter of " + allNewItems.size() + " inserted items at position " + insertPos);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error notifying adapter: " + e.getMessage());
                        }
                    });

                    mMainHandler.postDelayed(() -> {
                        hideBottomLoader();
                        Log.d(TAG, "Multi-month bottom load completed successfully");
                        schedulePreloadingCheck();
                    }, 100);

                } catch (Exception e) {
                    Log.e(TAG, "Error during UI update: " + e.getMessage());
                    hideBottomLoader();
                } finally {
                    mIsUpdatingCache.set(false);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error loading next months: " + e.getMessage());
            mIsUpdatingCache.set(false);
            hideBottomLoader();
        }
    }

    /**
     * NEW: Schedule preloading check to maintain buffer zones
     */
    private void schedulePreloadingCheck() {
        final String mTAG = "schedulePreloadingCheck: ";
        Log.v(TAG, mTAG + "called.");

        // Schedule preloading check after a short delay
        mMainHandler.postDelayed(() -> {
            if (!mIsUpdatingCache.get() && mScrollVelocity == 0) {
                performPreloadingCheck();
            }
        }, 500); // Check after scrolling settles
    }

    /**
     * NEW: Perform silent preloading to maintain buffer zones
     */
    private void performPreloadingCheck() {
        final String mTAG = "performPreloadingCheck: ";
        Log.v(TAG, mTAG + "called");

        if (mIsUpdatingCache.get()) {
            Log.d(TAG, mTAG + "Cache update in progress - skipping preload check");
            return;
        }

        // Get current scroll position
        int currentPosition = getCurrentVisiblePosition();
        if (currentPosition == RecyclerView.NO_POSITION) {
            return;
        }

        int cacheSize = mItemsCache.size();
        int bufferZone = PRELOAD_TRIGGER_WEEKS * 7; // Convert weeks to items

        // Check if we need to preload at the top
        if (currentPosition < bufferZone && !mIsPendingTopLoad.get()) {
            Log.d(TAG, mTAG + "Triggering silent top preload at position: " + currentPosition);
            triggerSilentTopLoad();
        }

        // Check if we need to preload at the bottom
        if (currentPosition > cacheSize - bufferZone && !mIsPendingBottomLoad.get()) {
            Log.d(TAG, mTAG + "Triggering silent bottom preload at position: " + currentPosition);
            triggerSilentBottomLoad();
        }

        // Check if cache is too large and needs cleanup
        if (cacheSize > MAX_CACHED_MONTHS * 35) { // ~35 items per month
            Log.d(TAG, mTAG + "Cache too large (" + cacheSize + "), scheduling cleanup");
            scheduleCleanupIfNeeded();
        }
    }

    /**
     * NEW: Silent top load without visible loader
     */
    private void triggerSilentTopLoad() {
        final String mTAG = "triggerSilentTopLoad: ";

        if (mIsUpdatingCache.get() || mIsPendingTopLoad.get()) {
            return;
        }

        Log.d(TAG, mTAG + "Starting silent top load");
        mIsPendingTopLoad.set(true);

        // Execute without showing loader
        mBackgroundHandler.postDelayed(() -> {
            executeSilentTopLoad();
        }, 100);
    }

    /**
     * NEW: Silent bottom load without visible loader
     */
    private void triggerSilentBottomLoad() {
        final String mTAG = "triggerSilentBottomLoad: ";

        if (mIsUpdatingCache.get() || mIsPendingBottomLoad.get()) {
            return;
        }

        Log.d(TAG, mTAG + "Starting silent bottom load");
        mIsPendingBottomLoad.set(true);

        // Execute without showing loader
        mBackgroundHandler.postDelayed(() -> {
            executeSilentBottomLoad();
        }, 100);
    }

    /**
     * NEW: Execute silent top load (similar to executeTopLoad but without loader UI)
     */
    private void executeSilentTopLoad() {
        final String mTAG = "executeSilentTopLoad: ";

        if (!mIsPendingTopLoad.compareAndSet(true, false)) {
            return;
        }

        if (!mIsUpdatingCache.compareAndSet(false, true)) {
            mIsPendingTopLoad.set(true);
            return;
        }

        try {
            LocalDate firstMonth = findFirstMonthInCache();
            if (firstMonth == null) {
                mIsUpdatingCache.set(false);
                return;
            }

            // Load 2 months silently
            List<SharedViewModels.ViewItem> allNewItems = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                LocalDate monthToLoad = firstMonth.minusMonths(i);
                List<SharedViewModels.ViewItem> monthItems = generateMonthItems(monthToLoad);
                if (!monthItems.isEmpty()) {
                    allNewItems.addAll(0, monthItems);
                }
            }

            if (!allNewItems.isEmpty()) {
                mMainHandler.post(() -> {
                    try {
                        mItemsCache.addAll(0, allNewItems);
                        if (mTodayPosition >= 0) mTodayPosition += allNewItems.size();
                        mCurrentCenterPosition += allNewItems.size();

                        mMainHandler.post(() -> {
                            if (getFragmentAdapter() != null) {
                                getFragmentAdapter().notifyItemRangeInserted(0, allNewItems.size());
                            }
                        });

                        maintainScrollPosition(allNewItems.size());
                        Log.d(TAG, mTAG + "Silent top load completed: " + allNewItems.size() + " items");
                    } finally {
                        mIsUpdatingCache.set(false);
                    }
                });
            } else {
                mIsUpdatingCache.set(false);
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error in silent top load: " + e.getMessage());
            mIsUpdatingCache.set(false);
        }
    }

    /**
     * NEW: Execute silent bottom load (similar to executeBottomLoad but without loader UI)
     */
    private void executeSilentBottomLoad() {
        final String mTAG = "executeSilentBottomLoad: ";

        if (!mIsPendingBottomLoad.compareAndSet(true, false)) {
            return;
        }

        if (!mIsUpdatingCache.compareAndSet(false, true)) {
            mIsPendingBottomLoad.set(true);
            return;
        }

        try {
            LocalDate lastMonth = findLastMonthInCache();
            if (lastMonth == null) {
                mIsUpdatingCache.set(false);
                return;
            }

            // Load 2 months silently
            List<SharedViewModels.ViewItem> allNewItems = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                LocalDate monthToLoad = lastMonth.plusMonths(i);
                List<SharedViewModels.ViewItem> monthItems = generateMonthItems(monthToLoad);
                if (!monthItems.isEmpty()) {
                    allNewItems.addAll(monthItems);
                }
            }

            if (!allNewItems.isEmpty()) {
                mMainHandler.post(() -> {
                    try {
                        int insertPos = mItemsCache.size();
                        mItemsCache.addAll(allNewItems);

                        if (mTodayPosition < 0) {
                            int todayInNew = SharedViewModels.DataConverter.findTodayPosition(allNewItems);
                            if (todayInNew >= 0) {
                                mTodayPosition = insertPos + todayInNew;
                            }
                        }

                        mMainHandler.post(() -> {
                            if (getFragmentAdapter() != null) {
                                getFragmentAdapter().notifyItemRangeInserted(insertPos, allNewItems.size());
                            }
                        });

                        Log.d(TAG, mTAG + "Silent bottom load completed: " + allNewItems.size() + " items");
                    } finally {
                        mIsUpdatingCache.set(false);
                    }
                });
            } else {
                mIsUpdatingCache.set(false);
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error in silent bottom load: " + e.getMessage());
            mIsUpdatingCache.set(false);
        }
    }

    /**
     * Trigger top load operation (previous months).
     * Sets up loading state and schedules execution.
     * FIXED: Trigger top load with better state management
     */
    protected void triggerTopLoad() {
        final String mTAG = ":triggerTopLoad: ";

        if (mIsInitialScrolling) {
            if (DEBUG_BASEFRAGMENT) {
                Log.d(TAG, mTAG + "Skipping top load during initial scroll");
            }
            return; // SKIP durante scroll iniziale
        }
        // NULL CHECK aggiunto
        if (mDataManager == null) {
            Log.e(TAG, mTAG + "mDataManager is null");
            return;
        }

        // Check if already loading or pending
        if (mIsUpdatingCache.get()) {
            Log.w(TAG, mTAG + "Cache update in progress - ignoring trigger");
            return;
        }

        if (mIsPendingTopLoad.get()) {
            Log.w(TAG, mTAG + "Top load already pending - ignoring trigger");
            return;
        }

        if (mShowingTopLoader) {
            Log.w(TAG, mTAG + "Top loader already showing - ignoring trigger");
            return;
        }

        Log.d(TAG, mTAG + "Triggering top load");

        mIsPendingTopLoad.set(true);
        showTopLoader();

        // Execute with a small delay to ensure loader is visible
        mBackgroundHandler.postDelayed(() -> {
            Log.d(TAG, mTAG + "Executing delayed top load");
            executeTopLoad();
        }, 200); // Increased delay to ensure loader visibility
    }

    /**
     * Trigger bottom load operation (next months).
     * Sets up loading state and schedules execution.
     * FIXED: Trigger bottom load with better state management
     */
    protected void triggerBottomLoad() {
        final String mTAG = "triggerBottomLoad: ";

        if (mIsInitialScrolling) {
            if (DEBUG_BASEFRAGMENT) {
                Log.d(TAG, mTAG + "Skipping bottom load during initial scroll");
            }
            return; // SKIP durante scroll iniziale
        }
        // NULL CHECK aggiunto
        if (mDataManager == null) {
            Log.e(TAG, mTAG + "mDataManager is null");
            return;
        }

        // Check if already loading or pending
        if (mIsUpdatingCache.get()) {
            Log.w(TAG, mTAG + "Cache update in progress - ignoring trigger");
            return;
        }

        if (mIsPendingBottomLoad.get()) {
            Log.w(TAG, mTAG + "Bottom load already pending - ignoring trigger");
            return;
        }

        if (mShowingBottomLoader) {
            Log.w(TAG, mTAG + "Bottom loader already showing - ignoring trigger");
            return;
        }

        Log.d(TAG, mTAG + "Triggering bottom load");

        mIsPendingBottomLoad.set(true);
        showBottomLoader();

        // Execute with a small delay to ensure loader is visible
        mBackgroundHandler.postDelayed(() -> {
            Log.d(TAG, mTAG + "Executing delayed bottom load");
            executeBottomLoad();
        }, 200); // Increased delay to ensure loader visibility
    }

//    /**
//     * Add a month to the cache by generating its view items.
//     * DEBUG: made protected insteadof private
//     *
//     * @param monthDate the month to add
//     */
//    protected void addMonthToCache(LocalDate monthDate) {
//        List<SharedViewModels.ViewItem> monthItems = generateMonthItems(monthDate);
//        mItemsCache.addAll(monthItems);
//    }

    /**
     * Generate view items for a specific month using the fragment-specific converter.
     *
     * @param monthDate the month to generate items for
     * @return list of view items for the month
     */
    private List<SharedViewModels.ViewItem> generateMonthItems(LocalDate monthDate) {
        List<Day> monthDays = mDataManager.getMonthDays(monthDate);
        return convertMonthData(monthDays, monthDate);
    }

    /**
     * Process pending operations when scroll stops.
     * Executes any pending load operations that were deferred during scrolling.
     */
    protected void processPendingOperations() {
        final String mTAG = "processPendingOperations: ";

        if (mIsPendingTopLoad.get() && !mIsUpdatingCache.get()) {
            if (DEBUG_BASEFRAGMENT) Log.d(TAG, mTAG + "Processing pending top load");
            executeTopLoad();
        }
        if (mIsPendingBottomLoad.get() && !mIsUpdatingCache.get()) {
            if (DEBUG_BASEFRAGMENT) Log.d(TAG, mTAG + "Processing pending bottom load");
            executeBottomLoad();
        }
    }

    /**
     * Schedule cache cleanup if cache size exceeds limits.
     * Prevents memory issues by cleaning old cache entries when cache grows too large.
     */
    protected void scheduleCleanupIfNeeded() {
        final String mTAG = "scheduleCleanupIfNeeded: ";

        int maxElements = QD_MAX_CACHE_SIZE * 35; // ~35 elements per month
        if (mItemsCache.size() > maxElements) {
            if (DEBUG_BASEFRAGMENT)
                Log.d(TAG, mTAG + "Scheduling cache cleanup, current size: " + mItemsCache.size());

            mBackgroundHandler.postDelayed(() -> {
                if (!mIsUpdatingCache.get() && mScrollVelocity == 0) {
                    cleanupCache();
                }
            }, 1000);
        }
    }

    /**
     * Clean up cache by removing elements far from current position.
     * Works with both LinearLayoutManager and GridLayoutManager.
     */
    private void cleanupCache() {
        final String mTAG = "cleanupCache: ";

        if (!mIsUpdatingCache.compareAndSet(false, true)) return;

        mMainHandler.post(() -> {
            try {
                // Get current position safely for any LayoutManager type
                int currentPos = getCurrentVisiblePosition();
                int targetSize = QD_MONTHS_CACHE_RADIUS * 35;

                // If we can't determine position, use a safe fallback
                if (currentPos == RecyclerView.NO_POSITION) {
                    currentPos = mItemsCache.size() / 2; // Use middle as fallback
                    if (DEBUG_BASEFRAGMENT) Log.w(TAG, mTAG + "Using fallback position for cleanup");
                }

                // Remove from start if necessary
                while (mItemsCache.size() > targetSize && currentPos > targetSize / 2) {
                    mItemsCache.remove(0);
                    currentPos--;
                    if (mTodayPosition > 0) mTodayPosition--;
                    if (mCurrentCenterPosition > 0) mCurrentCenterPosition--;

                    // POST adapter notification to next frame
                    mMainHandler.post(() -> {
                        if (getFragmentAdapter() != null) {
                            getFragmentAdapter().notifyItemRemoved(0);
                        }
                    });
                }

                // Remove from end if necessary
                while (mItemsCache.size() > targetSize &&
                        currentPos < mItemsCache.size() - targetSize / 2) {
                    int lastIndex = mItemsCache.size() - 1;
                    mItemsCache.remove(lastIndex);

                    // POST adapter notification to next frame
                    final int finalLastIndex = lastIndex;
                    mMainHandler.post(() -> {
                        if (getFragmentAdapter() != null) {
                            getFragmentAdapter().notifyItemRemoved(finalLastIndex);
                        }
                    });
                }

                if (DEBUG_BASEFRAGMENT)
                    Log.d(TAG, mTAG + "Cache cleaned, new size: " + mItemsCache.size());
            } finally {
                mIsUpdatingCache.set(false);
            }
        });
    }

    /**
     * Get current visible position safely for any LayoutManager type.
     * Handles both LinearLayoutManager and GridLayoutManager with fallbacks.
     *
     * @return current visible position or NO_POSITION if not determinable
     */
    private int getCurrentVisiblePosition() {
        final String mTAG = "getCurrentVisiblePosition: ";

        if (mRecyclerView == null) return RecyclerView.NO_POSITION;

        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) return RecyclerView.NO_POSITION;

        try {
            // Handle LinearLayoutManager (DaysListFragment)
            if (layoutManager instanceof LinearLayoutManager) {
                LinearLayoutManager linearManager = (LinearLayoutManager) layoutManager;
                int firstVisible = linearManager.findFirstCompletelyVisibleItemPosition();
                if (firstVisible != RecyclerView.NO_POSITION) {
                    return firstVisible;
                }
                // Fallback to partially visible
                return linearManager.findFirstVisibleItemPosition();
            }

            // Handle GridLayoutManager (CalendarFragment)
            else if (layoutManager instanceof GridLayoutManager) {
                GridLayoutManager gridManager = (GridLayoutManager) layoutManager;
                int firstVisible = gridManager.findFirstCompletelyVisibleItemPosition();
                if (firstVisible != RecyclerView.NO_POSITION) {
                    return firstVisible;
                }
                // Fallback to partially visible
                return gridManager.findFirstVisibleItemPosition();
            }

            // For other LayoutManager types, use generic approach
            else {
                // Try to get first visible child
                View firstChild = layoutManager.getChildAt(0);
                if (firstChild != null) {
                    return mRecyclerView.getChildAdapterPosition(firstChild);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error getting current position: " + e.getMessage());
        }

        return RecyclerView.NO_POSITION;
    }

    // =============== LOADING INDICATOR MANAGEMENT ===============

    /**
     * Show top loading indicator.
     * Adds loading item to cache and notifies adapter on next frame.
     * Override showTopLoader with more debugging
     */
    private void showTopLoader() {
        final String mTAG = "showTopLoader: ";

        if (mShowingTopLoader) {
            Log.w(TAG, mTAG + "Top loader already showing - ignoring request");
            return;
        }

        mShowingTopLoader = true;

        SharedViewModels.LoadingItem loader = new SharedViewModels.LoadingItem(
                SharedViewModels.LoadingItem.LoadingType.TOP);
        mItemsCache.add(0, loader);

        if (mTodayPosition >= 0) mTodayPosition++;
        mCurrentCenterPosition++;

        Log.w(TAG, mTAG + "Added top loader - cache size now: " + mItemsCache.size());

        // POST ADAPTER NOTIFICATION TO NEXT FRAME
        mMainHandler.post(() -> {
            if (getFragmentAdapter() != null) {
                getFragmentAdapter().notifyItemInserted(0);
                Log.w(TAG, mTAG + "Notified adapter of top loader insertion");
            }
        });
    }

    /**
     * Show bottom loading indicator.
     * Adds loading item to cache and notifies adapter on next frame.
     * Override showBottomLoader with more debugging
     */
    private void showBottomLoader() {
        final String mTAG = "showBottomLoader: ";

        if (mShowingBottomLoader) {
            Log.w(TAG, mTAG + "Bottom loader already showing - ignoring request");
            return;
        }

        mShowingBottomLoader = true;

        SharedViewModels.LoadingItem loader = new SharedViewModels.LoadingItem(
                SharedViewModels.LoadingItem.LoadingType.BOTTOM);
        mItemsCache.add(loader);

        Log.w(TAG, mTAG + "Added bottom loader - cache size now: " + mItemsCache.size());

        // POST ADAPTER NOTIFICATION TO NEXT FRAME
        mMainHandler.post(() -> {
            if (getFragmentAdapter() != null) {
                getFragmentAdapter().notifyItemInserted(mItemsCache.size() - 1);
                Log.w(TAG, mTAG + "Notified adapter of bottom loader insertion");
            }
        });
    }

    /**
     * FIXED: Hide top loader with better error handling
     */
    private void hideTopLoader() {
        final String mTAG = "hideTopLoader: ";
        Log.v(TAG, mTAG + "called.");

        if (!mShowingTopLoader) {
            Log.d(TAG, mTAG + "Top loader not showing - nothing to hide");
            return;
        }

        try {
            for (int i = 0; i < mItemsCache.size(); i++) {
                if (mItemsCache.get(i) instanceof SharedViewModels.LoadingItem) {
                    SharedViewModels.LoadingItem loading = (SharedViewModels.LoadingItem) mItemsCache.get(i);
                    if (loading.loadingType == SharedViewModels.LoadingItem.LoadingType.TOP) {
                        mItemsCache.remove(i);
                        if (mTodayPosition > i) mTodayPosition--;
                        if (mCurrentCenterPosition > i) mCurrentCenterPosition--;

                        Log.d(TAG, mTAG + "Removed top loader from position " + i);

                        // POST ADAPTER NOTIFICATION TO NEXT FRAME
                        final int finalI = i;
                        mMainHandler.post(() -> {
                            try {
                                if (getFragmentAdapter() != null) {
                                    getFragmentAdapter().notifyItemRemoved(finalI);
                                    Log.d(TAG, mTAG + "Notified adapter of top loader removal");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, mTAG + "Error notifying adapter of loader removal: " + e.getMessage());
                            }
                        });
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error hiding top loader: " + e.getMessage());
        } finally {
            mShowingTopLoader = false;
        }
    }

    /**
     * Hide bottom loading indicator.
     * Removes loading item from cache and notifies adapter on next frame.
     */
    private void hideBottomLoader() {
        final String mTAG = "hideBottomLoader: ";

        if (!mShowingBottomLoader) {
            Log.d(TAG, mTAG + "Bottom loader not showing - nothing to hide");
            return;
        }

        Log.d(TAG, mTAG + "Hiding bottom loader");

        try {
            for (int i = mItemsCache.size() - 1; i >= 0; i--) {
                if (mItemsCache.get(i) instanceof SharedViewModels.LoadingItem) {
                    SharedViewModels.LoadingItem loading = (SharedViewModels.LoadingItem) mItemsCache.get(i);
                    if (loading.loadingType == SharedViewModels.LoadingItem.LoadingType.BOTTOM) {
                        mItemsCache.remove(i);

                        Log.d(TAG, mTAG + "Removed bottom loader from position " + i);

                        // POST ADAPTER NOTIFICATION TO NEXT FRAME
                        final int finalI = i;
                        mMainHandler.post(() -> {
                            try {
                                if (getFragmentAdapter() != null) {
                                    getFragmentAdapter().notifyItemRemoved(finalI);
                                    Log.d(TAG, mTAG + "Notified adapter of bottom loader removal");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, mTAG + "Error notifying adapter of loader removal: " + e.getMessage());
                            }
                        });
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error hiding bottom loader: " + e.getMessage());
        } finally {
            mShowingBottomLoader = false;
        }
    }

    // === UTILITY METHODS ===

    /**
     * Reset all control flags to initial state.
     * Used during initialization and cleanup.
     * DEBUG: made protected insteadof private
     */
    protected void resetControlFlags() {
        mIsUpdatingCache.set(false);
        mIsPendingTopLoad.set(false);
        mIsPendingBottomLoad.set(false);
        mShowingTopLoader = false;
        mShowingBottomLoader = false;
    }

    /**
     * Find the first month date in the cache.
     * Used for determining what previous month to load.
     *
     * @return first month date or null if cache is empty
     */
    private LocalDate findFirstMonthInCache() {
        for (SharedViewModels.ViewItem item : mItemsCache) {
            if (item instanceof SharedViewModels.MonthHeader) {
                return ((SharedViewModels.MonthHeader) item).monthDate;
            }
            if (item instanceof SharedViewModels.DayItem) {
                return ((SharedViewModels.DayItem) item).monthDate;
            }
        }
        return null;
    }

    /**
     * Find the last month date in the cache.
     * Used for determining what next month to load.
     *
     * @return last month date or null if cache is empty
     */
    private LocalDate findLastMonthInCache() {
        for (int i = mItemsCache.size() - 1; i >= 0; i--) {
            SharedViewModels.ViewItem item = mItemsCache.get(i);
            if (item instanceof SharedViewModels.MonthHeader) {
                return ((SharedViewModels.MonthHeader) item).monthDate;
            }
            if (item instanceof SharedViewModels.DayItem) {
                return ((SharedViewModels.DayItem) item).monthDate;
            }
        }
        return null;
    }

    /// === LIFECYCLE MANAGEMENT ===

    /**
     * Handle fragment start lifecycle.
     * Updates preferences and triggers refresh if needed.
     */
    @Override
    public void onStart() {
        super.onStart();
        final String mTAG = "onStart: ";
        Log.v(TAG, mTAG + "called.");

        if (QDue.getQuattrodue() != null) {
            QDue.getQuattrodue().updatePreferences(getActivity());
            if (QDue.getQuattrodue().isRefresh()) {
                QDue.getQuattrodue().setRefresh(false);
                refreshData();
            }
        }
    }

    /**
     * Handle fragment resume lifecycle.
     * Updates today's position and refreshes adapter.
     */
    @Override
    public void onResume() {
        super.onResume();
        final String mTAG = "onResume: ";
        Log.v(TAG, mTAG + "Fragment resumed: " + getClass().getSimpleName());

        // Update today's position in case date changed
        mTodayPosition = SharedViewModels.DataConverter.findTodayPosition(mItemsCache);
        if (getFragmentAdapter() != null) {
            getFragmentAdapter().notifyDataSetChanged();
        }
    }

    /**
     * Handle fragment view destruction.
     * Cleans up handlers, cache, and listeners to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        final String mTAG = "onDestroyView: ";
        Log.v(TAG, mTAG + "called.");

        // Cancel all pending operations
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
        }
        if (mBackgroundHandler != null) {
            mBackgroundHandler.removeCallbacksAndMessages(null);
        }

        // Clean up resources
        if (mItemsCache != null) {
            mItemsCache.clear();
        }
        if (mRecyclerView != null) {
            mRecyclerView.clearOnScrollListeners();
        }

        if (DEBUG_BASEFRAGMENT)
            Log.d(TAG, mTAG + "Fragment view destroyed: " + getClass().getSimpleName());
    }

    /**
     * Refresh data when preferences change.
     * Clears data manager cache and regenerates data.
     */
    public void refreshData() {
        final String mTAG = "refreshData: ";
        Log.v(TAG, mTAG + "called.");

        // Clear data manager cache
        mDataManager.clearCache();

        // Regenerate data
        setupInfiniteScrolling();
    }

    /**
     * Notify updates from external sources.
     * Updates user team and refreshes adapter.
     */
    @SuppressLint("NotifyDataSetChanged")
    public void notifyUpdates() {
        final String mTAG = "notifyUpdates: ";
        Log.v(TAG, mTAG + "called.");

        // Update user team if necessary
        onUserTeamChanged();

        if (getFragmentAdapter() != null) {
            getFragmentAdapter().notifyDataSetChanged();
        }
    }

    /**
     * Called when user team changes.
     * Subclasses can override for specific logic.
     */
    protected void onUserTeamChanged() {
        final String mTAG = "onUserTeamChanged: ";
        Log.v(TAG, mTAG + "User team changed");
    }

    // =======================================================
    // === ABSTRACT METHODS THAT SUBCLASSES MUST IMPLEMENT ===
    // =======================================================

    /**
     * Find and setup views.
     */
    protected abstract void findViews(View rootView);

    /**
     * Convert data in fragment specific format.
     */
    protected abstract List<SharedViewModels.ViewItem> convertMonthData(List<Day> days, LocalDate monthDate);

    /**
     * Fragment specific adapter setup.
     * Subclass create a specific Adapter instance
     * which is given to the RecyclerView with mRecyclerView,setAdapter( adapter )
     */
    protected abstract void setupAdapter();

    /**
     * Get the current adapter.
     */
    protected abstract BaseAdapter getFragmentAdapter();

    /**
     * Set adapter, an extension of BaseAdapter
     */
    protected abstract void setFragmentAdapter(BaseAdapter adapter);


    // ==================== EVENTS LOADING METHODS ====================

    /**
     * Load events for the current visible period (non-blocking).
     * This method runs in background and updates UI when complete.
     */
    /**
     * Load events for the current visible period (non-blocking).
     * This method runs in background and updates UI when complete.
     */
    protected void loadEventsForCurrentPeriod() {
        if (mIsLoadingEvents.get()) {
            Log.d(TAG, "Events already loading, skipping");
            return;
        }

        mIsLoadingEvents.set(true);

        // Calculate date range for current cache
        LocalDate startDate = getCurrentPeriodStart();
        LocalDate endDate = getCurrentPeriodEnd();

        Log.d(TAG, "Loading events for period: " + startDate + " to " + endDate);

        // Load events asynchronously
        CompletableFuture.supplyAsync(() -> {
            try {
                // Convert LocalDate to LocalDateTime for DAO method
                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
                return mEventsDatabase.eventDao().getEventsInDateRange(startDateTime, endDateTime);
            } catch (Exception e) {
                Log.e(TAG, "Error loading events from database", e);
                return new ArrayList<LocalEvent>();
            }
        }).thenAccept(events -> {
            Log.d(TAG, "Loaded " + events.size() + " events from database");

            // Process events into date-grouped map
            Map<LocalDate, List<LocalEvent>> eventsMap = groupEventsByDate(events);

            Log.d(TAG, "Grouped into " + eventsMap.size() + " dates with events");

            // Debug: Log alcuni esempi
            int debugCount = 0;
            for (Map.Entry<LocalDate, List<LocalEvent>> entry : eventsMap.entrySet()) {
                if (debugCount < 3) { // Solo primi 3 per debug
                    LocalDate date = entry.getKey();
                    List<LocalEvent> dateEvents = entry.getValue();
                    Log.d(TAG, "DEBUG: Date " + date + " has " + dateEvents.size() + " events");
                    debugCount++;
                }
            }

            // Update cache on main thread
            mMainHandler.post(() -> {
                updateEventsCache(eventsMap);
                notifyEventsDataChanged();
                mIsLoadingEvents.set(false);
            });
    }).exceptionally(throwable -> {
            Log.e(TAG, "Failed to load events", throwable);
            mMainHandler.post(() -> {
                mIsLoadingEvents.set(false);
            });
            return null;
        });
    }

    /**
     * Update events cache and notify adapter.
     * Update events cache and notify adapter - FIXED VERSION
     * PROBLEMA: il metodo sovrascrive invece di fare merge
     */
    protected void updateEventsCache(Map<LocalDate, List<LocalEvent>> newEventsMap) {
        if (newEventsMap != null && !newEventsMap.isEmpty()) {
            // FIX: Fare MERGE invece di putAll che può sovrascrivere
            for (Map.Entry<LocalDate, List<LocalEvent>> entry : newEventsMap.entrySet()) {
                LocalDate date = entry.getKey();
                List<LocalEvent> newEvents = entry.getValue();

                if (mEventsCache.containsKey(date)) {
                    // Merge con eventi esistenti per questa data
                    List<LocalEvent> existingEvents = mEventsCache.get(date);
                    if (existingEvents != null) {
                        // Creare lista combinata evitando duplicati
                        List<LocalEvent> mergedEvents = new ArrayList<>(existingEvents);
                        for (LocalEvent newEvent : newEvents) {
                            if (!mergedEvents.contains(newEvent)) {
                                mergedEvents.add(newEvent);
                            }
                        }
                        mEventsCache.put(date, mergedEvents);
                    } else {
                        mEventsCache.put(date, new ArrayList<>(newEvents));
                    }
                } else {
                    // Nuova data, aggiungi direttamente
                    mEventsCache.put(date, new ArrayList<>(newEvents));
                }
            }

            Log.d(TAG, "Updated events cache with " + newEventsMap.size() + " new dates, total cache: " + mEventsCache.size() + " dates");
        } else {
            Log.w(TAG, "Attempted to update events cache with null or empty map - IGNORING");
            // FIX: NON svuotare la cache se arriva una mappa vuota
            return;
        }
    }


    /**
     * Group events by their date range for efficient lookup.
     * FIXED: Now handles multi-day events correctly by adding them to all affected dates
     */
    private Map<LocalDate, List<LocalEvent>> groupEventsByDate(List<LocalEvent> events) {
        Map<LocalDate, List<LocalEvent>> grouped = new HashMap<>();

        for (LocalEvent event : events) {
            LocalDate startDate = event.getStartDate();
            LocalDate endDate = event.getEndDate();

            // SINGLE DAY EVENT: Add only to start date
            if (endDate == null || startDate.equals(endDate)) {
                grouped.computeIfAbsent(startDate, k -> new ArrayList<>()).add(event);
                Log.v(TAG, "Single day event: " + event.getTitle() + " on " + startDate);
            }
            // MULTI-DAY EVENT: Add to all dates in range
            else {
                LocalDate currentDate = startDate;
                int dayCount = 0;

                while (!currentDate.isAfter(endDate) && dayCount < 365) { // Safety limit
                    grouped.computeIfAbsent(currentDate, k -> new ArrayList<>()).add(event);
                    Log.v(TAG, "Multi-day event: " + event.getTitle() + " added to " + currentDate);

                    currentDate = currentDate.plusDays(1);
                    dayCount++;
                }

                Log.d(TAG, "Multi-day event '" + event.getTitle() + "' spans " + dayCount + " days (" + startDate + " to " + endDate + ")");
            }
        }

        return grouped;
    }

    /**
     * Get start date of current cached period.
     */
    private LocalDate getCurrentPeriodStart() {
        if (mCurrentDate != null) {
            return mCurrentDate.minusMonths(QD_MONTHS_CACHE_RADIUS);
        }
        return LocalDate.now().minusMonths(QD_MONTHS_CACHE_RADIUS);
    }

    /**
     * Get end date of current cached period.
     */
    private LocalDate getCurrentPeriodEnd() {
        if (mCurrentDate != null) {
            return mCurrentDate.plusMonths(QD_MONTHS_CACHE_RADIUS);
        }
        return LocalDate.now().plusMonths(QD_MONTHS_CACHE_RADIUS);
    }

    /**
     * Notify subclasses that events data has changed.
     * Subclasses should override this to update their adapters.
     */
    protected void notifyEventsDataChanged() {
        // Default implementation - subclasses should override
        Log.d(TAG, "Events data changed - " + mEventsCache.size() + " dates with events");
    }

    /**
     * Get events for a specific date.
     * @param date The date to get events for
     * @return List of events for the date, or empty list if none
     */
    protected List<LocalEvent> getEventsForDate(LocalDate date) {
        List<LocalEvent> events = mEventsCache.get(date);
        return events != null ? events : new ArrayList<>();
    }

    /**
     * Check if a date has any events.
     * @param date The date to check
     * @return true if the date has events
     */
    protected boolean hasEventsForDate(LocalDate date) {
        return mEventsCache.containsKey(date) && !mEventsCache.get(date).isEmpty();
    }

    // ==================== ENHANCED CACHE LOADING ====================

    /**
     * Enhanced cache loading that also loads events for new periods.
     */
    //@Override
    protected void addMonthToCache(LocalDate monthDate) {
        List<SharedViewModels.ViewItem> monthItems = generateMonthItems(monthDate);
        mItemsCache.addAll(monthItems);
        //addMonthToCache(monthDate);

        // Load events for the new month
        loadEventsForMonth(monthDate);
    }

    /**
     * Load events for a specific month.
     */
    protected void loadEventsForMonth(LocalDate monthDate) {
        CompletableFuture.supplyAsync(() -> {
            try {
                LocalDate startOfMonth = monthDate.withDayOfMonth(1);
                LocalDate endOfMonth = monthDate.withDayOfMonth(monthDate.lengthOfMonth());

                // Convert to LocalDateTime for DAO method
                LocalDateTime startDateTime = startOfMonth.atStartOfDay();
                LocalDateTime endDateTime = endOfMonth.atTime(23, 59, 59);

                return mEventsDatabase.eventDao().getEventsInDateRange(startDateTime, endDateTime);
            } catch (Exception e) {
                Log.e(TAG, "Error loading events for month " + monthDate, e);
                return new ArrayList<LocalEvent>();
            }
        }).thenAccept(events -> {
            Map<LocalDate, List<LocalEvent>> monthEvents = groupEventsByDate(events);
            mMainHandler.post(() -> {
                updateEventsCache(monthEvents);
                notifyEventsDataChanged();
            });
        });
    }

    // ==================== PUBLIC API FOR SUBCLASSES ====================

    /**
     * Refresh events data (call when events are added/modified).
     */
    public void refreshEventsData() {
        mEventsCache.clear();
        loadEventsForCurrentPeriod();
    }

    /**
     * Get events cache for adapter integration.
     * @return Current events cache
     */
    protected Map<LocalDate, List<LocalEvent>> getEventsCache() {
        return new HashMap<>(mEventsCache); // Return copy for thread safety
    }

}