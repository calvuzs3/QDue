package net.calvuz.qdue.ui.dayslist;

import static net.calvuz.qdue.quattrodue.Costants.QD_MAX_CACHE_SIZE;
import static net.calvuz.qdue.quattrodue.Costants.QD_MONTHS_CACHE_SIZE;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.utils.Log;
import net.calvuz.qdue.utils.ThemeUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fragment per la visualizzazione lista dei turni con scrolling infinito ottimizzato.
 * Utilizza la stessa logica migliorata del CalendarViewFragment.
 */
public class DayslistViewFragment extends Fragment {

    private static final String TAG = "DayslistViewFragment";
    private static final boolean LOG_ENABLED = true;

    // Identificatori per i diversi tipi di view nell'adapter
    private static final int VIEW_TYPE_MONTH_HEADER = 0;
    private static final int VIEW_TYPE_DAY = 1;
    private static final int VIEW_TYPE_LOADING = 2;

    private QuattroDue mQD;
    private RecyclerView mDaysRecyclerView;
    private MonthsAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private FloatingActionButton mFabGoToToday;

    // Cache dei mesi per lo scrolling infinito
    private List<Object> mItemsCache; // Contiene MonthData, DayData e LoadingItem
    private int mCurrentCenterPosition;
    private LocalDate mCurrentDate;

    // Sistema di controllo per evitare operazioni multiple simultanee
    private final AtomicBoolean mIsUpdatingCache = new AtomicBoolean(false);
    private final AtomicBoolean mIsPendingTopLoad = new AtomicBoolean(false);
    private final AtomicBoolean mIsPendingBottomLoad = new AtomicBoolean(false);

    // Handler per gestire operazioni asincrone
    private Handler mMainHandler;
    private Handler mBackgroundHandler;

    // Posizione del giorno contenente oggi
    private int mTodayDayPosition = -1;

    // Flag per indicare se stiamo mostrando indicatori di caricamento
    private boolean mShowingTopLoader = false;
    private boolean mShowingBottomLoader = false;

    // Controllo dello scroll per evitare aggiornamenti durante scroll veloce
    private long mLastScrollTime = 0;
    private int mLastScrollDirection = 0; // -1 = su, 1 = giù, 0 = fermo
    private int mScrollVelocity = 0;
    private static final int MAX_SCROLL_VELOCITY = 20;
    private static final long SCROLL_SETTLE_DELAY = 150; // ms

    // Listener per comunicazione con l'activity
    private OnQuattroDueHomeFragmentInteractionListener mListener = null;

    public DayslistViewFragment() {
        // Costruttore vuoto richiesto
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inizializza i gestori
        mMainHandler = new Handler(Looper.getMainLooper());
        mBackgroundHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dayslist_view, container, false);

        if (LOG_ENABLED) Log.d(TAG, "onCreateView");

        // Inizializza le viste
        mDaysRecyclerView = root.findViewById(R.id.rv_dayslist);
        mFabGoToToday = root.findViewById(R.id.fab_go_to_today);

        // Configura il RecyclerView per lo scroll verticale
        mLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mDaysRecyclerView.setLayoutManager(mLayoutManager);

        // Configura il FAB
        if (mFabGoToToday != null) {
            mFabGoToToday.setOnClickListener(v -> scrollToToday());
        }

        // Inizializza QuattroDue
        if (getActivity() != null) {
            mQD = QuattroDue.getInstance(getActivity().getApplicationContext());
            setupInfiniteDaysList();
        }

        return root;
    }

    /**
     * Configura la lista giorni con scrolling infinito.
     */
    private void setupInfiniteDaysList() {
        if (mQD == null) {
            if (LOG_ENABLED) Log.e(TAG, "setupInfiniteDaysList: mQD è null");
            return;
        }

        try {
            // Reset dei flag di controllo
            mIsUpdatingCache.set(false);
            mIsPendingTopLoad.set(false);
            mIsPendingBottomLoad.set(false);
            mShowingTopLoader = false;
            mShowingBottomLoader = false;

            // Inizializza la cache degli elementi
            mCurrentDate = mQD.getCursorDate();
            mItemsCache = new ArrayList<>();

            // Genera i mesi nella cache
            for (int i = -QD_MONTHS_CACHE_SIZE; i <= QD_MONTHS_CACHE_SIZE; i++) {
                LocalDate monthDate = mCurrentDate.plusMonths(i);
                List<Object> monthItems = generateMonthItems(monthDate);
                mItemsCache.addAll(monthItems);

                // Trova la posizione del giorno contenente oggi
                findTodayPosition(monthItems, mItemsCache.size() - monthItems.size());
            }

            // Posizione centrale nella cache (approssimativamente)
            mCurrentCenterPosition = mItemsCache.size() / 2;

            // Crea e imposta l'adapter
            mAdapter = new MonthsAdapter(mItemsCache, mQD.getUserHalfTeam());
            mDaysRecyclerView.setAdapter(mAdapter);

            // Posiziona il RecyclerView al giorno corrente se trovato
            if (mTodayDayPosition >= 0) {
                mLayoutManager.scrollToPosition(mTodayDayPosition);
            } else {
                mLayoutManager.scrollToPosition(mCurrentCenterPosition);
            }

            // Aggiunge il listener per lo scrolling infinito
            mDaysRecyclerView.addOnScrollListener(new ImprovedInfiniteScrollListener());

            if (LOG_ENABLED)
                Log.d(TAG, "Lista giorni infinita configurata con " + mItemsCache.size() + " elementi");
        } catch (Exception e) {
            if (LOG_ENABLED)
                Log.e(TAG, "Errore nella configurazione della lista giorni: " + e.getMessage());
        }
    }

    /**
     * Genera gli elementi per un mese specifico (header + giorni).
     */
    private List<Object> generateMonthItems(LocalDate monthDate) {
        List<Object> monthItems = new ArrayList<>();

        try {
            // Ottiene i giorni del mese
            List<Day> days = getMonthDays(monthDate);

            if (!days.isEmpty()) {
                // Aggiunge l'header del mese
                String monthTitle = formatMonthTitle(monthDate);
                monthItems.add(new MonthHeaderData(monthTitle, monthDate));

                // Aggiunge tutti i giorni del mese
                for (Day day : days) {
                    monthItems.add(new DayData(day, monthDate));
                }

                if (LOG_ENABLED) Log.d(TAG, "Generati " + monthItems.size() +
                        " elementi per " + monthDate + " (1 header + " + days.size() + " giorni)");
            }

        } catch (Exception e) {
            if (LOG_ENABLED)
                Log.e(TAG, "Errore nella generazione elementi per " + monthDate + ": " + e.getMessage());
        }

        return monthItems;
    }

    /**
     * Ottiene i giorni per un mese specifico.
     */
    private List<Day> getMonthDays(LocalDate monthDate) {
        try {
            List<Day> days = mQD.getShiftsForMonth(monthDate);

            // Aggiorna i flag "oggi"
            LocalDate today = LocalDate.now();
            for (Day day : days) {
                if (day.getDate() != null) {
                    day.setIsToday(day.getDate().equals(today));
                }
            }

            return days;
        } catch (Exception e) {
            if (LOG_ENABLED)
                Log.e(TAG, "Errore nell'ottenere i giorni per " + monthDate + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Formatta il titolo del mese.
     */
    private String formatMonthTitle(LocalDate monthDate) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter;

        if (monthDate.getYear() == today.getYear()) {
            formatter = DateTimeFormatter.ofPattern("MMMM", Locale.ITALIAN);
        } else {
            formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN);
        }

        return monthDate.format(formatter);
    }

    /**
     * Trova la posizione di oggi negli elementi del mese.
     */
    private void findTodayPosition(List<Object> monthItems, int startIndex) {
        if (mTodayDayPosition >= 0) return; // Già trovato

        LocalDate today = LocalDate.now();

        for (int i = 0; i < monthItems.size(); i++) {
            Object item = monthItems.get(i);
            if (item instanceof DayData) {
                DayData dayData = (DayData) item;
                if (dayData.getDay().getDate() != null &&
                        dayData.getDay().getDate().equals(today)) {
                    mTodayDayPosition = startIndex + i;
                    if (LOG_ENABLED)
                        Log.d(TAG, "Trovato oggi alla posizione: " + mTodayDayPosition);
                    break;
                }
            }
        }
    }

    /**
     * Listener migliorato per lo scrolling infinito.
     */
    private class ImprovedInfiniteScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            long currentTime = System.currentTimeMillis();

            // Calcola la velocità di scroll
            long timeDiff = currentTime - mLastScrollTime;
            if (timeDiff > 0) {
                mScrollVelocity = (int) (Math.abs(dy) / timeDiff * 16);
            }

            // Determina la direzione dello scroll
            int currentDirection = dy > 0 ? 1 : (dy < 0 ? -1 : 0);

            // Se lo scroll è troppo veloce, ignora gli aggiornamenti
            if (mScrollVelocity > MAX_SCROLL_VELOCITY) {
                mLastScrollTime = currentTime;
                mLastScrollDirection = currentDirection;
                return;
            }

            // Throttling
            if (timeDiff < 50) return;

            mLastScrollTime = currentTime;
            mLastScrollDirection = currentDirection;

            // Ottieni le posizioni visibili
            int firstVisiblePosition = mLayoutManager.findFirstVisibleItemPosition();
            int lastVisiblePosition = mLayoutManager.findLastVisibleItemPosition();

            if (firstVisiblePosition == RecyclerView.NO_POSITION ||
                    lastVisiblePosition == RecyclerView.NO_POSITION) return;

            // Logica per il caricamento
            handleScrollBasedLoading(firstVisiblePosition, lastVisiblePosition, currentDirection);

            // Aggiorna la visibilità del FAB
            updateFabVisibility(firstVisiblePosition, lastVisiblePosition);
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
     * Gestisce il caricamento basato sulla posizione dello scroll.
     */
    private void handleScrollBasedLoading(int firstVisible, int lastVisible, int direction) {
        // Controlla se siamo vicini all'inizio
        if (firstVisible <= 10 && direction <= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingTopLoad.get() && !mShowingTopLoader) {

            if (LOG_ENABLED) Log.d(TAG, "Triggering top load at position: " + firstVisible);
            mIsPendingTopLoad.set(true);
            showTopLoader();

            mBackgroundHandler.postDelayed(this::executeTopLoad, 100);
        }

        // Controlla se siamo vicini alla fine
        if (lastVisible >= mItemsCache.size() - 10 && direction >= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingBottomLoad.get() && !mShowingBottomLoader) {

            if (LOG_ENABLED) Log.d(TAG, "Triggering bottom load at position: " + lastVisible);
            mIsPendingBottomLoad.set(true);
            showBottomLoader();

            mBackgroundHandler.postDelayed(this::executeBottomLoad, 100);
        }
    }

    /**
     * Processa le operazioni pendenti.
     */
    private void processPendingOperations() {
        if (mIsPendingTopLoad.get() && !mIsUpdatingCache.get()) {
            executeTopLoad();
        }

        if (mIsPendingBottomLoad.get() && !mIsUpdatingCache.get()) {
            executeBottomLoad();
        }
    }

    /**
     * Esegue il caricamento dei mesi precedenti.
     */
    private void executeTopLoad() {
        if (!mIsPendingTopLoad.compareAndSet(true, false)) {
            return;
        }

        if (!mIsUpdatingCache.compareAndSet(false, true)) {
            if (LOG_ENABLED) Log.w(TAG, "executeTopLoad: Cache già in aggiornamento");
            return;
        }

        try {
            addPreviousMonth();
        } finally {
            mIsUpdatingCache.set(false);
        }
    }

    /**
     * Esegue il caricamento dei mesi successivi.
     */
    private void executeBottomLoad() {
        if (!mIsPendingBottomLoad.compareAndSet(true, false)) {
            return;
        }

        if (!mIsUpdatingCache.compareAndSet(false, true)) {
            if (LOG_ENABLED) Log.w(TAG, "executeBottomLoad: Cache già in aggiornamento");
            return;
        }

        try {
            addNextMonth();
        } finally {
            mIsUpdatingCache.set(false);
        }
    }

    /**
     * Programma la pulizia della cache se necessario.
     */
    private void scheduleCleanupIfNeeded() {
        if (mItemsCache.size() > QD_MAX_CACHE_SIZE * 35) { // ~35 elementi per mese
            mBackgroundHandler.postDelayed(() -> {
                if (!mIsUpdatingCache.get() && mScrollVelocity == 0) {
                    cleanupCache();
                }
            }, 1000);
        }
    }

    /**
     * Mostra l'indicatore di caricamento superiore.
     */
    private void showTopLoader() {
        if (mShowingTopLoader) return;

        mMainHandler.post(() -> {
            mShowingTopLoader = true;
            mItemsCache.add(0, new LoadingItem(LoadingItem.Type.TOP));

            if (mTodayDayPosition >= 0) {
                mTodayDayPosition++;
            }
            mCurrentCenterPosition++;

            if (mAdapter != null) {
                mAdapter.notifyItemInserted(0);
            }

            if (LOG_ENABLED) Log.d(TAG, "Mostrato loader superiore");
        });
    }

    /**
     * Mostra l'indicatore di caricamento inferiore.
     */
    private void showBottomLoader() {
        if (mShowingBottomLoader) return;

        mMainHandler.post(() -> {
            mShowingBottomLoader = true;
            mItemsCache.add(new LoadingItem(LoadingItem.Type.BOTTOM));

            if (mAdapter != null) {
                mAdapter.notifyItemInserted(mItemsCache.size() - 1);
            }

            if (LOG_ENABLED) Log.d(TAG, "Mostrato loader inferiore");
        });
    }

    /**
     * Rimuove l'indicatore di caricamento superiore.
     */
    private void hideTopLoader() {
        if (!mShowingTopLoader) return;

        mMainHandler.post(() -> {
            for (int i = 0; i < mItemsCache.size(); i++) {
                Object item = mItemsCache.get(i);
                if (item instanceof LoadingItem && ((LoadingItem) item).getType() == LoadingItem.Type.TOP) {
                    mItemsCache.remove(i);

                    if (mTodayDayPosition > i) {
                        mTodayDayPosition--;
                    }
                    if (mCurrentCenterPosition > i) {
                        mCurrentCenterPosition--;
                    }

                    if (mAdapter != null) {
                        mAdapter.notifyItemRemoved(i);
                    }
                    break;
                }
            }

            mShowingTopLoader = false;
            if (LOG_ENABLED) Log.d(TAG, "Nascosto loader superiore");
        });
    }

    /**
     * Rimuove l'indicatore di caricamento inferiore.
     */
    private void hideBottomLoader() {
        if (!mShowingBottomLoader) return;

        mMainHandler.post(() -> {
            for (int i = mItemsCache.size() - 1; i >= 0; i--) {
                Object item = mItemsCache.get(i);
                if (item instanceof LoadingItem && ((LoadingItem) item).getType() == LoadingItem.Type.BOTTOM) {
                    mItemsCache.remove(i);

                    if (mAdapter != null) {
                        mAdapter.notifyItemRemoved(i);
                    }
                    break;
                }
            }

            mShowingBottomLoader = false;
            if (LOG_ENABLED) Log.d(TAG, "Nascosto loader inferiore");
        });
    }

    /**
     * Aggiunge mese precedente alla cache.
     */
    private void addPreviousMonth() {
        if (mItemsCache.isEmpty()) return;

        try {
            // Trova il primo mese reale
            LocalDate firstMonth = null;
            for (Object item : mItemsCache) {
                if (item instanceof MonthHeaderData) {
                    firstMonth = ((MonthHeaderData) item).getMonthDate();
                    break;
                } else if (item instanceof DayData) {
                    firstMonth = ((DayData) item).getMonthDate();
                    break;
                }
            }

            if (firstMonth == null) return;

            // Genera il mese precedente
            LocalDate previousMonth = firstMonth.minusMonths(1);
            List<Object> newMonthItems = generateMonthItems(previousMonth);

            mMainHandler.post(() -> {
                // Inserisci dopo il loader superiore se presente
                int insertPosition = mShowingTopLoader ? 1 : 0;
                mItemsCache.addAll(insertPosition, newMonthItems);

                // Aggiorna le posizioni
                if (mTodayDayPosition >= insertPosition) {
                    mTodayDayPosition += newMonthItems.size();
                } else {
                    // Controlla se oggi è nel nuovo mese
                    findTodayPosition(newMonthItems, insertPosition);
                }

                if (mCurrentCenterPosition >= insertPosition) {
                    mCurrentCenterPosition += newMonthItems.size();
                }

                // Notifica l'adapter
                if (mAdapter != null) {
                    mAdapter.notifyItemRangeInserted(insertPosition, newMonthItems.size());

                    // Mantieni la posizione dello scroll
                    int currentFirst = mLayoutManager.findFirstVisibleItemPosition();
                    if (currentFirst >= 0) {
                        mLayoutManager.scrollToPositionWithOffset(currentFirst + newMonthItems.size(), 0);
                    }
                }

                // Rimuovi il loader
                hideTopLoader();

                if (LOG_ENABLED) Log.d(TAG, "Aggiunto mese precedente: " + previousMonth +
                        " (" + newMonthItems.size() + " elementi)");
            });

        } catch (Exception e) {
            if (LOG_ENABLED)
                Log.e(TAG, "Errore nell'aggiungere mese precedente: " + e.getMessage());
            hideTopLoader();
        }
    }

    /**
     * Aggiunge mese successivo alla cache.
     */
    private void addNextMonth() {
        if (mItemsCache.isEmpty()) return;

        try {
            // Trova l'ultimo mese reale
            LocalDate lastMonth = null;
            for (int i = mItemsCache.size() - 1; i >= 0; i--) {
                Object item = mItemsCache.get(i);
                if (item instanceof MonthHeaderData) {
                    lastMonth = ((MonthHeaderData) item).getMonthDate();
                    break;
                } else if (item instanceof DayData) {
                    lastMonth = ((DayData) item).getMonthDate();
                    break;
                }
            }

            if (lastMonth == null) return;

            // Genera il mese successivo
            LocalDate nextMonth = lastMonth.plusMonths(1);
            List<Object> newMonthItems = generateMonthItems(nextMonth);

            mMainHandler.post(() -> {
                // Inserisci prima del loader inferiore se presente
                int insertPosition = mShowingBottomLoader ? mItemsCache.size() - 1 : mItemsCache.size();
                mItemsCache.addAll(insertPosition, newMonthItems);

                // Controlla se oggi è nel nuovo mese
                if (mTodayDayPosition < 0) {
                    findTodayPosition(newMonthItems, insertPosition);
                }

                // Notifica l'adapter
                if (mAdapter != null) {
                    mAdapter.notifyItemRangeInserted(insertPosition, newMonthItems.size());
                }

                // Rimuovi il loader
                hideBottomLoader();

                if (LOG_ENABLED) Log.d(TAG, "Aggiunto mese successivo: " + nextMonth +
                        " (" + newMonthItems.size() + " elementi)");
            });

        } catch (Exception e) {
            if (LOG_ENABLED)
                Log.e(TAG, "Errore nell'aggiungere mese successivo: " + e.getMessage());
            hideBottomLoader();
        }
    }

    /**
     * Pulisce la cache rimuovendo elementi lontani.
     */
    private void cleanupCache() {
        if (mItemsCache.size() <= QD_MAX_CACHE_SIZE * 35) return;

        if (!mIsUpdatingCache.compareAndSet(false, true)) {
            return;
        }

        try {
            mMainHandler.post(() -> {
                try {
                    int currentVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
                    int targetCacheSize = QD_MONTHS_CACHE_SIZE * 35; // ~35 elementi per mese

                    // Rimuovi dall'inizio se necessario
                    while (mItemsCache.size() > targetCacheSize &&
                            currentVisiblePosition > QD_MONTHS_CACHE_SIZE * 15) {
                        mItemsCache.remove(0);
                        currentVisiblePosition--;

                        if (mTodayDayPosition > 0) mTodayDayPosition--;
                        if (mCurrentCenterPosition > 0) mCurrentCenterPosition--;

                        if (mAdapter != null) {
                            mAdapter.notifyItemRemoved(0);
                        }
                    }

                    // Rimuovi dalla fine se necessario
                    while (mItemsCache.size() > targetCacheSize &&
                            currentVisiblePosition < mItemsCache.size() - QD_MONTHS_CACHE_SIZE * 15) {
                        int lastIndex = mItemsCache.size() - 1;
                        mItemsCache.remove(lastIndex);

                        if (mAdapter != null) {
                            mAdapter.notifyItemRemoved(lastIndex);
                        }
                    }

                    if (LOG_ENABLED)
                        Log.d(TAG, "Cache pulita, dimensione attuale: " + mItemsCache.size());
                } finally {
                    mIsUpdatingCache.set(false);
                }
            });
        } catch (Exception e) {
            if (LOG_ENABLED) Log.e(TAG, "Errore nella pulizia della cache: " + e.getMessage());
            mIsUpdatingCache.set(false);
        }
    }

    /**
     * Aggiorna la visibilità del FAB.
     */
    private void updateFabVisibility(int firstVisible, int lastVisible) {
        if (mFabGoToToday == null) return;

        boolean showFab = true;

        if (mTodayDayPosition >= 0) {
            showFab = !(firstVisible <= mTodayDayPosition && lastVisible >= mTodayDayPosition);
        }

        if (showFab && mFabGoToToday.getVisibility() != View.VISIBLE) {
            mFabGoToToday.show();
        } else if (!showFab && mFabGoToToday.getVisibility() == View.VISIBLE) {
            mFabGoToToday.hide();
        }
    }

    /**
     * Scrolla al giorno di oggi.
     */
    private void scrollToToday() {
        if (mTodayDayPosition >= 0 && mTodayDayPosition < mItemsCache.size()) {
            mDaysRecyclerView.smoothScrollToPosition(mTodayDayPosition);
            if (LOG_ENABLED)
                Log.d(TAG, "Scrolling a oggi, posizione: " + mTodayDayPosition);
        } else {
            if (LOG_ENABLED) Log.d(TAG, "Oggi non in cache, ricostruzione lista");
            LocalDate today = LocalDate.now();
            mCurrentDate = LocalDate.of(today.getYear(), today.getMonth(), 1);
            setupInfiniteDaysList();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (LOG_ENABLED) Log.d(TAG, "onStart");

        if (mQD != null) {
            mQD.updatePreferences(getActivity());
            if (mQD.isRefresh()) {
                mQD.setRefresh(false);
                setupInfiniteDaysList();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (LOG_ENABLED) Log.d(TAG, "onResume");

        // Aggiorna la posizione di oggi
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (LOG_ENABLED) Log.d(TAG, "onAttach");
        if (context instanceof OnQuattroDueHomeFragmentInteractionListener) {
            mListener = (OnQuattroDueHomeFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context +
                    " deve implementare OnQuattroDueHomeFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (LOG_ENABLED) Log.d(TAG, "onDetach");
        mListener = null;
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
        if (mDaysRecyclerView != null) {
            mDaysRecyclerView.clearOnScrollListeners();
        }

        mAdapter = null;
        mLayoutManager = null;
    }

    /**
     * Notifica gli aggiornamenti dell'interfaccia.
     */
    public void notifyUpdates() {
        if (LOG_ENABLED) Log.d(TAG, "notifyUpdates chiamato");

        // Ricarica la cache se necessario
        if (mQD != null) {
            LocalDate newCurrentDate = mQD.getCursorDate();
            if (!newCurrentDate.equals(mCurrentDate)) {
                mCurrentDate = newCurrentDate;
                setupInfiniteDaysList();
            } else if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * Interfaccia per comunicazione con l'activity.
     */
    public interface OnQuattroDueHomeFragmentInteractionListener {
        void onQuattroDueHomeFragmentInteractionListener(long id);
    }

    // ==================== CLASSI DATI ====================

    /**
     * Classe per rappresentare un elemento di caricamento.
     */
    public static class LoadingItem {
        public enum Type {
            TOP, BOTTOM
        }

        private Type type;

        public LoadingItem(Type type) {
            this.type = type;
        }

        public Type getType() {
            return type;
        }
    }

    /**
     * Classe per rappresentare un header di mese.
     */
    public static class MonthHeaderData {
        private String monthTitle;
        private LocalDate monthDate;

        public MonthHeaderData(String monthTitle, LocalDate monthDate) {
            this.monthTitle = monthTitle;
            this.monthDate = monthDate;
        }

        public String getMonthTitle() {
            return monthTitle;
        }

        public LocalDate getMonthDate() {
            return monthDate;
        }
    }

    /**
     * Classe per rappresentare i dati di un giorno.
     */
    public static class DayData {
        private Day day;
        private LocalDate monthDate;

        public DayData(Day day, LocalDate monthDate) {
            this.day = day;
            this.monthDate = monthDate;
        }

        public Day getDay() {
            return day;
        }

        public LocalDate getMonthDate() {
            return monthDate;
        }
    }

    // ==================== ADAPTER ====================

    /**
     * Adapter per la lista dei giorni con scrolling infinito e indicatori di caricamento.
     */
    private class MonthsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<Object> itemsData;
        private HalfTeam userHalfTeam;
        private int mNumShifts = 3; // Numero predefinito di turni

        public MonthsAdapter(List<Object> itemsData, HalfTeam userHalfTeam) {
            this.itemsData = itemsData;
            this.userHalfTeam = userHalfTeam;
        }

        public void updateUserHalfTeam(HalfTeam userHalfTeam) {
            this.userHalfTeam = userHalfTeam;
        }

        @Override
        public int getItemViewType(int position) {
            Object item = itemsData.get(position);
            if (item instanceof LoadingItem) {
                return VIEW_TYPE_LOADING;
            } else if (item instanceof MonthHeaderData) {
                return VIEW_TYPE_MONTH_HEADER;
            } else {
                return VIEW_TYPE_DAY;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            switch (viewType) {
                case VIEW_TYPE_LOADING:
                    View loadingView = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_loading_calendar, parent, false);
                    return new LoadingViewHolder(loadingView);

                case VIEW_TYPE_MONTH_HEADER:
                    View headerView = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_month_header, parent, false);
                    return new MonthHeaderViewHolder(headerView);

                default: // VIEW_TYPE_DAY
                    View dayView = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_dayslist_row, parent, false);
                    return new DayViewHolder(dayView, mNumShifts);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object item = itemsData.get(position);

            if (holder instanceof LoadingViewHolder) {
                ((LoadingViewHolder) holder).bind((LoadingItem) item);
            } else if (holder instanceof MonthHeaderViewHolder) {
                ((MonthHeaderViewHolder) holder).bind((MonthHeaderData) item);
            } else if (holder instanceof DayViewHolder) {
                ((DayViewHolder) holder).bind((DayData) item, userHalfTeam, getContext());
            }
        }

        @Override
        public int getItemCount() {
            return itemsData.size();
        }

        /**
         * ViewHolder per gli indicatori di caricamento.
         */
        class LoadingViewHolder extends RecyclerView.ViewHolder {
            private ProgressBar progressBar;
            private TextView loadingText;

            public LoadingViewHolder(@NonNull View itemView) {
                super(itemView);
                progressBar = itemView.findViewById(R.id.progress_bar);
                loadingText = itemView.findViewById(R.id.tv_loading);
            }

            public void bind(LoadingItem loadingItem) {
                if (loadingItem.getType() == LoadingItem.Type.TOP) {
                    loadingText.setText("Caricamento giorni precedenti...");
                } else {
                    loadingText.setText("Caricamento giorni successivi...");
                }

                progressBar.setVisibility(View.VISIBLE);
            }
        }

        /**
         * ViewHolder per gli header dei mesi.
         */
        class MonthHeaderViewHolder extends RecyclerView.ViewHolder {
            private TextView tvMonthTitle;

            public MonthHeaderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMonthTitle = itemView.findViewById(R.id.tv_month_title);
            }

            public void bind(MonthHeaderData monthHeaderData) {
                tvMonthTitle.setText(monthHeaderData.getMonthTitle());
            }
        }

        /**
         * ViewHolder per i giorni con ottimizazioni prestazioni.
         */
        class DayViewHolder extends RecyclerView.ViewHolder {
            private TextView tday, twday;  // giorno del mese, giorno della settimana
            private TextView[] shiftTexts; // array di textview per i turni
            private TextView ttE, ttR;     // testo per eventi e riposi
            private View mView;            // root view

            // Cache per i colori per evitare chiamate ripetute
            private int sCachedNormalTextColor = 0;
            private int sCachedSundayTextColor = 0;
            private int sCachedTodayBackgroundColor = 0;
            private int sCachedNormalBackgroundColor = 0;
            private int sCachedSundayBackgroundColor = 0;
            private int sCachedUserShiftBackgroundColor = 0;
            private int sCachedUserShiftTextColor = 0;

            public DayViewHolder(@NonNull View itemView, int numShifts) {
                super(itemView);
                mView = itemView;
                tday = itemView.findViewById(R.id.tday);
                twday = itemView.findViewById(R.id.twday);

                // Inizializza array per le TextView dei turni
                shiftTexts = new TextView[numShifts];

                // Assegna le TextView in base al loro ID
                for (int i = 0; i < numShifts && i < 5; i++) { // Massimo 5 turni supportati
                    int resId = itemView.getResources().getIdentifier("tt" + (i + 1), "id",
                            itemView.getContext().getPackageName());
                    if (resId != 0) {
                        shiftTexts[i] = itemView.findViewById(resId);
                    }
                }

                ttR = itemView.findViewById(R.id.ttR);
//                ttE = itemView.findViewById(R.id.ttE);
//
                // Inizializza la cache dei colori
                initializeColorCache(itemView.getContext());
            }

            private void initializeColorCache(Context context) {
                if (sCachedNormalTextColor == 0) {
                    sCachedNormalTextColor = ThemeUtils.getOnNormalBackgroundColor(context);
                    sCachedSundayTextColor = ThemeUtils.getSundayTextColor(context);
                    sCachedTodayBackgroundColor = ThemeUtils.getTodayBackgroundColor(context);
                    sCachedNormalBackgroundColor = ThemeUtils.getNormalBackgroundColor(context);
                    sCachedSundayBackgroundColor = ThemeUtils.getSundayBackgroundColor(context);
                    sCachedUserShiftBackgroundColor = ThemeUtils.getMaterialPrimaryContainerColor(context);
                    sCachedUserShiftTextColor = ThemeUtils.getMaterialOnPrimaryContainerColor(context);
                }
            }

            public void bind(DayData dayData, HalfTeam userHalfTeam, Context context) {
                Day day = dayData.getDay();
                if (day == null) return;

                android.content.res.Resources r = context.getResources();
                boolean isSunday = day.getDayOfWeek() == java.time.DayOfWeek.SUNDAY.getValue();

                // Imposta il numero del giorno
                tday.setText(r.getString(R.string.str_scheme_num, day.getDayOfMonth()));

                // Imposta il nome del giorno della settimana
                twday.setText(r.getString(R.string.str_scheme, day.getDayOfWeekAsString()));

                // Resetta tutti i colori dei turni
                resetShiftViews();
                mView.setBackgroundColor(sCachedNormalBackgroundColor);

                // Imposta i testi per i turni
                List<net.calvuz.qdue.quattrodue.models.Shift> shifts = day.getShifts();
                int numShifts = Math.min(shifts.size(), mNumShifts);

                for (int i = 0; i < numShifts; i++) {
                    if (shiftTexts[i] != null) {
                        try {
                            String teamText = shifts.get(i).getTeamsAsString();
                            shiftTexts[i].setText(teamText != null && !teamText.isEmpty() ?
                                    r.getString(R.string.str_scheme, teamText) : "");
                        } catch (Exception e) {
                            shiftTexts[i].setText("");
                        }
                    }
                }

                // Imposta il testo per le squadre a riposo
                String tR = day.getOffWorkHalfTeamsAsString();
                ttR.setText(tR != null && !tR.isEmpty() ? r.getString(R.string.str_scheme, tR) : "");

                // Imposta il testo per gli eventi
                String tE = day.hasEvents() ? "*" : "  ";
//                ttE.setText(tE);

                // Trova la posizione della squadra dell'utente
                int userPosition = -1;
                if (userHalfTeam != null) {
                    userPosition = day.getInWichTeamIsHalfTeam(userHalfTeam);
                }

                // Evidenzia SOLO il turno dell'utente
                if (userPosition >= 0 && userPosition < numShifts && shiftTexts[userPosition] != null) {
                    shiftTexts[userPosition].setBackgroundColor(sCachedUserShiftBackgroundColor);
                    shiftTexts[userPosition].setTextColor(isSunday ? sCachedSundayTextColor : sCachedUserShiftTextColor);
                }

                // Gestisci i colori per il giorno corrente e la domenica
                if (day.getIsToday()) {
                    // Colore per oggi (priorità massima)
                    mView.setBackgroundColor(sCachedTodayBackgroundColor);
                    setAllTextColors(sCachedNormalTextColor);
                } else if (isSunday) {
                    // Colore per la domenica
                    mView.setBackgroundColor(sCachedSundayBackgroundColor);
                    setAllTextColors(sCachedSundayTextColor);
                } else {
                    // Colori normali
                    setAllTextColors(sCachedNormalTextColor);
                }
            }

            /**
             * Reimposta i colori dei turni ai valori predefiniti.
             */
            private void resetShiftViews() {
                if (shiftTexts != null) {
                    for (TextView tv : shiftTexts) {
                        if (tv != null) {
                            tv.setBackgroundColor(Color.TRANSPARENT);
                            tv.setTextColor(sCachedNormalTextColor);
                        }
                    }
                }
            }

            /**
             * Imposta il colore del testo per tutti gli elementi.
             */
            private void setAllTextColors(int color) {
                tday.setTextColor(color);
                twday.setTextColor(color);
//                ttE.setTextColor(color);
                ttR.setTextColor(color);

                // Non cambiare il colore dei turni evidenziati dell'utente
                for (int i = 0; i < shiftTexts.length; i++) {
                    if (shiftTexts[i] != null &&
                            shiftTexts[i].getBackground() == null ||
                            ((android.graphics.drawable.ColorDrawable) shiftTexts[i].getBackground()).getColor() == Color.TRANSPARENT) {
                        shiftTexts[i].setTextColor(color);
                    }
                }
            }
        }
    }
}