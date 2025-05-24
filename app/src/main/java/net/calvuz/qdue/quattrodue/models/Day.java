package net.calvuz.qdue.quattrodue.models;

import static net.calvuz.qdue.quattrodue.QuattroDue.HALFTEAM_ALL;

import androidx.annotation.NonNull;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import net.calvuz.qdue.quattrodue.Costants;
import net.calvuz.qdue.quattrodue.utils.Log;

/**
 * Rappresenta un giorno del calendario con i suoi turni di lavoro.
 */
public class Day implements Cloneable {

    // TAG
    public final static String TAG = Day.class.getSimpleName();

    // Configurazione del logging
    private final static boolean LOG_ENABLED = Costants.QD_LOG_ENABLED;
    private final static boolean LOG_SHIFTS = LOG_ENABLED;
    private final static boolean LOG_STOPS = LOG_ENABLED;

    // Formattatore per i giorni della settimana
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("EEEE");

    // Dati principali
    private LocalDate localDate;  // NON final per permettere la modifica
    private boolean isToday;
    private List<Shift> shifts;
    private List<HalfTeam> offWorkHalfTeams;

    /**
     * Crea un nuovo giorno con la data specificata.
     *
     * @param date La data del giorno
     */
    public Day(LocalDate date) {
        if (LOG_ENABLED) Log.v(TAG, "New Day {" + date + "}");

        this.localDate = date;
        this.shifts = new ArrayList<>();

        // Imposta isToday se è oggi
        LocalDate today = LocalDate.now();
        this.isToday = date.equals(today);

        if (LOG_ENABLED) Log.d(TAG, date + " - " + this.localDate);

        // Inizializza le squadre a riposo con tutte le squadre disponibili
        try {
            this.offWorkHalfTeams = new ArrayList<>(HALFTEAM_ALL);
        } catch (Exception e) {
            Log.e(TAG, "HALFTEAM.clone() : " + e.getMessage());
        }
    }

    /**
     * Trova l'indice del turno in cui si trova una determinata squadra.
     *
     * @param halfTeam La squadra da cercare
     * @return L'indice del turno o -1 se la squadra non è in turno
     */
    public int getInWichTeamIsHalfTeam(HalfTeam halfTeam) {
        if (shifts == null || shifts.isEmpty()) {
            return -1;
        }

        for (int i = 0; i < shifts.size(); i++) {
            for (HalfTeam ht : shifts.get(i).getHalfTeams()) {
                if (ht.isSameTeamAs(halfTeam)) {
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * Restituisce tutti i turni del giorno.
     *
     * @return La lista dei turni
     */
    public List<Shift> getShifts() {
        return shifts != null ? Collections.unmodifiableList(shifts) : new ArrayList<>();
    }

    /**
     * Imposta i turni del giorno creando un clone per ogni turno passato.
     *
     * @param shifts I turni da copiare
     */
    private void setShifts(@NonNull List<Shift> shifts) {
        this.shifts = new ArrayList<>();

        for (Shift shift : shifts) {
            try {
                addShift(shift.clone());
            } catch (CloneNotSupportedException e) {
                Log.e(TAG, "setShifts: " + e.getMessage());
            }
        }
    }

    /**
     * Aggiunge un nuovo turno al giorno.
     *
     * @param shift Il turno da aggiungere
     */
    public void addShift(Shift shift) {
        if (shifts == null) {
            shifts = new ArrayList<>();
        }

        shifts.add(shift);
        if (LOG_SHIFTS) Log.d(TAG, "addShift: " + shift);

        // Rimuove le squadre del turno dalla lista delle squadre a riposo
        if (offWorkHalfTeams != null && !offWorkHalfTeams.isEmpty()) {
            Iterator<HalfTeam> itr = offWorkHalfTeams.iterator();

            while (itr.hasNext()) {
                HalfTeam offWorkTeam = itr.next();

                for (HalfTeam shiftTeam : shift.getHalfTeams()) {
                    if (shiftTeam.isSameTeamAs(offWorkTeam)) {
                        itr.remove();
                        break;
                    }
                }
            }
        }
    }

    /**
     * Imposta le squadre a riposo creando un clone per ogni squadra passata.
     *
     * @param offWorkHalfTeams La lista delle squadre a riposo
     */
    private void setOffWorkHalfTeams(@NonNull List<HalfTeam> offWorkHalfTeams) {
        this.offWorkHalfTeams = new ArrayList<>();

        for (HalfTeam team : offWorkHalfTeams) {
            try {
                addOffWorkHalfTeam(team.clone());
            } catch (CloneNotSupportedException e) {
                Log.e(TAG, "setOffWorkHalfTeam: " + e.getMessage());
            }
        }
    }

    /**
     * Aggiunge una squadra alla lista delle squadre a riposo.
     *
     * @param halfTeam La squadra da aggiungere
     */
    private void addOffWorkHalfTeam(HalfTeam halfTeam) {
        if (this.offWorkHalfTeams == null) {
            this.offWorkHalfTeams = new ArrayList<>();
        }
        this.offWorkHalfTeams.add(halfTeam);
    }

    /**
     * Restituisce il giorno del mese.
     *
     * @return Il giorno del mese
     */
    public int getDayOfMonth() {
        return localDate.getDayOfMonth();
    }

    /**
     * Restituisce il giorno della settimana.
     *
     * @return Il giorno della settimana (1-7, dove 1 è lunedì e 7 è domenica)
     */
    public int getDayOfWeek() {
        return localDate.getDayOfWeek().getValue();
    }

    /**
     * Restituisce il nome del giorno della settimana.
     *
     * @return Il nome del giorno della settimana
     */
    public String getDayOfWeekAsString() {
        return localDate.format(DAY_FORMATTER);
    }

    /**
     * Restituisce una stringa con tutte le squadre a riposo.
     *
     * @return Una stringa con i nomi delle squadre a riposo
     */
    public String getOffWorkHalfTeamsAsString() {
        if (offWorkHalfTeams == null || offWorkHalfTeams.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (HalfTeam halfTeam : offWorkHalfTeams) {
            result.append(halfTeam.getName());
        }
        return result.toString();
    }

    /**
     * Restituisce una stringa con le squadre di un determinato turno.
     *
     * @param position L'indice del turno
     * @return Una stringa con i nomi delle squadre del turno
     */
    public String getTeamsAsString(int position) {
        if (shifts == null || shifts.size() <= position) {
            return "";
        }

        return shifts.get(position).getTeamsAsString();
    }

    /**
     * Verifica se questo giorno è oggi.
     *
     * @return true se è oggi, false altrimenti
     */
    public boolean getIsToday() {
        return this.isToday;
    }

    /**
     * Imposta se questo giorno è oggi.
     *
     * @param isToday true se è oggi, false altrimenti
     */
    public void setIsToday(boolean isToday) {
        this.isToday = isToday;
    }

    /**
     * Imposta la data del giorno.
     *
     * @param date La nuova data
     */
    public void setLocalDate(@NonNull LocalDate date) {
        // Ora possiamo modificare la data perché localDate non è final
        this.localDate = date;
        if (LOG_ENABLED) Log.d(TAG, "setLocalDate: " + date);
    }

    /**
     * Restituisce la data del giorno.
     *
     * @return La data
     */
    public LocalDate getDate() {
        return this.localDate;
    }

    /**
     * Verifica se il giorno ha eventi.
     *
     * @return true se ha eventi, false altrimenti
     */
    public boolean hasEvents() {
        return false;
    }

    /**
     * Imposta un turno come fermata impianti.
     *
     * @param shiftIndex L'indice del turno (1-based)
     */
    public void setStop(int shiftIndex) {
        if (shifts != null && shifts.size() >= shiftIndex && shiftIndex > 0) {
            shifts.get(shiftIndex - 1).setStop(true);
            if (LOG_STOPS) Log.v(TAG, "setStop: " + shiftIndex);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return TAG + "{" + this.localDate + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Day other = (Day) obj;
        return Objects.equals(localDate, other.localDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localDate);
    }

    @NonNull
    @Override
    public Day clone() throws CloneNotSupportedException {
        Day cloned = (Day) super.clone();

        // Reimposta isToday su false per il clone
        cloned.setIsToday(false);

        // Clona la data
        cloned.localDate = localDate;

        // Clona le squadre a riposo
        if (this.offWorkHalfTeams != null) {
            cloned.setOffWorkHalfTeams(this.offWorkHalfTeams);
        }

        // Clona i turni
        if (this.shifts != null) {
            cloned.setShifts(this.shifts);
        }

        return cloned;
    }
}