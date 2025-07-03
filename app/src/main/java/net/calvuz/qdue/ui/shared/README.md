# Architettura Sistema Click Eventi - QDue

## Overview
Il sistema di click per la visualizzazione degli eventi implementa un'architettura modulare che differenzia il comportamento tra Calendar View e DaysList View.

## 1. Gerarchia delle Classi Base

### BaseFragmentLegacy
- **Ruolo**: Gestione base degli eventi, cache e lifecycle
- **Responsabilità**:
    - Gestione cache eventi (`mEventsCache`)
    - Interfaccia `EventsRefreshInterface`
    - Metodi `getEventsForDate()`, `refreshEventsData()`

### BaseClickFragmentLegacy extends BaseFragmentLegacy
- **Ruolo**: Gestione click e selection mode
- **Responsabilità**:
    - Implementa `DayLongClickListener`
    - Gestione selection mode e long-click
    - **Eventi Preview Manager** integrato (`mEventsPreviewManager`)
    - Metodi astratti per subclassi:
      ```java
      protected abstract BaseClickAdapterLegacy getClickAdapter();
      protected abstract EventsPreviewManager.ViewType getEventsPreviewViewType();
      ```

## 2. Implementazioni Fragment Specifiche

### CalendarViewFragmentLegacy extends BaseClickFragmentLegacy
```java
@Override
protected EventsPreviewManager.ViewType getEventsPreviewViewType() {
    return EventsPreviewManager.ViewType.CALENDAR_VIEW;
}

@Override
protected BaseClickAdapterLegacy getClickAdapter() {
    return mLegacyAdapter; // CalendarAdapterLegacy
}
```

### DayslistViewFragmentLegacy extends BaseClickFragmentLegacy
```java
@Override
protected EventsPreviewManager.ViewType getEventsPreviewViewType() {
    return EventsPreviewManager.ViewType.DAYS_LIST;
}

@Override
protected BaseClickAdapterLegacy getClickAdapter() {
    return mLegacyAdapter; // DaysListAdapterLegacy
}
```

## 3. Gerarchia Adapter

### BaseAdapterLegacy
- **Ruolo**: Funzionalità base condivise
- **Responsabilità**:
    - Binding dati comuni
    - ViewHolder management

### BaseClickAdapterLegacy extends BaseAdapterLegacy
- **Ruolo**: Sistema click unificato
- **Responsabilità**:
    - Implementa `DayLongClickListener`
    - Gestione selection mode
    - **Interface LongClickCapable** per ViewHolder
    - **Interface DayRegularClickListener** per click normali

### CalendarAdapterLegacy extends BaseClickAdapterLegacy
```java
public class CalendarDayViewHolder extends BaseMaterialDayViewHolder 
    implements LongClickCapable {
    // Layout specifico calendario (48x48dp)
    // Eventi indicator compatto
}
```

### DaysListAdapterLegacy extends BaseClickAdapterLegacy
```java
public class DayslistDayViewHolder extends BaseMaterialDayViewHolder 
    implements LongClickCapable {
    // Layout specifico lista (56dp altezza)
    // Eventi indicator integrato
}
```

## 4. Sistema Events Preview

### EventsPreviewManager
- **Ruolo**: Router che delega alla implementazione corretta
- **Strategia**: Factory pattern basato su ViewType

```java
public enum ViewType {
    DAYS_LIST, CALENDAR_VIEW
}

// Delegation logic
switch (viewType) {
    case DAYS_LIST:
        mCurrentImplementation = new DaysListEventsPreview(mContext);
        break;
    case CALENDAR_VIEW:
        mCurrentImplementation = new CalendarEventsPreview(mContext);
        break;
}
```

### BaseEventsPreview
- **Ruolo**: Logica comune per entrambe le implementazioni
- **Metodi astratti**:
  ```java
  protected abstract void showEventsPreviewImpl(LocalDate date, List<LocalEvent> events, View anchorView);
  protected abstract void hideEventsPreviewImpl();
  protected abstract String getViewType();
  ```

### CalendarEventsPreview -> CalendarEventsBottomSheet
- **Implementazione**: Bottom Sheet Dialog
- **Motivo**: Celle calendario piccole (48x48dp), meglio modal overlay
- **Features**:
    - Header formattato con data
    - RecyclerView con EventsAdapter
    - Pulsanti azioni (Add Event, Navigate)
    - Gestione empty state

### DaysListEventsPreview
- **Implementazione**: PLACEHOLDER per Phase 3
- **Piano Futuro**: Espansione in-place della MaterialCardView
- **Motivo**: Righe lista più alte (56dp), spazio per espansione
- **Current**: Toast placeholder

## 5. Flusso di Click Events

### Click Normale (Eventi Preview)
```
User Click Day
    ↓
Fragment.handleDayRegularClick()
    ↓
mEventsPreviewManager.showEventsPreview()
    ↓
EventsPreviewManager routes per ViewType
    ↓
CalendarEventsBottomSheet.show() OR DaysListEventsPreview.show()
```

### Long Click (Selection Mode)
```
User Long Click Day
    ↓
BaseClickAdapterLegacy.onDayLongClick()
    ↓
FloatingDayToolbar.show()
    ↓
Selection Mode attivato
    ↓
Quick Actions disponibili (Ferie, Malattia, etc.)
```

## 6. ViewHolder Click Integration

### BaseMaterialDayViewHolder implements LongClickCapable
```java
// Setup in bindDayData()
private void setupClickListener() {
    itemView.setOnClickListener(v -> {
        if (mIsSelectionMode) {
            handleSelectionModeClick(); // Toggle selection
        } else {
            handleRegularModeClick(); // Show events preview
        }
    });
}

private void handleRegularModeClick() {
    if (mRegularClickListener != null && mCurrentDay != null) {
        mRegularClickListener.onDayRegularClick(mCurrentDay, mCurrentDate, itemView, mCurrentPosition);
    }
}
```

## 7. Eventi Actions

### EventsPreviewInterface.EventQuickAction
```java
public enum EventQuickAction {
    EDIT,           // Apri editor evento
    DELETE,         // Elimina evento  
    DUPLICATE,      // Duplica evento
    TOGGLE_COMPLETE // Toggle completamento
}
```

### EventsPreviewInterface.EventGeneralAction
```java
public enum EventGeneralAction {
    ADD_EVENT,                      // Aggiungi nuovo evento
    NAVIGATE_TO_EVENTS_ACTIVITY,    // Vai a EventsActivity
    REFRESH_EVENTS                  // Refresh dati eventi
}
```

## 8. Integrazione Eventi Data

### BaseFragmentLegacy - Eventi Cache
```java
// Cache eventi thread-safe
protected Map<LocalDate, List<LocalEvent>> mEventsCache = new ConcurrentHashMap<>();

// Loading eventi per periodo
private void loadEventsForCurrentPeriod() {
    // Async loading da database
    // Espansione eventi multi-day
    // Update cache + UI
}

// Espansione eventi multi-day
private Map<LocalDate, List<LocalEvent>> groupEventsByDate(List<LocalEvent> events) {
    // Eventi single-day: aggiungi alla data start
    // Eventi multi-day: aggiungi a tutte le date nel range
}
```

### Adapter Integration
```java
// In CalendarAdapterLegacy e DaysListAdapterLegacy
private void setupEventsIndicator(ViewHolder holder, DayItem dayItem) {
    List<LocalEvent> events = getEventsForDate(date);
    
    if (!events.isEmpty()) {
        // Mostra badge con count
        // Applica colore priorità
        int priorityColor = mEventHelper.getHighestPriorityColor(events);
        holder.eventsIndicator.getBackground().setTint(priorityColor);
    }
}
```

## 9. Thread Safety & Performance

### Main Thread Operations
- UI updates sempre su main thread
- Cache updates con `mMainHandler.post()`
- Eventi loading asincrono con `CompletableFuture`

### Memory Management
- Eventi cache con limite dimensioni
- Cleanup automatico eventi vecchi
- ViewHolder recycling ottimizzato

## 10. Testing & Debug

### Debug Methods
```java
// In ogni componente principale
public void debugState() {
    // Log stato corrente
    // Verifica listener setup
    // Check cache consistency
}

// Testing integration
public void debugEventsIntegration() {
    // Test complete flow
    // Verify click handling
    // Check preview show/hide
}
```

Questa architettura garantisce:
- **Separazione responsabilità** tra Calendar e DaysList
- **Riuso codice** attraverso classi base
- **Flessibilità** per future implementazioni
- **Performance** con cache ottimizzate
- **User Experience** differenziata per ogni view type