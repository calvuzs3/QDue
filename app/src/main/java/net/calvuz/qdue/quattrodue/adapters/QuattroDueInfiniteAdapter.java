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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import net.calvuz.qdue.utils.Log;

/**
 * Adapter per la visualizzazione infinita dei giorni in una RecyclerView.
 * Implementa uno scrolling infinito caricando dinamicamente i giorni.
 */
public class QuattroDueInfiniteAdapter extends RecyclerView.Adapter<QuattroDueInfiniteAdapter.ViewHolder> {

    private static final String TAG = "InfiniteAdapter";
    private static final boolean LOG_ENABLED = true;

    // Costanti per il calcolo delle posizioni
    private static final int INITIAL_POSITION = 10000; // Posizione centrale virtuale
    private static final int BUFFER_SIZE = 100; // Numero di giorni da caricare in anticipo

    private final Context mContext;
    private final QuattroDue mQuattroDue;
    private final LayoutInflater mInflater;
    private int mNumShifts = 3; // Numero predefinito di turni

    // Data di riferimento (centro dello scroll)
    private LocalDate mReferenceDate;

    // Costante per il giorno della settimana domenica
    private static final int SUNDAY_VALUE = DayOfWeek.SUNDAY.getValue();

    /**
     * Costruttore con contesto.
     *
     * @param context Contesto dell'applicazione
     */
    public QuattroDueInfiniteAdapter(Context context) {
        mContext = context;
        mQuattroDue = QuattroDue.getInstance(context);
        mInflater = LayoutInflater.from(context);

        // Imposta la data di riferimento a oggi
        mReferenceDate = LocalDate.now();

        if (LOG_ENABLED) {
            Log.d(TAG, "Adapter inizializzato con data di riferimento: " + mReferenceDate);
        }
    }

    /**
     * Imposta il numero di turni visualizzati.
     *
     * @param numShifts Numero di turni
     */
    public void setNumShifts(int numShifts) {
        if (numShifts < 1) numShifts = 1; // Almeno un turno
        if (numShifts > 5) numShifts = 5; // Massimo 5 turni

        mNumShifts = numShifts;
        notifyDataSetChanged();
    }

    /**
     * Calcola la data per una determinata posizione.
     *
     * @param position Posizione nell'adapter
     * @return Data corrispondente
     */
    private LocalDate getDateForPosition(int position) {
        int offset = position - INITIAL_POSITION;
        return mReferenceDate.plusDays(offset);
    }

    /**
     * Calcola la posizione per una determinata data.
     *
     * @param date Data
     * @return Posizione nell'adapter
     */
    public int getPositionForDate(LocalDate date) {
        long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(mReferenceDate, date);
        return INITIAL_POSITION + (int) daysDifference;
    }

    /**
     * Trova la posizione del giorno corrente.
     *
     * @return Posizione del giorno corrente
     */
    public int getTodayPosition() {
        return getPositionForDate(LocalDate.now());
    }

    /**
     * ViewHolder per mantenere i riferimenti alle viste.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tday, twday;  // giorno del mese, giorno della settimana
        public TextView[] shiftTexts; // array di textview per i turni
        public TextView ttE, ttR;     // testo per eventi e riposi
        public View mView;            // root view

        public ViewHolder(View view, int numShifts) {
            super(view);
            mView = view;
            tday = view.findViewById(R.id.tday);
            twday = view.findViewById(R.id.twday);

            // Inizializza array per le TextView dei turni
            shiftTexts = new TextView[numShifts];

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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.qdue_dayslist_row, parent, false);
        return new ViewHolder(view, mNumShifts);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Resources r = mContext.getResources();

        // Calcola la data per questa posizione
        LocalDate currentDate = getDateForPosition(position);

        // Genera il giorno usando QuattroDue
        Day day = mQuattroDue.getDayForDate(currentDate);

        if (day == null) {
            // Fallback: crea un giorno vuoto
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

        // Ottiene la squadra dell'utente
        HalfTeam userHalfTeam = mQuattroDue.getUserHalfTeam();

        // Trova la posizione della squadra dell'utente
        int userPosition = -1;
        if (userHalfTeam != null) {
            userPosition = day.getInWichTeamIsHalfTeam(userHalfTeam);
        }

        // Evidenzia il turno dell'utente
        if (userPosition >= 0 && userPosition < numShifts && holder.shiftTexts[userPosition] != null) {
            holder.shiftTexts[userPosition].setBackgroundColor(r.getColor(R.color.colorBackgroundLightBlue));
            holder.shiftTexts[userPosition].setTextColor(r.getColor(R.color.colorTextWhite));
        }

        // Gestisce le fermate degli impianti
        for (int i = 0; i < numShifts; i++) {
            if (i < shifts.size() && shifts.get(i).isStop() && holder.shiftTexts[i] != null) {
                holder.shiftTexts[i].setBackgroundColor(r.getColor(R.color.colorBackgroundRed));
            }
        }

        // Applica i colori personalizzati dei turni
        applyShiftColors(holder, shifts);

        // Evidenzia il giorno corrente
        if (day.getIsToday()) {
            holder.mView.setBackgroundColor(r.getColor(R.color.colorBackgroundYellow));
        } else {
            holder.mView.setBackgroundColor(0); // Trasparente
        }
    }

    /**
     * Applica i colori personalizzati dei turni.
     */
    private void applyShiftColors(ViewHolder holder, List<Shift> shifts) {
        for (int i = 0; i < Math.min(shifts.size(), mNumShifts); i++) {
            ShiftType shiftType = shifts.get(i).getShiftType();
            if (shiftType != null && holder.shiftTexts[i] != null) {
                int color = shiftType.getColor();
                if (color != 0 &&
                        holder.shiftTexts[i].getBackgroundTintList() == null) {
                    holder.shiftTexts[i].setBackgroundColor(color);
                }
            }
        }
    }

    /**
     * Reimposta i colori dei turni ai valori predefiniti.
     */
    private void resetShiftColors(ViewHolder holder) {
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
        // Restituisce un numero molto grande per simulare lo scroll infinito
        return Integer.MAX_VALUE;
    }

    /**
     * Aggiorna la data di riferimento.
     *
     * @param referenceDate Nuova data di riferimento
     */
    public void setReferenceDate(LocalDate referenceDate) {
        mReferenceDate = referenceDate;
        notifyDataSetChanged();

        if (LOG_ENABLED) {
            Log.d(TAG, "Data di riferimento aggiornata: " + mReferenceDate);
        }
    }
}