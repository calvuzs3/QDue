package net.calvuz.qdue.ui.shared;

import static net.calvuz.qdue.quattrodue.Costants.QD_MAX_CACHE_SIZE;
import static net.calvuz.qdue.quattrodue.Costants.QD_MONTHS_CACHE_SIZE;

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

import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.utils.CalendarDataManager;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fragment base che implementa la logica comune per DayslistViewFragment e CalendarViewFragment.
 * Gestisce il scrolling infinito, la cache dei dati unificata e le operazioni comuni.
 */
public abstract class BaseFragment extends Fragment {

    private static final String TAG = "BaseFragment";
    private static final boolean LOG_ENABLED = true;

    // Componenti comuni
    protected QuattroDue mQD;
    protected CalendarDataManager mDataManager;
    protected RecyclerView mRecyclerView;
    protected LinearLayoutManager mLayoutManager;
    protected FloatingActionButton mFabGoToToday;

    // Cache degli elementi per lo scrolling infinito
    protected List<SharedViewModels.ViewItem> mItemsCache;
    protected int mCurrentCenterPosition;
    protected LocalDate mCurrentDate;

    // Sistema di controllo per operazioni concorrenti
    protected final AtomicBoolean mIsUpdatingCache = new AtomicBoolean(false);
    protected final AtomicBoolean mIsPendingTopLoad = new AtomicBoolean(false);
    protected final AtomicBoolean mIsPendingBottomLoad = new AtomicBoolean(false);

    // Gestori per operazioni asincrone
    protected Handler mMainHandler;
    protected Handler mBackgroundHandler;

    // Posizione di oggi
    protected int mTodayPosition = -1;

    // Flag per indicatori di caricamento
    protected boolean mShowingTopLoader = false;
    protected boolean mShowingBottomLoader = false;

    // Controllo dello scroll per evitare aggiornamenti durante scroll veloce
    protected long mLastScrollTime = 0;
    protected int mScrollVelocity = 0;
    protected static final int MAX_SCROLL_VELOCITY = 25;
    protected static final long SCROLL_SETTLE_DELAY = 150;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inizializza i gestori
        mMainHandler = new Handler(Looper.getMainLooper());
        mBackgroundHandler = new Handler(Looper.getMainLooper());

        // Inizializza il data manager
        mDataManager = CalendarDataManager.getInstance();

        if (LOG_ENABLED) Log.d(TAG, "onCreate: " + getClass().getSimpleName());
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Le sottoclassi devono implementare findViews() per inizializzare le viste
        findViews(view);

        // Configura il RecyclerView
        setupRecyclerView();

        // Configura il FAB
        setupFAB();

        // Inizializza QuattroDue e il data manager
        if (getActivity() != null) {
            mQD = QuattroDue.getInstance(getActivity().getApplicationContext());
            mDataManager.initialize(mQD);
            setupInfiniteScrolling();
        }
    }

    /**
     * Override del setupRecyclerView per supportare layout manager personalizzati.
     */

    protected void setupRecyclerView() {
        if (mRecyclerView == null) {
            Log.e(TAG, "mRecyclerView è null - implementare findViews()");
            return;
        }

        // Verifica se esiste già un LayoutManager
        // Le sottoclassi possono override questo metodo per layout manager specifici

        RecyclerView.LayoutManager existingLayoutManager = mRecyclerView.getLayoutManager();

        if (existingLayoutManager == null) {
            // Crea e imposta un nuovo LinearLayoutManager
            mLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            mRecyclerView.setLayoutManager(mLayoutManager);
        } else if (existingLayoutManager instanceof LinearLayoutManager) {
            // Se esiste già un LinearLayoutManager, utilizzalo
            mLayoutManager = (LinearLayoutManager) existingLayoutManager;
        } else {
            // Per altri tipi di LayoutManager, mantieni null e usa i metodi di fallback
            mLayoutManager = null;
        }

        // Ottimizzazioni comuni
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(null);
//        mRecyclerView.setDrawingCacheEnabled(true);
//        mRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
    }

    /**
     * Configura il FAB comune.
     */
    protected void setupFAB() {
        if (mFabGoToToday != null) {
            mFabGoToToday.setOnClickListener(v -> scrollToToday());
            mFabGoToToday.hide(); // Inizialmente nascosto
        }
    }


    /**
     * Setup dello scrolling infinito con logica comune ottimizzata.
     */
    protected void setupInfiniteScrolling() {
        setupInfiniteScrolling( true);
    }

    /**
     * Setup dello scrolling infinito con logica comune ottimizzata.
     * @param useLayoutManager specifica se utilizzare lo scrolling di LinearLayoutManager
     *                         oppure la classe che eredita implementa una sua versione
     *                         e quindi non serve - genererebbe un eccezione
     */
    protected void setupInfiniteScrolling(boolean useLayoutManager) {
        if (mQD == null || mDataManager == null) {
            Log.e(TAG, "setupInfiniteScrolling: componenti non inizializzati");
            return;
        }

        try {
            // Reset dei flag di controllo
            resetControlFlags();

            // Inizializza la cache
            mCurrentDate = mQD.getCursorDate();
            mItemsCache = new ArrayList<>();

            // Pre-carica i mesi nella cache
            mDataManager.preloadMonthsAround(mCurrentDate, QD_MONTHS_CACHE_SIZE);

            // Genera i mesi iniziali per la visualizzazione
            for (int i = -QD_MONTHS_CACHE_SIZE; i <= QD_MONTHS_CACHE_SIZE; i++) {
                LocalDate monthDate = mCurrentDate.plusMonths(i);
                addMonthToCache(monthDate);
            }

            // Trova la posizione di oggi
            mTodayPosition = SharedViewModels.DataConverter.findTodayPosition(mItemsCache);

            // Posizione centrale nella cache
            mCurrentCenterPosition = mItemsCache.size() / 2;

            // Setup adapter
            setupAdapter();

            // Setup scroll listener ottimizzato
            mRecyclerView.addOnScrollListener(new OptimizedInfiniteScrollListener());

            // Scrolla alla posizione appropriata - se useLayoutManager
            if (useLayoutManager) {
                scrollToInitialPosition();
            }

            Log.d(TAG, "Setup infinito completato: " + mItemsCache.size() +
                    " elementi, oggi alla posizione: " + mTodayPosition);

        } catch (Exception e) {
            Log.e(TAG, "Error setupInfiniteScrolling(): " + e.getMessage());
        }
    }

    /**
     * Listener ottimizzato per scroll infinito con controllo velocità.
     */
    private class OptimizedInfiniteScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            long currentTime = System.currentTimeMillis();

            // Calcola velocità di scroll
            if (mLastScrollTime > 0) {
                long timeDiff = currentTime - mLastScrollTime;
                if (timeDiff > 0) {
                    mScrollVelocity = (int) (Math.abs(dy) / timeDiff * 16); // Normalizzato a 60fps
                }
            }

            // Throttling per evitare aggiornamenti troppo frequenti
            if (currentTime - mLastScrollTime < 100) return; // 100ms = ~10fps
            mLastScrollTime = currentTime;

            // Se scroll troppo veloce, ignora gli aggiornamenti
            if (mScrollVelocity > MAX_SCROLL_VELOCITY) return;

            // Ottieni posizioni visibili
            int firstVisible = getFirstVisiblePosition();
            int lastVisible = getLastVisiblePosition();

            if (firstVisible == RecyclerView.NO_POSITION) return;

            // Gestisci caricamento basato su scroll
            handleScrollBasedLoading(firstVisible, lastVisible, dy);

            // Aggiorna visibilità FAB
            updateFabVisibility(firstVisible, lastVisible);
        }

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            if (LOG_ENABLED) {
                String stateStr = newState == RecyclerView.SCROLL_STATE_IDLE ? "IDLE" :
                        newState == RecyclerView.SCROLL_STATE_DRAGGING ? "DRAGGING" : "SETTLING";
                Log.v(TAG, "Scroll state: " + stateStr + ", velocity: " + mScrollVelocity);
            }

            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                mScrollVelocity = 0;

                // Processa operazioni pendenti dopo che lo scroll si è fermato
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
     * Espone executeTopLoad come protected per le sottoclassi.
     */
    protected void executeTopLoad() {
        if (!mIsPendingTopLoad.compareAndSet(true, false)) return;
        if (!mIsUpdatingCache.compareAndSet(false, true)) return;

        try {
            LocalDate firstMonth = findFirstMonthInCache();
            if (firstMonth != null) {
                LocalDate prevMonth = firstMonth.minusMonths(1);

                mMainHandler.post(() -> {
                    try {
                        List<SharedViewModels.ViewItem> newItems = generateMonthItems(prevMonth);
                        mItemsCache.addAll(0, newItems);

                        // Aggiorna posizioni
                        if (mTodayPosition >= 0) mTodayPosition += newItems.size();
                        mCurrentCenterPosition += newItems.size();

                        // Notifica adapter
                        getAdapter().notifyItemRangeInserted(0, newItems.size());

                        // Mantieni posizione scroll - adattato per diversi LayoutManager
                        maintainScrollPosition(newItems.size());

                        hideTopLoader();

                        if (LOG_ENABLED) Log.d(TAG, "Aggiunto mese precedente: " + prevMonth);
                    } finally {
                        mIsUpdatingCache.set(false);
                    }
                });
            } else {
                mIsUpdatingCache.set(false);
                hideTopLoader();
            }
        } catch (Exception e) {
            if (LOG_ENABLED) Log.e(TAG, "Errore caricamento precedente: " + e.getMessage());
            mIsUpdatingCache.set(false);
            hideTopLoader();
        }
    }

    /**
     * Espone executeBottomLoad come protected per le sottoclassi.
     */
    protected void executeBottomLoad() {
        if (!mIsPendingBottomLoad.compareAndSet(true, false)) return;
        if (!mIsUpdatingCache.compareAndSet(false, true)) return;

        try {
            LocalDate lastMonth = findLastMonthInCache();
            if (lastMonth != null) {
                LocalDate nextMonth = lastMonth.plusMonths(1);

                mMainHandler.post(() -> {
                    try {
                        List<SharedViewModels.ViewItem> newItems = generateMonthItems(nextMonth);
                        int insertPos = mItemsCache.size();
                        mItemsCache.addAll(newItems);

                        // Controlla se oggi è nei nuovi elementi
                        if (mTodayPosition < 0) {
                            int todayInNew = SharedViewModels.DataConverter.findTodayPosition(newItems);
                            if (todayInNew >= 0) {
                                mTodayPosition = insertPos + todayInNew;
                            }
                        }

                        // Notifica adapter
                        getAdapter().notifyItemRangeInserted(insertPos, newItems.size());

                        hideBottomLoader();

                        if (LOG_ENABLED) Log.d(TAG, "Aggiunto mese successivo: " + nextMonth);
                    } finally {
                        mIsUpdatingCache.set(false);
                    }
                });
            } else {
                mIsUpdatingCache.set(false);
                hideBottomLoader();
            }
        } catch (Exception e) {
            if (LOG_ENABLED) Log.e(TAG, "Errore caricamento successivo: " + e.getMessage());
            mIsUpdatingCache.set(false);
            hideBottomLoader();
        }
    }

    /**
     * Gestisce il caricamento basato sulla posizione dello scroll.
     */
    private void handleScrollBasedLoading(int firstVisible, int lastVisible, int scrollDirection) {
        // Caricamento verso l'alto (mesi precedenti)
        if (firstVisible <= 10 && scrollDirection <= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingTopLoad.get() && !mShowingTopLoader) {

            if (LOG_ENABLED) Log.d(TAG, "Triggering top load at position: " + firstVisible);
            triggerTopLoad();
        }

        // Caricamento verso il basso (mesi successivi)
        if (lastVisible >= mItemsCache.size() - 10 && scrollDirection >= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingBottomLoad.get() && !mShowingBottomLoader) {

            if (LOG_ENABLED) Log.d(TAG, "Triggering bottom load at position: " + lastVisible);
            triggerBottomLoad();
        }
    }

    /**
     * Avvia il caricamento dei mesi precedenti.
     * Protected - se una sottoclasse implementa uno scroll listener personalizzato
     * deve accedere a questi metodi
     */
    protected void triggerTopLoad() {
        mIsPendingTopLoad.set(true);
        showTopLoader();
        mBackgroundHandler.postDelayed(this::executeTopLoad, 100);
    }

    /**
     * Avvia il caricamento dei mesi successivi.
     * Protected - se una sottoclasse implementa uno scroll listener personalizzato
     * deve accedere a questi metodi
     */
    protected void triggerBottomLoad() {
        mIsPendingBottomLoad.set(true);
        showBottomLoader();
        mBackgroundHandler.postDelayed(this::executeBottomLoad, 100);
    }

    /**
     * Aggiunge un mese alla cache.
     */
    private void addMonthToCache(LocalDate monthDate) {
        List<SharedViewModels.ViewItem> monthItems = generateMonthItems(monthDate);
        mItemsCache.addAll(monthItems);
    }

    /**
     * Genera gli elementi per un mese utilizzando il convertitore specifico del fragment.
     */
    private List<SharedViewModels.ViewItem> generateMonthItems(LocalDate monthDate) {
        List<Day> monthDays = mDataManager.getMonthDays(monthDate);
        return convertMonthData(monthDays, monthDate);
    }

    /**
     * Processa le operazioni pendenti quando lo scroll si ferma.
     * Protected - se una sottoclasse implementa uno scroll listener personalizzato
     * deve accedere a questi metodi
     */
    protected void processPendingOperations() {
        if (mIsPendingTopLoad.get() && !mIsUpdatingCache.get()) {
            executeTopLoad();
        }
        if (mIsPendingBottomLoad.get() && !mIsUpdatingCache.get()) {
            executeBottomLoad();
        }
    }

    /**
     * Programma la pulizia della cache se necessario.
     * Protected - se una sottoclasse implementa uno scroll listener personalizzato
     * deve accedere a questi metodi
     */
    protected void scheduleCleanupIfNeeded() {
        int maxElements = QD_MAX_CACHE_SIZE * 35; // ~35 elementi per mese
        if (mItemsCache.size() > maxElements) {
            mBackgroundHandler.postDelayed(() -> {
                if (!mIsUpdatingCache.get() && mScrollVelocity == 0) {
                    cleanupCache();
                }
            }, 1000);
        }
    }

    /**
     * Pulisce la cache rimuovendo elementi lontani dalla posizione corrente.
     */
    private void cleanupCache() {
        if (!mIsUpdatingCache.compareAndSet(false, true)) return;

        mMainHandler.post(() -> {
            try {
                int currentPos = mLayoutManager.findFirstCompletelyVisibleItemPosition();
                int targetSize = QD_MONTHS_CACHE_SIZE * 35;

                // Rimuovi dall'inizio se necessario
                while (mItemsCache.size() > targetSize && currentPos > targetSize / 2) {
                    mItemsCache.remove(0);
                    currentPos--;
                    if (mTodayPosition > 0) mTodayPosition--;
                    if (mCurrentCenterPosition > 0) mCurrentCenterPosition--;
                    getAdapter().notifyItemRemoved(0);
                }

                // Rimuovi dalla fine se necessario
                while (mItemsCache.size() > targetSize &&
                        currentPos < mItemsCache.size() - targetSize / 2) {
                    int lastIndex = mItemsCache.size() - 1;
                    mItemsCache.remove(lastIndex);
                    getAdapter().notifyItemRemoved(lastIndex);
                }

                if (LOG_ENABLED) Log.d(TAG, "Cache pulita, dimensione: " + mItemsCache.size());
            } finally {
                mIsUpdatingCache.set(false);
            }
        });
    }

    /**
     * Scrolla alla posizione iniziale appropriata.
     * protected - gridlayoutmanager have to scroll to initial position
     *             by themselves
     */
    protected void scrollToInitialPosition() {
        if (mTodayPosition >= 0) {
            mLayoutManager.scrollToPosition(mTodayPosition);
        } else {
            mLayoutManager.scrollToPosition(mCurrentCenterPosition);
        }
    }

    /**
     * Scrolla al giorno di oggi.
     */
    protected void scrollToToday() {
        if (mTodayPosition >= 0 && mTodayPosition < mItemsCache.size()) {
            mRecyclerView.smoothScrollToPosition(mTodayPosition);
            if (LOG_ENABLED) Log.d(TAG, "Scrolling a oggi, posizione: " + mTodayPosition);
        } else {
            // Ricostruisci centrato su oggi
            if (LOG_ENABLED) Log.d(TAG, "Oggi non in cache, ricostruzione");
            mCurrentDate = LocalDate.now().withDayOfMonth(1);
            setupInfiniteScrolling();
        }
    }

    // === GESTIONE LOADER ===

    private void showTopLoader() {
        if (mShowingTopLoader) return;
        mShowingTopLoader = true;

        SharedViewModels.LoadingItem loader = new SharedViewModels.LoadingItem(
                SharedViewModels.LoadingItem.LoadingType.TOP);
        mItemsCache.add(0, loader);

        if (mTodayPosition >= 0) mTodayPosition++;
        mCurrentCenterPosition++;

        getAdapter().notifyItemInserted(0);
    }

    private void showBottomLoader() {
        if (mShowingBottomLoader) return;
        mShowingBottomLoader = true;

        SharedViewModels.LoadingItem loader = new SharedViewModels.LoadingItem(
                SharedViewModels.LoadingItem.LoadingType.BOTTOM);
        mItemsCache.add(loader);

        getAdapter().notifyItemInserted(mItemsCache.size() - 1);
    }

    private void hideTopLoader() {
        if (!mShowingTopLoader) return;

        for (int i = 0; i < mItemsCache.size(); i++) {
            if (mItemsCache.get(i) instanceof SharedViewModels.LoadingItem) {
                SharedViewModels.LoadingItem loading = (SharedViewModels.LoadingItem) mItemsCache.get(i);
                if (loading.loadingType == SharedViewModels.LoadingItem.LoadingType.TOP) {
                    mItemsCache.remove(i);
                    if (mTodayPosition > i) mTodayPosition--;
                    if (mCurrentCenterPosition > i) mCurrentCenterPosition--;
                    getAdapter().notifyItemRemoved(i);
                    break;
                }
            }
        }
        mShowingTopLoader = false;
    }

    private void hideBottomLoader() {
        if (!mShowingBottomLoader) return;

        for (int i = mItemsCache.size() - 1; i >= 0; i--) {
            if (mItemsCache.get(i) instanceof SharedViewModels.LoadingItem) {
                SharedViewModels.LoadingItem loading = (SharedViewModels.LoadingItem) mItemsCache.get(i);
                if (loading.loadingType == SharedViewModels.LoadingItem.LoadingType.BOTTOM) {
                    mItemsCache.remove(i);
                    getAdapter().notifyItemRemoved(i);
                    break;
                }
            }
        }
        mShowingBottomLoader = false;
    }

    // === UTILITY METHODS ===

    private void resetControlFlags() {
        mIsUpdatingCache.set(false);
        mIsPendingTopLoad.set(false);
        mIsPendingBottomLoad.set(false);
        mShowingTopLoader = false;
        mShowingBottomLoader = false;
    }

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

    // === LIFECYCLE ===

    @Override
    public void onStart() {
        super.onStart();
        if (LOG_ENABLED) Log.d(TAG, "onStart: " + getClass().getSimpleName());

        if (mQD != null) {
            mQD.updatePreferences(getActivity());
            if (mQD.isRefresh()) {
                mQD.setRefresh(false);
                refreshData();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (LOG_ENABLED) Log.d(TAG, "onResume: " + getClass().getSimpleName());

        // Aggiorna la posizione di oggi
        mTodayPosition = SharedViewModels.DataConverter.findTodayPosition(mItemsCache);
        if (getAdapter() != null) {
            getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Cancella tutte le operazioni pendenti
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
        }
        if (mBackgroundHandler != null) {
            mBackgroundHandler.removeCallbacksAndMessages(null);
        }

        // Pulisci le risorse
        if (mItemsCache != null) {
            mItemsCache.clear();
        }
        if (mRecyclerView != null) {
            mRecyclerView.clearOnScrollListeners();
        }

        if (LOG_ENABLED) Log.d(TAG, "onDestroyView: " + getClass().getSimpleName());
    }

    /**
     * Refresh dei dati quando cambiano le preferenze.
     */
    public void refreshData() {
        if (LOG_ENABLED) Log.d(TAG, "refreshData chiamato");

        // Pulisci cache del data manager
        mDataManager.clearCache();

        // Rigenera i dati
        setupInfiniteScrolling();
    }

    /**
     * Notifica aggiornamenti dall'esterno.
     */
    public void notifyUpdates() {
        if (LOG_ENABLED) Log.d(TAG, "notifyUpdates chiamato");

        // Aggiorna team utente se necessario
        onUserTeamChanged();

        if (getAdapter() != null) {
            getAdapter().notifyDataSetChanged();
        }
    }

    /**
     * Chiamato quando cambia il team dell'utente.
     */
    protected void onUserTeamChanged() {
        // Le sottoclassi possono override per logica specifica
        Log.d(TAG, "onUserTeamChanged");
    }

    // ==================== AGGIUNTE / MODIFICHE ALLA BASECALENDARFRAGMENT ====================

    /*
        Per supportare meglio il CalendarViewFragment, la classe BaseCalendarFragment
        dovrebbe esporre alcuni metodi come protected invece che private:

        Nella classe BaseCalendarFragment, cambia questi metodi da private a protected:
        - executeTopLoad()
        - executeBottomLoad()
        - processPendingOperations()
        - scheduleCleanupIfNeeded()
        - triggerTopLoad()
        - triggerBottomLoad()

        E aggiungi questo metodo per supportare layout manager personalizzati:

        protected void setCustomLayoutManager(RecyclerView.LayoutManager layoutManager) {
            if (mRecyclerView != null) {
                mRecyclerView.setLayoutManager(layoutManager);
                if (layoutManager instanceof LinearLayoutManager) {
                    mLayoutManager = (LinearLayoutManager) layoutManager;
                }
            }
        }
    */

    /**
     * Permette alle sottoclassi di impostare un LayoutManager personalizzato.
     */
    protected void setCustomLayoutManager(RecyclerView.LayoutManager layoutManager) {
        if (mRecyclerView != null) {
            mRecyclerView.setLayoutManager(layoutManager);
            if (layoutManager instanceof LinearLayoutManager) {
                mLayoutManager = (LinearLayoutManager) layoutManager;
            } else {
                // Per GridLayoutManager o altri, mantieni riferimento per compatibilità
                mLayoutManager = null;
            }
        }
    }

    /**
     * Mantiene la posizione di scroll adattandosi al tipo di LayoutManager.
     */
    private void maintainScrollPosition(int itemsAdded) {
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();

        if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager linearManager = (LinearLayoutManager) layoutManager;
            linearManager.scrollToPositionWithOffset(itemsAdded, 0);
        } else if (layoutManager instanceof GridLayoutManager) {
            GridLayoutManager gridManager = (GridLayoutManager) layoutManager;
            int currentFirst = gridManager.findFirstVisibleItemPosition();
            if (currentFirst >= 0) {
                gridManager.scrollToPosition(currentFirst + itemsAdded);
            }
        }
    }

    /**
     * Utility per ottenere la prima posizione visibile indipendentemente dal LayoutManager.
     */
    protected int getFirstVisiblePosition() {
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();

        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
        } else if (layoutManager instanceof GridLayoutManager) {
            return ((GridLayoutManager) layoutManager).findFirstVisibleItemPosition();
        }

        return RecyclerView.NO_POSITION;
    }

    /**
     * Utility per ottenere l'ultima posizione visibile indipendentemente dal LayoutManager.
     */
    protected int getLastVisiblePosition() {
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();

        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
        } else if (layoutManager instanceof GridLayoutManager) {
            return ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
        }

        return RecyclerView.NO_POSITION;
    }

    /**
     * Scroll sicuro che funziona con qualsiasi LayoutManager.
     */
    protected void scrollToPositionSafely(int position) {
        if (position < 0 || position >= mItemsCache.size()) return;

        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();

        if (layoutManager instanceof LinearLayoutManager) {
            ((LinearLayoutManager) layoutManager).scrollToPosition(position);
        } else if (layoutManager instanceof GridLayoutManager) {
            ((GridLayoutManager) layoutManager).scrollToPosition(position);
        } else {
            mRecyclerView.scrollToPosition(position);
        }
    }

    /**
     * Smooth scroll sicuro che funziona con qualsiasi LayoutManager.
     */
    protected void smoothScrollToPositionSafely(int position) {
        if (position < 0 || position >= mItemsCache.size()) return;

        mRecyclerView.smoothScrollToPosition(position);
    }

    // =========================================================
    // === METODI ASTRATTI DA IMPLEMENTARE NELLE SOTTOCLASSI ===

    /**
     * Trova e inizializza le viste specifiche del fragment.
     */
    protected abstract void findViews(View rootView);

    /**
     * Converte i dati del mese nel formato richiesto dal fragment specifico.
     */
    protected abstract List<SharedViewModels.ViewItem> convertMonthData(List<Day> days, LocalDate monthDate);

    /**
     * Setup dell'adapter specifico del fragment.
     * La classe figlia, crea un istanza di Adapter propria e specifica
     * La assegna al recyclerView con mRecyclerView,setAdapter( adapter )
     */
    protected abstract void setupAdapter();

    /**
     * Restituisce l'adapter corrente.
     */
    protected abstract RecyclerView.Adapter<?> getAdapter();

    /**
     * Aggiorna la visibilità del FAB basata sulla posizione di scroll.
     */
    protected abstract void updateFabVisibility(int firstVisible, int lastVisible);
}