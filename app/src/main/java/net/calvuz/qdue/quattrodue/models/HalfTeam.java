package net.calvuz.qdue.quattrodue.models;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Rappresenta una squadra (o mezza squadra) dei lavoratori.
 * Ogni HalfTeam è identificato da un nome univoco (solitamente un singolo carattere).
 *
 * @author Luke (originale)
 * @author Aggiornato 21/05/2025
 */
public class HalfTeam implements Cloneable, Comparable<HalfTeam> {

    // TAG
    public static final String TAG = HalfTeam.class.getSimpleName();

    // Proprietà principali
    private final String name;

    /**
     * Crea una nuova mezza squadra con il nome specificato (singolo carattere).
     *
     * @param c Carattere identificativo della squadra
     */
    public HalfTeam(String c) {
        this.name = c;
    }

    /**
     * Restituisce il nome della squadra.
     *
     * @return Nome della squadra
     */
    public String getName() {
        return name;
    }

    /**
     * Verifica se questa squadra corrisponde a un'altra per nome.
     * Metodo di utilità compatibile con la versione originale.
     *
     * @param other Altra squadra da confrontare
     * @return true se le squadre hanno lo stesso nome, false altrimenti
     */
    public boolean isSameTeamAs(HalfTeam other) {
        if (other == null) return false;
        return Objects.equals(other.getName(), this.name);
    }

    @NonNull
    @Override
    public String toString() {
        return TAG + "{" + this.name + "}";
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        HalfTeam other = (HalfTeam) obj;
        return Objects.equals(this.name, other.name);
    }

    @Override
    public HalfTeam clone() throws CloneNotSupportedException {
        HalfTeam clone = (HalfTeam) super.clone();
        // Non è necessaria altra logica perché il nome è immutabile
        // e non conserviamo altri stati mutabili
        return clone;
    }

    /**
     * Confronta questa squadra con un'altra per l'ordinamento.
     * L'ordinamento è basato sul nome.
     *
     * @param other Altra squadra da confrontare
     * @return valore negativo se questa squadra precede l'altra,
     * valore positivo se questa squadra segue l'altra,
     * zero se le squadre sono uguali
     */
    @Override
    public int compareTo(HalfTeam other) {
        return this.name.compareTo(other.name);
    }
}
