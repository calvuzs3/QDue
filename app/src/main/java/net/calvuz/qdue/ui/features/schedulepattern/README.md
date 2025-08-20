# User Schedule Pattern Creation Feature

## 📋 Overview

The User Schedule Pattern Creation feature allows users to create personalized work schedule patterns that repeat cyclically from a specified start date. Users build their pattern by adding "tasselli" (tiles) representing individual days, each containing either a work shift or a rest day.

## 🚀 Quick Start

### Launching the Feature

```java
// Create new pattern
Intent intent = UserSchedulePatternCreationActivity.createIntent(context);
startActivity(intent);

// Edit existing pattern
Intent editIntent = UserSchedulePatternCreationActivity.createEditIntent(context, assignmentId);
startActivity(editIntent);
```

### Integration with Existing Calendar System

```java
// The feature integrates with existing repositories:
// - ShiftRepository: Access to predefined shifts (morning, night, afternoon)
// - UserScheduleAssignmentRepository: Persistence of user patterns
// - RecurrenceRuleRepository: Pattern rule storage
// - CalendarService: Main calendar operations
```

## 🏗️ Architecture

### Components Structure

```
ui.features.schedulepattern/
├── UserSchedulePatternCreationActivity     # Main standalone activity
├── adapters/
│   ├── PatternDayItemAdapter              # RecyclerView for pattern days
│   └── ShiftSelectionAdapter              # Horizontal shift selection
├── models/
│   └── PatternDay                         # UI model for pattern days
├── services/
│   ├── UserSchedulePatternService         # Business logic interface
│   └── impl/UserSchedulePatternServiceImpl # Implementation
└── di/
    └── SchedulePatternModule              # Dependency injection
```

### Dependency Injection

```java
// In Activity.inject() method:
@Override
public void inject(@NonNull ServiceProvider serviceProvider) {
    mCalendarService = serviceProvider.getCalendarService();
    
    SchedulePatternModule module = new SchedulePatternModule(this, serviceProvider);
    mShiftRepository = module.getShiftRepository();
    mLocaleManager = module.getLocaleManager();
}
```

## 📱 User Interface

### Key Features

- **Single Page Form**: All functionality in one activity
- **Start Date Selection**: DatePicker for pattern beginning
- **Pattern Construction**: Add shifts or rest days in sequence
- **Real-time Preview**: Visual representation of pattern
- **Material Design 3**: Modern Android design system

### UI Components

1. **Start Date Section**: Date selection with clear formatting
2. **Available Shifts Section**: Horizontal scrollable list of shift options
3. **Pattern Days Section**: Vertical list of pattern sequence
4. **Action Controls**: Save, preview, and management buttons

## 🔧 Business Logic

### Pattern Creation Process

1. **Load Available Shifts**: Query ShiftRepository for templates
2. **Build Pattern Sequence**: User adds PatternDay objects
3. **Generate RecurrenceRule**: Convert to domain recurrence pattern
4. **Create Assignment**: Link user to pattern with start date
5. **Integrate with Calendar**: Available through CalendarService

### Validation Rules

- **Pattern Length**: Must have at least one day, max 365 days
- **Start Date**: Cannot be in the past
- **Shift Validation**: All shifts must exist in system
- **Rest Day Handling**: Rest days represented as null shifts

### Data Flow

```
User Input → PatternDay List → RecurrenceRule → UserScheduleAssignment → Database
```

## 🌍 Internationalization

Full i18n support using the project's core.architecture.common.i18n system:

- **String Resources**: `strings_schedule_pattern.xml`
- **LocaleManager**: Centralized locale management
- **Domain Localization**: Localized domain models

### Key String Categories

- Activity titles and labels
- Shift type names
- Validation messages
- Dialog content
- Content descriptions for accessibility

## 📊 Data Models

### PatternDay

```java
public class PatternDay {
    private int dayNumber;      // Position in sequence (1-based)
    private Shift shift;        // Work shift or null for rest
    private boolean isEditable; // Whether day can be modified
    
    // Key methods
    public boolean isRestDay()
    public boolean isWorkDay()
    public String getDisplayName()
    public PatternDayType getType()
}
```

### Integration Models

- **RecurrenceRule**: Google Calendar RRULE compatible patterns
- **UserScheduleAssignment**: User assignment to custom patterns
- **Shift**: Existing shift templates from database
- **WorkScheduleDay**: Integration with calendar system

## 🎨 Visual Design

### Color System

```xml
<!-- Shift Type Colors -->
<color name="shift_morning_color">#FFA726</color>
<color name="shift_afternoon_color">#FF7043</color>
<color name="shift_night_color">#5C6BC0</color>
<color name="shift_default_color">#66BB6A</color>

<!-- Rest Day Colors -->
<color name="rest_day_background">#F3E5F5</color>
<color name="rest_day_icon_color">#9C27B0</color>
```

### Icon System

- **Morning Shifts**: Sun icon (`ic_wb_sunny_24`)
- **Night Shifts**: Moon icon (`ic_nightlight_24`)
- **Rest Days**: Bed icon (`ic_hotel_24`)
- **Default Shifts**: Work icon (`ic_work_24`)

## 🧪 Testing

### Unit Testing

```java
// Test PatternDay logic
@Test
public void testPatternDay_RestDay() {
    PatternDay restDay = new PatternDay(1, null);
    assertTrue(restDay.isRestDay());
    assertFalse(restDay.isWorkDay());
}

// Test service validation
@Test
public void testPatternValidation_EmptyPattern() {
    List<PatternDay> emptyPattern = new ArrayList<>();
    OperationResult<Void> result = patternService.validatePatternDays(emptyPattern);
    assertFalse(result.isSuccess());
}
```

### Integration Testing

- Repository integration
- RecurrenceRule generation
- Calendar service integration

## 🚀 Future Extensions

### Planned Enhancements

- **Pattern Templates**: Predefined common work patterns
- **Team Patterns**: Collaborative pattern sharing
- **Pattern Analytics**: Work-life balance insights
- **Exception Handling**: Temporary pattern modifications
- **Import/Export**: Pattern sharing between users

### API Extensions

```java
// Future API additions
public interface UserSchedulePatternService {
    CompletableFuture<OperationResult<List<PatternTemplate>>> getPatternTemplates();
    CompletableFuture<OperationResult<PatternAnalytics>> analyzePattern(String assignmentId);
    CompletableFuture<OperationResult<Boolean>> sharePattern(String assignmentId, String targetUserId);
}
```

## 📝 Usage Examples

### Basic Pattern Creation

```java
// Create a simple 4-day pattern: Work, Work, Rest, Rest
List<PatternDay> pattern = Arrays.asList(
    new PatternDay(1, morningShift),
    new PatternDay(2, afternoonShift),
    new PatternDay(3, null), // Rest day
    new PatternDay(4, null)  // Rest day
);

LocalDate startDate = LocalDate.now().plusDays(1);
patternService.createUserPattern(pattern, startDate)
    .thenAccept(result -> {
        if (result.isSuccess()) {
            Log.d(TAG, "Pattern created: " + result.getData().getId());
        }
    });
```

### Pattern Validation

```java
// Validate pattern before creation
OperationResult<Void> validation = patternService.validatePatternConfiguration(
    patternDays, startDate, "My Work Pattern");
    
if (!validation.isSuccess()) {
    showError("Validation failed: " + validation.getErrorMessage());
    return;
}
```

### Pattern Preview

```java
// Generate 14-day preview
patternService.generatePatternPreview(patternDays, startDate, 14)
    .thenAccept(result -> {
        if (result.isSuccess()) {
            List<WorkScheduleDay> preview = result.getData();
            displayPreview(preview);
        }
    });
```

## 🔗 Integration Points

### Required Dependencies

- `net.calvuz.qdue.core.services.CalendarService`
- `net.calvuz.qdue.domain.calendar.repositories.ShiftRepository`
- `net.calvuz.qdue.domain.calendar.repositories.UserScheduleAssignmentRepository`
- `net.calvuz.qdue.domain.calendar.repositories.RecurrenceRuleRepository`

### Optional Dependencies

- `net.calvuz.qdue.domain.calendar.engines.RecurrenceCalculator`
- `net.calvuz.qdue.core.backup.CoreBackupManager`

## 📄 License

This feature is part of the QDue project and follows the same licensing terms as the main project.

---

**Version**: 1.0.0 - Initial Implementation  
**Author**: QDue Development Team  
**Since**: Clean Architecture Phase 2