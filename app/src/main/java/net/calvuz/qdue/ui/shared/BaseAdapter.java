package net.calvuz.qdue.ui.shared;

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
import net.calvuz.qdue.utils.ThemeUtils;

import java.util.List;

/**
 * Adapter base unificato per entrambe le visualizzazioni (DaysList e Calendar).
 * Le sottoclassi specializzano solo il binding specifico mantenendo la logica comune.
 */
public abstract class BaseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected static final String TAG = "BaseAdapter";

    // View Types comuni
    protected static final int VIEW_TYPE_MONTH_HEADER = 0;
    protected static final int VIEW_TYPE_DAY = 1;
    protected static final int VIEW_TYPE_LOADING = 2;
    protected static final int VIEW_TYPE_EMPTY = 3;

    // View Types specifici (le sottoclassi possono aggiungerne)
    protected static final int VIEW_TYPE_CUSTOM_START = 100;

    protected final Context mContext;
    protected List<SharedViewModels.ViewItem> mItems;
    protected HalfTeam mUserHalfTeam;
    protected final int mNumShifts;

    // Cache dei colori condivisa
    protected int mCachedNormalTextColor = 0;
    protected int mCachedSundayTextColor = 0;
    protected int mCachedTodayBackgroundColor = 0;
    protected int mCachedUserShiftBackgroundColor = 0;
    protected int mCachedUserShiftTextColor = 0;

    public BaseAdapter(Context context, List<SharedViewModels.ViewItem> items,
                       HalfTeam userHalfTeam, int numShifts) {
        this.mContext = context;
        this.mItems = items;
        this.mUserHalfTeam = userHalfTeam;
        this.mNumShifts = numShifts;
        initializeColorCache();
    }

    // ==================== METODI PUBBLICI COMUNI ====================

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<SharedViewModels.ViewItem> newItems) {
        this.mItems = newItems;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateUserTeam(HalfTeam newUserTeam) {
        this.mUserHalfTeam = newUserTeam;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems != null ? mItems.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= mItems.size()) return VIEW_TYPE_DAY;

        SharedViewModels.ViewItem item = mItems.get(position);
        switch (item.getType()) {
            case HEADER:
                return VIEW_TYPE_MONTH_HEADER;
            case LOADING:
                return VIEW_TYPE_LOADING;
            case EMPTY:
                return VIEW_TYPE_EMPTY;
            case DAY:
            default:
                return getCustomViewType(item, position);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case VIEW_TYPE_MONTH_HEADER:
                return createMonthHeaderViewHolder(inflater, parent);

            case VIEW_TYPE_LOADING:
                return createLoadingViewHolder(inflater, parent);

            case VIEW_TYPE_EMPTY:
                return createEmptyViewHolder(inflater, parent);

            case VIEW_TYPE_DAY:
                return createDayViewHolder(inflater, parent);

            default:
                return createCustomViewHolder(inflater, parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position >= mItems.size()) return;

        SharedViewModels.ViewItem item = mItems.get(position);

        if (holder instanceof MonthHeaderViewHolder) {
            SharedViewModels.MonthHeader header = (SharedViewModels.MonthHeader) item;
            bindMonthHeader((MonthHeaderViewHolder) holder, header);

        } else if (holder instanceof LoadingViewHolder) {
            SharedViewModels.LoadingItem loading = (SharedViewModels.LoadingItem) item;
            bindLoading((LoadingViewHolder) holder, loading);

        } else if (holder instanceof EmptyViewHolder) {
            bindEmpty((EmptyViewHolder) holder);

        } else if (holder instanceof DayViewHolder) {
            SharedViewModels.DayItem dayItem = (SharedViewModels.DayItem) item;
            bindDay((DayViewHolder) holder, dayItem, position);

        } else {
            bindCustomViewHolder(holder, item, position);
        }
    }

    // ==================== FACTORY METHODS (POSSONO ESSERE OVERRIDE) ====================

    protected RecyclerView.ViewHolder createMonthHeaderViewHolder(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.item_month_header, parent, false);
        return new MonthHeaderViewHolder(view);
    }

    protected RecyclerView.ViewHolder createLoadingViewHolder(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.item_loading_calendar, parent, false);
        return new LoadingViewHolder(view);
    }

    protected RecyclerView.ViewHolder createEmptyViewHolder(LayoutInflater inflater, ViewGroup parent) {
        // Per le celle vuote, usa un layout semplice o invisibile
        View view = new View(parent.getContext());
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return new EmptyViewHolder(view);
    }

    protected RecyclerView.ViewHolder createDayViewHolder(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.item_dayslist_row, parent, false);
        return new DayViewHolder(view);
    }

    // ==================== BINDING METHODS (POSSONO ESSERE OVERRIDE) ====================

    protected void bindMonthHeader(MonthHeaderViewHolder holder, SharedViewModels.MonthHeader header) {
        holder.tvMonthTitle.setText(header.title);
    }

    protected void bindLoading(LoadingViewHolder holder, SharedViewModels.LoadingItem loading) {
        holder.loadingText.setText(loading.message);
        holder.progressBar.setVisibility(View.VISIBLE);
    }

    protected void bindEmpty(EmptyViewHolder holder) {
        // Le celle vuote non hanno contenuto
        holder.itemView.setVisibility(View.INVISIBLE);
    }

    protected void bindDay(DayViewHolder holder, SharedViewModels.DayItem dayItem, int position) {
        Day day = dayItem.day;
        if (day == null) return;

        android.content.res.Resources r = mContext.getResources();
        boolean isSunday = dayItem.isSunday();
        boolean isToday = dayItem.isToday();

        // Imposta il numero del giorno
        holder.tday.setText(r.getString(R.string.str_scheme_num, day.getDayOfMonth()));

        // Imposta il nome del giorno della settimana
        holder.twday.setText(r.getString(R.string.str_scheme, day.getDayOfWeekAsString()));

        // Reset colori e background
        resetDayViewColors(holder);

        // Imposta i testi per i turni
        bindShiftsToDay(holder, day);

        // Imposta il testo per le squadre a riposo
        String restTeams = day.getOffWorkHalfTeamsAsString();
        holder.ttR.setText(restTeams != null && !restTeams.isEmpty() ?
                r.getString(R.string.str_scheme, restTeams) : "");

        // Trova e evidenzia il turno dell'utente
        highlightUserShift(holder, day);

        // Gestisci colori speciali (oggi e domenica)
        applySpecialDayColors(holder, isToday, isSunday);
    }

    // ==================== HELPER METHODS PER BINDING ====================

    protected void bindShiftsToDay(DayViewHolder holder, Day day) {
        List<Shift> shifts = day.getShifts();
        int numShifts = Math.min(shifts.size(), mNumShifts);

        for (int i = 0; i < numShifts; i++) {
            if (holder.shiftTexts[i] != null) {
                try {
                    String teamText = shifts.get(i).getTeamsAsString();
                    holder.shiftTexts[i].setText(teamText != null && !teamText.isEmpty() ?
                            mContext.getString(R.string.str_scheme, teamText) : "");
                } catch (Exception e) {
                    holder.shiftTexts[i].setText("");
                }
            }
        }
    }

    protected void highlightUserShift(DayViewHolder holder, Day day) {
        int userPosition = -1;
        if (mUserHalfTeam != null) {
            userPosition = day.getInWichTeamIsHalfTeam(mUserHalfTeam);
        }

        if (userPosition >= 0 && userPosition < mNumShifts && holder.shiftTexts[userPosition] != null) {
            holder.shiftTexts[userPosition].setBackgroundColor(mCachedUserShiftBackgroundColor);
            holder.shiftTexts[userPosition].setTextColor(mCachedUserShiftTextColor);
        }
    }

    protected void applySpecialDayColors(DayViewHolder holder, boolean isToday, boolean isSunday) {
        if (isToday) {
            holder.mView.setBackgroundColor(mCachedTodayBackgroundColor);
            setAllDayTextColors(holder, mCachedNormalTextColor);
        } else if (isSunday) {
            setAllDayTextColors(holder, mCachedSundayTextColor);
        } else {
            setAllDayTextColors(holder, mCachedNormalTextColor);
        }
    }

    protected void resetDayViewColors(DayViewHolder holder) {
        holder.mView.setBackgroundColor(Color.TRANSPARENT);

        for (TextView tv : holder.shiftTexts) {
            if (tv != null) {
                tv.setBackgroundColor(Color.TRANSPARENT);
                tv.setTextColor(mCachedNormalTextColor);
            }
        }
    }

    protected void setAllDayTextColors(DayViewHolder holder, int color) {
        holder.tday.setTextColor(color);
        holder.twday.setTextColor(color);
        holder.ttR.setTextColor(color);

        for (TextView tv : holder.shiftTexts) {
            if (tv != null) {
                // Non cambiare colore dei turni evidenziati
                if (tv.getBackground() == null ||
                        ((android.graphics.drawable.ColorDrawable) tv.getBackground()).getColor() == Color.TRANSPARENT) {
                    tv.setTextColor(color);
                }
            }
        }
    }

    // ==================== METODI ASTRATTI/VIRTUALI PER ESTENSIBILITÀ ====================

    /**
     * Le sottoclassi possono override per view types personalizzati.
     */
    protected int getCustomViewType(SharedViewModels.ViewItem item, int position) {
        return VIEW_TYPE_DAY;
    }

    /**
     * Le sottoclassi possono override per ViewHolder personalizzati.
     */
    protected RecyclerView.ViewHolder createCustomViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        // Default: crea un DayViewHolder
        return createDayViewHolder(inflater, parent);
    }

    /**
     * Le sottoclassi possono override per binding personalizzato.
     */
    protected void bindCustomViewHolder(RecyclerView.ViewHolder holder, SharedViewModels.ViewItem item, int position) {
        // Default: nessun binding personalizzato
    }

    // ==================== UTILITY METHODS ====================

    protected void initializeColorCache() {
        if (mCachedNormalTextColor == 0) {
            mCachedNormalTextColor = ThemeUtils.getOnNormalBackgroundColor(mContext);
//            mCachedNormalTextColor = ThemeUtils.getOnPri(mContext);
            mCachedSundayTextColor = ThemeUtils.getSundayTextColor(mContext);
            mCachedTodayBackgroundColor = ThemeUtils.getTodayBackgroundColor(mContext);
            mCachedUserShiftBackgroundColor = ThemeUtils.getMaterialPrimaryContainerColor(mContext);
            mCachedUserShiftTextColor = ThemeUtils.getMaterialOnPrimaryContainerColor(mContext);
        }
    }

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

    // ==================== VIEW HOLDERS COMUNI ====================

    public static class MonthHeaderViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvMonthTitle;

        public MonthHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMonthTitle = itemView.findViewById(R.id.tv_month_title);
        }
    }

    public static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public final ProgressBar progressBar;
        public final TextView loadingText;

        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progress_bar);
            loadingText = itemView.findViewById(R.id.tv_loading);
        }
    }

    public static class EmptyViewHolder extends RecyclerView.ViewHolder {
        public EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public class DayViewHolder extends RecyclerView.ViewHolder {
        public final TextView tday, twday;
        public final TextView[] shiftTexts;
        public final TextView ttR;
        public final View mView;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
            tday = itemView.findViewById(R.id.tday);
            twday = itemView.findViewById(R.id.twday);
            ttR = itemView.findViewById(R.id.ttR);

            shiftTexts = new TextView[mNumShifts];
            for (int i = 0; i < mNumShifts && i < 5; i++) {
                @SuppressLint("DiscouragedApi") int resId = itemView.getResources().getIdentifier("tt" + (i + 1), "id",
                        itemView.getContext().getPackageName());
                if (resId != 0) {
                    shiftTexts[i] = itemView.findViewById(resId);
                }
            }
        }
    }
}