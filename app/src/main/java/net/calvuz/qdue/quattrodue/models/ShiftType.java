package net.calvuz.qdue.quattrodue.models;

import android.os.Build;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Rappresenta un tipo di turno con orario, durata e colore associato.
 */
public class ShiftType {

    private static final String TAG = ShiftType.class.getSimpleName();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Proprietà principali
    private final String name;             // Nome del turno (es. "Mattino")
    private final String description;      // Descrizione (es. "Turno del mattino 6-14")
    private final LocalTime startTime;     // Orario di inizio
    private final Duration duration;       // Durata del turno
    private final @ColorInt int color;     // Colore associato al turno

    /**
     * Costruttore completo con tutti i parametri.
     *
     * @param name Nome del turno
     * @param description Descrizione del turno
     * @param startHour Ora di inizio (0-23)
     * @param startMinute Minuto di inizio (0-59)
     * @param durationHours Durata in ore
     * @param durationMinutes Durata in minuti aggiuntivi
     * @param color Colore associato al turno (formato ARGB)
     */
    public ShiftType(String name, String description, int startHour, int startMinute,
                     int durationHours, int durationMinutes, @ColorInt int color) {
        this.name = name;
        this.description = description;
        this.startTime = LocalTime.of(startHour, startMinute);
        this.duration = Duration.ofHours(durationHours).plusMinutes(durationMinutes);
        this.color = color;
    }

    /**
     * Costruttore compatibile con la versione precedente.
     *
     * @param name Nome del turno
     * @param description Descrizione del turno
     * @param startHour Ora di inizio (0-23)
     * @param startMinute Minuto di inizio (0-59)
     * @param durationHours Durata in ore
     * @param durationMinutes Durata in minuti aggiuntivi
     */
    public ShiftType(String name, String description, int startHour, int startMinute,
                     int durationHours, int durationMinutes) {
        this(name, description, startHour, startMinute, durationHours, durationMinutes, 0); // Colore predefinito
    }

    /**
     * Restituisce il nome del turno.
     *
     * @return Nome del turno
     */
    public String getName() {
        return name;
    }

    /**
     * Restituisce la descrizione del turno.
     *
     * @return Descrizione del turno
     */
    public String getDescription() {
        return description;
    }

    /**
     * Restituisce l'ora di inizio.
     *
     * @return Ora di inizio (0-23)
     */
    public int getStartHour() {
        return startTime.getHour();
    }

    /**
     * Restituisce il minuto di inizio.
     *
     * @return Minuto di inizio (0-59)
     */
    public int getStartMinute() {
        return startTime.getMinute();
    }

    /**
     * Restituisce l'orario di inizio come oggetto LocalTime.
     *
     * @return Orario di inizio
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * Restituisce la durata del turno in ore.
     *
     * @return Ore di durata
     */
    public int getDurationHours() {
        return (int) duration.toHours();
    }

    /**
     * Restituisce i minuti aggiuntivi della durata.
     *
     * @return Minuti aggiuntivi di durata (0-59)
     */
    public int getDurationMinutes() {
        return duration.toMinutesPart();
    }

    /**
     * Restituisce la durata totale come oggetto Duration.
     *
     * @return Durata del turno
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * Restituisce il colore associato al turno.
     *
     * @return Colore in formato ARGB
     */
    public @ColorInt int getColor() {
        return color;
    }

    /**
     * Calcola l'orario di fine del turno.
     *
     * @return Orario di fine
     */
    public LocalTime getEndTime() {
        return startTime.plus(duration);
    }

    /**
     * Verifica se un determinato orario è compreso nel turno.
     *
     * @param time Orario da verificare
     * @return true se l'orario è compreso nel turno, false altrimenti
     */
    public boolean includes(LocalTime time) {
        return !time.isBefore(startTime) && !time.isAfter(getEndTime());
    }

    /**
     * Restituisce una rappresentazione testuale dell'orario di inizio.
     *
     * @return Orario di inizio formattato (es. "06:00")
     */
    public String getFormattedStartTime() {
        return startTime.format(TIME_FORMATTER);
    }

    /**
     * Restituisce una rappresentazione testuale dell'orario di fine.
     *
     * @return Orario di fine formattato (es. "14:00")
     */
    public String getFormattedEndTime() {
        return getEndTime().format(TIME_FORMATTER);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, startTime, duration, color);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ShiftType other = (ShiftType) obj;
        return Objects.equals(name, other.name) &&
                Objects.equals(description, other.description) &&
                Objects.equals(startTime, other.startTime) &&
                Objects.equals(duration, other.duration) &&
                color == other.color;
    }

    @NonNull
    @Override
    public String toString() {
        return "ShiftType{" + name + " " + getFormattedStartTime() + "-" + getFormattedEndTime() + "}";
    }
}
