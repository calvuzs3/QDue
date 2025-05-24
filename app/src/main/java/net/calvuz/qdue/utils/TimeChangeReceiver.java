package net.calvuz.qdue.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import net.calvuz.qdue.quattrodue.QuattroDue;

/**
 * BroadcastReceiver per rilevare i cambiamenti dell'ora di sistema.
 * Aggiorna automaticamente l'applicazione quando l'ora del sistema cambia.
 */
public class TimeChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "TimeChangeReceiver";
    private static final boolean LOG_ENABLED = true;

    public interface TimeChangeListener {
        void onTimeChanged();
        void onDateChanged();
        void onTimezoneChanged();
    }

    private TimeChangeListener listener;

    public TimeChangeReceiver() {
        // Costruttore vuoto per la registrazione nel manifest
    }

    public TimeChangeReceiver(TimeChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        if (LOG_ENABLED) {
            Log.d(TAG, "Ricevuto broadcast: " + action);
        }

        switch (action) {
            case Intent.ACTION_TIME_CHANGED:
                handleTimeChanged(context);
                break;
            case Intent.ACTION_DATE_CHANGED:
                handleDateChanged(context);
                break;
            case Intent.ACTION_TIMEZONE_CHANGED:
                handleTimezoneChanged(context);
                break;
            case Intent.ACTION_TIME_TICK:
                // Chiamato ogni minuto - uso per verifiche periodiche
                handleTimeTick(context);
                break;
        }
    }

    /**
     * Gestisce il cambiamento dell'ora di sistema.
     */
    private void handleTimeChanged(Context context) {
        if (LOG_ENABLED) {
            Log.d(TAG, "Ora di sistema cambiata");
        }

        // Aggiorna QuattroDue
        updateQuattroDue(context);

        // Notifica il listener
        if (listener != null) {
            listener.onTimeChanged();
        }
    }

    /**
     * Gestisce il cambiamento della data di sistema.
     */
    private void handleDateChanged(Context context) {
        if (LOG_ENABLED) {
            Log.d(TAG, "Data di sistema cambiata");
        }

        // Aggiorna QuattroDue
        updateQuattroDue(context);

        // Notifica il listener
        if (listener != null) {
            listener.onDateChanged();
        }
    }

    /**
     * Gestisce il cambiamento del fuso orario.
     */
    private void handleTimezoneChanged(Context context) {
        if (LOG_ENABLED) {
            Log.d(TAG, "Fuso orario cambiato");
        }

        // Aggiorna QuattroDue
        updateQuattroDue(context);

        // Notifica il listener
        if (listener != null) {
            listener.onTimezoneChanged();
        }
    }

    /**
     * Gestisce il tick del tempo (chiamato ogni minuto).
     * Utile per verifiche periodiche del cambio giorno.
     */
    private void handleTimeTick(Context context) {
        // Verifica se è cambiato il giorno
        // Questa verifica è meno invasiva del controllare ogni secondo
        java.time.LocalDate today = java.time.LocalDate.now();

        // Qui potresti salvare l'ultima data conosciuta e confrontarla
        // Per semplicità, delego questa logica a QuattroDue
        QuattroDue qd = QuattroDue.getInstance(context);
        if (qd != null) {
            // Forza un aggiornamento leggero
            qd.setRefresh(true);
        }
    }

    /**
     * Aggiorna i dati di QuattroDue dopo un cambio di tempo.
     */
    private void updateQuattroDue(Context context) {
        try {
            QuattroDue qd = QuattroDue.getInstance(context);
            if (qd != null) {
                // Forza un refresh completo
                qd.refresh(context);
                if (LOG_ENABLED) {
                    Log.d(TAG, "QuattroDue aggiornato dopo cambio tempo");
                }
            }
        } catch (Exception e) {
            if (LOG_ENABLED) {
                Log.e(TAG, "Errore durante l'aggiornamento di QuattroDue: " + e.getMessage());
            }
        }
    }

    /**
     * Crea un IntentFilter per registrare tutti gli eventi di tempo.
     */
    public static IntentFilter createIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);
        return filter;
    }

    /**
     * Crea un IntentFilter solo per gli eventi critici (senza TIME_TICK).
     */
    public static IntentFilter createCriticalIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        return filter;
    }
}