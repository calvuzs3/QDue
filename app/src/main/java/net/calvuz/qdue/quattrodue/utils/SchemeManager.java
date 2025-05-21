package net.calvuz.qdue.quattrodue.utils;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.quattrodue.models.ShiftType;
import net.calvuz.qdue.utils.Log;

/**
 * Classe di utilità per la gestione dello schema dei turni.
 * Questa classe implementa la logica per applicare lo schema fisso dei turni.
 */
public final class SchemeManager {

    private static final String TAG = "SchemeManager";

    // Costanti per lo schema
    private static final int NUMERO_TURNI_AL_GIORNO = 3;

    // Schema fisso di rotazione (ogni riga è un giorno, ogni colonna è un turno)
    // Per ogni elemento, la lista di caratteri rappresenta le squadre assegnate a quel turno
    private static final char[][][] SCHEME = new char[][][]{
            {{'A', 'B'}, {'C', 'D'}, {'E', 'F'}, {'G', 'H', 'I'}},
            {{'A', 'B'}, {'C', 'D'}, {'E', 'F'}, {'G', 'H', 'I'}},
            {{'A', 'H'}, {'D', 'I'}, {'G', 'F'}, {'E', 'C', 'B'}},
            {{'A', 'H'}, {'D', 'I'}, {'G', 'F'}, {'E', 'C', 'B'}},
            {{'C', 'H'}, {'E', 'I'}, {'G', 'B'}, {'A', 'D', 'F'}},
            {{'C', 'H'}, {'E', 'I'}, {'G', 'B'}, {'A', 'D', 'F'}},
            {{'C', 'D'}, {'E', 'F'}, {'A', 'B'}, {'G', 'H', 'I'}},
            {{'C', 'D'}, {'E', 'F'}, {'A', 'B'}, {'G', 'H', 'I'}},
            {{'D', 'I'}, {'G', 'F'}, {'A', 'H'}, {'E', 'C', 'B'}},
            {{'D', 'I'}, {'G', 'F'}, {'A', 'H'}, {'E', 'C', 'B'}},
            {{'E', 'I'}, {'G', 'B'}, {'C', 'H'}, {'A', 'D', 'F'}},
            {{'E', 'I'}, {'G', 'B'}, {'C', 'H'}, {'A', 'D', 'F'}},
            {{'E', 'F'}, {'A', 'B'}, {'C', 'D'}, {'G', 'H', 'I'}},
            {{'E', 'F'}, {'A', 'B'}, {'C', 'D'}, {'G', 'H', 'I'}},
            {{'G', 'F'}, {'A', 'H'}, {'D', 'I'}, {'E', 'C', 'B'}},
            {{'G', 'F'}, {'A', 'H'}, {'D', 'I'}, {'E', 'C', 'B'}},
            {{'G', 'B'}, {'C', 'H'}, {'E', 'I'}, {'A', 'D', 'F'}},
            {{'G', 'B'}, {'C', 'H'}, {'E', 'I'}, {'A', 'D', 'F'}}
    };

    // Numero di giorni nella ripetizione del ciclo
    private static final int CYCLE_LENGTH = SCHEME.length;

    // Data di riferimento per l'inizio dello schema
    private static LocalDate referenceStartDate = LocalDate.of(2018, 11, 7);

    // Non permettere l'istanziazione
    private SchemeManager() {}

    /**
     * Imposta la data di riferimento per l'inizio dello schema.
     *
     * @param date Data di inizio dello schema
     */
    public static void setReferenceStartDate(LocalDate date) {
        if (date != null) {
            referenceStartDate = date;
        }
    }

    /**
     * Ottiene la data di riferimento per l'inizio dello schema.
     *
     * @return Data di inizio dello schema
     */
    public static LocalDate getReferenceStartDate() {
        return referenceStartDate;
    }

    /**
     * Genera i giorni base del ciclo dello schema.
     * Questi giorni servono come template per generare i giorni effettivi.
     *
     * @param shiftTypes Lista dei tipi di turno
     * @return Lista dei giorni dello schema
     */
    public static List<Day> generateCycleDays(List<ShiftType> shiftTypes) {
        List<Day> schemeDays = new ArrayList<>(CYCLE_LENGTH);

        try {
            // Genera i giorni del ciclo
            for (int dayIndex = 0; dayIndex < CYCLE_LENGTH; dayIndex++) {
                // Crea un giorno di riferimento
                LocalDate dayDate = referenceStartDate.plusDays(dayIndex);
                Day day = new Day(dayDate);

                // Configura i turni secondo lo schema
                for (int shiftIndex = 0; shiftIndex < NUMERO_TURNI_AL_GIORNO; shiftIndex++) {
                    // Crea un nuovo turno
                    Shift shift = new Shift(shiftTypes.get(shiftIndex));

                    // Aggiunge le squadre secondo lo schema
                    for (char teamChar : SCHEME[dayIndex][shiftIndex]) {
                        HalfTeam team = HalfTeamFactory.getByChar(teamChar);
                        shift.addTeam(team);
                    }

                    // Aggiunge il turno al giorno
                    day.addShift(shift);
                }

                // Aggiunge il giorno alla lista
                schemeDays.add(day);
            }
        } catch (Exception e) {
            Log.e(TAG, "Errore durante la generazione dei giorni del ciclo: " + e.getMessage());
        }

        return schemeDays;
    }

    /**
     * Genera i giorni per un mese specifico applicando lo schema a rotazione.
     *
     * @param targetDate Data del mese di destinazione
     * @param cycleDays Giorni template del ciclo
     * @return Lista dei giorni configurati per il mese
     */
    public static List<Day> generateDaysForMonth(LocalDate targetDate, List<Day> cycleDays) {
        List<Day> monthDays = new ArrayList<>();

        if (targetDate == null || cycleDays == null || cycleDays.isEmpty()) {
            return monthDays;
        }

        try {
            // Imposta la data al primo giorno del mese
            LocalDate firstDayOfMonth = LocalDate.of(targetDate.getYear(), targetDate.getMonth(), 1);

            // Calcola i giorni tra la data di riferimento e il primo giorno del mese
            int daysBetween = (int) Period.between(referenceStartDate, firstDayOfMonth).toTotalMonths() * 30 +
                    Period.between(referenceStartDate, firstDayOfMonth).getDays();

            // Calcola l'indice di partenza nel ciclo
            int startIndex = Math.floorMod(daysBetween, CYCLE_LENGTH);

            // Ottiene il numero di giorni nel mese
            int daysInMonth = targetDate.lengthOfMonth();

            // Genera i giorni del mese
            LocalDate currentDate = firstDayOfMonth;

            for (int i = 0; i < daysInMonth; i++) {
                int cycleIndex = (startIndex + i) % CYCLE_LENGTH;

                // Clona il giorno dal ciclo
                Day day = cycleDays.get(cycleIndex).clone();

                // Imposta la data corretta
                day.setLocalDate(currentDate);

                // Aggiunge il giorno alla lista
                monthDays.add(day);

                // Passa al giorno successivo
                currentDate = currentDate.plusDays(1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Errore durante la generazione dei giorni del mese: " + e.getMessage());
        }

        return monthDays;
    }

    /**
     * Verifica se un giorno ha una squadra specifica in turno.
     *
     * @param day Giorno da verificare
     * @param team Squadra da cercare
     * @return Indice del turno (0-based) se la squadra è in turno, -1 altrimenti
     */
    public static int findTeamShiftIndex(Day day, HalfTeam team) {
        if (day == null || team == null) {
            return -1;
        }

        List<Shift> shifts = day.getShifts();
        if (shifts == null || shifts.isEmpty()) {
            return -1;
        }

        for (int i = 0; i < shifts.size(); i++) {
            if (shifts.get(i).containsHalfTeam(team)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Trova la prossima data in cui una squadra è in turno, a partire da una data specificata.
     *
     * @param startDate Data di inizio della ricerca
     * @param team Squadra da cercare
     * @param maxDaysToCheck Numero massimo di giorni da controllare
     * @return Data del prossimo turno, o null se non trovato entro il limite
     */
    public static LocalDate findNextShiftDate(LocalDate startDate, HalfTeam team, int maxDaysToCheck) {
        if (startDate == null || team == null || maxDaysToCheck <= 0) {
            return null;
        }

        try {
            // Lista dei giorni template del ciclo
            List<ShiftType> shiftTypes = new ArrayList<>();
            for (int i = 0; i < NUMERO_TURNI_AL_GIORNO; i++) {
                shiftTypes.add(ShiftTypeFactory.createCustom(String.valueOf(i + 1),
                        "Turno " + (i + 1), 5 + i*8, 0, 8, 0));
            }

            List<Day> cycleDays = generateCycleDays(shiftTypes);

            // Cerca nei giorni futuri
            LocalDate currentDate = startDate;

            for (int i = 0; i < maxDaysToCheck; i++) {
                // Calcola i giorni tra la data di riferimento e la data corrente
                int daysBetween = (int) Period.between(referenceStartDate, currentDate).toTotalMonths() * 30 +
                        Period.between(referenceStartDate, currentDate).getDays();

                // Calcola l'indice nel ciclo
                int cycleIndex = Math.floorMod(daysBetween, CYCLE_LENGTH);

                // Verifica se la squadra è in turno in questo giorno
                Day day = cycleDays.get(cycleIndex);
                if (findTeamShiftIndex(day, team) >= 0) {
                    return currentDate;
                }

                // Passa al giorno successivo
                currentDate = currentDate.plusDays(1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Errore durante la ricerca del prossimo turno: " + e.getMessage());
        }

        return null; // Non trovato entro il limite
    }

    /**
     * Ottiene tutte le squadre che lavorano in una data specifica.
     *
     * @param date Data di interesse
     * @return Lista delle squadre in turno
     */
    public static List<HalfTeam> getTeamsWorkingOnDate(LocalDate date) {
        List<HalfTeam> workingTeams = new ArrayList<>();

        if (date == null) {
            return workingTeams;
        }

        try {
            // Calcola i giorni tra la data di riferimento e la data specificata
            int daysBetween = (int) Period.between(referenceStartDate, date).toTotalMonths() * 30 +
                    Period.between(referenceStartDate, date).getDays();

            // Calcola l'indice nel ciclo
            int cycleIndex = Math.floorMod(daysBetween, CYCLE_LENGTH);

            // Ottiene le squadre in turno per ogni tipo di turno
            for (int shiftIndex = 0; shiftIndex < NUMERO_TURNI_AL_GIORNO; shiftIndex++) {
                char[] teamChars = SCHEME[cycleIndex][shiftIndex];
                for (char teamChar : teamChars) {
                    HalfTeam team = HalfTeamFactory.getByChar(teamChar);
                    workingTeams.add(team);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Errore durante il recupero delle squadre in turno: " + e.getMessage());
        }

        return workingTeams;
    }

    /**
     * Ottiene tutte le squadre che sono a riposo in una data specifica.
     *
     * @param date Data di interesse
     * @return Lista delle squadre a riposo
     */
    public static List<HalfTeam> getTeamsOffWorkOnDate(LocalDate date) {
        // Lista di tutte le squadre
        List<HalfTeam> allTeams = HalfTeamFactory.getAllTeams();

        // Lista delle squadre in turno
        List<HalfTeam> workingTeams = getTeamsWorkingOnDate(date);

        // Crea una copia di tutte le squadre
        List<HalfTeam> offWorkTeams = new ArrayList<>(allTeams);

        // Rimuove le squadre in turno
        offWorkTeams.removeAll(workingTeams);

        return offWorkTeams;
    }
}
