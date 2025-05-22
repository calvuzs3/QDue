package net.calvuz.qdue.quattrodue.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
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
 * Modificato per evidenziare solo i turni dell'utente in base alle preferenze.
 */
public class QuattroDueListAdapter extends RecyclerView.Adapter<QuattroDueListAdapter.ViewHolder> {

    private static final String TAG = "QuattroDueListAdapter";
    private static final boolean LOG_ENABLED = true;

    private final Context mContext;
    private ArrayList<Day> mDaysList;
    private final LayoutInflater mInflater;
    private int mNumShifts = 3; // Numero predefinito di turni

    // Preferenze dell'utente
    private boolean mShowStops = true;

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

        // Carica le preferenze dell'utente
        loadUserPreferences();
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
     * Carica le preferenze dell'utente dalle SharedPreferences
     */
    private void loadUserPreferences() {
        if (mContext != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            mShowStops = prefs.getBoolean("preference_show_stops", true);
        }
    }

    /**
     * Aggiorna le preferenze dell'utente
     */
    public void updateUserPreferences() {
        loadUserPreferences();
        notifyDataSetChanged();
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
        resetShiftViews(holder);

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

        // Evidenzia SOLO il turno dell'utente
        if (userPosition >= 0 && userPosition < numShifts && holder.shiftTexts[userPosition] != null) {
            holder.shiftTexts[userPosition].setBackgroundColor(r.getColor(R.color.colorBackgroundLightBlue));
            holder.shiftTexts[userPosition].setTextColor(r.getColor(R.color.colorTextWhite));
        }

        // Gestisce le fermate degli impianti solo se l'opzione è abilitata
        if (mShowStops) {
            for (int i = 0; i < numShifts; i++) {
                if (i < shifts.size() && shifts.get(i).isStop() && holder.shiftTexts[i] != null) {
                    // Se è anche il turno dell'utente, aggiungiamo solo un bordo rosso invece di cambiare lo sfondo
                    if (i == userPosition) {
                        // Indica che è una fermata anche se è il turno dell'utente
                        // Possiamo usare un bordo o un altro indicatore visivo
                        holder.shiftTexts[i].setBackgroundColor(r.getColor(R.color.colorBackgroundRed));
                        holder.shiftTexts[i].setTextColor(r.getColor(R.color.colorTextWhite));
                    } else {
                        holder.shiftTexts[i].setBackgroundColor(r.getColor(R.color.colorBackgroundRed));
                    }
                }
            }
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
     *
     * @param holder ViewHolder contenente le viste
     */
    private void resetShiftViews(ViewHolder holder) {
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