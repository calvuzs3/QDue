package net.calvuz.qdue.quattrodue.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.calvuz.qdue.quattrodue.models.HalfTeam;

/**
 * Factory per creare e gestire le squadre (HalfTeam).
 * Implementa un sistema di cache per le squadre predefinite.
 */
public final class HalfTeamFactory {

    private static final String TAG = "HalfTeamFactory";

    // Cache delle squadre per nome
    private static final Map<String, HalfTeam> teamCache = new HashMap<>();

    // Non permettere l'istanziazione
    private HalfTeamFactory() {}

    // Inizializzazione statica delle squadre predefinite
    static {
        // Inizializza le squadre di base da A a I
        for (char c = 'A'; c <= 'I'; c++) {
            String name = String.valueOf(c);
            teamCache.put(name, new HalfTeam(name));
        }
    }

    /**
     * Ottiene una squadra dal nome. Se la squadra esiste nella cache,
     * restituisce l'istanza esistente, altrimenti ne crea una nuova.
     *
     * @param name Nome della squadra
     * @return HalfTeam corrispondente, o null se il nome è null
     */
    public static HalfTeam getByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        // Converti il nome in maiuscolo (per garantire coerenza)
        String upperName = name.toUpperCase();

        // Cerca nella cache
        HalfTeam team = teamCache.get(upperName);

        // Se non trovato, crea una nuova istanza e aggiungila alla cache
        if (team == null) {
            team = new HalfTeam(upperName);
            teamCache.put(upperName, team);
        }

        return team;
    }

    /**
     * Ottiene una squadra dal carattere.
     *
     * @param c Carattere identificativo della squadra
     * @return HalfTeam corrispondente
     */
    public static HalfTeam getByChar(char c) {
        return getByName(String.valueOf(c));
    }

    /**
     * Crea una nuova squadra personalizzata.
     * Questa squadra NON viene aggiunta alla cache.
     *
     * @param name Nome della squadra
     * @param description Descrizione della squadra
     * @return Nuova HalfTeam
     */
    public static HalfTeam createCustom(String name, String description) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        return new HalfTeam(name);
    }

    /**
     * Ottiene tutte le squadre predefinite (A-I).
     *
     * @return Lista delle squadre predefinite
     */
    public static List<HalfTeam> getAllTeams() {
        return new ArrayList<>(teamCache.values());
    }

    /**
     * Ottiene tutte le squadre con nomi nei caratteri specificati.
     *
     * @param names Array di nomi delle squadre
     * @return Lista delle squadre corrispondenti
     */
    public static List<HalfTeam> getTeamsByNames(String[] names) {
        List<HalfTeam> teams = new ArrayList<>();

        if (names == null || names.length == 0) {
            return teams;
        }

        for (String name : names) {
            HalfTeam team = getByName(name);
            if (team != null) {
                teams.add(team);
            }
        }

        return teams;
    }

    /**
     * Ottiene tutte le squadre con nomi nei caratteri specificati.
     *
     * @param chars Array di caratteri identificativi delle squadre
     * @return Lista delle squadre corrispondenti
     */
    public static List<HalfTeam> getTeamsByChars(char[] chars) {
        List<HalfTeam> teams = new ArrayList<>();

        if (chars == null || chars.length == 0) {
            return teams;
        }

        for (char c : chars) {
            HalfTeam team = getByChar(c);
            if (team != null) {
                teams.add(team);
            }
        }

        return teams;
    }

    /**
     * Verifica se una squadra è valida (ha un nome non nullo e ben formato).
     *
     * @param team Squadra da verificare
     * @return true se la squadra è valida, false altrimenti
     */
    public static boolean isValidTeam(HalfTeam team) {
        return team != null && team.getName() != null && !team.getName().isEmpty();
    }
}
