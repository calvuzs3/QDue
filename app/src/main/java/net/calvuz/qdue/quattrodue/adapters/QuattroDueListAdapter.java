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
import java.util.ArrayList;
import java.util.List;

import net.calvuz.qdue.utils.Log;

/**
 * Adapter per la visualizzazione dei giorni in una RecyclerView.
 * Aggiornato per supportare colori personalizzati per i turni.
 */
public class QuattroDueListAdapter extends RecyclerView.Adapter<QuattroDueListAdapter.ViewHolder> {

    private static final String TAG = "QuattroDueListAdapter";
    private static final boolean LOG_ENABLED = true;

    private final Context mContext;
    private ArrayList<Day> mDaysList;
    private final LayoutInflater mInflater;
    private int mNumShifts = 3; // Numero predefinito di turni

    // Costante per il giorno della settimana domenica
    private static final int SUNDAY_VALUE = DayOfWeek.SUNDAY.getValue();

    /**
     * Costruttore con contesto e dati.
     *
     * @param context Contesto dell'applicazione
     * @param daysList Lista dei giorni da visualizzare
     */
    public QuattroDueListAdapter(Context context, ArrayList<Day> daysList) {
        mContext = context;
        mDaysList = daysList != null ? daysList : new ArrayList<>();
        mInflater = LayoutInflater.from(context);
    }

    /**
     * Costruttore vuoto per inizializzazioni successive.
     */
    public QuattroDueListAdapter() {
        mContext = null;
        mDaysList = new ArrayList<>();
        mInflater = null;
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
            // Nota: questo presuppone che i tuoi TextView per i turni abbiano ID tt1, tt2, tt3, ecc.
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
    public void onBindViewHolder(@NonNull QuattroDueListAdapter.ViewHolder holder, int position) {
        Resources r = mContext.getResources();
        Day day = mDaysList.get(position);

        // Imposta il numero del giorno
        holder.tday.setText(r.getString(R.string.str_scheme_num, day.getDayOfMonth()));

        // Imposta il nome del giorno della settimana
        holder.twday.setText(r.getString(R.string.str_scheme, day.getDayOfWeekAsString()));

        // Resetta tutti i colori dei turni
        resetShiftColors(holder);

        // Imposta i testi per i turni
        List<Shift> shifts = day.getShifts();
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
        if (day.getDayOfWeek() == SUNDAY_VALUE) {
            holder.tday.setTextColor(r.getColor(R.color.colorTextRed));
            holder.twday.setTextColor(r.getColor(R.color.colorTextRed));
        } else {
            holder.tday.setTextColor(r.getColor(R.color.colorTextBlack));
            holder.twday.setTextColor(r.getColor(R.color.colorTextBlack));
        }

        // Ottiene la squadra dell'utente
        QuattroDue qd = QuattroDue.getInstance(mContext);
        HalfTeam userHalfTeam = qd.getUserHalfTeam();

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
     *
     * @param holder ViewHolder contenente le viste
     * @param shifts Lista dei turni
     */
    private void applyShiftColors(ViewHolder holder, List<Shift> shifts) {
        // Applica i colori personalizzati solo se non è già stato evidenziato per l'utente
        // o per le fermate
        for (int i = 0; i < Math.min(shifts.size(), mNumShifts); i++) {
            // Ottiene il colore del tipo di turno
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
     *
     * @param holder ViewHolder contenente le viste
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

    /**
     * Imposta la lista dei giorni.
     *
     * @param daysList Lista dei giorni
     */
    public void setDaysList(ArrayList<Day> daysList) {
        mDaysList = daysList != null ? daysList : new ArrayList<>();
        notifyDataSetChanged();

        if (LOG_ENABLED) {
            Log.d(TAG, "setDaysList: aggiornata lista con " + mDaysList.size() + " giorni");
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mDaysList.size();
    }
}
