package net.calvuz.qdue.ui.calendar;

import static net.calvuz.qdue.quattrodue.Costants.QD_MAX_CACHE_SIZE;
import static net.calvuz.qdue.quattrodue.Costants.QD_MONTHS_CACHE_SIZE;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.utils.Log;
import net.calvuz.qdue.utils.ThemeUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fragment per la visualizzazione calendario dei turni dell'utente con scrolling infinito ottimizzato.
 * Risolve i problemi di scroll veloce e comportamento imprevedibile.
 */
public class CalendarViewFragment extends Fragment {

    private static final String TAG = "CalendarViewFragment";
    private static final boolean LOG_ENABLED = true;

    // Identificatori per i diversi tipi di view nell'adapter
    private static final int VIEW_TYPE_MONTH = 0;
    private static final int VIEW_TYPE_LOADING = 1;

    private QuattroDue mQD;
    private RecyclerView mMonthsRecyclerView;
    private MonthsAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private FloatingActionButton mFabGoToToday;

    // Cache dei mesi per lo scrolling infinito
    private List<Object> mItemsCache;
    private int mCurrentCenterPosition;
    private LocalDate mCurrentDate;

    // Sistema di controllo per evitare operazioni multiple simultanee
    private final AtomicBoolean mIsUpdatingCache = new AtomicBoolean(false);
    private final AtomicBoolean mIsPendingTopLoad = new AtomicBoolean(false);
    private final AtomicBoolean mIsPendingBottomLoad = new AtomicBoolean(false);

    // Handler per gestire operazioni asincrone
    private Handler mMainHandler;
    private Handler mBackgroundHandler;

    // Posizione del mese contenente oggi
    private int mTodayMonthPosition = -1;

    // Flag per indicare se stiamo mostrando indicatori di caricamento
    private boolean mShowingTopLoader = false;
    private boolean mShowingBottomLoader = false;

    // Controllo dello scroll per evitare aggiornamenti durante scroll veloce
    private long mLastScrollTime = 0;
    private int mLastScrollDirection = 0; // -1 = su, 1 = giù, 0 = fermo
    private int mScrollVelocity = 0;
    private static final int MAX_SCROLL_VELOCITY = 20;
    private static final long SCROLL_SETTLE_DELAY = 150; // ms

    public CalendarViewFragment() {
        // Costruttore vuoto richiesto
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inizializza i gestori
        mMainHandler = new Handler(Looper.getMainLooper());

        // Handler per operazioni in background (simulato con post delayed)
        mBackgroundHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_calendar_view, container, false);

        if (LOG_ENABLED) Log.d(TAG, "onCreateView");

        // Inizializza le viste
        mMonthsRecyclerView = root.findViewById(R.id.rv_calendar);
        mFabGoToToday = root.findViewById(R.id.fab_go_to_today);

        // Configura il RecyclerView per lo scroll verticale
        mLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mMonthsRecyclerView.setLayoutManager(mLayoutManager);

        // Configura il FAB
        mFabGoToToday.setOnClickListener(v -> scrollToToday());

        // Inizializza QuattroDue
        if (getActivity() != null) {
            mQD = QuattroDue.getInstance(getActivity().getApplicationContext());
            setupInfiniteCalendar();
        }

        return root;
    }

    /**
     * Adapter per la lista dei mesi con scrolling infinito e indicatori di caricamento.
     */
    private class MonthsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<Object> itemsData;
        private HalfTeam userHalfTeam;

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
            } else {
                return VIEW_TYPE_MONTH;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_LOADING) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_loading_calendar, parent, false);
                return new LoadingViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_calendar_month, parent, false);
                return new MonthViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object item = itemsData.get(position);

            if (holder instanceof LoadingViewHolder) {
                ((LoadingViewHolder) holder).bind((LoadingItem) item);
            } else if (holder instanceof MonthViewHolder) {
                ((MonthViewHolder) holder).bind((MonthData) item, userHalfTeam);
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
                    loadingText.setText("Caricamento mesi precedenti...");
                } else {
                    loadingText.setText("Caricamento mesi successivi...");
                }

                // Assicurati che il progress bar sia visibile e animato
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        /**
         * ViewHolder per ogni mese.
         */
        class MonthViewHolder extends RecyclerView.ViewHolder {
            private TextView tvMonthTitle;
            private RecyclerView rvCalendarGrid;
            private CalendarViewAdapter calendarViewAdapter;

            public MonthViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMonthTitle = itemView.findViewById(R.id.tv_month_title);
                rvCalendarGrid = itemView.findViewById(R.id.rv_calendar_grid);

                // Configura la griglia interna
                rvCalendarGrid.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(itemView.getContext(), 7));
                rvCalendarGrid.setNestedScrollingEnabled(false);

                // Ottimizzazioni per prestazioni
                rvCalendarGrid.setHasFixedSize(true);
                rvCalendarGrid.setItemAnimator(null); // Disabilita animazioni per migliorare prestazioni
            }

            public void bind(MonthData monthData, HalfTeam userHalfTeam) {
                // Imposta il titolo del mese
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", QDue.getLocale());
                tvMonthTitle.setText(monthData.getMonthDate().format(formatter));

                // Configura l'adapter della griglia
                if (calendarViewAdapter == null) {
                    calendarViewAdapter = new CalendarViewAdapter(monthData.getCalendarDays(), userHalfTeam);
                    rvCalendarGrid.setAdapter(calendarViewAdapter);
                } else {
                    // Riutilizza l'adapter esistente per migliorare prestazioni
                    calendarViewAdapter.updateData(monthData.getCalendarDays(), userHalfTeam);
                }
            }
        }
    }

    /**
     * Adapter per la griglia del calendario con ottimizzazioni per prestazioni.
     */
    private class CalendarViewAdapter extends RecyclerView.Adapter<CalendarViewAdapter.CalendarViewHolder> {

        private List<CalendarDay> calendarDays;
        private HalfTeam userHalfTeam;

        public CalendarViewAdapter(List<CalendarDay> calendarDays, HalfTeam userHalfTeam) {
            this.calendarDays = calendarDays;
            this.userHalfTeam = userHalfTeam;
        }

        /**
         * Aggiorna i dati dell'adapter senza ricreare tutto.
         */
        public void updateData(List<CalendarDay> newCalendarDays, HalfTeam newUserHalfTeam) {
            this.calendarDays = newCalendarDays;
            this.userHalfTeam = newUserHalfTeam;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_day, parent, false);
            return new CalendarViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
            CalendarDay calendarDay = calendarDays.get(position);

            if (calendarDay.isEmpty()) {
                holder.bind(null, null);
            } else {
                holder.bind(calendarDay.getDay(), userHalfTeam);
            }
        }

        @Override
        public int getItemCount() {
            return calendarDays.size();
        }

        /**
         * ViewHolder per ogni cella del calendario con ottimizzazioni.
         */
        class CalendarViewHolder extends RecyclerView.ViewHolder {
            private TextView tvDayNumber;
            private TextView tvDayName;
            private View vShiftIndicator;
            private View itemView;

            // Cache per i colori per evitare chiamate ripetute
            private int sCachedNormalTextColor = 0;
            private int sCachedSundayTextColor = 0;
            private int sCachedTodayBackgroundColor = 0;

            public CalendarViewHolder(@NonNull View itemView) {
                super(itemView);
                this.itemView = itemView;
                tvDayNumber = itemView.findViewById(R.id.tv_day_number);
                tvDayName = itemView.findViewById(R.id.tv_day_name);
                vShiftIndicator = itemView.findViewById(R.id.v_shift_indicator);

                // Inizializza la cache dei colori
                if (sCachedNormalTextColor == 0) {
                    sCachedNormalTextColor = ThemeUtils.getOnNormalBackgroundColor(itemView.getContext());
                    sCachedSundayTextColor = ThemeUtils.getSundayTextColor(itemView.getContext());
                    sCachedTodayBackgroundColor = ThemeUtils.getTodayBackgroundColor(itemView.getContext());
                }
            }

            public void bind(Day day, HalfTeam userHalfTeam) {
                if (day == null) {
                    // Cella vuota - ottimizzazione: imposta tutto in una volta
                    tvDayNumber.setText("");
                    tvDayName.setText("");
                    vShiftIndicator.setVisibility(View.INVISIBLE);
                    itemView.setBackgroundColor(Color.TRANSPARENT);
                    return;
                }

                // Imposta il numero del giorno
                tvDayNumber.setText(String.valueOf(day.getDayOfMonth()));

                // Nascondi il nome del giorno per mantenere il layout pulito
                tvDayName.setVisibility(View.GONE);

                // Evidenzia il giorno corrente
                if (day.getIsToday()) {
                    itemView.setBackgroundColor(sCachedTodayBackgroundColor);
                } else {
                    itemView.setBackgroundColor(Color.TRANSPARENT);
                }

                // Colore rosso per la domenica
                if (day.getDayOfWeek() == DayOfWeek.SUNDAY.getValue()) {
                    tvDayNumber.setTextColor(sCachedSundayTextColor);
                } else {
                    tvDayNumber.setTextColor(sCachedNormalTextColor);
                }

                // Verifica se l'utente ha un turno in questo giorno
                int userShiftPosition = -1;
                if (userHalfTeam != null) {
                    userShiftPosition = day.getInWichTeamIsHalfTeam(userHalfTeam);
                }

                if (userShiftPosition >= 0) {
                    // L'utente ha un turno - mostra l'indicatore colorato
                    vShiftIndicator.setVisibility(View.VISIBLE);

                    // Ottiene il colore del turno usando la cache
                    int shiftColor = getShiftColorFromTheme(itemView.getContext(), userShiftPosition);
                    if (shiftColor != 0) {
                        vShiftIndicator.setBackgroundColor(shiftColor);
                    } else {
                        // Colore di default se non configurato
                        vShiftIndicator.setBackgroundColor(
                                itemView.getContext().getResources().getColor(R.color.background_user_shift));
                    }
                } else {
                    // L'utente non ha turni
                    vShiftIndicator.setVisibility(View.INVISIBLE);
                }
            }

            /**
             * Ottiene il colore configurato per il turno con cache statica.
             */
            private int[] sShiftColorCache = new int[5];
            private boolean sCacheInitialized = false;

            private int getShiftColorFromTheme(android.content.Context context, int shiftPosition) {
                // Inizializza la cache dei colori una sola volta
                if (!sCacheInitialized) {
                    sShiftColorCache[0] = ThemeUtils.getFirstShiftColor(context);
                    sShiftColorCache[1] = ThemeUtils.getSecondShiftColor(context);
                    sShiftColorCache[2] = ThemeUtils.getThirdShiftColor(context);
                    sShiftColorCache[3] = ThemeUtils.getFourthShiftColor(context);
                    sShiftColorCache[4] = ThemeUtils.getFifthShiftColor(context);
                    sCacheInitialized = true;
                }

                if (shiftPosition >= 0 && shiftPosition < sShiftColorCache.length) {
                    return sShiftColorCache[shiftPosition];
                } else {
                    // Colore di fallback usando il colore primario
                    return ThemeUtils.getDynamicPrimaryColor(context);
                }
            }
        }
    }

    /* Configura il calendario con scrolling infinito.
     */
    private void setupInfiniteCalendar() {
        if (mQD == null) {
            if (LOG_ENABLED) Log.e(TAG, "setupInfiniteCalendar: mQD è null");
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
                MonthData monthData = generateMonthData(monthDate);
                mItemsCache.add(monthData);

                // Trova la posizione del mese contenente oggi
                if (isTodayInMonth(monthDate)) {
                    mTodayMonthPosition = mItemsCache.size() - 1;
                }
            }

            // Posizione centrale nella cache
            mCurrentCenterPosition = QD_MONTHS_CACHE_SIZE;

            // Crea e imposta l'adapter
            mAdapter = new MonthsAdapter(mItemsCache, mQD.getUserHalfTeam());
            mMonthsRecyclerView.setAdapter(mAdapter);

            // Posiziona il RecyclerView al mese corrente
            mMonthsRecyclerView.scrollToPosition(mCurrentCenterPosition);

            // Aggiunge il listener per lo scrolling infinito migliorato
            mMonthsRecyclerView.addOnScrollListener(new ImprovedInfiniteScrollListener());

            if (LOG_ENABLED)
                Log.d(TAG, "Calendario infinito configurato con " + mItemsCache.size() + " elementi");
        } catch (Exception e) {
            if (LOG_ENABLED)
                Log.e(TAG, "Errore nella configurazione del calendario: " + e.getMessage());
        }
    }

    /**
     * Listener migliorato per lo scrolling infinito con controllo della velocità.
     */
    private class ImprovedInfiniteScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            long currentTime = System.currentTimeMillis();

            // Calcola la velocità di scroll
            long timeDiff = currentTime - mLastScrollTime;
            if (timeDiff > 0) {
                mScrollVelocity = (int) (Math.abs(dy) / timeDiff * 16); // Normalizzato a 60fps
            }

            // Determina la direzione dello scroll
            int currentDirection = dy > 0 ? 1 : (dy < 0 ? -1 : 0);

            // Se lo scroll è troppo veloce, ignora gli aggiornamenti
            if (mScrollVelocity > MAX_SCROLL_VELOCITY) {
                mLastScrollTime = currentTime;
                mLastScrollDirection = currentDirection;
                return;
            }

            // Throttling per evitare aggiornamenti troppo frequenti
            if (timeDiff < 50) { // 50ms = ~20fps
                return;
            }

            mLastScrollTime = currentTime;
            mLastScrollDirection = currentDirection;

            // Ottieni le posizioni visibili
            int firstVisiblePosition = mLayoutManager.findFirstVisibleItemPosition();
            int lastVisiblePosition = mLayoutManager.findLastVisibleItemPosition();

            if (firstVisiblePosition == RecyclerView.NO_POSITION ||
                    lastVisiblePosition == RecyclerView.NO_POSITION) return;

            // Logica per il caricamento con controlli migliorati
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
                // Lo scroll si è fermato - processa eventuali operazioni pendenti
                mScrollVelocity = 0;

                // Delay per assicurarsi che lo scroll sia completamente fermo
                mMainHandler.postDelayed(() -> {
                    if (mScrollVelocity == 0) { // Verifica che sia ancora fermo
                        processPendingOperations();
                        scheduleCleanupIfNeeded();
                    }
                }, SCROLL_SETTLE_DELAY);
            }
        }
    }

    /**
     * Gestisce il caricamento basato sulla posizione dello scroll con controlli migliorati.
     */
    private void handleScrollBasedLoading(int firstVisible, int lastVisible, int direction) {
        // Controlla se siamo vicini all'inizio (scrolling verso l'alto)
        if (firstVisible <= 3 && direction <= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingTopLoad.get() && !mShowingTopLoader) {

            if (LOG_ENABLED) Log.d(TAG, "Triggering top load at position: " + firstVisible);
            mIsPendingTopLoad.set(true);
            showTopLoader();

            // Delay per evitare operazioni durante scroll attivo
            mBackgroundHandler.postDelayed(this::executeTopLoad, 100);
        }

        // Controlla se siamo vicini alla fine (scrolling verso il basso)
        if (lastVisible >= mItemsCache.size() - 4 && direction >= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingBottomLoad.get() && !mShowingBottomLoader) {

            if (LOG_ENABLED) Log.d(TAG, "Triggering bottom load at position: " + lastVisible);
            mIsPendingBottomLoad.set(true);
            showBottomLoader();

            // Delay per evitare operazioni durante scroll attivo
            mBackgroundHandler.postDelayed(this::executeBottomLoad, 100);
        }
    }

    /**
     * Processa le operazioni pendenti quando lo scroll si è fermato.
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
            return; // Già processato da un'altra chiamata
        }

        if (!mIsUpdatingCache.compareAndSet(false, true)) {
            if (LOG_ENABLED) Log.w(TAG, "executeTopLoad: Cache già in aggiornamento");
            return;
        }

        try {
            addPreviousMonths();
        } finally {
            mIsUpdatingCache.set(false);
        }
    }

    /**
     * Esegue il caricamento dei mesi successivi.
     */
    private void executeBottomLoad() {
        if (!mIsPendingBottomLoad.compareAndSet(true, false)) {
            return; // Già processato da un'altra chiamata
        }

        if (!mIsUpdatingCache.compareAndSet(false, true)) {
            if (LOG_ENABLED) Log.w(TAG, "executeBottomLoad: Cache già in aggiornamento");
            return;
        }

        try {
            addNextMonths();
        } finally {
            mIsUpdatingCache.set(false);
        }
    }

    /**
     * Programma la pulizia della cache se necessario.
     */
    private void scheduleCleanupIfNeeded() {
        if (mItemsCache.size() > QD_MAX_CACHE_SIZE * 2) {
            mBackgroundHandler.postDelayed(() -> {
                if (!mIsUpdatingCache.get() && mScrollVelocity == 0) {
                    cleanupCache();
                }
            }, 1000); // Attesa di 1 secondo per assicurarsi che non ci sia attività
        }
    }

    /**
     * Genera i dati per un mese specifico.
     */
    private MonthData generateMonthData(LocalDate monthDate) {
        List<Day> monthDays = getMonthDays(monthDate);
        List<CalendarDay> calendarDays = prepareCalendarData(monthDays, monthDate);
        return new MonthData(monthDate, calendarDays);
    }

    /**
     * Ottiene i giorni per un mese specifico.
     */
    private List<Day> getMonthDays(LocalDate monthDate) {
        try {
            List<Day> days = mQD.getShiftsForMonth(monthDate);

            LocalDate today = LocalDate.now();
            for (Day day : days) {
                if (day.getDate() != null) {
                    day.setIsToday(day.getDate().equals(today));
                }
            }

            return days;
        } catch (Exception e) {
            if (LOG_ENABLED)
                Log.e(TAG, "Errore nell'ottenere i giorni per il mese " + monthDate + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Prepara i dati per la visualizzazione calendario di un mese.
     */
    private List<CalendarDay> prepareCalendarData(List<Day> monthDays, LocalDate monthDate) {
        List<CalendarDay> calendarDays = new ArrayList<>();

        if (monthDays.isEmpty()) {
            LocalDate firstDay = LocalDate.of(monthDate.getYear(), monthDate.getMonth(), 1);
            int daysInMonth = firstDay.lengthOfMonth();

            for (int i = 1; i <= daysInMonth; i++) {
                LocalDate dayDate = firstDay.withDayOfMonth(i);
                Day day = new Day(dayDate);
                calendarDays.add(new CalendarDay(day));
            }
        } else {
            LocalDate firstDay = monthDays.get(0).getDate();
            int dayOfWeek = firstDay.getDayOfWeek().getValue();
            int emptyCellsAtStart = (dayOfWeek == 7) ? 0 : dayOfWeek;

            for (int i = 0; i < emptyCellsAtStart; i++) {
                calendarDays.add(new CalendarDay());
            }

            for (Day day : monthDays) {
                calendarDays.add(new CalendarDay(day));
            }
        }

        while (calendarDays.size() % 7 != 0) {
            calendarDays.add(new CalendarDay());
        }

        return calendarDays;
    }

    /**
     * Mostra l'indicatore di caricamento in cima alla lista.
     */
    private void showTopLoader() {
        if (mShowingTopLoader) return;

        mMainHandler.post(() -> {
            mShowingTopLoader = true;
            mItemsCache.add(0, new LoadingItem(LoadingItem.Type.TOP));

            // Aggiorna le posizioni
            if (mTodayMonthPosition >= 0) {
                mTodayMonthPosition++;
            }
            mCurrentCenterPosition++;

            if (mAdapter != null) {
                mAdapter.notifyItemInserted(0);
            }

            if (LOG_ENABLED) Log.d(TAG, "Mostrato loader superiore");
        });
    }

    /**
     * Mostra l'indicatore di caricamento in fondo alla lista.
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

                    if (mTodayMonthPosition > i) {
                        mTodayMonthPosition--;
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
     * Aggiunge mesi precedenti alla cache.
     */
    private void addPreviousMonths() {
        if (mItemsCache.isEmpty()) return;

        try {
            // Trova il primo mese reale
            MonthData firstMonth = null;
            for (Object item : mItemsCache) {
                if (item instanceof MonthData) {
                    firstMonth = (MonthData) item;
                    break;
                }
            }

            if (firstMonth == null) return;

            // Genera il mese precedente
            LocalDate previousMonth = firstMonth.getMonthDate().minusMonths(1);
            MonthData newMonthData = generateMonthData(previousMonth);

            mMainHandler.post(() -> {
                // Inserisci dopo il loader superiore se presente
                int insertPosition = mShowingTopLoader ? 1 : 0;
                mItemsCache.add(insertPosition, newMonthData);

                // Aggiorna le posizioni
                if (mTodayMonthPosition >= insertPosition) {
                    mTodayMonthPosition++;
                } else if (isTodayInMonth(previousMonth)) {
                    mTodayMonthPosition = insertPosition;
                }

                if (mCurrentCenterPosition >= insertPosition) {
                    mCurrentCenterPosition++;
                }

                // Notifica l'adapter
                if (mAdapter != null) {
                    mAdapter.notifyItemInserted(insertPosition);

                    // Mantieni la posizione dello scroll in modo fluido
                    int currentFirst = mLayoutManager.findFirstVisibleItemPosition();
                    if (currentFirst >= 0) {
                        mLayoutManager.scrollToPositionWithOffset(currentFirst + 1, 0);
                    }
                }

                // Rimuovi il loader
                hideTopLoader();

                if (LOG_ENABLED) Log.d(TAG, "Aggiunto mese precedente: " + previousMonth);
            });

        } catch (Exception e) {
            if (LOG_ENABLED)
                Log.e(TAG, "Errore nell'aggiungere mesi precedenti: " + e.getMessage());
            hideTopLoader();
        }
    }

    /**
     * Aggiunge mesi successivi alla cache.
     */
    private void addNextMonths() {
        if (mItemsCache.isEmpty()) return;

        try {
            // Trova l'ultimo mese reale
            MonthData lastMonth = null;
            for (int i = mItemsCache.size() - 1; i >= 0; i--) {
                if (mItemsCache.get(i) instanceof MonthData) {
                    lastMonth = (MonthData) mItemsCache.get(i);
                    break;
                }
            }

            if (lastMonth == null) return;

            // Genera il mese successivo
            LocalDate nextMonth = lastMonth.getMonthDate().plusMonths(1);
            MonthData newMonthData = generateMonthData(nextMonth);

            mMainHandler.post(() -> {
                // Inserisci prima del loader inferiore se presente
                int insertPosition = mShowingBottomLoader ? mItemsCache.size() - 1 : mItemsCache.size();
                mItemsCache.add(insertPosition, newMonthData);

                // Aggiorna la posizione di oggi se necessario
                if (mTodayMonthPosition < 0 && isTodayInMonth(nextMonth)) {
                    mTodayMonthPosition = insertPosition;
                }

                // Notifica l'adapter
                if (mAdapter != null) {
                    mAdapter.notifyItemInserted(insertPosition);
                }

                // Rimuovi il loader
                hideBottomLoader();

                if (LOG_ENABLED) Log.d(TAG, "Aggiunto mese successivo: " + nextMonth);
            });

        } catch (Exception e) {
            if (LOG_ENABLED)
                Log.e(TAG, "Errore nell'aggiungere mesi successivi: " + e.getMessage());
            hideBottomLoader();
        }
    }

    /**
     * Pulisce la cache rimuovendo i mesi più lontani.
     */
    private void cleanupCache() {
        if (mItemsCache.size() <= QD_MAX_CACHE_SIZE * 2) return;

        if (!mIsUpdatingCache.compareAndSet(false, true)) {
            return; // Già in corso un aggiornamento
        }

        try {
            mMainHandler.post(() -> {
                try {
                    int currentVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
                    int targetCacheSize = QD_MONTHS_CACHE_SIZE * 2;

                    // Rimuovi dall'inizio se necessario
                    while (mItemsCache.size() > targetCacheSize && currentVisiblePosition > QD_MONTHS_CACHE_SIZE) {
                        mItemsCache.remove(0);
                        currentVisiblePosition--;

                        if (mTodayMonthPosition > 0) mTodayMonthPosition--;
                        if (mCurrentCenterPosition > 0) mCurrentCenterPosition--;

                        if (mAdapter != null) {
                            mAdapter.notifyItemRemoved(0);
                        }
                    }

                    // Rimuovi dalla fine se necessario
                    while (mItemsCache.size() > targetCacheSize &&
                            currentVisiblePosition < mItemsCache.size() - QD_MONTHS_CACHE_SIZE) {
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
     * Verifica se oggi è nel mese specificato.
     */
    private boolean isTodayInMonth(LocalDate monthDate) {
        LocalDate today = LocalDate.now();
        return today.getYear() == monthDate.getYear() &&
                today.getMonth() == monthDate.getMonth();
    }

    /**
     * Aggiorna la visibilità del FAB.
     */
    private void updateFabVisibility(int firstVisible, int lastVisible) {
        boolean showFab = true;

        if (mTodayMonthPosition >= 0) {
            showFab = !(firstVisible <= mTodayMonthPosition && lastVisible >= mTodayMonthPosition);
        }

        if (showFab && mFabGoToToday.getVisibility() != View.VISIBLE) {
            mFabGoToToday.show();
        } else if (!showFab && mFabGoToToday.getVisibility() == View.VISIBLE) {
            mFabGoToToday.hide();
        }
    }

    /**
     * Scrolla al mese contenente oggi.
     */
    private void scrollToToday() {
        if (mTodayMonthPosition >= 0 && mTodayMonthPosition < mItemsCache.size()) {
            mMonthsRecyclerView.smoothScrollToPosition(mTodayMonthPosition);
            if (LOG_ENABLED)
                Log.d(TAG, "Scrolling al mese corrente, posizione: " + mTodayMonthPosition);
        } else {
            if (LOG_ENABLED) Log.d(TAG, "Mese corrente non in cache, ricostruzione calendario");
            LocalDate today = LocalDate.now();
            mCurrentDate = LocalDate.of(today.getYear(), today.getMonth(), 1);
            setupInfiniteCalendar();
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
        if (mMonthsRecyclerView != null) {
            mMonthsRecyclerView.clearOnScrollListeners();
        }

        mAdapter = null;
        mLayoutManager = null;
    }

// [Continua con le classi interne LoadingItem, MonthData, CalendarDay e gli Adapter...]
// Le classi rimangono invariate dalla versione precedente

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
     * Classe per rappresentare i dati di un mese.
     */
    public static class MonthData {
        private LocalDate monthDate;
        private List<CalendarDay> calendarDays;

        public MonthData(LocalDate monthDate, List<CalendarDay> calendarDays) {
            this.monthDate = monthDate;
            this.calendarDays = calendarDays;
        }

        public LocalDate getMonthDate() {
            return monthDate;
        }

        public List<CalendarDay> getCalendarDays() {
            return calendarDays;
        }
    }

    /**
     * Classe per rappresentare una cella del calendario.
     */
    public static class CalendarDay {
        private Day day;
        private boolean isEmpty;

        public CalendarDay() {
            this.isEmpty = true;
            this.day = null;
        }

        public CalendarDay(Day day) {
            this.isEmpty = false;
            this.day = day;
        }

        public Day getDay() {
            return day;
        }

        public boolean isEmpty() {
            return isEmpty;
        }
    }
}