package net.calvuz.qdue.quattrodue.utils;

import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.quattrodue.models.ShiftType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory per creare turni (Shift)
 */
public final class ShiftFactory {

    // TAG
    private static final String TAG = ShiftFactory.class.getSimpleName();

    // Non permettere istanziazione
    private ShiftFactory() {}

    /**
     * Crea un turno standard (mattino, pomeriggio o notte) per una data specifica.
     *
     * @param type Tipo di turno (1=mattino, 2=pomeriggio, 3=notte)
     * @param date Data del turno
     * @return Nuovo Shift
     */
    public static Shift createStandardShift(int type, LocalDate date) {
        ShiftType shiftType;

        switch (type) {
            case 1:
                shiftType = ShiftTypeFactory.MORNING;
                break;
            case 2:
                shiftType = ShiftTypeFactory.AFTERNOON;
                break;
            case 3:
                shiftType = ShiftTypeFactory.NIGHT;
                break;
            default:
                throw new IllegalArgumentException("Tipo di turno non valido: " + type);
        }

        return new Shift(shiftType, date);
    }

    /**
     * Crea tutti e tre i turni standard per una data specifica.
     *
     * @param date Data dei turni
     * @return Lista di 3 turni (mattino, pomeriggio, notte)
     */
    public static List<Shift> createDailyShifts(LocalDate date) {
        List<Shift> shifts = new ArrayList<>(3);
        shifts.add(createStandardShift(1, date));
        shifts.add(createStandardShift(2, date));
        shifts.add(createStandardShift(3, date));
        return shifts;
    }

    /**
     * Crea un turno personalizzato.
     *
     * @param shiftType Tipo di turno
     * @param date Data del turno
     * @param isStop Indica se il turno è in fermata
     * @param teams Squadre assegnate al turno
     * @return Nuovo Shift
     */
    public static Shift createCustomShift(ShiftType shiftType, LocalDate date,
                                          boolean isStop, List<HalfTeam> teams) {
        Shift shift = new Shift(shiftType, date);
        shift.setStop(isStop);

        if (teams != null) {
            for (HalfTeam team : teams) {
                shift.addTeam(team);
            }
        }

        return shift;
    }
}
