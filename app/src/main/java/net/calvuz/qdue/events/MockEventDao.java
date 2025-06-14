package net.calvuz.qdue.events;

import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator; // Rimuovi questo se non più usato altrove per remove()
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// ... (altre parti della tua classe)

public class MockEventDao {

    private static final String TAG = "MockEventDao";
    private static final List<LocalEvent> sInMemoryEvents = new CopyOnWriteArrayList<>();

    // ... (altri metodi come insertEvent, getAllEvents, ecc.)

    /**
     * Remove event by ID (helper method) - CORRETTO
     * Questo metodo ora raccoglie gli eventi da rimuovere e poi li rimuove
     * per evitare UnsupportedOperationException con l'iteratore di CopyOnWriteArrayList.
     */
    private void removeEventById(String id) {
        if (id == null) {
            Log.w(TAG, "Trying to remove event with null ID");
            return;
        }

        List<LocalEvent> eventsToRemove = new ArrayList<>();
        for (LocalEvent event : sInMemoryEvents) {
            if (id.equals(event.getId())) {
                eventsToRemove.add(event);
                // Non c'è bisogno di un break qui se gli ID potrebbero non essere unici
                // e vuoi rimuovere tutte le corrispondenze.
                // Se l'ID è garantito essere unico, puoi aggiungere 'break;'
                // per una leggera ottimizzazione.
            }
        }

        if (!eventsToRemove.isEmpty()) {
            sInMemoryEvents.removeAll(eventsToRemove);
            Log.d(TAG, "Removed " + eventsToRemove.size() + " event(s) with ID: " + id);
        } else {
            Log.d(TAG, "No event found with ID to remove: " + id);
        }
    }

    /**
     * Delete events by package ID
     * Anche questo metodo dovrebbe essere aggiornato se usava iterator.remove()
     * sulla CopyOnWriteArrayList direttamente.
     */
    public void deleteEventsByPackageId(String packageId) {
        Log.d(TAG, "deleteEventsByPackageId: " + packageId);

        List<LocalEvent> eventsToRemove = new ArrayList<>();
        for (LocalEvent event : sInMemoryEvents) {
            if (packageId.equals(event.getPackageId())) {
                eventsToRemove.add(event);
            }
        }

        if (!eventsToRemove.isEmpty()) {
            sInMemoryEvents.removeAll(eventsToRemove);
            Log.d(TAG, "Deleted " + eventsToRemove.size() + " events for package: " + packageId);
        } else {
            Log.d(TAG, "No events found for package to delete: " + packageId);
        }
    }


    // ... (il resto della tua classe MockEventDao)
    // Assicurati che anche altri metodi che modificano sInMemoryEvents
    // durante l'iterazione siano gestiti correttamente.
    // Ad esempio, il tuo `insertEvent` ha già una logica che chiama
    // `removeEventById` prima, quindi se `removeEventById` è corretto,
    // `insertEvent` dovrebbe funzionare bene. Lo stesso per `updateEvent`.

    /**
     * Insert new event
     */
    public void insertEvent(LocalEvent event) {
        if (event == null) {
            Log.w(TAG, "Trying to insert null event");
            return;
        }

        // Check for duplicate IDs and replace if found
        // Questa chiamata ora userà la versione corretta di removeEventById
        removeEventById(event.getId());

        sInMemoryEvents.add(event);
        Log.d(TAG, "insertEvent: " + event.getTitle() + " (ID: " + event.getId() + ")");
    }

    /**
     * Update event (replace existing with same ID)
     */
    public void updateEvent(LocalEvent event) {
        if (event == null) {
            Log.w(TAG, "Trying to update null event");
            return;
        }
        // Questa chiamata ora userà la versione corretta di removeEventById
        removeEventById(event.getId());
        sInMemoryEvents.add(event); // Poi aggiunge la versione aggiornata
        Log.d(TAG, "updateEvent: " + event.getTitle() + " (ID: " + event.getId() + ")");
    }

    // ... (altri metodi: deleteAllLocalEvents, getEventsForDate, ecc. non dovrebbero essere affetti
    //      se non usano iterator.remove() su sInMemoryEvents)


    /**
     * Get events for a specific date
     */
    public List<LocalEvent> getEventsForDate(LocalDate date) {
        List<LocalEvent> eventsForDate = new ArrayList<>();

        for (LocalEvent event : sInMemoryEvents) {
            if (event.getDate().equals(date)) {
                eventsForDate.add(event);
            }
        }

        Log.d(TAG, "getEventsForDate " + date + ": found " + eventsForDate.size() + " events");
        return eventsForDate;
    }

    /**
     * Get events for date range
     */
    public List<LocalEvent> getEventsForDateRange(LocalDate startDate, LocalDate endDate) {
        List<LocalEvent> eventsInRange = new ArrayList<>();

        for (LocalEvent event : sInMemoryEvents) {
            LocalDate eventDate = event.getDate();
            if (!eventDate.isBefore(startDate) && !eventDate.isAfter(endDate)) {
                eventsInRange.add(event);
            }
        }

        Log.d(TAG, "getEventsForDateRange " + startDate + " to " + endDate +
                ": found " + eventsInRange.size() + " events");
        return eventsInRange;
    }

    /**
     * Get all events (useful for debugging and testing)
     */
    public List<LocalEvent> getAllEvents() {
        Log.d(TAG, "getAllEvents: total " + sInMemoryEvents.size() + " events");
        return new ArrayList<>(sInMemoryEvents); // Restituisce una copia per evitare modifiche esterne dirette
    }

    /**
     * Get event by ID
     */
    public LocalEvent getEventById(String id) {
        for (LocalEvent event : sInMemoryEvents) {
            if (id.equals(event.getId())) {
                return event;
            }
        }
        return null;
    }

    /**
     * MOCK: Check if event exists by ID
     */
    public boolean eventExists(String id) {
        return getEventById(id) != null;
    }

    /**
     * Delete all local events
     */
    public void deleteAllLocalEvents() {
        int count = sInMemoryEvents.size();
        sInMemoryEvents.clear(); // .clear() è sicuro da usare su CopyOnWriteArrayList
        Log.d(TAG, "deleteAllLocalEvents: removed " + count + " events");
    }

    /**
     * Get statistics for debugging
     */
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("MockEventDao Statistics:\n");
        info.append("Total events: ").append(sInMemoryEvents.size()).append("\n");

        // Count by package
        java.util.Map<String, Integer> packageCounts = new java.util.HashMap<>();
        for (LocalEvent event : sInMemoryEvents) {
            String packageId = event.getPackageId();
            if (packageId == null) packageId = "local"; // Gestisce packageId nulli
            packageCounts.put(packageId, packageCounts.getOrDefault(packageId, 0) + 1);
        }

        info.append("By package:\n");
        for (java.util.Map.Entry<String, Integer> entry : packageCounts.entrySet()) {
            info.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        return info.toString();
    }
}