package net.calvuz.qdue.ui.dayslist;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment che mostra la visualizzazione lista dei turni con scrolling infinito.
 * Aggiornato per supportare la navigazione fluida tra i mesi senza limiti.
 */
public class DayslistViewFragment extends Fragment {

    private static final String TAG = "DayslistViewFragment";
    private static final boolean LOG_ENABLED = true;

    // Numero di mesi da mantenere in cache (prima e dopo il mese corrente)
    private static final int MONTHS_CACHE_SIZE = 6;
    // Numero massimo di mesi in memoria per evitare memory leak
    private static final int MAX_CACHE_SIZE = 24;

    private OnQuattroDueHomeFragmentInteractionListener mListener = null;

    private QuattroDue mQD;
    private RecyclerView mRecyclerView;
    private InfiniteDaysListAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private FloatingActionButton mFabGoToToday;

    // Cache dei giorni per lo scrolling infinito
    private List<DayData> mDaysCache;
    private int mCurrentCenterPosition;
    private LocalDate mCurrentDate;

    // Flag per evitare chiamate ricorsive durante lo scroll
    private boolean mIsUpdatingCache = false;

    // Posizione del giorno di oggi
    private int mTodayPosition = -1;

    private View mRoot;

    /**
     * Costruttore vuoto obbligatorio per il fragment manager.
     */
    public DayslistViewFragment() {
        // Costruttore vuoto richiesto
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LOG_ENABLED) Log.d(TAG, "onCreate");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.fragment_dayslist_view, container, false);
        if (LOG_ENABLED) Log.d(TAG, "onCreateView");

        // Inizializza le viste
        mRecyclerView = mRoot.findViewById(R.id.rv_dayslist);
        mFabGoToToday = mRoot.findViewById(R.id.fab_go_to_today);

        // Configura il RecyclerView per lo scroll verticale
        mLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Configura il FAB
        if (mFabGoToToday != null) {
            mFabGoToToday.setOnClickListener(v -> scrollToToday());
        }

        // Inizializza QuattroDue
        if (getActivity() != null) {
            mQD = QuattroDue.getInstance(getActivity().getApplicationContext());
            setupInfiniteScrolling();
        }

        return mRoot;
    }

    /**
     * Configura lo scrolling infinito per i giorni.
     */
    private void setupInfiniteScrolling() {
        if (mQD == null) {
            if (LOG_ENABLED) Log.e(TAG, "setupInfiniteScrolling: mQD è null");
            return;
        }

        try {
            // Inizializza la cache dei giorni
            mCurrentDate = mQD.getCursorDate();
            mDaysCache = new ArrayList<>();

            // Genera i giorni nella cache (MONTHS_CACHE_SIZE mesi prima e dopo)
            for (int i = -MONTHS_CACHE_SIZE; i <= MONTHS_CACHE_SIZE; i++) {
                LocalDate monthDate = mCurrentDate.plusMonths(i);
                List<DayData> monthDays = generateMonthDays(monthDate);
                mDaysCache.addAll(monthDays);
            }

            // Trova la posizione di oggi
            findTodayPosition();

            // Posizione centrale nella cache (approssimativamente)
            mCurrentCenterPosition = mDaysCache.size() / 2;

            // Crea e imposta l'adapter
            mAdapter = new InfiniteDaysListAdapter(getContext(), mDaysCache);
            mRecyclerView.setAdapter(mAdapter);

            // Posiziona il RecyclerView al mese corrente
            scrollToCurrentMonth();

            // Aggiunge il listener per lo scrolling infinito
            mRecyclerView.addOnScrollListener(new InfiniteScrollListener());

            if (LOG_ENABLED)
                Log.d(TAG, "Scrolling infinito configurato con " + mDaysCache.size() + " giorni");
        } catch (Exception e) {
            if (LOG_ENABLED)
                Log.e(TAG, "Errore nella configurazione dello scrolling infinito: " + e.getMessage());
        }
    }

    /**
     * Genera i dati per i giorni di un mese specifico.
     */
    private List<DayData> generateMonthDays(LocalDate monthDate) {
        List<DayData> monthDays = new ArrayList<>();

        try {
            // Utilizza il metodo pubblico di QuattroDue per generare i giorni
            List<Day> days = mQD.getShiftsForMonth(monthDate);

            if (LOG_ENABLED) Log.d(TAG, "generateMonthDays per " + monthDate +
                    ": trovati " + days.size() + " giorni");

            if (!days.isEmpty()) {
                // Crea il titolo del mese
                String monthTitle = formatMonthTitle(monthDate);

                // Aggiunge l'header del mese come primo elemento
                monthDays.add(new DayData(monthTitle, monthDate));
                if (LOG_ENABLED) Log.d(TAG, "Aggiunto header: " + monthTitle);

                // Aggiorna i flag "oggi" per tutti i giorni
                LocalDate today = LocalDate.now();
                for (Day day : days) {
                    if (day.getDate() != null) {
                        day.setIsToday(day.getDate().equals(today));
                    }
                    monthDays.add(new DayData(day, monthDate));
                }

                if (LOG_ENABLED) Log.d(TAG, "Totale elementi aggiunti per " + monthDate +
                        ": " + monthDays.size() + " (1 header + " + days.size() + " giorni)");
            }

        } catch (Exception e) {
            if (LOG_ENABLED)
                Log.e(TAG, "Errore nell'ottenere i giorni per il mese " + monthDate + ": " + e.getMessage());
        }

        return monthDays;
    }

    /**
     * Formatta il titolo del mese.
     * Mostra solo il nome del mese se è dello stesso anno corrente,
     * altrimenti include anche l'anno.
     */
    private String formatMonthTitle(LocalDate monthDate) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter;

        if (monthDate.getYear() == today.getYear()) {
            // Stesso anno: mostra solo il mese
            formatter = DateTimeFormatter.ofPattern("MMMM", Locale.ITALIAN);
        } else {
            // Anno diverso: mostra mese e anno
            formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN);
        }

        return monthDate.format(formatter);
    }

    /**
     * Trova la posizione di oggi nella cache.
     */
    private void findTodayPosition() {
        LocalDate today = LocalDate.now();
        mTodayPosition = -1;

        for (int i = 0; i < mDaysCache.size(); i++) {
            DayData dayData = mDaysCache.get(i);
            // Salta gli header dei mesi
            if (dayData.isMonthHeader()) continue;

            if (dayData.getDay().getDate() != null && dayData.getDay().getDate().equals(today)) {
                mTodayPosition = i;
                break;
            }
        }

        if (LOG_ENABLED) Log.d(TAG, "Posizione di oggi: " + mTodayPosition);
    }

    /**
     * Scrolla al mese corrente.
     */
    private void scrollToCurrentMonth() {
        if (mTodayPosition >= 0) {
            // Scrolla a oggi se trovato
            mLayoutManager.scrollToPosition(mTodayPosition);
        } else {
            // Altrimenti scrolla al centro della cache
            mLayoutManager.scrollToPosition(mCurrentCenterPosition);
        }
    }

    /**
     * Listener per lo scrolling infinito.
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

            // Controlla se siamo vicini all'inizio (primi 30 giorni)
            if (firstVisiblePosition <= 30 && firstVisiblePosition >= 0) {
                scheduleAddPreviousDays();
            }

            // Controlla se siamo vicini alla fine (ultimi 30 giorni)
            if (lastVisiblePosition >= mDaysCache.size() - 30) {
                scheduleAddNextDays();
            }

            // Aggiorna la visibilità del FAB
            updateFabVisibility(firstVisiblePosition, lastVisiblePosition);
        }

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            // Quando lo scroll si ferma, esegui la pulizia della cache
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                if (mDaysCache.size() > MAX_CACHE_SIZE * 31) { // ~31 giorni per mese
                    scheduleCleanupCache();
                }
            }
        }
    }

    /**
     * Programma l'aggiunta di giorni precedenti in modo sicuro.
     */
    private void scheduleAddPreviousDays() {
        if (mRecyclerView != null) {
            mRecyclerView.post(this::addPreviousDays);
        }
    }

    /**
     * Programma l'aggiunta di giorni successivi in modo sicuro.
     */
    private void scheduleAddNextDays() {
        if (mRecyclerView != null) {
            mRecyclerView.post(this::addNextDays);
        }
    }

    /**
     * Programma la pulizia della cache in modo sicuro.
     */
    private void scheduleCleanupCache() {
        if (mRecyclerView != null) {
            mRecyclerView.post(this::cleanupCache);
        }
    }

    /**
     * Aggiunge giorni precedenti alla cache.
     */
    private void addPreviousDays() {
        if (mDaysCache.isEmpty() || mIsUpdatingCache) return;

        mIsUpdatingCache = true;
        try {
            // Trova il primo mese nella cache
            DayData firstDay = mDaysCache.get(0);
            LocalDate firstMonth = firstDay.getMonthDate();
            LocalDate previousMonth = firstMonth.minusMonths(1);

            // Genera i giorni del mese precedente
            List<DayData> newDays = generateMonthDays(previousMonth);

            // Aggiunge all'inizio della cache
            mDaysCache.addAll(0, newDays);

            // Aggiorna la posizione di oggi
            if (mTodayPosition >= 0) {
                mTodayPosition += newDays.size();
            }

            // Aggiorna la posizione corrente
            mCurrentCenterPosition += newDays.size();

            // Notifica l'adapter in modo sicuro
            if (mAdapter != null) {
                mAdapter.notifyItemRangeInserted(0, newDays.size());
                // Mantieni la posizione dello scroll
                mLayoutManager.scrollToPositionWithOffset(newDays.size(), 0);
            }

            if (LOG_ENABLED) Log.d(TAG, "Aggiunto mese precedente: " + previousMonth +
                    " (" + newDays.size() + " giorni)");
        } catch (Exception e) {
            if (LOG_ENABLED)
                Log.e(TAG, "Errore nell'aggiungere giorni precedenti: " + e.getMessage());
        } finally {
            mIsUpdatingCache = false;
        }
    }

    /**
     * Aggiunge giorni successivi alla cache.
     */
    private void addNextDays() {
        if (mDaysCache.isEmpty() || mIsUpdatingCache) return;

        mIsUpdatingCache = true;
        try {
            // Trova l'ultimo mese nella cache
            DayData lastDay = mDaysCache.get(mDaysCache.size() - 1);
            LocalDate lastMonth = lastDay.getMonthDate();
            LocalDate nextMonth = lastMonth.plusMonths(1);

            // Genera i giorni del mese successivo
            List<DayData> newDays = generateMonthDays(nextMonth);

            // Posizione di inserimento
            int insertPosition = mDaysCache.size();

            // Aggiunge alla fine della cache
            mDaysCache.addAll(newDays);


            // Notifica l'adapter in modo sicuro
            if (mAdapter != null) {
                mAdapter.notifyItemRangeInserted(insertPosition, newDays.size());
            }

            if (LOG_ENABLED) Log.d(TAG, "Aggiunto mese successivo: " + nextMonth +
                    " (" + newDays.size() + " giorni)");
        } catch (Exception e) {
            if (LOG_ENABLED)
                Log.e(TAG, "Errore nell'aggiungere giorni successivi: " + e.getMessage());
        } finally {
            mIsUpdatingCache = false;
        }
    }

    /**
     * Pulisce la cache rimuovendo i giorni più lontani.
     */
    private void cleanupCache() {
        if (mDaysCache.size() <= MAX_CACHE_SIZE * 31 || mIsUpdatingCache) return;

        mIsUpdatingCache = true;
        try {
            int currentVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
            int targetCacheSize = MONTHS_CACHE_SIZE * 31; // Circa 6 mesi * 31 giorni

            // Rimuovi i giorni troppo lontani dall'inizio
            while (mDaysCache.size() > targetCacheSize &&
                    currentVisiblePosition > targetCacheSize / 2) {
                mDaysCache.remove(0);
                mAdapter.notifyItemRemoved(0);
                currentVisiblePosition--;
                mCurrentCenterPosition--;
                if (mTodayPosition > 0) mTodayPosition--;
            }

            // Rimuovi i giorni troppo lontani dalla fine
            while (mDaysCache.size() > targetCacheSize &&
                    currentVisiblePosition < mDaysCache.size() - targetCacheSize / 2) {
                int lastIndex = mDaysCache.size() - 1;
                mDaysCache.remove(lastIndex);
                mAdapter.notifyItemRemoved(lastIndex);
            }

            if (LOG_ENABLED) Log.d(TAG, "Cache pulita, dimensione attuale: " + mDaysCache.size());
        } catch (Exception e) {
            if (LOG_ENABLED) Log.e(TAG, "Errore nella pulizia della cache: " + e.getMessage());
        } finally {
            mIsUpdatingCache = false;
        }
    }

    /**
     * Aggiorna la visibilità del FAB in base alla posizione dello scroll.
     */
    private void updateFabVisibility(int firstVisible, int lastVisible) {
        if (mFabGoToToday == null) return;

        boolean showFab = true;

        // Nascondi il FAB se oggi è visibile
        if (mTodayPosition >= 0) {
            showFab = !(firstVisible <= mTodayPosition && lastVisible >= mTodayPosition);
        }

        // Anima la visibilità del FAB
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
        if (mTodayPosition >= 0 && mTodayPosition < mDaysCache.size()) {
            // Scroll fluido a oggi
            mRecyclerView.smoothScrollToPosition(mTodayPosition);
            if (LOG_ENABLED)
                Log.d(TAG, "Scrolling a oggi, posizione: " + mTodayPosition);
        } else {
            // Se non troviamo oggi nella cache, ricostruisci la cache centrata su oggi
            if (LOG_ENABLED) Log.d(TAG, "Oggi non in cache, ricostruzione cache");
            LocalDate today = LocalDate.now();
            mCurrentDate = LocalDate.of(today.getYear(), today.getMonth(), 1);
            setupInfiniteScrolling();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (LOG_ENABLED) Log.d(TAG, "onStart");

        // Legge le preferenze
        if (mQD != null) {
            mQD.updatePreferences(getActivity());
            if (mQD.isRefresh()) {
                mQD.setRefresh(false);
                // Ricostruisci la cache se necessario
                setupInfiniteScrolling();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (LOG_ENABLED) Log.d(TAG, "onResume");

        // Aggiorna la posizione di oggi
        findTodayPosition();
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
        // Pulisci le risorse per evitare memory leak
        if (mDaysCache != null) {
            mDaysCache.clear();
        }
        if (mRecyclerView != null) {
            mRecyclerView.clearOnScrollListeners();
        }

        mAdapter = null;
        mLayoutManager = null;
    }

    /**
     * Notifica gli aggiornamenti dell'interfaccia.
     */
    public void notifyUpdates() {
        if (LOG_ENABLED) Log.d(TAG, "notifyUpdates chiamato");

        // Aggiorna i dati se il mese corrente è cambiato
        LocalDate newCurrentDate = mQD.getCursorDate();
        if (!newCurrentDate.equals(mCurrentDate)) {
            mCurrentDate = newCurrentDate;
            // Ricarica la cache per il nuovo mese
            setupInfiniteScrolling();
        } else if (mAdapter != null) {
            // Aggiorna solo i dati esistenti
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Interfaccia per la comunicazione con l'activity.
     */
    public interface OnQuattroDueHomeFragmentInteractionListener {
        void onQuattroDueHomeFragmentInteractionListener(long id);
    }

    /**
     * Classe per rappresentare i dati di un giorno con informazioni sul mese.
     */
    public static class DayData {
        private Day day;
        private LocalDate monthDate;
        private boolean isMonthHeader;
        private String monthTitle;

        public DayData(Day day, LocalDate monthDate) {
            this.day = day;
            this.monthDate = monthDate;
            this.isMonthHeader = false;
            this.monthTitle = null;
        }

        // Costruttore per header del mese
        public DayData(String monthTitle, LocalDate monthDate) {
            this.day = null;
            this.monthDate = monthDate;
            this.isMonthHeader = true;
            this.monthTitle = monthTitle;
        }

        public Day getDay() {
            return day;
        }

        public LocalDate getMonthDate() {
            return monthDate;
        }

        public boolean isMonthHeader() {
            return isMonthHeader;
        }

        public String getMonthTitle() {
            return monthTitle;
        }
    }

    /**
     * Adapter per la lista infinita dei giorni.
     */
    private static class InfiniteDaysListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_MONTH_HEADER = 0;
        private static final int VIEW_TYPE_DAY = 1;

        private final Context mContext;
        private List<DayData> mDaysData;
        private final LayoutInflater mInflater;
        private int mNumShifts = 3; // Numero predefinito di turni

        public InfiniteDaysListAdapter(Context context, List<DayData> daysData) {
            mContext = context;
            mDaysData = daysData != null ? daysData : new ArrayList<>();
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getItemViewType(int position) {
            return mDaysData.get(position).isMonthHeader() ? VIEW_TYPE_MONTH_HEADER : VIEW_TYPE_DAY;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_MONTH_HEADER) {
                View view = mInflater.inflate(R.layout.item_month_header, parent, false);
                return new MonthHeaderViewHolder(view);
            } else {
                View view = mInflater.inflate(R.layout.item_dayslist_row, parent, false);
                return new DayViewHolder(view, mNumShifts);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            DayData dayData = mDaysData.get(position);

            if (holder instanceof MonthHeaderViewHolder) {
                ((MonthHeaderViewHolder) holder).bind(dayData.getMonthTitle());
            } else if (holder instanceof DayViewHolder) {
                Day day = dayData.getDay();
                if (day != null) {
                    bindDayToViewHolder((DayViewHolder) holder, day, mContext);
                }
            }
        }

        @Override
        public int getItemCount() {
            return mDaysData.size();
        }

        /**
         * ViewHolder per l'header del mese.
         */
        public static class MonthHeaderViewHolder extends RecyclerView.ViewHolder {
            private android.widget.TextView tvMonthTitle;

            public MonthHeaderViewHolder(View view) {
                super(view);
                tvMonthTitle = view.findViewById(R.id.tv_month_title);
            }

            public void bind(String monthTitle) {
                tvMonthTitle.setText(monthTitle);
            }
        }

        /**
         * Lega i dati del giorno al ViewHolder
         */
        private void bindDayToViewHolder(DayViewHolder holder, Day day, Context context) {

            android.content.res.Resources r = context.getResources();

            // Imposta il numero del giorno
            holder.tday.setText(r.getString(R.string.str_scheme_num, day.getDayOfMonth()));

            // Imposta il nome del giorno della settimana
            holder.twday.setText(r.getString(R.string.str_scheme, day.getDayOfWeekAsString()));

            // Resetta tutti i colori dei turni
            resetShiftViews(holder);

            // Imposta i testi per i turni
            List<net.calvuz.qdue.quattrodue.models.Shift> shifts = day.getShifts();
            int numShifts = Math.min(shifts.size(), mNumShifts);

            for (int i = 0; i < numShifts; i++) {
                if (holder.shiftTexts[i] != null) {
                    try {
                        String teamText = shifts.get(i).getTeamsAsString();
                        holder.shiftTexts[i].setText(teamText != null && !teamText.isEmpty() ?
                                r.getString(R.string.str_scheme, teamText) : "");
                    } catch (Exception e) {
                        holder.shiftTexts[i].setText("");
                    }
                }
            }

            // Imposta il testo per le squadre a riposo
            String tR = day.getOffWorkHalfTeamsAsString();
            holder.ttR.setText(tR != null && !tR.isEmpty() ? r.getString(R.string.str_scheme, tR) : "");

            // Imposta il testo per gli eventi
            String tE = day.hasEvents() ? "*" : "  ";
            holder.ttE.setText(tE);

            // Colore rosso per la domenica
            if (day.getDayOfWeek() == java.time.DayOfWeek.SUNDAY.getValue()) {
                holder.tday.setTextColor(r.getColor(R.color.colorTextRed));
                holder.twday.setTextColor(r.getColor(R.color.colorTextRed));
            } else {
                holder.tday.setTextColor(r.getColor(R.color.colorTextBlack));
                holder.twday.setTextColor(r.getColor(R.color.colorTextBlack));
            }

            // Ottiene la squadra dell'utente
            QuattroDue qd = QuattroDue.getInstance(context);
            net.calvuz.qdue.quattrodue.models.HalfTeam userHalfTeam = qd.getUserHalfTeam();

            // Trova la posizione della squadra dell'utente
            int userPosition = -1;
            if (userHalfTeam != null) {
                userPosition = day.getInWichTeamIsHalfTeam(userHalfTeam);
            }

            // Evidenzia SOLO il turno dell'utente
            if (userPosition >= 0 && userPosition < numShifts && holder.shiftTexts[userPosition] != null) {
                holder.shiftTexts[userPosition].setBackgroundColor(r.getColor(R.color.colorBackgroundLightBlue));
                holder.shiftTexts[userPosition].setTextColor(r.getColor(R.color.colorTextWhite));
            }

            // Evidenzia il giorno corrente
            if (day.getIsToday()) {
                holder.mView.setBackgroundColor(r.getColor(R.color.colorBackgroundYellow));
            } else {
                holder.mView.setBackgroundColor(0); // Trasparente
            }
        }

        /**
         * Reimposta i colori e i testi dei turni ai valori predefiniti.
         */
        private void resetShiftViews(DayViewHolder holder) {
            if (holder.shiftTexts != null) {
                for (android.widget.TextView tv : holder.shiftTexts) {
                    if (tv != null) {
                        tv.setBackgroundColor(0); // Trasparente
                        tv.setTextColor(mContext.getResources().getColor(R.color.colorTextBlack));
                    }
                }
            }
        }

        /**
         * ViewHolder per i giorni.
         */
        public static class DayViewHolder extends RecyclerView.ViewHolder {
            public android.widget.TextView tday, twday;  // giorno del mese, giorno della settimana
            public android.widget.TextView[] shiftTexts; // array di textview per i turni
            public android.widget.TextView ttE, ttR;     // testo per eventi e riposi
            public View mView;            // root view

            public DayViewHolder(View view, int numShifts) {
                super(view);
                mView = view;
                tday = view.findViewById(R.id.tday);
                twday = view.findViewById(R.id.twday);

                // Inizializza array per le TextView dei turni
                shiftTexts = new android.widget.TextView[numShifts];

                // Assegna le TextView in base al loro ID
                for (int i = 0; i < numShifts && i < 5; i++) { // Massimo 5 turni supportati
                    int resId = view.getResources().getIdentifier("tt" + (i+1), "id", view.getContext().getPackageName());
                    if (resId != 0) {
                        shiftTexts[i] = view.findViewById(resId);
                    }
                }

                ttR = view.findViewById(R.id.ttR);
                ttE = view.findViewById(R.id.ttE);
            }
        }
    }
}