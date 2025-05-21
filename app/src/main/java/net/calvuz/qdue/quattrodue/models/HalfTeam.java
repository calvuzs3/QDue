package net.calvuz.qdue.quattrodue.models;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Rappresenta una squadra (o mezza squadra) dei lavoratori.
 * Ogni HalfTeam è identificato da un nome univoco (solitamente un singolo carattere).
 *
 * @author Luke (originale)
 * @author Aggiornato 21/05/2025
 */
public class HalfTeam implements Cloneable, Comparable<HalfTeam> {

    /* TAG */
    public static final String TAG = HalfTeam.class.getSimpleName();

    /* Proprietà della mezza squadra */
    private final String name;
    private final String description;
    private final List<String> members;

    /**
     * Crea una nuova mezza squadra con il nome specificato (singolo carattere).
     *
     * @param c Carattere identificativo della squadra
     */
    public HalfTeam(char c) {
        this(String.valueOf(c));
    }

    /**
     * Crea una nuova mezza squadra con il nome specificato.
     *
     * @param name Nome della squadra
     */
    public HalfTeam(String name) {
        this(name, "");
    }

    /**
     * Crea una nuova mezza squadra con nome e descrizione.
     *
     * @param name Nome della squadra
     * @param description Descrizione della squadra
     */
    public HalfTeam(String name, String description) {
        this.name = name;
        this.description = description;
        this.members = new ArrayList<>();
    }

    /**
     * Costruttore completo per creare una mezza squadra con membri.
     *
     * @param name Nome della squadra
     * @param description Descrizione della squadra
     * @param members Lista dei membri della squadra
     */
    public HalfTeam(String name, String description, List<String> members) {
        this.name = name;
        this.description = description;
        this.members = new ArrayList<>(members);
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
     * Restituisce la descrizione della squadra.
     *
     * @return Descrizione della squadra
     */
    public String getDescription() {
        return description;
    }

    /**
     * Restituisce la lista dei membri della squadra.
     *
     * @return Lista immutabile dei membri
     */
    public List<String> getMembers() {
        return Collections.unmodifiableList(members);
    }

    /**
     * Aggiunge un membro alla squadra.
     *
     * @param member Nome del membro da aggiungere
     * @return true se il membro è stato aggiunto, false altrimenti
     */
    public boolean addMember(String member) {
        if (member == null || member.trim().isEmpty()) {
            return false;
        }

        return members.add(member.trim());
    }

    /**
     * Rimuove un membro dalla squadra.
     *
     * @param member Nome del membro da rimuovere
     * @return true se il membro è stato rimosso, false altrimenti
     */
    public boolean removeMember(String member) {
        return members.remove(member);
    }

    /**
     * Restituisce il numero di membri nella squadra.
     *
     * @return Numero di membri
     */
    public int getMemberCount() {
        return members.size();
    }

    /**
     * Verifica se la squadra ha membri.
     *
     * @return true se la squadra ha membri, false altrimenti
     */
    public boolean hasMembers() {
        return !members.isEmpty();
    }

    /**
     * Verifica se questa squadra è uguale a un'altra squadra.
     * Il confronto è basato solo sul nome.
     *
     * @param other Altra squadra da confrontare
     * @return true se le squadre hanno lo stesso nome, false altrimenti
     */
    public boolean equals(HalfTeam other) {
        if (other == null) return false;
        return Objects.equals(other.getName(), this.name);
    }

    /**
     * Verifica se questa squadra corrisponde a un'altra per nome.
     * Metodo di utilità compatibile con la versione originale.
     *
     * @param other Altra squadra da confrontare
     * @return true se le squadre hanno lo stesso nome, false altrimenti
     */
    public boolean isSameTeam(HalfTeam other) {
        if (other == null) return false;
        return Objects.equals(other.getName(), this.name);
    }

    @NonNull
    @Override
    public String toString() {
        return TAG + "{" + this.name + (hasMembers() ? ", membri: " + members.size() : "") + "}";
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
     *         valore positivo se questa squadra segue l'altra,
     *         zero se le squadre sono uguali
     */
    @Override
    public int compareTo(HalfTeam other) {
        return this.name.compareTo(other.name);
    }

    /**
     * Builder per creare istanze di HalfTeam in modo fluente.
     */
    public static class Builder {
        private String name;
        private String description = "";
        private final List<String> members = new ArrayList<>();

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withName(char c) {
            this.name = String.valueOf(c);
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder addMember(String member) {
            if (member != null && !member.trim().isEmpty()) {
                this.members.add(member.trim());
            }
            return this;
        }

        public Builder addMembers(List<String> members) {
            if (members != null) {
                members.forEach(member -> {
                    if (member != null && !member.trim().isEmpty()) {
                        this.members.add(member.trim());
                    }
                });
            }
            return this;
        }

        public HalfTeam build() {
            return new HalfTeam(name, description, members);
        }
    }
}
