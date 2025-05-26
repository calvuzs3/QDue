package net.calvuz.qdue.quattrodue.models;

import androidx.annotation.NonNull;

import net.calvuz.qdue.quattrodue.Costants;
import net.calvuz.qdue.quattrodue.utils.Log;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rappresenta un turno di lavoro in un giorno specifico.
 * Un turno è caratterizzato da un tipo (ShiftType) e può includere più squadre (HalfTeam).
 *
 * @author Luke (originale)
 * @author Aggiornato 21/05/2025
 */
public class Shift implements Cloneable {

    // TAG
    private static final String TAG = Shift.class.getSimpleName();

    // Configurazione del logging
    private final static boolean LOG_ENABLED = Costants.QD_LOG_ENABLED;

    // Proprietà del turno
    private final ShiftType shiftType;
    private boolean stop; // true se il turno è in fermata
    private final Set<HalfTeam> halfTeams;
    private LocalDate date; // Data opzionale del turno

    /**
     * Crea un nuovo turno con il tipo specificato.
     *
     * @param shiftType Tipo di turno
     */
    public Shift(ShiftType shiftType) {
        this.shiftType = shiftType;
        this.stop = false;
        this.halfTeams = new HashSet<>();
    }

    /**
     * Crea un nuovo turno con tipo e data.
     *
     * @param shiftType Tipo di turno
     * @param date Data del turno
     */
    public Shift(ShiftType shiftType, LocalDate date) {
        this.shiftType = shiftType;
        this.stop = false;
        this.halfTeams = new HashSet<>();
        this.date = date;
    }

    /**
     * Verifica se il turno è in fermata.
     *
     * @return true se il turno è in fermata, false altrimenti
     */
    public boolean isStop() {
        return stop;
    }

    /**
     * Imposta lo stato di fermata del turno.
     *
     * @param stop true se il turno è in fermata, false altrimenti
     */
    public void setStop(boolean stop) {
        this.stop = stop;
    }

    /**
     * Aggiunge una squadra al turno.
     *
     * @param halfTeam Squadra da aggiungere
     */
    public void addTeam(HalfTeam halfTeam) {
        if (halfTeam == null) {
            if (LOG_ENABLED) Log.w(TAG, "Tentativo di aggiungere una squadra null");
            return;
        }

        halfTeams.add(halfTeam);
    }

    /**
     * Restituisce tutte le squadre assegnate al turno.
     *
     * @return Set immutabile delle squadre
     */
    public Set<HalfTeam> getHalfTeams() {
        return Collections.unmodifiableSet(halfTeams);
    }

    /**
     * Verifica se una specifica squadra è assegnata a questo turno.
     *
     * @param halfTeam Squadra da verificare
     * @return true se la squadra è assegnata al turno, false altrimenti
     */
    public boolean containsHalfTeam(HalfTeam halfTeam) {
        if (halfTeam == null) return false;

        return halfTeams.stream()
                .anyMatch(ht -> ht.isSameTeamAs(halfTeam));
    }

    /**
     * Restituisce una stringa con i nomi di tutte le squadre del turno.
     *
     * @return Stringa concatenata con i nomi delle squadre
     */
    public String getTeamsAsString() {
        return halfTeams.stream()
                .map(HalfTeam::getName)
                .collect(Collectors.joining());
    }

    /**
     * Restituisce il tipo di turno.
     *
     * @return Tipo di turno
     */
    public ShiftType getShiftType() {
        return shiftType;
    }

    /**
     * Restituisce la data del turno.
     *
     * @return Data del turno, o null se non specificata
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Imposta la data del turno.
     *
     * @param date Data del turno
     */
    public void setDate(LocalDate date) {
        this.date = date;
    }

    /**
     * Restituisce il nome del turno.
     *
     * @return Nome del turno
     */
    public String getName() {
        return shiftType != null ? shiftType.getName() : "Sconosciuto";
    }

    /**
     * Restituisce l'ora di inizio del turno.
     *
     * @return Ora di inizio formattata
     */
    public String getStartTime() {
        return shiftType != null ? shiftType.getFormattedStartTime() : "";
    }

    /**
     * Restituisce l'ora di fine del turno.
     *
     * @return Ora di fine formattata
     */
    public String getEndTime() {
        return shiftType != null ? shiftType.getFormattedEndTime() : "";
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(TAG);
        sb.append("{");

        if (shiftType != null) {
            sb.append(shiftType.getName());
        } else {
            sb.append("Sconosciuto");
        }

        if (isStop()) {
            sb.append("-stop");
        }

        if (date != null) {
            sb.append(" ").append(date);
        }

        if (!halfTeams.isEmpty()) {
            sb.append(" HalfTeams: ").append(getTeamsAsString());
        }

        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Shift other = (Shift) obj;
        return Objects.equals(shiftType, other.shiftType) &&
                stop == other.stop &&
                Objects.equals(date, other.date) &&
                halfTeams.size() == other.halfTeams.size() &&
                halfTeams.containsAll(other.halfTeams);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shiftType, stop, date, halfTeams.size());
    }

    @Override
    public Shift clone() throws CloneNotSupportedException {
        Shift clone = (Shift) super.clone();

        // Copia lo stato di fermata
        clone.setStop(this.stop);

        // Copia la data
        if (this.date != null) {
            clone.setDate(LocalDate.of(this.date.getYear(),
                    this.date.getMonth(),
                    this.date.getDayOfMonth()));
        }

        // Clona le squadre
        for (HalfTeam team : this.halfTeams) {
            try {
                clone.addTeam(team.clone());
            } catch (CloneNotSupportedException e) {
                Log.e(TAG, "Errore durante la clonazione della squadra: " + e.getMessage());
            }
        }

        return clone;
    }

    /**
     * Builder per creare istanze di Shift in modo fluente.
     */
    public static class Builder {
        private ShiftType shiftType;
        private boolean stop = false;
        private LocalDate date;
        private final Set<HalfTeam> halfTeams = new HashSet<>();

        public Builder withShiftType(ShiftType shiftType) {
            this.shiftType = shiftType;
            return this;
        }

        public Builder withStop(boolean stop) {
            this.stop = stop;
            return this;
        }

        public Builder withDate(LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder addTeam(HalfTeam halfTeam) {
            if (halfTeam != null) {
                this.halfTeams.add(halfTeam);
            }
            return this;
        }

        public Builder addTeams(Set<HalfTeam> teams) {
            if (teams != null) {
                this.halfTeams.addAll(teams);
            }
            return this;
        }

        public Shift build() {
            Shift shift = new Shift(shiftType, date);
            shift.setStop(stop);
            halfTeams.forEach(shift::addTeam);
            return shift;
        }
    }
}
