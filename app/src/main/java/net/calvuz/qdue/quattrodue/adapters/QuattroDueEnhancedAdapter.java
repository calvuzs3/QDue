package net.calvuz.qdue.quattrodue.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.quattrodue.models.ShiftType;
import net.calvuz.qdue.quattrodue.utils.HeaderManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import net.calvuz.qdue.utils.Log;

/**
 * Adapter avanzato per la visualizzazione infinita dei giorni con header mensili.
 * Include titoli per ogni mese e evidenziazione solo dei turni dell'utente.
 */
public class QuattroDueEnhancedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "EnhancedAdapter";
    private static final boolean LOG_ENABLED = true;

    // Tipi di view
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_DAY = 1;

    // Costanti per il calcolo delle posizioni
    private static final int INITIAL_POSITION = 10000; // Posizione centrale virtuale
    private static final int BUFFER_SIZE = 100; // Numero di giorni da caricare in anticipo

    private final Context mContext;
    private final QuattroDue mQuattroDue;
    private final LayoutInflater mInflater;
    private int mNumShifts = 3; // Numero predefinito di turni

    // Data di riferimento (centro dello scroll)
    private LocalDate mReferenceDate;

    // Manager per gli header
    private HeaderManager mHeaderManager;

    // Formatter per i titoli dei mesi
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");

    // Costante per il giorno della settimana domenica
    private static final int SUNDAY_VALUE = DayOfWeek.SUNDAY.getValue();

    /**
     * Costruttore con contesto.
     */
    public QuattroDueEnhancedAdapter(Context context) {
        mContext = context;
        mQuattroDue = QuattroDue.getInstance(context);
        mInflater = LayoutInflater.from(context);

        // Imposta la data di riferimento a oggi
        mReferenceDate = LocalDate.now();

        // Inizializza l'header manager
        mHeaderManager = new HeaderManager(mReferenceDate, INITIAL_POSITION);

        if (LOG_ENABLED) {
            Log.d(TAG, "Adapter inizializzato con data di riferimento: " + mReferenceDate);
        }
    }

    /**
     * Imposta il numero di turni visualizzati.
     */
    public void setNumShifts(int numShifts) {
        if (numShifts < 1) numShifts = 1;
        if (numShifts > 5) numShifts = 5;

        mNumShifts = numShifts;
        notifyDataSetChanged();
    }

    /**
     * Calcola la data per una determinata posizione (solo per i giorni).
     */
    private LocalDate getDateForPosition(int position) {
        // Conta quanti header ci sono prima di questa posizione
        int dayPosition = getDayPositionFromAdapterPosition(position);
        int offset = dayPosition - INITIAL_POSITION;
        return mReferenceDate.plusDays(offset);
    }

    /**
     * Converte una posizione dell'adapter in posizione del giorno (escludendo gli header).
     */
    private int getDayPositionFromAdapterPosition(int adapterPosition) {
        // Calcola approssimativamente quanti header ci sono
        // Ogni mese ha circa 30 giorni, quindi ogni 30 posizioni c'è un header
        return adapterPosition - (adapterPosition / 31);
    }

    /**
     * Converte una posizione del giorno in posizione dell'adapter (includendo gli header).
     */
    private int getAdapterPositionFromDayPosition(int dayPosition) {
        // Aggiunge gli header necessari
        return dayPosition + (dayPosition / 30);
    }

    /**
     * Calcola la posizione per una determinata data.
     */
    public int getPositionForDate(LocalDate date) {
        return mHeaderManager.getAdapterPositionForDate(date);
    }

    /**
     * Trova la posizione del giorno corrente.
     */
    public int getTodayPosition() {
        return getPositionForDate(LocalDate.now());
    }

    /**
     * Determina se la posizione corrisponde a un header mensile.
     */
    private boolean isHeaderPosition(int position) {
        return mHeaderManager.isHeaderPosition(position);
    }

    /**
     * Ottiene la data per una posizione specifica, gestendo sia header che giorni.
     */
    private LocalDate getDateForItemPosition(int position) {
        return mHeaderManager.getDateForAdapterPosition(position);
    }

    @Override
    public int getItemViewType(int position) {
        return isHeaderPosition(position) ? VIEW_TYPE_HEADER : VIEW_TYPE_DAY;
    }

    /**
     * ViewHolder per gli header mensili.
     */
    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public TextView monthTitle;

        public HeaderViewHolder(View view) {
            super(view);
//            monthTitle = view.findViewById(R.id.month_title);
        }
    }

    /**
     * ViewHolder per i giorni.
     */
    public static class DayViewHolder extends RecyclerView.ViewHolder {
        public TextView tday, twday;
        public TextView[] shiftTexts;
        public TextView ttE, ttR;
        public View mView;

        public DayViewHolder(View view, int numShifts) {
            super(view);
            mView = view;
            tday = view.findViewById(R.id.tday);
            twday = view.findViewById(R.id.twday);

            // Inizializza array per le TextView dei turni
            shiftTexts = new TextView[numShifts];

            for (int i = 0; i < numShifts && i < 5; i++) {
                int resId = view.getResources().getIdentifier("tt" + (i+1), "id", view.getContext().getPackageName());
                if (resId != 0) {
                    shiftTexts[i] = view.findViewById(resId);
                }
            }

            ttR = view.findViewById(R.id.ttR);
            ttE = view.findViewById(R.id.ttE);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.qdue_month_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.qdue_dayslist_row, parent, false);
            return new DayViewHolder(view, mNumShifts);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            bindHeaderViewHolder((HeaderViewHolder) holder, position);
        } else if (holder instanceof DayViewHolder) {
            bindDayViewHolder((DayViewHolder) holder, position);
        }
    }

    /**
     * Configura un header mensile.
     */
    private void bindHeaderViewHolder(HeaderViewHolder holder, int position) {
        LocalDate monthDate = mHeaderManager.getMonthForHeaderPosition(position);
        if (monthDate != null) {
            String monthTitle = monthDate.format(MONTH_FORMATTER);
//            holder.monthTitle.setText(monthTitle);

            if (LOG_ENABLED) {
                Log.v(TAG, "Header creato per: " + monthTitle + " alla posizione " + position);
            }
        }
    }

    /**
     * Configura un giorno.
     */
    private void bindDayViewHolder(DayViewHolder holder, int position) {
        Resources r = mContext.getResources();

        // Calcola la data per questa posizione
        LocalDate currentDate = getDateForPosition(position);

        // Genera il giorno usando QuattroDue
        Day day = mQuattroDue.getDayForDate(currentDate);

        if (day == null) {
            day = new Day(currentDate);
        }

        // Imposta il numero del giorno
        holder.tday.setText(r.getString(R.string.str_scheme_num, day.getDayOfMonth()));

        // Imposta il nome del giorno della settimana
        holder.twday.setText(r.getString(R.string.str_scheme, day.getDayOfWeekAsString()));

        // Resetta tutti i colori dei turni
        resetShiftColors(holder);

        // Imposta i testi per i turni
        List<Shift> shifts = day.getShifts();
        int numShifts = Math.min(shifts.size(), mNumShifts);

        for (int i = 0; i < mNumShifts; i++) {
            if (holder.shiftTexts[i] != null) {
                if (i < numShifts) {
                    try {
                        String teamText = shifts.get(i).getTeamsAsString();
                        holder.shiftTexts[i].setText(teamText != null && !teamText.isEmpty() ?
                                r.getString(R.string.str_scheme, teamText) : "");
                    } catch (Exception e) {
                        holder.shiftTexts[i].setText("");
                    }
                } else {
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
        if (day.getDayOfWeek() == SUNDAY_VALUE) {
            holder.tday.setTextColor(r.getColor(R.color.colorTextRed));
            holder.twday.setTextColor(r.getColor(R.color.colorTextRed));
        } else {
            holder.tday.setTextColor(r.getColor(R.color.colorTextBlack));
            holder.twday.setTextColor(r.getColor(R.color.colorTextBlack));
        }

        // **MODIFICA PRINCIPALE**: Evidenzia SOLO i turni dell'utente
        HalfTeam userHalfTeam = mQuattroDue.getUserHalfTeam();
        int userPosition = -1;
        if (userHalfTeam != null) {
            userPosition = day.getInWichTeamIsHalfTeam(userHalfTeam);
        }

        // Evidenzia solo il turno dell'utente (non più tutti i background dei turni)
        if (userPosition >= 0 && userPosition < numShifts && holder.shiftTexts[userPosition] != null) {
            holder.shiftTexts[userPosition].setBackgroundColor(r.getColor(R.color.colorBackgroundLightBlue));
            holder.shiftTexts[userPosition].setTextColor(r.getColor(R.color.colorTextWhite));
        }

        // Gestisce le fermate degli impianti (solo se non è già evidenziato per l'utente)
        for (int i = 0; i < numShifts; i++) {
            if (i < shifts.size() && shifts.get(i).isStop() && holder.shiftTexts[i] != null && i != userPosition) {
                holder.shiftTexts[i].setBackgroundColor(r.getColor(R.color.colorBackgroundRed));
            }
        }

        // **RIMOSSO**: Non applica più i colori personalizzati per tutti i turni
        // Solo l'utente e le fermate hanno colori speciali

        // Evidenzia il giorno corrente
        if (day.getIsToday()) {
            holder.mView.setBackgroundColor(r.getColor(R.color.colorBackgroundYellow));
        } else {
            holder.mView.setBackgroundColor(0); // Trasparente
        }
    }

    /**
     * Reimposta i colori dei turni ai valori predefiniti.
     */
    private void resetShiftColors(DayViewHolder holder) {
        if (holder.shiftTexts != null) {
            for (TextView tv : holder.shiftTexts) {
                if (tv != null) {
                    tv.setBackgroundColor(0); // Trasparente
                    tv.setTextColor(mContext.getResources().getColor(R.color.colorTextBlack));
                }
            }
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return Integer.MAX_VALUE;
    }

    /**
     * Aggiorna la data di riferimento.
     */
    public void setReferenceDate(LocalDate referenceDate) {
        mReferenceDate = referenceDate;
        mHeaderManager = new HeaderManager(mReferenceDate, INITIAL_POSITION);
        notifyDataSetChanged();

        if (LOG_ENABLED) {
            Log.d(TAG, "Data di riferimento aggiornata: " + mReferenceDate);
        }
    }
}