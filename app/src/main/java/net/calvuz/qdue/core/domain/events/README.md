# Qdue Events Structure Analysis Report

## Overview
Il sistema events di qdue implementa un'architettura flessibile per la gestione di eventi simili a 
Google Calendar, con supporto per eventi locali, importazione da fonti esterne, e integrazione futura
con Google Calendar.

## Core Architecture

### 1. CalendarEvent Interface
**File:** `CalendarEvent.java`
**Scopo:** Interfaccia principale che definisce il contratto per tutti i tipi di eventi

**Metodi principali:**
```java
String getId();
String getTitle();
String getDescription();
LocalDateTime getStartTime();
LocalDateTime getEndTime();
LocalDate getDate();
EventType getEventType();
EventPriority getPriority();
EventSource getSource();
boolean isAllDay();
String getLocation();
Map<String, String> getCustomProperties();
```

### 2. LocalEvent Implementation
**File:** `LocalEvent.java`
**Scopo:** Implementazione concreta dell'interfaccia CalendarEvent per eventi memorizzati localmente

**Caratteristiche chiave:**
- **Entity Room** con indici ottimizzati per performance
- **Supporto per package esterni** tramite `packageId`, `sourceUrl`, `packageVersion`
- **Tracking degli aggiornamenti** con `lastUpdated`
- **Metodi di utilità** per formattazione e validazione

**Indici database:**
- `start_time` - per query temporali
- `package_id` - per gestione package esterni
- `event_type` - per filtri per tipo
- `priority` - per ordinamento per priorità
- `time_range` (start_time, end_time) - per range queries

## Event Classification

### 3. EventType Enum
**File:** `EventType.java`
**Categorie principali:**

#### General Events
- `GENERAL` - Eventi generici
- `MEETING` - Riunioni
- `TRAINING` - Formazione

#### Production Events
- `MAINTENANCE` - Manutenzione
- `EMERGENCY` - Emergenza

#### Safety & Compliance
- `SAFETY_DRILL` - Prova Sicurezza
- `AUDIT` - Audit

#### Production Stops (Industrial Focus)
- `STOP_PLANNED` - Fermata Programmata
- `STOP_UNPLANNED` - Fermata Non Programmata
- `STOP_SHORTAGE` - Fermata Carenza Ordini
- `STOP_SHORTAGE_RAW_MATERIALS` - Fermata Carenza Materie Prime

#### Shift Management
- `SHIFT_CHANGE` - Cambio Turno
- `SHIFT_SWAP` - Scambio Turno con collega
- `COMPENSATION` - Compensazione
- `OVERTIME` - Straordinario

#### User Absence Events
- `VACATION` - Ferie
- `SICK_LEAVE` - Malattia
- `PERSONAL_LEAVE` - Permesso personale
- `SPECIAL_LEAVE` - Permesso da legge (es. 104)
- `SYNDICATE_LEAVE` - Permesso per sindacalista

#### Import/External
- `IMPORTED` - Eventi importati
- `OTHER` - Altri eventi

**Caratteristiche:**
- Ogni tipo ha `displayName`, `color`, `emoji`
- Metodo `isProductionStop()` per identificare fermate produttive
- Calcolo automatico del colore del testo per leggibilità

### 4. EventPriority Enum
**File:** `EventPriority.java`
**Livelli:** `LOW`, `NORMAL`, `HIGH`, `URGENT`
**Caratteristiche:** Ogni priorità ha `displayName` e `color` associato

### 5. EventSource Enum
**File:** `EventSource.java`
**Sorgenti supportate:**
- `LOCAL` - Eventi creati localmente nell'app
- `GOOGLE_CALENDAR` - Eventi da Google Calendar API
- `EXTERNAL_URL` - Eventi da package URL esterni
- `COMPANY_FEED` - Eventi da feed RSS/API aziendali

## Data Layer

### 6. EventsTypeConverters
**File:** `EventsTypeConverters.java`
**Scopo:** Conversioni Room per tipi custom

**Conversioni supportate:**
- `LocalDateTime` ↔ `String` (ISO format)
- `EventType` ↔ `String` (enum name)
- `EventPriority` ↔ `String` (enum name)
- `Map<String, String>` ↔ `JSON String` (custom properties)
- `Boolean` ↔ `Integer` (SQLite compatibility)

**Caratteristiche:**
- Gestione robusta degli errori con fallback a valori default
- Supporto per retrocompatibilità con enum deprecati

## External Integration

### 7. GoogleCalendarEventWrapper
**File:** `GoogleCalendarEventWrapper.java`
**Scopo:** Wrapper per eventi Google Calendar (implementazione futura)

**Caratteristiche:**
- Implementa `CalendarEvent` interface
- Mapping intelligente dei tipi Google → EventType locale
- Conversione automatica timezone
- Preservazione metadati Google in `customProperties`

### 8. Event Package System
**Files:** `EventPackageManagerExtension.java`, `BackupManager.java`

**Funzionalità:**
- **Import/Export** eventi in formato JSON standardizzato
- **Package management** per aggiornamenti incrementali
- **Backup automatico** eventi locali
- **Validazione strutturale** JSON con error handling robusto

**Struttura Package JSON:**
```json
{
  "package_info": {
    "id": "package_id",
    "name": "Package Name", 
    "version": "1.0.0",
    "description": "Description",
    "created_date": "2025-01-01",
    "valid_from": "2025-01-01",
    "valid_to": "2025-12-31",
    "author": "Author",
    "contact_email": "email@domain.com"
  },
  "events": [...]
}
```

## UI Integration

### 9. Event Display Helpers
**Files:** `EventIndicatorHelper.java`, `SimpleEnhancedDaysListAdapter.java`

**Caratteristiche:**
- **Sistema di scoring** per priorità e criticità eventi
- **Indicatori visivi** con colori e badge
- **Logica di dominanza** per eventi multipli nello stesso giorno
- **Accessibilità** con descrizioni leggibili

**Scoring System:**
- Eventi più critici (STOP_UNPLANNED) hanno score 10
- Eventi generali hanno score 1
- Sistema analogo per priorità (URGENT=4, LOW=1)

### 10. Event Operations Interface
**File:** `EventsEventOperationsInterface.java`
**Operazioni CRUD:**
- `triggerEventDeletion()` - Eliminazione con conferma/undo
- `triggerEventEdit()` - Modifica evento
- `triggerEventDuplicate()` - Duplicazione evento
- `triggerEventShare()` - Condivisione tramite system intent
- `triggerAddToCalendar()` - Aggiunta a calendario di sistema

## Key Design Patterns

### 1. **Interface Segregation**
- `CalendarEvent` definisce contratto comune
- Implementazioni specifiche (`LocalEvent`, `GoogleCalendarEventWrapper`)
- Interfacce dedicate per operazioni (`EventsEventOperationsInterface`)

### 2. **Strategy Pattern**
- `EventSource` enum per gestire diverse sorgenti dati
- Type converters specializzati per ogni tipo di dato

### 3. **Factory Pattern**
- `EventPackageManagerExtension` per conversioni JSON → LocalEvent
- Generator utilities per creazione automatica eventi

### 4. **Observer Pattern**
- Callback interfaces per operazioni asincrone (`ImportCallback`, `EventDeletionListener`)

