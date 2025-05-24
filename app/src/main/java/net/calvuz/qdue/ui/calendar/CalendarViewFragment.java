package net.calvuz.qdue.ui.calendar;

import static net.calvuz.qdue.quattrodue.Costants.QD_MAX_CACHE_SIZE;
import static net.calvuz.qdue.quattrodue.Costants.QD_MONTHS_CACHE_SIZE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment per la visualizzazione calendario dei turni dell'utente con scrolling infinito.
 * Permette di navigare tra i mesi con scroll verticale fluido.
 */
public class CalendarViewFragment extends Fragment {

    private static final String TAG = "CalendarViewFragment";
    private static final boolean LOG_ENABLED = true;

    private QuattroDue mQD;
    private RecyclerView mMonthsRecyclerView;
    private MonthsAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private FloatingActionButton mFabGoToToday;

    // Cache dei mesi per lo scrolling infinito
    private List<MonthData> mMonthsCache;
    private int mCurrentCenterPosition;
    private LocalDate mCurrentDate;

    // Flag per evitare chiamate ricorsive durante lo scroll
    private boolean mIsUpdatingCache = false;

    // Posizione del mese contenente oggi
    private int mTodayMonthPosition = -1;

    public CalendarViewFragment() {
        // Costruttore vuoto richiesto
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
     * Configura il calendario con scrolling infinito.
     */
    private void setupInfiniteCalendar() {
        if (mQD == null) {
            if (LOG_ENABLED) Log.e(TAG, "setupInfiniteCalendar: mQD è null");
            return;
        }

        try {
            // Inizializza la cache dei mesi
            mCurrentDate = mQD.getCursorDate();
            mMonthsCache = new ArrayList<>();

            // Genera i mesi nella cache (MONTHS_CACHE_SIZE prima e dopo)
            for (int i = -QD_MONTHS_CACHE_SIZE; i <= QD_MONTHS_CACHE_SIZE; i++) {
                LocalDate monthDate = mCurrentDate.plusMonths(i);
                MonthData monthData = generateMonthData(monthDate);
                mMonthsCache.add(monthData);

                // Trova la posizione del mese contenente oggi
                if (isTodayInMonth(monthDate)) {
                    mTodayMonthPosition = mMonthsCache.size() - 1;
                }
            }

            // Posizione centrale nella cache
            mCurrentCenterPosition = QD_MONTHS_CACHE_SIZE;

            // Crea e imposta l'adapter
            mAdapter = new MonthsAdapter(mMonthsCache, mQD.getUserHalfTeam());
            mMonthsRecyclerView.setAdapter(mAdapter);

            // Posiziona il RecyclerView al mese corrente
            mMonthsRecyclerView.scrollToPosition(mCurrentCenterPosition);

            // Aggiunge il listener per lo scrolling infinito
            mMonthsRecyclerView.addOnScrollListener(new InfiniteScrollListener());

            if (LOG_ENABLED)
                Log.d(TAG, "Calendario infinito configurato con " + mMonthsCache.size() + " mesi");
        } catch (Exception e) {
            if (LOG_ENABLED)
                Log.e(TAG, "Errore nella configurazione del calendario: " + e.getMessage());
        }
    }

    /**
     * Genera i dati per un mese specifico.
     */
    private MonthData generateMonthData(LocalDate monthDate) {
        // Crea un mese temporaneo per ottenere i giorni
        List<Day> monthDays = getMonthDays(monthDate);
        List<CalendarDay> calendarDays = prepareCalendarData(monthDays, monthDate);

        return new MonthData(monthDate, calendarDays);
    }

    /**
     * Ottiene i giorni per un mese specifico.
     * Utilizza il metodo pubblico di QuattroDue per generare i giorni.
     */
    private List<Day> getMonthDays(LocalDate monthDate) {
        try {
            // Utilizza il nuovo metodo pubblico di QuattroDue
            List<Day> days = mQD.getShiftsForMonth(monthDate);

            // Aggiorna i flag "oggi" per tutti i giorni
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
            // Se non ci sono giorni, crea almeno la struttura del mese
            LocalDate firstDay = LocalDate.of(monthDate.getYear(), monthDate.getMonth(), 1);
            int daysInMonth = firstDay.lengthOfMonth();

            for (int i = 1; i <= daysInMonth; i++) {
                LocalDate dayDate = firstDay.withDayOfMonth(i);
                Day day = new Day(dayDate);
                calendarDays.add(new CalendarDay(day));
            }
        } else {
            // Ottiene il primo giorno del mese
            LocalDate firstDay = monthDays.get(0).getDate();

            // Calcola le celle vuote all'inizio
            int dayOfWeek = firstDay.getDayOfWeek().getValue();
            int emptyCellsAtStart = (dayOfWeek == 7) ? 0 : dayOfWeek;

            // Aggiunge le celle vuote
            for (int i = 0; i < emptyCellsAtStart; i++) {
                calendarDays.add(new CalendarDay());
            }

            // Aggiunge i giorni del mese
            for (Day day : monthDays) {
                calendarDays.add(new CalendarDay(day));
            }
        }

        // Completa con celle vuote per raggiungere un multiplo di 7
        while (calendarDays.size() % 7 != 0) {
            calendarDays.add(new CalendarDay());
        }

        return calendarDays;
    }

    /**
     * Listener per lo scrolling infinito con gestione dello scroll veloce.
     */
    private class InfiniteScrollListener extends RecyclerView.OnScrollListener {
        private static final int SCROLL_THRESHOLD = 10; // Velocità massima gestibile
        private long lastScrollTime = 0;

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            // Evita aggiornamenti durante scroll veloce o cache update
            if (mIsUpdatingCache || Math.abs(dy) > SCROLL_THRESHOLD) return;

            // Throttling: evita aggiornamenti troppo frequenti
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastScrollTime < 100) return; // 100ms di throttling
            lastScrollTime = currentTime;

            int firstVisiblePosition = mLayoutManager.findFirstVisibleItemPosition();
            int lastVisiblePosition = mLayoutManager.findLastVisibleItemPosition();

            // Validazione posizioni
            if (firstVisiblePosition == RecyclerView.NO_POSITION ||
                    lastVisiblePosition == RecyclerView.NO_POSITION) return;

            // Controlla se siamo vicini all'inizio
            if (firstVisiblePosition <= 1 && firstVisiblePosition >= 0) {
                scheduleAddPreviousMonths();
            }

            // Controlla se siamo vicini alla fine
            if (lastVisiblePosition >= mMonthsCache.size() - 2) {
                scheduleAddNextMonths();
            }

            // Aggiorna la visibilità del FAB
            updateFabVisibility(firstVisiblePosition, lastVisiblePosition);
        }

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            // Quando lo scroll si ferma, esegui la pulizia della cache
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                if (mMonthsCache.size() > QD_MAX_CACHE_SIZE) {
                    scheduleCleanupCache();
                }
            }
        }
    }

    /**
     * Programma l'aggiunta di mesi precedenti in modo sicuro.
     */
    private void scheduleAddPreviousMonths() {
        if (mMonthsRecyclerView != null) {
            mMonthsRecyclerView.post(this::addPreviousMonths);
        }
    }

    /**
     * Programma l'aggiunta di mesi successivi in modo sicuro.
     */
    private void scheduleAddNextMonths() {
        if (mMonthsRecyclerView != null) {
            mMonthsRecyclerView.post(this::addNextMonths);
        }
    }

    /**
     * Programma la pulizia della cache in modo sicuro.
     */
    private void scheduleCleanupCache() {
        if (mMonthsRecyclerView != null) {
            mMonthsRecyclerView.post(this::cleanupCache);
        }
    }

    /**
     * Aggiunge mesi precedenti alla cache (versione sicura).
     */
    private void addPreviousMonths() {
        if (mMonthsCache.isEmpty() || mIsUpdatingCache) return;

        mIsUpdatingCache = true;
        try {
            MonthData firstMonth = mMonthsCache.get(0);
            LocalDate previousMonth = firstMonth.getMonthDate().minusMonths(1);

            MonthData newMonthData = generateMonthData(previousMonth);
            mMonthsCache.add(0, newMonthData);

            // Aggiorna la posizione del mese contenente oggi
            if (mTodayMonthPosition >= 0) {
                mTodayMonthPosition++;
            } else if (isTodayInMonth(previousMonth)) {
                mTodayMonthPosition = 0;
            }

            // Aggiorna la posizione corrente
            mCurrentCenterPosition++;

            // Notifica l'adapter in modo sicuro
            if (mAdapter != null) {
                mAdapter.notifyItemInserted(0);
                // Mantieni la posizione dello scroll
                mLayoutManager.scrollToPositionWithOffset(1, 0);
            }

            if (LOG_ENABLED) Log.d(TAG, "Aggiunto mese precedente: " + previousMonth);
        } catch (Exception e) {
            if (LOG_ENABLED)
                Log.e(TAG, "Errore nell'aggiungere mesi precedenti: " + e.getMessage());
        } finally {
            mIsUpdatingCache = false;
        }
    }

    /**
     * Aggiunge mesi successivi alla cache (versione sicura).
     */
    private void addNextMonths() {
        if (mMonthsCache.isEmpty() || mIsUpdatingCache) return;

        mIsUpdatingCache = true;
        try {
            MonthData lastMonth = mMonthsCache.get(mMonthsCache.size() - 1);
            LocalDate nextMonth = lastMonth.getMonthDate().plusMonths(1);

            MonthData newMonthData = generateMonthData(nextMonth);
            mMonthsCache.add(newMonthData);

            // Aggiorna la posizione del mese contenente oggi se necessario
            if (mTodayMonthPosition < 0 && isTodayInMonth(nextMonth)) {
                mTodayMonthPosition = mMonthsCache.size() - 1;
            }

            // Notifica l'adapter in modo sicuro
            if (mAdapter != null) {
                mAdapter.notifyItemInserted(mMonthsCache.size() - 1);
            }

            if (LOG_ENABLED) Log.d(TAG, "Aggiunto mese successivo: " + nextMonth);
        } catch (Exception e) {
            if (LOG_ENABLED)
                Log.e(TAG, "Errore nell'aggiungere mesi successivi: " + e.getMessage());
        } finally {
            mIsUpdatingCache = false;
        }
    }

    /**
     * Pulisce la cache rimuovendo i mesi più lontani per gestire la memoria.
     */
    private void cleanupCache() {
        if (mMonthsCache.size() <= QD_MAX_CACHE_SIZE || mIsUpdatingCache) return;

        mIsUpdatingCache = true;
        try {
            int currentVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();

            // Rimuovi i mesi troppo lontani dall'inizio
            while (mMonthsCache.size() > QD_MAX_CACHE_SIZE && currentVisiblePosition > QD_MONTHS_CACHE_SIZE) {
                mMonthsCache.remove(0);
                mAdapter.notifyItemRemoved(0);
                currentVisiblePosition--;
                mCurrentCenterPosition--;
            }

            // Rimuovi i mesi troppo lontani dalla fine
            while (mMonthsCache.size() > QD_MAX_CACHE_SIZE &&
                    currentVisiblePosition < mMonthsCache.size() - QD_MONTHS_CACHE_SIZE) {
                int lastIndex = mMonthsCache.size() - 1;
                mMonthsCache.remove(lastIndex);
                mAdapter.notifyItemRemoved(lastIndex);
            }

            if (LOG_ENABLED) Log.d(TAG, "Cache pulita, dimensione attuale: " + mMonthsCache.size());
        } catch (Exception e) {
            if (LOG_ENABLED) Log.e(TAG, "Errore nella pulizia della cache: " + e.getMessage());
        } finally {
            mIsUpdatingCache = false;
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
     * Aggiorna la visibilità del FAB in base alla posizione dello scroll.
     */
    private void updateFabVisibility(int firstVisible, int lastVisible) {
        boolean showFab = true;

        // Nascondi il FAB se il mese corrente è visibile
        if (mTodayMonthPosition >= 0) {
            showFab = !(firstVisible <= mTodayMonthPosition && lastVisible >= mTodayMonthPosition);
        }

        // Anima la visibilità del FAB
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
        if (mTodayMonthPosition >= 0 && mTodayMonthPosition < mMonthsCache.size()) {
            // Scroll fluido al mese contenente oggi
            mMonthsRecyclerView.smoothScrollToPosition(mTodayMonthPosition);
            if (LOG_ENABLED)
                Log.d(TAG, "Scrolling al mese corrente, posizione: " + mTodayMonthPosition);
        } else {
            // Se non troviamo oggi nella cache, ricostruisci il calendario centrato su oggi
            if (LOG_ENABLED) Log.d(TAG, "Mese corrente non in cache, ricostruzione calendario");
            LocalDate today = LocalDate.now();
            mCurrentDate = LocalDate.of(today.getYear(), today.getMonth(), 1);
            setupInfiniteCalendar();
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Pulisci le risorse per evitare memory leak
        if (mMonthsCache != null) {
            mMonthsCache.clear();
        }
        if (mMonthsRecyclerView != null) {
            mMonthsRecyclerView.clearOnScrollListeners();
        }

        mAdapter = null;
        mLayoutManager = null;
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

    /**
     * Adapter per la lista dei mesi con scrolling infinito.
     */
    private static class MonthsAdapter extends RecyclerView.Adapter<MonthsAdapter.MonthViewHolder> {

        private List<MonthData> monthsData;
        private HalfTeam userHalfTeam;

        public MonthsAdapter(List<MonthData> monthsData, HalfTeam userHalfTeam) {
            this.monthsData = monthsData;
            this.userHalfTeam = userHalfTeam;
        }

        public void updateUserHalfTeam(HalfTeam userHalfTeam) {
            this.userHalfTeam = userHalfTeam;
        }

        @NonNull
        @Override
        public MonthViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_month, parent, false);
            return new MonthViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
            MonthData monthData = monthsData.get(position);
            holder.bind(monthData, userHalfTeam);
        }

        @Override
        public int getItemCount() {
            return monthsData.size();
        }

        /**
         * ViewHolder per ogni mese.
         */
        static class MonthViewHolder extends RecyclerView.ViewHolder {
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
            }

            public void bind(MonthData monthData, HalfTeam userHalfTeam) {
                // Imposta il titolo del mese
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN);
                tvMonthTitle.setText(monthData.getMonthDate().format(formatter));

                // Configura l'adapter della griglia
                calendarViewAdapter = new CalendarViewAdapter(monthData.getCalendarDays(), userHalfTeam);
                rvCalendarGrid.setAdapter(calendarViewAdapter);
            }
        }
    }

    /**
     * Adapter per la griglia del calendario (riutilizzato dal codice precedente).
     */
    private static class CalendarViewAdapter extends RecyclerView.Adapter<CalendarViewAdapter.CalendarViewHolder> {

        private List<CalendarDay> calendarDays;
        private HalfTeam userHalfTeam;

        public CalendarViewAdapter(List<CalendarDay> calendarDays, HalfTeam userHalfTeam) {
            this.calendarDays = calendarDays;
            this.userHalfTeam = userHalfTeam;
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
         * ViewHolder per ogni cella del calendario.
         */
        static class CalendarViewHolder extends RecyclerView.ViewHolder {
            private TextView tvDayNumber;
            private TextView tvDayName;
            private View vShiftIndicator;
            private View itemView;

            public CalendarViewHolder(@NonNull View itemView) {
                super(itemView);
                this.itemView = itemView;
                tvDayNumber = itemView.findViewById(R.id.tv_day_number);
                tvDayName = itemView.findViewById(R.id.tv_day_name);
                vShiftIndicator = itemView.findViewById(R.id.v_shift_indicator);
            }

            public void bind(Day day, HalfTeam userHalfTeam) {
                if (day == null) {
                    // Cella vuota
                    tvDayNumber.setText("");
                    tvDayName.setText("");
                    vShiftIndicator.setVisibility(View.GONE);
                    itemView.setBackgroundResource(0);
                    return;
                }

                // Imposta il numero del giorno
                tvDayNumber.setText(String.valueOf(day.getDayOfMonth()));

                // Nascondi il nome del giorno per mantenere il layout pulito
                tvDayName.setVisibility(View.GONE);

                // Evidenzia il giorno corrente
                if (day.getIsToday()) {
                    itemView.setBackgroundResource(R.color.colorBackgroundYellow);
                } else {
                    itemView.setBackgroundResource(0);
                }

                // Colore rosso per la domenica
                if (day.getDayOfWeek() == DayOfWeek.SUNDAY.getValue()) {
                    tvDayNumber.setTextColor(itemView.getContext().getResources().getColor(R.color.colorTextRed));
                } else {
                    tvDayNumber.setTextColor(itemView.getContext().getResources().getColor(R.color.colorTextBlack));
                }

                // Verifica se l'utente ha un turno in questo giorno
                int userShiftPosition = -1;
                if (userHalfTeam != null) {
                    userShiftPosition = day.getInWichTeamIsHalfTeam(userHalfTeam);
                }

                if (userShiftPosition >= 0) {
                    // L'utente ha un turno - mostra l'indicatore colorato
                    vShiftIndicator.setVisibility(View.VISIBLE);

                    // Ottiene il colore del turno dalle preferenze
                    int shiftColor = getShiftColor(day, userShiftPosition);
                    if (shiftColor != 0) {
                        vShiftIndicator.setBackgroundColor(shiftColor);
                    } else {
                        // Colore di default se non configurato
                        vShiftIndicator.setBackgroundColor(
                                itemView.getContext().getResources().getColor(R.color.colorBackgroundLightBlue));
                    }
                } else {
                    // L'utente non ha turni
                    vShiftIndicator.setVisibility(View.GONE);
                }
            }

            /**
             * Ottiene il colore configurato per il turno.
             */
            private int getShiftColor(Day day, int shiftPosition) {
                try {
                    if (shiftPosition < day.getShifts().size()) {
                        return day.getShifts().get(shiftPosition).getShiftType().getColor();
                    }
                } catch (Exception e) {
                    if (LOG_ENABLED)
                        Log.e(TAG, "Errore nell'ottenere il colore del turno: " + e.getMessage());
                }
                return 0;
            }
        }
    }
}