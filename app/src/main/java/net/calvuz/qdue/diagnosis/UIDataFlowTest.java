package net.calvuz.qdue.diagnosis;

import static net.calvuz.qdue.core.common.Lib.isDebugBuild;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.core.di.impl.ServiceProviderImpl;
import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleShift;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.calendar.models.Shift;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.domain.calendar.usecases.GenerateUserScheduleUseCase;
import net.calvuz.qdue.preferences.QDuePreferences;
import net.calvuz.qdue.ui.features.swipecalendar.di.SwipeCalendarModule;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * UIDataFlowTest - Test the complete data flow from Repository to UI
 *
 * <p>Specifically tests the SwipeCalendar data loading pipeline to identify
 * where the UI data gets lost or corrupted.</p>
 */
public class UIDataFlowTest {

    private static final String TAG = "UIDataFlowTest";

    /**
     * Add debug menu option to run diagnostics (for debug builds)
     */
    public static void addDebugMenuOption(Activity activity, android.view.Menu menu) {
        // Only add in debug builds
        if (isDebugBuild(activity)) {
            menu.add(0, 444444, 0, "🔧 UI Data Flow")
                    .setOnMenuItemClickListener(item -> {
                        // Test completo del pattern
                        new Thread(() -> {
                            String results = UIDataFlowTest.testCompleteDataFlow(activity.getApplicationContext());
                            android.util.Log.v("UIDATAFLOW", results);
                        }).start();
                        return true;
                    });
            menu.add( 0, 447777, 0, "🔍 Direct Path Comparison" )
                    .setOnMenuItemClickListener( item -> {
                        new Thread( () -> {
                            String results = testDirectPathComparison( activity.getApplicationContext() );
                            Log.v( "PATHCOMPARISON", results );
                            System.out.println( results );
                        } ).start();
                        return true;
                    } );
        }
    }


    /**
     * Test complete data flow: Repository → UseCase → SwipeCalendarModule → UI
     */
    public static String testCompleteDataFlow(Context context) {

        // DEBUG Database tables
        debugDatabaseTables( context );

        // Complete Data Flow
        StringBuilder report = new StringBuilder();

        report.append("=== UI DATA FLOW TEST ===\n");
        report.append("Testing: Repository → UseCase → SwipeCalendarModule → UI\n\n");

        try {
            // Step 1: Initialize components
            ServiceProvider serviceProvider = new ServiceProviderImpl(context);
            WorkScheduleRepository repository = serviceProvider.getWorkScheduleService();

            LocalDate testDate = LocalDate.now().plusDays(1); // Tomorrow (likely a work day)
            YearMonth testMonth = YearMonth.from(testDate);
            String userId = QDuePreferences.getUserId( context );

            report.append("Test Parameters:\n");
            report.append("  Date: ").append(testDate).append("\n");
            report.append("  Month: ").append(testMonth).append("\n");
            report.append("  User ID: ").append(userId).append("\n\n");

            // Step 2: Test Repository Level
            report.append("STEP 1: Repository Level\n");
            report.append("------------------------\n");

            WorkScheduleDay repoDay = repository.getWorkScheduleForDate(testDate, userId).join().getData();

            if (repoDay != null) {
                report.append("✅ Repository returned WorkScheduleDay\n");
                report.append("  Date: ").append(repoDay.getDate()).append("\n");
                report.append("  Shifts count: ").append(repoDay.getShifts().size()).append("\n");

                // Detailed shift analysis
                for (int i = 0; i < repoDay.getShifts().size(); i++) {
                    WorkScheduleShift shift = repoDay.getShifts().get(i);
                    report.append("  Shift ").append(i + 1).append(":\n");

                    // Shift details
                    Shift shiftInfo = shift.getShift();
                    report.append( "    Shift ID: " ).append( shiftInfo.getId() ).append( "\n" );
                    report.append("    Shift Name: ").append(shiftInfo.getName()).append("\n");
                    report.append("    Start Time: ").append(shiftInfo.getStartTime()).append("\n");
                    report.append("    End Time: ").append(shiftInfo.getEndTime()).append("\n");

                    // Team details - THIS IS CRITICAL
                    if (shift.getTeams() != null) {
                        report.append("    Teams count: ").append(shift.getTeams().size()).append("\n");

                        if (shift.getTeams().isEmpty()) {
                            report.append("    ❌ NO TEAMS ASSIGNED - This could be the UI issue!\n");
                        } else {
                            for (int j = 0; j < shift.getTeams().size(); j++) {
                                Team team = shift.getTeams().get(j);
                                if (team != null) {
                                    report.append("      Team ").append(j + 1).append(": ")
                                            .append(team.getName()).append(" (").append(team.getId()).append(")\n");
                                } else {
                                    report.append("      Team ").append(j + 1).append(": NULL\n");
                                }
                            }
                        }
                    } else {
                        report.append("    ❌ Teams list is NULL!\n");
                    }

                    // Other properties
                    report.append("    Description: ").append(shift.getDescription()).append("\n");
                    //report.append("    User Relevant: ").append(shift .isUserRelevant()).append("\n");
                }
            } else {
                report.append("❌ Repository returned NULL WorkScheduleDay\n");
            }

            // Step 3: Test UseCase Level
            report.append("\nSTEP 2: UseCase Level\n");
            report.append("---------------------\n");

            try {
                GenerateUserScheduleUseCase useCase = serviceProvider.getCalendarServiceProvider().getUserScheduleUseCase();

                if (useCase != null) {
                    WorkScheduleDay useCaseDay = useCase.executeForDate(userId, testDate).join().getData();

                    if (useCaseDay != null) {
                        report.append("✅ UseCase returned WorkScheduleDay\n");
                        report.append("  Shifts count: ").append(useCaseDay.getShifts().size()).append("\n");

                        // Compare with repository
                        boolean dataMatches = compareWorkScheduleDays(repoDay, useCaseDay);
                        if (dataMatches) {
                            report.append("✅ UseCase data matches Repository data\n");
                        } else {
                            report.append("❌ UseCase data differs from Repository data\n");
                        }
                    } else {
                        report.append("❌ UseCase returned NULL WorkScheduleDay\n");
                    }
                } else {
                    report.append("❌ UseCase is NULL\n");
                }
            } catch (Exception e) {
                report.append("❌ UseCase test failed: ").append(e.getMessage()).append("\n");
            }

            // Step 4: Test SwipeCalendarModule Level
            report.append("\nSTEP 3: SwipeCalendarModule Level\n");
            report.append("----------------------------------\n");

            try {
                // Initialize SwipeCalendarModule the same way SwipeCalendarFragment does
                SwipeCalendarModule calendarModule = new SwipeCalendarModule(
                        context,
                        serviceProvider.getCalendarServiceProvider(),
                        serviceProvider.getEventsService(),
                        serviceProvider.getQDueUserService(),
                        repository
                );

                // Test data loading the same way MonthPagerAdapter does
                GenerateUserScheduleUseCase moduleUseCase = calendarModule.getUserScheduleUseCase();

                if (moduleUseCase != null) {
                    report.append("✅ SwipeCalendarModule UseCase available\n");

                    // Test month data loading (like AsyncDataLoader.loadWorkScheduleForMonth)
                    Map<LocalDate, WorkScheduleDay> monthData = moduleUseCase.executeForMonth(userId, testMonth).join().getData();

                    if (monthData != null) {
                        report.append("✅ Module month data loaded: ").append(monthData.size()).append(" days\n");

                        // Check our specific test date
                        WorkScheduleDay moduleDay = monthData.get(testDate);
                        if (moduleDay != null) {
                            report.append("✅ Test date found in month data\n");
                            report.append("  Module shifts: ").append(moduleDay.getShifts().size()).append("\n");

                            // This is the data that goes to the UI!
                            if (moduleDay.getShifts().isEmpty()) {
                                report.append("❌ CRITICAL: Module data has no shifts for UI!\n");
                            } else {
                                report.append("✅ Module data has shifts for UI\n");

                                // Check team assignments in module data
                                for (WorkScheduleShift shift : moduleDay.getShifts()) {
                                    if (shift.getTeams() == null || shift.getTeams().isEmpty()) {
                                        report.append("❌ CRITICAL: Module shift has no teams - UI won't display!\n");
                                    } else {
                                        report.append("✅ Module shift has ").append(shift.getTeams().size()).append(" teams\n");
                                    }
                                }
                            }
                        } else {
                            report.append("❌ Test date NOT found in month data\n");
                        }
                    } else {
                        report.append("❌ Module month data is NULL\n");
                    }
                } else {
                    report.append("❌ SwipeCalendarModule UseCase is NULL\n");
                }

            } catch (Exception e) {
                report.append("❌ SwipeCalendarModule test failed: ").append(e.getMessage()).append("\n");
            }

            // Step 5: Test user assignment chain
            report.append("\nSTEP 4: User Assignment Chain\n");
            report.append("-----------------------------\n");

            try {
                // Check user team assignment
                Team userTeam = repository.getTeamForUser(userId).join().getData();
                if (userTeam != null) {
                    report.append("✅ User ").append(userId).append(" assigned to team: ")
                            .append(userTeam.getName()).append(" (").append(userTeam.getId()).append(")\n");
                } else {
                    report.append("❌ User ").append(userId).append(" has no team assignment\n");
                }

                // Check if this team is working on test date
                if (userTeam != null) {
                    boolean teamWorking = repository.isWorkingDayForTeam(testDate, userTeam).join().getData();
                    report.append("Team ").append(userTeam.getName()).append(" working on ")
                            .append(testDate).append(": ").append(teamWorking).append("\n");
                }

            } catch (Exception e) {
                report.append("❌ User assignment check failed: ").append(e.getMessage()).append("\n");
            }

        } catch (Exception e) {
            report.append("❌ Complete data flow test failed: ").append(e.getMessage()).append("\n");
        }

        report.append("\n=== END UI DATA FLOW TEST ===\n");

        String finalReport = report.toString();
        Log.i(TAG, finalReport);

        return finalReport;
    }

    /**
     * Compare two WorkScheduleDay objects for differences
     */
    private static boolean compareWorkScheduleDays(WorkScheduleDay day1, WorkScheduleDay day2) {
        if (day1 == null && day2 == null) return true;
        if (day1 == null || day2 == null) return false;

        if (!day1.getDate().equals(day2.getDate())) return false;
        if (day1.getShifts().size() != day2.getShifts().size()) return false;

        // Simple comparison - could be more detailed
        return true;
    }

    /**
     * Test specifically what SwipeCalendarDayAdapter receives
     */
    public static String testDayAdapterData(Context context) {
        StringBuilder report = new StringBuilder();

        report.append("=== DAY ADAPTER DATA TEST ===\n");

        try {
            ServiceProvider serviceProvider = new ServiceProviderImpl(context);
            WorkScheduleRepository repository = serviceProvider.getWorkScheduleService();

            // Test multiple dates to see the pattern
            LocalDate startDate = LocalDate.now();

            report.append("Testing what DayAdapter would receive:\n");
            report.append("Date       | Shifts | Teams | UI Displayable?\n");
            report.append("-----------|--------|-------|----------------\n");

            for (int i = 0; i < 10; i++) {
                LocalDate testDate = startDate.plusDays(i);

                try {
                    String userID = QDuePreferences.getUserId( context );
                    WorkScheduleDay day = repository.getWorkScheduleForDate(testDate, userID).join().getData();

                    if (day != null && !day.getShifts().isEmpty()) {
                        for (WorkScheduleShift shift : day.getShifts()) {
                            shift.getTeams();
                            int teamCount = shift.getTeams().size();
                            boolean displayable = teamCount > 0;

                            report.append(String.format( QDue.getLocale(), "%-10s | %6d | %5d | %s\n",
                                    testDate, 1, teamCount, displayable ? "YES" : "NO"));
                        }
                    } else {
                        report.append(String.format(QDue.getLocale(), "%-10s | %6d | %5d | %s\n",
                                testDate, 0, 0, "NO (Rest)"));
                    }

                } catch (Exception e) {
                    report.append(String.format("%-10s | ERROR | %s\n", testDate, e.getMessage()));
                }
            }

        } catch (Exception e) {
            report.append("❌ Day adapter test failed: ").append(e.getMessage()).append("\n");
        }

        report.append("=== END DAY ADAPTER TEST ===\n");

        String finalReport = report.toString();
        Log.i(TAG + "_ADAPTER", finalReport);

        return finalReport;
    }

    /**
     * Easy integration: run this when calendar appears empty
     */
    public static void diagnoseUIDataFlow(Context context) {
        Log.w(TAG, "=== DIAGNOSING UI DATA FLOW ===");

        String flowResults = testCompleteDataFlow(context);
        String adapterResults = testDayAdapterData(context);

        // Determine likely cause
        if (flowResults.contains("NO TEAMS ASSIGNED")) {
            Log.e(TAG, "❌ LIKELY CAUSE: Shifts generated without teams - UI filtering them out");
        } else if (flowResults.contains("Module data has no shifts")) {
            Log.e(TAG, "❌ LIKELY CAUSE: Data lost in SwipeCalendarModule");
        } else if (adapterResults.contains("UI Displayable? NO")) {
            Log.e(TAG, "❌ LIKELY CAUSE: UI filtering logic too restrictive");
        } else {
            Log.i(TAG, "✅ Data flow appears correct - check UI rendering logic");
        }

        Log.w(TAG, "=== UI DATA FLOW DIAGNOSIS COMPLETE ===");
    }


    /**
     * Add this method to UIDataFlowTest class for quick database inspection
     */
    public static String dumpDatabaseTables(Context context) {
        StringBuilder result = new StringBuilder();
        result.append("=== DATABASE DUMP - UserScheduleAssignment & RecurrenceRule ===\n\n");

        try {
            ServiceProvider serviceProvider = ServiceProviderImpl.getInstance(context);

            // === RECURRENCE RULES TABLE ===
            result.append("📋 RECURRENCE RULES:\n");
            result.append("=".repeat(50)).append("\n");

            serviceProvider.getCalendarService()
                    .getAllRecurrenceRules()
                    .thenAccept(rulesResult -> {
                        if (rulesResult.isSuccess() && rulesResult.getData() != null) {
                            List<RecurrenceRule> rules = rulesResult.getData();
                            result.append("Total Rules: ").append(rules.size()).append("\n\n");

                            for (int i = 0; i < rules.size(); i++) {
                                RecurrenceRule rule = rules.get(i);
                                result.append(String.format("[%d] ID: %s\n", i+1, rule.getId()));
                                result.append("    Name: ").append(rule.getName()).append("\n");
                                result.append("    Frequency: ").append(rule.getFrequency()).append("\n");
                                result.append("    Interval: ").append(rule.getInterval()).append("\n");
                                result.append("    Cycle Length: ").append(rule.getCycleLength()).append("\n");
                                result.append("    Active: ").append(rule.isActive()).append("\n");
                                result.append("    Created: ").append(rule.getCreatedAt()).append("\n\n");
                            }
                        } else {
                            result.append("❌ No RecurrenceRules found or error: ")
                                    .append(rulesResult.getErrorMessage()).append("\n\n");
                        }
                    }).join();

            // === USER SCHEDULE ASSIGNMENTS TABLE ===
            result.append("\n👤 USER SCHEDULE ASSIGNMENTS:\n");
            result.append("=".repeat(50)).append("\n");

            // Get all active user IDs first
            serviceProvider.getCalendarServiceProvider()
                    .getUserScheduleAssignmentRepository()
                    .getAllActiveUserIds()
                    .thenAccept(userIdsResult -> {
                        if (userIdsResult.isSuccess() && userIdsResult.getData() != null) {
                            List<String> userIds = userIdsResult.getData();
                            result.append("Active Users: ").append(userIds.size()).append("\n");
                            result.append("User IDs: ").append(userIds.toString()).append("\n\n");

                            // For each user, get their assignments
                            for (String userId : userIds) {
                                result.append("USER ").append(userId).append(" ASSIGNMENTS:\n");
                                result.append("-".repeat(30)).append("\n");

                                serviceProvider.getCalendarServiceProvider()
                                        .getUserScheduleAssignmentRepository()
                                        .getActiveAssignmentsForUser(userId)
                                        .thenAccept(assignmentsResult -> {
                                            if (assignmentsResult.isSuccess() && assignmentsResult.getData() != null) {
                                                List<UserScheduleAssignment> assignments = assignmentsResult.getData();

                                                if (assignments.isEmpty()) {
                                                    result.append("  ❌ No active assignments\n\n");
                                                } else {
                                                    for (int i = 0; i < assignments.size(); i++) {
                                                        UserScheduleAssignment assignment = assignments.get(i);
                                                        result.append(String.format(QDue.getLocale(), "  [%d] ID: %s\n", i+1, assignment.getId()));
                                                        result.append("      Team: ").append(assignment.getTeamId())
                                                                .append(" (").append(assignment.getTeamName()).append(")\n");
                                                        result.append("      RecurrenceRule ID: ").append(assignment.getRecurrenceRuleId()).append("\n");
                                                        result.append("      Status: ").append(assignment.getStatus()).append("\n");
                                                        result.append("      Priority: ").append(assignment.getPriority()).append("\n");
                                                        result.append("      Start: ").append(assignment.getStartDate()).append("\n");
                                                        result.append("      End: ").append(assignment.getEndDate() != null ? assignment.getEndDate() : "PERMANENT").append("\n");
                                                        result.append("      Is Permanent: ").append(assignment.isPermanent()).append("\n");
                                                        result.append("      Title: ").append(assignment.getTitle()).append("\n\n");
                                                    }
                                                }
                                            } else {
                                                result.append("  ❌ Error getting assignments: ")
                                                        .append(assignmentsResult.getErrorMessage()).append("\n\n");
                                            }
                                        }).join();
                            }
                        } else {
                            result.append("❌ No active users found or error: ")
                                    .append(userIdsResult.getErrorMessage()).append("\n\n");
                        }
                    }).join();

            // === SPECIFIC CHECKS ===
            result.append("\n🔍 SPECIFIC CHECKS:\n");
            result.append("=".repeat(50)).append("\n");

            LocalDate today = LocalDate.now();
            String testUserId = QDuePreferences.getUserId( context );

            result.append("Testing getActiveAssignmentForUser(").append(testUserId)
                    .append(", ").append(today).append("):\n");

            serviceProvider.getCalendarServiceProvider()
                    .getUserScheduleAssignmentRepository()
                    .getActiveAssignmentForUser(testUserId, today)
                    .thenAccept(assignmentResult -> {
                        if (assignmentResult.isSuccess()) {
                            if (assignmentResult.getData() != null) {
                                UserScheduleAssignment assignment = assignmentResult.getData();
                                result.append("✅ FOUND Assignment:\n");
                                result.append("   ID: ").append(assignment.getId()).append("\n");
                                result.append("   Team: ").append(assignment.getTeamId()).append("\n");
                                result.append("   RecurrenceRule: ").append(assignment.getRecurrenceRuleId()).append("\n");
                                result.append("   Status: ").append(assignment.getStatus()).append("\n");
                            } else {
                                result.append("❌ NULL - No active assignment found for user ")
                                        .append(testUserId).append(" on ").append(today).append("\n");
                            }
                        } else {
                            result.append("❌ ERROR: ").append(assignmentResult.getErrorMessage()).append("\n");
                        }
                    }).join();

        } catch (Exception e) {
            result.append("❌ EXCEPTION during database dump: ").append(e.getMessage()).append("\n");
            Log.e("DatabaseDump", "Error during dump", e);
        }

        result.append("\n=== END DATABASE DUMP ===\n");
        return result.toString();
    }

    /**
     * Quick method to run database dump and log results
     * Call this from any Activity or add to debug menu
     */
    public static void debugDatabaseTables(Context context) {
        new Thread(() -> {
            String dumpResult = dumpDatabaseTables(context);
            Log.v("DATABASE_DUMP", dumpResult);

            // Also print to System.out for easier reading in logcat
            System.out.println("\n" + dumpResult);
        }).start();
    }


    /**
     * Test to directly compare the two paths: real assignment vs default assignment
     */
    public static String testDirectPathComparison(Context context) {
        StringBuilder result = new StringBuilder();
        result.append("=== DIRECT PATH COMPARISON TEST ===\n\n");

        try {
            ServiceProvider serviceProvider = ServiceProviderImpl.getInstance(context);
            WorkScheduleRepository repo = serviceProvider.getCalendarServiceProvider().getWorkScheduleRepository();

            LocalDate testDate = LocalDate.of(2025, 8, 28); // Use a work day
            String userId = QDuePreferences.getUserId( context );

            result.append("Testing date: ").append(testDate).append("\n");
            result.append("User ID: ").append(userId).append("\n\n");

            // PATH A: Force assignment creation (simulate assignment == null)
            result.append("🔍 PATH A: Default Assignment (createDefaultQuattroDueAssignment)\n");
            result.append("=".repeat(60)).append("\n");

            try {
                // Call the method directly with null assignment to force default creation
                WorkScheduleDay pathAResult = repo.getWorkScheduleForDate(testDate, null).join().getData(); // null user forces default

                if (pathAResult != null) {
                    result.append("✅ Path A generated WorkScheduleDay\n");
                    result.append("   Shifts count: ").append(pathAResult.getShifts().size()).append("\n");

                    for (int i = 0; i < pathAResult.getShifts().size(); i++) {
                        WorkScheduleShift shift = pathAResult.getShifts().get(i);
                        result.append("   [").append(i).append("] ").append(shift.getShift().getName())
                                .append(" (").append(shift.getStartTime()).append("-").append(shift.getEndTime()).append(")\n");
                        result.append("       Teams: ").append(shift.getTeams() != null ? shift.getTeams().size() : "null").append("\n");
                    }
                } else {
                    result.append("❌ Path A returned null\n");
                }

            } catch (Exception e) {
                result.append("❌ Path A error: ").append(e.getMessage()).append("\n");
            }

            // PATH B: Real assignment from database
            result.append("\n🔍 PATH B: Real Assignment (from database)\n");
            result.append("=".repeat(60)).append("\n");

            try {
                WorkScheduleDay pathBResult = repo.getWorkScheduleForDate(testDate, userId).join().getData();

                if (pathBResult != null) {
                    result.append("✅ Path B generated WorkScheduleDay\n");
                    result.append("   Shifts count: ").append(pathBResult.getShifts().size()).append("\n");

                    if (pathBResult.getShifts().isEmpty()) {
                        result.append("❌ PATH B HAS ZERO SHIFTS - This is the UI problem!\n");
                    } else {
                        for (int i = 0; i < pathBResult.getShifts().size(); i++) {
                            WorkScheduleShift shift = pathBResult.getShifts().get(i);
                            result.append("   [").append(i).append("] ").append(shift.getShift().getName())
                                    .append(" (").append(shift.getStartTime()).append("-").append(shift.getEndTime()).append(")\n");
                            result.append("       Teams: ").append(shift.getTeams() != null ? shift.getTeams().size() : "null").append("\n");
                        }
                    }
                } else {
                    result.append("❌ Path B returned null\n");
                }

            } catch (Exception e) {
                result.append("❌ Path B error: ").append(e.getMessage()).append("\n");
            }

            // COMPARISON
            result.append("\n📊 COMPARISON ANALYSIS\n");
            result.append("=".repeat(60)).append("\n");
            result.append("This test shows exactly where the data diverges.\n");
            result.append("If Path A has shifts but Path B doesn't, the problem is in:\n");
            result.append("- Assignment properties causing different behavior\n");
            result.append("- Exception handling removing shifts only from real assignments\n");
            result.append("- RecurrenceCalculator treating them differently\n");

        } catch (Exception e) {
            result.append("❌ Test failed: ").append(e.getMessage()).append("\n");
        }

        result.append("\n=== END DIRECT PATH COMPARISON ===\n");
        return result.toString();
    }
}