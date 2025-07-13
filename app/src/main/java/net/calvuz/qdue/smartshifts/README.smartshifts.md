# SmartShifts - Sistema Turni Avanzato per QDue

## 1. Architettura Database

### 1.1 Schema Entità Principali

```java
// ===== ENTITÀ CORE =====

@Entity(tableName = "shift_types")
public class ShiftType {
    @PrimaryKey 
    @NonNull
    public String id;                    // "morning", "afternoon", "night", "rest"
    
    @NonNull
    public String name;                  // "Mattina", "Pomeriggio", "Notte", "Riposo"
    
    @NonNull
    public String startTime;             // "06:00" (formato HH:mm)
    
    @NonNull
    public String endTime;               // "14:00"
    
    @NonNull
    public String colorHex;              // "#4CAF50" 
    
    @NonNull
    public String iconName;              // "ic_morning", "ic_afternoon", "ic_night", "ic_rest"
    
    public boolean isWorkingShift;       // false solo per "Riposo"
    public int sortOrder;                // ordine visualizzazione
    public boolean isCustom;             // true se aggiunto dall'utente
    public boolean isActive;             // soft delete
    public long createdAt;
    public long updatedAt;
}

@Entity(tableName = "shift_patterns")
public class ShiftPattern {
    @PrimaryKey 
    @NonNull
    public String id;                    // UUID generato
    
    @NonNull
    public String name;                  // "Ciclo Continuo 4-2", "Personalizzato Utente"
    
    public String description;           // descrizione dettagliata
    
    public int cycleLengthDays;          // 18, 15, 7, etc.
    
    @NonNull
    public String recurrenceRuleJson;    // JSON con schema ricorrenza
    
    public boolean isContinuousCycle;    // validazione continuous cycle
    public boolean isPredefined;         // true per pattern di sistema
    public boolean isActive;
    public String createdByUserId;       // null per pattern predefiniti
    public long createdAt;
    public long updatedAt;
}

@Entity(tableName = "user_shift_assignments")
public class UserShiftAssignment {
    @PrimaryKey 
    @NonNull
    public String id;
    
    @NonNull
    public String userId;                // identificativo utente
    
    @NonNull
    public String shiftPatternId;        // FK a shift_patterns
    
    @NonNull
    public String startDate;             // "2025-01-15" - data inizio ciclo
    
    public int cycleOffsetDays;          // sfalsamento rispetto alla squadra A (0 per utente singolo)
    public String teamName;              // "Squadra A", "Personale", etc.
    public String teamColorHex;          // colore squadra
    public boolean isActive;             // solo un assignment attivo per utente
    public long assignedAt;
}

// ===== EVENTI GENERATI =====

@Entity(tableName = "smart_shift_events",
        indices = {
            @Index(value = {"userId", "eventDate"}),
            @Index(value = {"shiftPatternId", "eventDate"}),
            @Index(value = {"masterEventId"})
        })
public class SmartShiftEvent {
    @PrimaryKey 
    @NonNull
    public String id;
    
    @NonNull
    public String eventType;             // "master", "instance", "exception"
    
    public String masterEventId;         // FK per eventi derivati
    
    @NonNull
    public String userId;
    
    @NonNull
    public String shiftPatternId;
    
    @NonNull
    public String shiftTypeId;           // FK a shift_types
    
    @NonNull
    public String eventDate;             // "2025-07-15"
    
    @NonNull
    public String startTime;             // "06:00"
    
    @NonNull
    public String endTime;               // "14:00"
    
    public int cycleDayNumber;           // giorno nel ciclo (1-18 per 4-2)
    public String status;                // "active", "modified", "deleted"
    public String exceptionReason;       // motivo modifica
    public long generatedAt;
    public long updatedAt;
}

// ===== CONTATTI SQUADRA =====

@Entity(tableName = "team_contacts")
public class TeamContact {
    @PrimaryKey 
    @NonNull
    public String id;
    
    @NonNull
    public String userId;                // proprietario del contatto
    
    @NonNull
    public String contactName;
    
    public String phoneNumber;
    public String email;
    public String shiftPatternId;        // pattern condiviso (opzionale)
    public String notes;                 // note cambio turno
    public String androidContactId;      // link a contatto telefono
    public boolean isActive;
    public long createdAt;
}
```

### 1.2 JSON Schema Ricorrenza

```json
{
  "pattern_type": "custom_cycle",
  "cycle_length": 18,
  "continuous_cycle_compliant": true,
  "shifts_sequence": [
    {"days": [1,2,3,4], "shift_type": "morning"},
    {"days": [5,6], "shift_type": "rest"},
    {"days": [7,8,9,10], "shift_type": "night"},
    {"days": [11,12], "shift_type": "rest"},
    {"days": [13,14,15,16], "shift_type": "afternoon"},
    {"days": [17,18], "shift_type": "rest"}
  ],
  "team_generation": {
    "max_teams": 9,
    "offset_pattern": "sequential", // 0, 2, 4, 6, 8, 10, 12, 14, 16
    "coverage_24h": true
  }
}
```

## 2. Pattern Predefiniti

### 2.1 Dati Iniziali ShiftTypes

```java
// Tipi turno predefiniti da inserire al primo avvio
// NOTA: I nomi vengono caricati da strings.xml per l'internazionalizzazione
public static final ShiftTypeTemplate[] DEFAULT_SHIFT_TYPES = {
    new ShiftTypeTemplate("morning", R.string.shift_type_morning, "06:00", "14:00", "#4CAF50", "ic_morning", true, 1),
    new ShiftTypeTemplate("afternoon", R.string.shift_type_afternoon, "14:00", "22:00", "#FF9800", "ic_afternoon", true, 2),
    new ShiftTypeTemplate("night", R.string.shift_type_night, "22:00", "06:00", "#3F51B5", "ic_night", true, 3),
    new ShiftTypeTemplate("rest", R.string.shift_type_rest, "00:00", "23:59", "#9E9E9E", "ic_rest", false, 4)
};

// Template per inizializzazione con supporto i18n
public static class ShiftTypeTemplate {
    public final String id;
    public final int nameResId;     // Resource ID per strings.xml
    public final String startTime;
    public final String endTime;
    public final String colorHex;
    public final String iconName;
    public final boolean isWorkingShift;
    public final int sortOrder;
    
    public ShiftTypeTemplate(String id, int nameResId, String startTime, String endTime, 
                           String colorHex, String iconName, boolean isWorkingShift, int sortOrder) {
        this.id = id;
        this.nameResId = nameResId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.colorHex = colorHex;
        this.iconName = iconName;
        this.isWorkingShift = isWorkingShift;
        this.sortOrder = sortOrder;
    }
    
    public ShiftType toShiftType(Context context) {
        ShiftType shiftType = new ShiftType();
        shiftType.id = this.id;
        shiftType.name = context.getString(this.nameResId);  // Risolve da strings.xml
        shiftType.startTime = this.startTime;
        shiftType.endTime = this.endTime;
        shiftType.colorHex = this.colorHex;
        shiftType.iconName = this.iconName;
        shiftType.isWorkingShift = this.isWorkingShift;
        shiftType.sortOrder = this.sortOrder;
        shiftType.isCustom = false;
        shiftType.isActive = true;
        shiftType.createdAt = System.currentTimeMillis();
        shiftType.updatedAt = System.currentTimeMillis();
        return shiftType;
    }
}
```

### 2.2 Pattern Predefiniti

```java
// Pattern da generare automaticamente
// NOTA: Nomi e descrizioni vengono caricati da strings.xml per l'internazionalizzazione
public static final ShiftPatternTemplate[] DEFAULT_PATTERNS = {
    // Ciclo Continuo 4-2 (quello attuale di QDue)
    new ShiftPatternTemplate(
        "continuous_4_2",
        R.string.pattern_continuous_4_2_name,
        R.string.pattern_continuous_4_2_desc,
        18,
        "4M-2R-4N-2R-4A-2R"
    ),
    
    // Ciclo Continuo 3-2
    new ShiftPatternTemplate(
        "continuous_3_2", 
        R.string.pattern_continuous_3_2_name,
        R.string.pattern_continuous_3_2_desc,
        15,
        "3M-2R-3N-2R-3A-2R"
    ),
    
    // Settimana Standard 5-2
    new ShiftPatternTemplate(
        "weekly_5_2",
        R.string.pattern_weekly_5_2_name,
        R.string.pattern_weekly_5_2_desc,
        7,
        "5M-2R"
    ),
    
    // Settimana Standard 6-1  
    new ShiftPatternTemplate(
        "weekly_6_1",
        R.string.pattern_weekly_6_1_name,
        R.string.pattern_weekly_6_1_desc,
        7,
        "6M-1R"
    )
};

// Template per inizializzazione pattern con supporto i18n
public static class ShiftPatternTemplate {
    public final String id;
    public final int nameResId;         // Resource ID per nome pattern
    public final int descriptionResId;  // Resource ID per descrizione
    public final int cycleLengthDays;
    public final String shortPattern;   // Pattern abbreviato per generazione JSON
    
    public ShiftPatternTemplate(String id, int nameResId, int descriptionResId, 
                              int cycleLengthDays, String shortPattern) {
        this.id = id;
        this.nameResId = nameResId;
        this.descriptionResId = descriptionResId;
        this.cycleLengthDays = cycleLengthDays;
        this.shortPattern = shortPattern;
    }
    
    public ShiftPattern toShiftPattern(Context context) {
        ShiftPattern pattern = new ShiftPattern();
        pattern.id = this.id;
        pattern.name = context.getString(this.nameResId);               // Risolve da strings.xml
        pattern.description = context.getString(this.descriptionResId); // Risolve da strings.xml
        pattern.cycleLengthDays = this.cycleLengthDays;
        pattern.recurrenceRuleJson = generateRecurrenceRule(this.shortPattern);
        pattern.isContinuousCycle = true;
        pattern.isPredefined = true;
        pattern.isActive = true;
        pattern.createdByUserId = null; // Pattern di sistema
        pattern.createdAt = System.currentTimeMillis();
        pattern.updatedAt = System.currentTimeMillis();
        return pattern;
    }
}
```

## 3. Architettura Android

### 3.1 Package Structure

```
net.calvuz.qdue.smartshifts/
├── data/
│   ├── entities/          // ShiftType, ShiftPattern, etc.
│   ├── dao/              // DAO interfaces
│   ├── repository/       // Repository pattern
│   └── database/         // SmartShiftsDatabase
├── domain/
│   ├── models/           // Domain models
│   ├── usecases/         // Business logic
│   └── generators/       // Shift generation algorithms
├── ui/
│   ├── main/            // SmartShiftsActivity principale
│   ├── setup/           // Setup wizard per pattern
│   ├── calendar/        // Vista calendario turni
│   ├── contacts/        // Gestione contatti squadra
│   └── settings/        // Configurazioni avanzate
└── utils/
    ├── validators/       // Continuous cycle validation
    ├── converters/      // JSON converters
    └── generators/      // Pattern generators
```

### 3.2 Database Integration

```java
@Database(
    entities = {
        ShiftType.class,
        ShiftPattern.class, 
        UserShiftAssignment.class,
        SmartShiftEvent.class,
        TeamContact.class
    },
    version = 1,
    exportSchema = false
)
@TypeConverters({SmartShiftsConverters.class})
public abstract class SmartShiftsDatabase extends RoomDatabase {
    
    public abstract ShiftTypeDao shiftTypeDao();
    public abstract ShiftPatternDao shiftPatternDao();
    public abstract UserShiftAssignmentDao userAssignmentDao();
    public abstract SmartShiftEventDao smartEventDao();
    public abstract TeamContactDao teamContactDao();
    
    // Singleton pattern con migration support
    private static volatile SmartShiftsDatabase INSTANCE;
    
    public static SmartShiftsDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (SmartShiftsDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        SmartShiftsDatabase.class,
                        "smartshifts_database"
                    )
                    .addCallback(new DatabaseCallback())  // Per dati iniziali
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
```

### 3.3 Dependency Injection Setup

```java
// Modulo Hilt per SmartShifts
@Module
@InstallIn(SingletonComponent.class)
public class SmartShiftsModule {
    
    @Provides
    @Singleton
    public SmartShiftsDatabase provideDatabase(@ApplicationContext Context context) {
        return SmartShiftsDatabase.getDatabase(context);
    }
    
    @Provides
    public ShiftPatternRepository provideShiftPatternRepository(
        SmartShiftsDatabase database,
        ShiftGeneratorEngine generatorEngine
    ) {
        return new ShiftPatternRepository(
            database.shiftPatternDao(),
            database.userAssignmentDao(),
            database.smartEventDao(),
            generatorEngine
        );
    }
    
    @Provides  
    @Singleton
    public ShiftGeneratorEngine provideShiftGenerator(
        SmartShiftsDatabase database
    ) {
        return new ShiftGeneratorEngine(
            database.shiftTypeDao(),
            database.shiftPatternDao()
        );
    }
}
```

## 4. Core Business Logic

### 4.1 Generatore Turni

```java
@Singleton
public class ShiftGeneratorEngine {
    
    private final ShiftTypeDao shiftTypeDao;
    private final ShiftPatternDao patternDao;
    
    @Inject
    public ShiftGeneratorEngine(ShiftTypeDao shiftTypeDao, ShiftPatternDao patternDao) {
        this.shiftTypeDao = shiftTypeDao;
        this.patternDao = patternDao;
    }
    
    /**
     * Genera eventi turno per un utente in un periodo specifico
     */
    public List<SmartShiftEvent> generateShiftsForPeriod(
        @NonNull String userId,
        @NonNull String shiftPatternId, 
        @NonNull LocalDate startDate,
        @NonNull LocalDate endDate,
        int cycleOffsetDays
    ) {
        
        ShiftPattern pattern = patternDao.getPatternById(shiftPatternId);
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern not found: " + shiftPatternId);
        }
        
        RecurrenceRule rule = RecurrenceRule.fromJson(pattern.recurrenceRuleJson);
        List<SmartShiftEvent> events = new ArrayList<>();
        
        LocalDate cycleStart = calculateCycleStart(pattern, startDate);
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            
            long daysSinceCycleStart = ChronoUnit.DAYS.between(cycleStart, date);
            long adjustedDays = daysSinceCycleStart - cycleOffsetDays;
            int cycleDayNumber = (int) ((adjustedDays % rule.getCycleLength()) + rule.getCycleLength()) % rule.getCycleLength() + 1;
            
            ShiftInfo shiftInfo = rule.getShiftForDay(cycleDayNumber);
            
            if (shiftInfo != null && !shiftInfo.getShiftType().equals("rest")) {
                SmartShiftEvent event = createShiftEvent(
                    userId, 
                    pattern, 
                    shiftInfo, 
                    date, 
                    cycleDayNumber
                );
                events.add(event);
            }
        }
        
        return events;
    }
    
    /**
     * Valida se un pattern è continuous cycle compliant
     * PLACEHOLDER - implementazione futura
     */
    public ContinuousCycleValidation validateContinuousCycle(ShiftPattern pattern) {
        // TODO: Implementare logica di validazione
        // - Verifica copertura 24h
        // - Calcola sovrapposizioni
        // - Verifica rotazione matematica
        return ContinuousCycleValidation.placeholder();
    }
    
    /**
     * Genera pattern per squadre multiple (se continuous cycle compliant)
     */
    public List<UserShiftAssignment> generateTeamAssignments(
        @NonNull String basePatternId,
        @NonNull LocalDate startDate,
        int numberOfTeams
    ) {
        
        ShiftPattern basePattern = patternDao.getPatternById(basePatternId);
        ContinuousCycleValidation validation = validateContinuousCycle(basePattern);
        
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Pattern non compatibile con ciclo continuo");
        }
        
        List<UserShiftAssignment> assignments = new ArrayList<>();
        
        for (int teamIndex = 0; teamIndex < numberOfTeams; teamIndex++) {
            int offsetDays = teamIndex * 2; // sfalsamento di 2 giorni per squadra
            
            UserShiftAssignment assignment = new UserShiftAssignment();
            assignment.id = UUID.randomUUID().toString();
            assignment.userId = "team_" + (char)('A' + teamIndex); // Team A, B, C...
            assignment.shiftPatternId = basePatternId;
            assignment.startDate = startDate.toString();
            assignment.cycleOffsetDays = offsetDays;
            assignment.teamName = "Squadra " + (char)('A' + teamIndex);
            assignment.teamColorHex = generateTeamColor(teamIndex);
            assignment.isActive = true;
            assignment.assignedAt = System.currentTimeMillis();
            
            assignments.add(assignment);
        }
        
        return assignments;
    }
}
```

### 4.2 Repository Pattern

```java
@Singleton 
public class SmartShiftsRepository {
    
    private final ShiftPatternDao patternDao;
    private final UserShiftAssignmentDao assignmentDao;
    private final SmartShiftEventDao eventDao;
    private final ShiftGeneratorEngine generator;
    
    @Inject
    public SmartShiftsRepository(
        ShiftPatternDao patternDao,
        UserShiftAssignmentDao assignmentDao, 
        SmartShiftEventDao eventDao,
        ShiftGeneratorEngine generator
    ) {
        this.patternDao = patternDao;
        this.assignmentDao = assignmentDao;
        this.eventDao = eventDao;
        this.generator = generator;
    }
    
    /**
     * Ottiene turni per utente in un mese specifico
     */
    public LiveData<List<SmartShiftEvent>> getUserShiftsForMonth(
        @NonNull String userId, 
        int year, 
        int month
    ) {
        
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        
        return eventDao.getEventsForUserInPeriod(
            userId,
            monthStart.toString(),
            monthEnd.toString()
        );
    }
    
    /**
     * Assegna un pattern ad un utente e genera eventi
     */
    public void assignPatternToUser(
        @NonNull String userId,
        @NonNull String patternId,
        @NonNull LocalDate startDate
    ) {
        
        // Disattiva assignment precedenti
        assignmentDao.deactivateUserAssignments(userId);
        
        // Crea nuovo assignment
        UserShiftAssignment assignment = new UserShiftAssignment();
        assignment.id = UUID.randomUUID().toString();
        assignment.userId = userId;
        assignment.shiftPatternId = patternId;
        assignment.startDate = startDate.toString();
        assignment.cycleOffsetDays = 0; // utente singolo
        assignment.teamName = "Personale";
        assignment.teamColorHex = "#2196F3";
        assignment.isActive = true;
        assignment.assignedAt = System.currentTimeMillis();
        
        assignmentDao.insert(assignment);
        
        // Genera eventi per i prossimi 12 mesi
        LocalDate endDate = startDate.plusMonths(12);
        List<SmartShiftEvent> events = generator.generateShiftsForPeriod(
            userId, patternId, startDate, endDate, 0
        );
        
        eventDao.insertAll(events);
    }
}
```

## 5. UI Components

### 5.1 Activity Principale

```java
@AndroidEntryPoint
public class SmartShiftsActivity extends AppCompatActivity {
    
    @Inject
    SmartShiftsRepository repository;
    
    private SmartShiftsViewModel viewModel;
    private ActivitySmartShiftsBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivitySmartShiftsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        viewModel = new ViewModelProvider(this).get(SmartShiftsViewModel.class);
        
        setupToolbar();
        setupBottomNavigation();
        setupFirstTimeSetup();
    }
    
    private void setupFirstTimeSetup() {
        viewModel.hasActiveAssignment().observe(this, hasAssignment -> {
            if (!hasAssignment) {
                // Mostra setup wizard per prima configurazione
                startActivity(new Intent(this, ShiftSetupWizardActivity.class));
            }
        });
    }
    
    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            
            switch (item.getItemId()) {
                case R.id.nav_calendar:
                    fragment = new SmartShiftsCalendarFragment();
                    break;
                case R.id.nav_patterns:
                    fragment = new ShiftPatternsFragment();
                    break;
                case R.id.nav_contacts:
                    fragment = new TeamContactsFragment();
                    break;
                case R.id.nav_settings:
                    fragment = new SmartShiftsSettingsFragment();
                    break;
            }
            
            if (fragment != null) {
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .commit();
                return true;
            }
            return false;
        });
    }
}
```

### 5.2 Setup Wizard

```java
@AndroidEntryPoint
public class ShiftSetupWizardActivity extends AppCompatActivity {
    
    @Inject 
    SmartShiftsRepository repository;
    
    private SetupWizardViewModel viewModel;
    private List<Fragment> wizardSteps;
    private int currentStep = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(SetupWizardViewModel.class);
        
        setupWizardSteps();
        showCurrentStep();
    }
    
    private void setupWizardSteps() {
        wizardSteps = Arrays.asList(
            new WelcomeStepFragment(),           // Benvenuto SmartShifts
            new PatternSelectionStepFragment(),  // Scelta pattern predefinito vs custom
            new CustomPatternStepFragment(),     // Creazione pattern personalizzato (se custom)
            new StartDateStepFragment(),         // Data inizio ciclo
            new ConfirmationStepFragment()       // Riepilogo e conferma
        );
    }
    
    public void goToNextStep() {
        if (currentStep < wizardSteps.size() - 1) {
            currentStep++;
            showCurrentStep();
        } else {
            // Completa setup e vai alla main activity
            completeSetup();
        }
    }
    
    private void completeSetup() {
        viewModel.finalizeSetup().observe(this, success -> {
            if (success) {
                startActivity(new Intent(this, SmartShiftsActivity.class));
                finish();
            } else {
                // Mostra errore
            }
        });
    }
}
```

## 6. Integrazione con QDue Esistente

### 6.1 Entry Point nel Menu

```xml
<!-- Nel navigation drawer esistente di QDue -->
<item
    android:id="@+id/nav_smart_shifts"
    android:icon="@drawable/ic_smart_shifts"
    android:title="@string/menu_smart_shifts" />
```

```java
// In MainActivity esistente
case R.id.nav_smart_shifts:
    startActivity(new Intent(this, SmartShiftsActivity.class));
    break;
```

### 6.2 Coesistenza Database

Il nuovo database `SmartShiftsDatabase` sarà completamente separato dal database esistente di QDue, permettendo:

- **Zero impatto** sul codice esistente
- **Rollback sicuro** se necessario
- **Migrazione graduale** in futuro
- **Testing isolato** delle nuove funzionalità

### 6.3 Condivisione Risorse

```java
// Utility condivise per mantenere consistency
public class QDueStyleUtils {
    
    public static void applyQDueTheme(Activity activity) {
        // Applica tema consistente con QDue esistente
    }
    
    public static int getQDueAccentColor(Context context) {
        // Usa gli stessi colori accent di QDue
    }
}
```

## 7. Validazione Continuous Cycle (Placeholder)

```java
public class ContinuousCycleValidation {
    
    private boolean isValid;
    private List<String> issues;
    private CoverageAnalysis coverage;
    
    public static ContinuousCycleValidation placeholder() {
        // PLACEHOLDER per implementazione futura
        ContinuousCycleValidation validation = new ContinuousCycleValidation();
        validation.isValid = true; // Assume sempre valido per ora
        validation.issues = new ArrayList<>();
        validation.coverage = new CoverageAnalysis();
        return validation;
    }
    
    // TODO: Implementare logica di validazione reale
    // - Verifica che ogni ora del giorno sia coperta
    // - Calcola se la rotazione è matematicamente corretta
    // - Identifica gap o sovrapposizioni
    // - Valida se supporta N squadre
}
```

## 8. Roadmap Implementazione

### Fase 1: Core Database & Models (Sprint 1-2)
- [x] Definizione entità database
- [x] Setup Room database
- [x] DAO interfaces
- [x] Dependency injection setup

### Fase 2: Business Logic (Sprint 3-4)
- [x] ShiftGeneratorEngine
- [x] Repository pattern
- [x] Pattern predefiniti
- [x] Algoritmi generazione turni

### Fase 3: UI Foundation (Sprint 5-6)
- [ ] SmartShiftsActivity
- [ ] Setup wizard
- [ ] Navigation structure
- [ ] Basic calendar view

### Fase 4: Advanced Features (Sprint 7-8)
- [ ] Custom pattern creator
- [ ] Team contacts management
- [ ] Settings & preferences
- [ ] Export/import functionality

### Fase 5: Integration & Polish (Sprint 9-10)
- [ ] QDue integration
- [ ] Continuous cycle validation
- [ ] Performance optimization
- [ ] Testing & bug fixes

## 12. Stima Sforzo

**Totale stimato**: ~10 sprint (20 settimane)

**Complessità**:
- Database & Core Logic: **Media**
- UI Components: **Media-Alta**
- Pattern Generation: **Alta**
- Integration: **Bassa**

**Rischi**:
- Algoritmi continuous cycle validation
- Performance con pattern complessi
- UX per creazione pattern custom

### 📝 **Note Implementative per Internazionalizzazione:**

1. **Template Class**: Tutti i pattern e shift types predefiniti ora usano resource ID invece di stringhe hardcoded
2. **Context Parameter**: Metodi che generano testo ora richiedono `Context` per accesso a `strings.xml`
3. **Helper Utilities**: Classe `SmartShiftsStringUtils` per gestione centralizzata delle stringhe
4. **Plurals Support**: Gestione forme plurali per conteggi (giorni, turni, etc.)
5. **Accessibility**: Stringhe per content description per screen reader
6. **Time Formats**: Formati data/ora configurabili per diverse localizzazioni

**🌍 Localizzazioni Future Supportate:**
- Italiano (default)
- Inglese (`strings.xml` in `/values-en/`)
- Francese (`strings.xml` in `/values-fr/`)
- Tedesco (`strings.xml` in `/values-de/`)
- Spagnolo (`strings.xml` in `/values-es/`)

Questa architettura garantisce una base solida, scalabile e **completamente internazionalizzabile** per il sistema SmartShifts, mantenendo la piena compatibilità con QDue esistente.