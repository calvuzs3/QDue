package net.calvuz.qdue.ui.dayslist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.ui.shared.SharedViewModels;
import net.calvuz.qdue.utils.ThemeUtils;

import java.util.List;

/**
 * Adapter unificato per DayslistViewFragment che utilizza SharedViewModels.
 * Gestisce header mesi, giorni, celle vuote e indicatori di caricamento.
 */
public class UnifiedDaysListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "UnifiedDaysListAdapter";

    // View Types
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_DAY = 1;
    private static final int VIEW_TYPE_LOADING = 2;

    private final Context mContext;
    private List<SharedViewModels.ViewItem> mItems;
    private HalfTeam mUserHalfTeam;
    private final int mNumShifts;

    // Cache dei colori per ottimizzare le prestazioni
    private int mCachedNormalTextColor = 0;
    private int mCachedSundayTextColor = 0;
    private int mCachedTodayBackgroundColor = 0;
    private int mCachedUserShiftBackgroundColor = 0;
    private int mCachedUserShiftTextColor = 0;

    public UnifiedDaysListAdapter(Context context, List<SharedViewModels.ViewItem> items,
                                  HalfTeam userHalfTeam, int numShifts) {
        this.mContext = context;
        this.mItems = items;
        this.mUserHalfTeam = userHalfTeam;
        this.mNumShifts = numShifts;

        // Inizializza cache colori
        initializeColorCache();
    }

    /**
     * Aggiorna i dati dell'adapter.
     */
    public void updateData(List<SharedViewModels.ViewItem> newItems) {
        this.mItems = newItems;
        notifyDataSetChanged();
    }

    /**
     * Aggiorna il team dell'utente.
     */
    public void updateUserTeam(HalfTeam newUserTeam) {
        this.mUserHalfTeam = newUserTeam;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= mItems.size()) return VIEW_TYPE_DAY; // Fallback sicuro

        SharedViewModels.ViewItem item = mItems.get(position);
        switch (item.getType()) {
            case HEADER:
                return VIEW_TYPE_HEADER;
            case LOADING:
                return VIEW_TYPE_LOADING;
            case DAY:
            default:
                return VIEW_TYPE_DAY;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case VIEW_TYPE_HEADER:
                View headerView = inflater.inflate(R.layout.item_month_header, parent, false);
                return new MonthHeaderViewHolder(headerView);

            case VIEW_TYPE_LOADING:
                View loadingView = inflater.inflate(R.layout.item_loading_calendar, parent, false);
                return new LoadingViewHolder(loadingView);

            case VIEW_TYPE_DAY:
            default:
                View dayView = inflater.inflate(R.layout.item_dayslist_row, parent, false);
                return new DayViewHolder(dayView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position >= mItems.size()) return; // Protezione

        SharedViewModels.ViewItem item = mItems.get(position);

        if (holder instanceof MonthHeaderViewHolder) {
            SharedViewModels.MonthHeader header = (SharedViewModels.MonthHeader) item;
            ((MonthHeaderViewHolder) holder).bind(header);

        } else if (holder instanceof LoadingViewHolder) {
            SharedViewModels.LoadingItem loading = (SharedViewModels.LoadingItem) item;
            ((LoadingViewHolder) holder).bind(loading);

        } else if (holder instanceof DayViewHolder) {
            SharedViewModels.DayItem dayItem = (SharedViewModels.DayItem) item;
            ((DayViewHolder) holder).bind(dayItem, mUserHalfTeam);
        }
    }

    @Override
    public int getItemCount() {
        return mItems != null ? mItems.size() : 0;
    }

    /**
     * Inizializza la cache dei colori per ottimizzare le prestazioni.
     */
    private void initializeColorCache() {
        if (mCachedNormalTextColor == 0) {
            mCachedNormalTextColor = ThemeUtils.getOnNormalBackgroundColor(mContext);
            mCachedSundayTextColor = ThemeUtils.getSundayTextColor(mContext);
            mCachedTodayBackgroundColor = ThemeUtils.getTodayBackgroundColor(mContext);
            mCachedUserShiftBackgroundColor = ThemeUtils.getMaterialPrimaryContainerColor(mContext);
            mCachedUserShiftTextColor = ThemeUtils.getMaterialOnPrimaryContainerColor(mContext);
        }
    }

    // ==================== VIEW HOLDERS ====================

    /**
     * ViewHolder per gli header dei mesi.
     */
    public static class MonthHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMonthTitle;

        public MonthHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMonthTitle = itemView.findViewById(R.id.tv_month_title);
        }

        public void bind(SharedViewModels.MonthHeader header) {
            tvMonthTitle.setText(header.title);
        }
    }

    /**
     * ViewHolder per gli indicatori di caricamento.
     */
    public static class LoadingViewHolder extends RecyclerView.ViewHolder {
        private final ProgressBar progressBar;
        private final TextView loadingText;

        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progress_bar);
            loadingText = itemView.findViewById(R.id.tv_loading);
        }

        public void bind(SharedViewModels.LoadingItem loading) {
            loadingText.setText(loading.message);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * ViewHolder ottimizzato per i giorni con cache dei colori.
     */
    public class DayViewHolder extends RecyclerView.ViewHolder {
        // Viste principali
        private final TextView tday, twday;  // giorno del mese, giorno della settimana
        private final TextView[] shiftTexts; // array di textview per i turni
        private final TextView ttR;          // testo per squadre a riposo
        private final View mView;            // root view

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
            tday = itemView.findViewById(R.id.tday);
            twday = itemView.findViewById(R.id.twday);
            ttR = itemView.findViewById(R.id.ttR);

            // Inizializza array per le TextView dei turni
            shiftTexts = new TextView[mNumShifts];
            for (int i = 0; i < mNumShifts && i < 5; i++) { // Massimo 5 turni supportati
                int resId = itemView.getResources().getIdentifier("tt" + (i + 1), "id",
                        itemView.getContext().getPackageName());
                if (resId != 0) {
                    shiftTexts[i] = itemView.findViewById(resId);
                }
            }
        }

        public void bind(SharedViewModels.DayItem dayItem, HalfTeam userHalfTeam) {
            Day day = dayItem.day;
            if (day == null) return;

            android.content.res.Resources r = mContext.getResources();
            boolean isSunday = dayItem.isSunday();
            boolean isToday = dayItem.isToday();

            // Imposta il numero del giorno
            tday.setText(r.getString(R.string.str_scheme_num, day.getDayOfMonth()));

            // Imposta il nome del giorno della settimana
            twday.setText(r.getString(R.string.str_scheme, day.getDayOfWeekAsString()));

            // Reset colori e background
            resetAllViews();

            // Imposta i testi per i turni
            List<Shift> shifts = day.getShifts();
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
            String restTeams = day.getOffWorkHalfTeamsAsString();
            ttR.setText(restTeams != null && !restTeams.isEmpty() ?
                    r.getString(R.string.str_scheme, restTeams) : "");

            // Trova la posizione della squadra dell'utente
            int userPosition = -1;
            if (userHalfTeam != null) {
                userPosition = day.getInWichTeamIsHalfTeam(userHalfTeam);
            }

            // Evidenzia il turno dell'utente
            if (userPosition >= 0 && userPosition < numShifts && shiftTexts[userPosition] != null) {
                shiftTexts[userPosition].setBackgroundColor(mCachedUserShiftBackgroundColor);
                shiftTexts[userPosition].setTextColor(mCachedUserShiftTextColor);
            }

            // Gestisci colori speciali (oggi e domenica)
            if (isToday) {
                // Priorità massima: giorno corrente
                mView.setBackgroundColor(mCachedTodayBackgroundColor);
                setAllTextColors(mCachedNormalTextColor);
            } else if (isSunday) {
                // Domenica
                setAllTextColors(mCachedSundayTextColor);
            } else {
                // Giorno normale
                setAllTextColors(mCachedNormalTextColor);
            }
        }

        /**
         * Reset di tutti i colori ai valori predefiniti.
         */
        private void resetAllViews() {
            mView.setBackgroundColor(Color.TRANSPARENT);

            // Reset colori turni
            for (TextView tv : shiftTexts) {
                if (tv != null) {
                    tv.setBackgroundColor(Color.TRANSPARENT);
                    tv.setTextColor(mCachedNormalTextColor);
                }
            }
        }

        /**
         * Imposta il colore del testo per tutti gli elementi base.
         */
        private void setAllTextColors(int color) {
            tday.setTextColor(color);
            twday.setTextColor(color);
            ttR.setTextColor(color);

            // Non cambiare il colore dei turni evidenziati dell'utente
            for (TextView tv : shiftTexts) {
                if (tv != null) {
                    // Controlla se il background è trasparente (non evidenziato)
                    if (tv.getBackground() == null ||
                            ((android.graphics.drawable.ColorDrawable) tv.getBackground()).getColor()
                                    == Color.TRANSPARENT) {
                        tv.setTextColor(color);
                    }
                }
            }
        }
    }

    // ==================== METODI DI UTILITÀ ====================

    /**
     * Trova la posizione di un giorno specifico nell'adapter.
     */
    public int findPositionForDate(java.time.LocalDate targetDate) {
        if (mItems == null || targetDate == null) return -1;

        for (int i = 0; i < mItems.size(); i++) {
            SharedViewModels.ViewItem item = mItems.get(i);
            if (item instanceof SharedViewModels.DayItem) {
                SharedViewModels.DayItem dayItem = (SharedViewModels.DayItem) item;
                if (dayItem.day.getDate().equals(targetDate)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Ottiene statistiche sull'adapter per debug.
     */
    public AdapterStats getStats() {
        if (mItems == null) return new AdapterStats(0, 0, 0, 0);

        int headers = 0, days = 0, loading = 0, empty = 0;

        for (SharedViewModels.ViewItem item : mItems) {
            switch (item.getType()) {
                case HEADER: headers++; break;
                case DAY: days++; break;
                case LOADING: loading++; break;
                case EMPTY: empty++; break;
            }
        }

        return new AdapterStats(headers, days, loading, empty);
    }

    public static class AdapterStats {
        public final int headers, days, loading, empty;

        AdapterStats(int headers, int days, int loading, int empty) {
            this.headers = headers;
            this.days = days;
            this.loading = loading;
            this.empty = empty;
        }

        @SuppressLint("DefaultLocale")
        @Override
        public String toString() {
            return String.format("Stats{headers=%d, days=%d, loading=%d, empty=%d}",
                    headers, days, loading, empty);
        }
    }
}