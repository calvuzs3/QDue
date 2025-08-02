# Calendar App - Complete Package Structure with Domain/Infrastructure Separation

## Root Package Structure
```
com.yourapp.calendar/
├── core/
│   ├── domain/                # Business Logic Layer
│   │   ├── preferences/       # App preferences domain
│   │   ├── user/             # User management domain
│   │   ├── quattrodue/       # Work schedule provider domain
│   │   └── events/           # Calendar events domain
│   └── infrastructure/       # Technical Infrastructure Layer
│       ├── common/           # Shared utilities, constants, extensions
│       ├── di/              # Dependency injection setup
│       ├── db/              # Database entities, DAOs, migrations
│       ├── backup/          # Data backup/restore functionality
│       └── services/        # Service interfaces and implementations
│           └── ServiceManager.java  # Service locator
└── ui/
    └── features/            # Presentation Features Layer
        ├── common/          # Shared UI components, base classes
        ├── calendar/        # Calendar view feature
        ├── dayslist/        # Days list view feature  
        ├── events/          # Events management UI feature
        ├── settings/        # Settings/preferences UI feature
        └── welcome/         # Welcome/onboarding feature
```

## Core Domain Layer Details

### 1. `core/domain/preferences/` Package
```
core/domain/preferences/
├── model/
│   ├── UserPreferences.java        # UI preferences (theme, colors, view)
│   ├── BusinessPreferences.java    # Business settings (team, notifications)
│   └── DisplayPreferences.java     # Calendar display settings
├── repository/
│   ├── PreferencesRepository.java  # Interface
│   └── PreferencesRepositoryImpl.java
├── service/
│   ├── PreferencesService.java     # Interface
│   └── PreferencesServiceImpl.java # Business logic
└── manager/
    └── PreferencesManager.java     # High-level preferences operations
```

### 2. `core/domain/user/` Package
```
core/domain/user/
├── model/
│   ├── User.java                    # User entity
│   ├── Team.java                    # User team information
│   └── AuthenticationState.java    # Auth state model
├── repository/
│   ├── UserRepository.java         # Interface
│   └── UserRepositoryImpl.java     # Implementation
├── service/
│   ├── AuthenticationService.java  # Interface
│   ├── AuthenticationServiceImpl.java
│   └── CalendarSyncService.java    # External calendar sync
└── manager/
    └── UserManager.java            # High-level user operations
```

### 3. `core/domain/quattrodue/` Package (Work Schedule Provider System)
```
core/domain/quattrodue/
├── model/
│   ├── WorkScheduleTemplate.java   # Base schedule templates
│   ├── CustomScheduleSchema.java   # User-defined schemas
│   ├── ShiftType.java              # Shift types with colors/times
│   ├── Team.java                   # Team definitions
│   └── RecurrenceRule.java         # Volatile recurrence rules
├── provider/
│   ├── WorkScheduleProvider.java   # Interface - generates volatile events
│   ├── FixedScheduleProvider.java  # 4-2 predefined schema
│   └── CustomScheduleProvider.java # User-defined schema
├── service/
│   ├── ScheduleGenerationService.java # Generates schedule events
│   └── TemplateService.java        # Manages templates & schemas
└── manager/
    └── WorkScheduleManager.java    # Orchestrates schedule generation
```

### 4. `core/domain/events/` Package (Events + Work Schedule Exceptions)
```
core/domain/events/
├── model/
│   ├── Event.java                  # Base calendar event
│   ├── WorkScheduleEvent.java      # Work schedule specific event
│   ├── EventMetadata.java          # Metadata for event classification
│   ├── EventType.java              # NORMAL, WORK_SHIFT, OVERTIME, EXCEPTION
│   └── EventRecurrence.java        # Standard recurrence rules
├── provider/
│   ├── WorkScheduleExceptionsProvider.java # Interface - manages exceptions
│   └── WorkScheduleExceptionsProviderImpl.java
├── repository/
│   ├── EventRepository.java        # Interface
│   └── EventRepositoryImpl.java    # Implementation (includes exceptions)
├── service/
│   ├── EventService.java           # Interface
│   ├── EventServiceImpl.java       # Core event operations
│   ├── EventMergeService.java      # Merges schedule + exceptions
│   └── RecurrenceService.java      # Standard recurrence logic
└── manager/
    └── EventManager.java           # High-level event operations
```

## Core Infrastructure Layer Details

### 1. `core/infrastructure/common/` Package
```
core/infrastructure/common/
├── constants/
│   ├── AppConstants.java           # Application-wide constants
│   ├── DatabaseConstants.java      # Database table names, queries
│   └── DateTimeConstants.java      # Date/time formatting constants
├── extensions/
│   ├── DateExtensions.java         # Date utility extensions
│   └── StringExtensions.java       # String utility extensions
├── utils/
│   ├── DateTimeUtils.java          # Date/time utilities
│   ├── ValidationUtils.java        # Input validation utilities
│   └── ColorUtils.java             # Color manipulation utilities
└── exceptions/
    ├── BusinessException.java      # Business logic exceptions
    └── DataAccessException.java    # Data access exceptions
```

### 2. `core/infrastructure/di/` Package
```
core/infrastructure/di/
├── modules/
│   ├── DatabaseModule.java         # Database dependency providers
│   ├── ServiceModule.java          # Service dependency providers
│   ├── RepositoryModule.java       # Repository dependency providers
│   └── ManagerModule.java          # Manager dependency providers
├── qualifiers/
│   └── ApplicationScope.java       # Custom scope annotations
└── ServiceLocator.java             # Main dependency injection orchestrator
```

### 3. `core/infrastructure/db/` Package
```
core/infrastructure/db/
├── entities/
│   ├── UserEntity.java             # User database entity
│   ├── EventEntity.java            # Event database entity
│   ├── PreferencesEntity.java      # Preferences database entity
│   └── WorkScheduleEntity.java     # Work schedule database entity
├── dao/
│   ├── UserDao.java                # User data access object
│   ├── EventDao.java               # Event data access object
│   ├── PreferencesDao.java         # Preferences data access object
│   └── WorkScheduleDao.java        # Work schedule data access object
├── migrations/
│   ├── Migration_1_2.java          # Database migration scripts
│   └── Migration_2_3.java
├── converters/
│   ├── DateConverter.java          # Date type converters
│   └── EnumConverter.java          # Enum type converters
└── CalendarDatabase.java           # Main database class
```

### 4. `core/infrastructure/services/` Package
```
core/infrastructure/services/
├── ServiceManager.java             # Main service locator
├── base/
│   └── BaseService.java            # Base service interface
├── database/
│   └── DatabaseService.java        # DB connection management
├── sync/
│   └── SyncService.java            # Background sync operations
├── notification/
│   └── NotificationService.java    # Event notifications
└── backup/
    └── BackupService.java          # Data backup operations
```

## UI Features Layer Details

### 1. `ui/features/calendar/` Package
```
ui/features/calendar/
├── view/
│   ├── CalendarActivity.java       # Main calendar activity
│   ├── MonthView.java              # Monthly calendar view
│   ├── WeekView.java               # Weekly calendar view
│   └── DayView.java                # Daily calendar view
├── adapter/
│   ├── CalendarAdapter.java        # Calendar grid adapter
│   └── EventAdapter.java           # Events list adapter
├── fragment/
│   ├── CalendarFragment.java       # Main calendar fragment
│   └── EventDetailsFragment.java   # Event details dialog
└── viewmodel/
    ├── CalendarViewModel.java      # Calendar presentation logic
    └── EventViewModel.java         # Event presentation logic
```

### 2. `ui/features/dayslist/` Package
```
ui/features/dayslist/
├── view/
│   ├── DaysListActivity.java       # Days list main activity
│   └── DayItemView.java            # Individual day item view
├── adapter/
│   └── DaysListAdapter.java        # Days list adapter
├── fragment/
│   └── DaysListFragment.java       # Main days list fragment
└── viewmodel/
    └── DaysListViewModel.java      # Days list presentation logic
```

### 3. `ui/features/events/` Package
```
ui/features/events/
├── view/
│   ├── EventFormActivity.java      # Event creation/editing form
│   ├── EventListActivity.java      # Events list view
│   └── RecurrenceDialog.java       # Recurrence configuration dialog
├── adapter/
│   └── EventListAdapter.java       # Events list adapter
├── fragment/
│   ├── EventFormFragment.java      # Event form fragment
│   └── EventListFragment.java      # Events list fragment
└── viewmodel/
    ├── EventFormViewModel.java     # Event form presentation logic
    └── EventListViewModel.java     # Events list presentation logic
```

### 4. `ui/features/settings/` Package
```
ui/features/settings/
├── view/
│   ├── SettingsActivity.java       # Main settings activity
│   ├── PreferencesFragment.java    # Preferences fragment
│   └── TeamSelectionDialog.java    # Team selection dialog
├── adapter/
│   └── SettingsAdapter.java        # Settings list adapter
└── viewmodel/
    └── SettingsViewModel.java      # Settings presentation logic
```

### 5. `ui/features/welcome/` Package
```
ui/features/welcome/
├── view/
│   ├── WelcomeActivity.java        # Welcome/onboarding activity
│   ├── IntroFragment.java          # Introduction fragment
│   └── SetupFragment.java          # Initial setup fragment
├── adapter/
│   └── OnboardingAdapter.java      # Onboarding slides adapter
└── viewmodel/
    └── WelcomeViewModel.java       # Welcome presentation logic
```

## Architecture Layers Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    UI Features Layer                        │
│  calendar │ dayslist │ events │ settings │ welcome         │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                 Core Domain Layer                           │
│   preferences │ user │ quattrodue │ events                 │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│              Core Infrastructure Layer                      │
│    common │ di │ db │ backup │ services                    │
└─────────────────────────────────────────────────────────────┘
```

## Key Benefits of This Structure

### 1. **Clear Separation of Concerns**
- **Domain**: Pure business logic, domain models, business rules
- **Infrastructure**: Technical concerns, data persistence, external services
- **UI Features**: Presentation logic, user interactions, view components

### 2. **Dependency Direction**
- UI Features → Domain (business operations)
- Domain → Infrastructure (technical services)
- Infrastructure → External (database, network, file system)

### 3. **Testability**
- Domain layer can be unit tested independently
- Infrastructure can be mocked for domain tests
- UI features can be tested with mocked domain layer

### 4. **Scalability**
- Easy to add new domain concepts
- Infrastructure changes don't affect domain
- UI features are self-contained and can be developed independently

This structure follows Clean Architecture principles while maintaining practical Android development patterns.