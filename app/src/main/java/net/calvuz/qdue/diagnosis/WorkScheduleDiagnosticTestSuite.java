package net.calvuz.qdue.diagnosis;

import static net.calvuz.qdue.core.common.Lib.isDebugBuild;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;

import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.core.di.impl.ServiceProviderImpl;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.di.CalendarServiceProvider;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.domain.calendar.usecases.GenerateUserScheduleUseCase;
import net.calvuz.qdue.quattrodue.Preferences;
import net.calvuz.qdue.quattrodue.Costants;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * WorkScheduleDiagnosticTestSuite - Comprehensive Diagnostic Tests
 *
 * <p>Test suite specifically designed to diagnose the "empty calendar" problem
 * by testing each component in the work schedule generation chain step-by-step.</p>
 *
 * <h3>Test Categories:</h3>
 * <ul>
 *   <li><strong>Level 1</strong>: Basic configuration (preferences, constants)</li>
 *   <li><strong>Level 2</strong>: Database integrity (shifts, teams, assignments)</li>
 *   <li><strong>Level 3</strong>: Repository operations</li>
 *   <li><strong>Level 4</strong>: Use case execution</li>
 *   <li><strong>Level 5</strong>: End-to-end data flow</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Diagnostic Suite
 */
public class WorkScheduleDiagnosticTestSuite {

    private static final String TAG = "WorkScheduleDiagnostic";

    // Test configuration
    private static final Long TEST_USER_ID = 1L;
    private static final LocalDate TEST_DATE = LocalDate.now();
    private static final YearMonth TEST_MONTH = YearMonth.from(TEST_DATE);

    // Components under test
    private Context mContext;
    private ServiceProvider mServiceProvider;
    private WorkScheduleRepository mWorkScheduleRepository;
    private GenerateUserScheduleUseCase mGenerateUserScheduleUseCase;

    // Test results tracking
    private final StringBuilder mTestResults = new StringBuilder();
    private int mTestsPassed = 0;
    private int mTestsFailed = 0;


    /**
     * Add debug menu option to run diagnostics (for debug builds)
     */
    public static void addDebugMenuOption(Activity activity, android.view.Menu menu) {
        // Only add in debug builds
        if (isDebugBuild(activity)) {
            menu.add(0, 66666, 0, "🔧 WorkSchedule Diag")
                    .setOnMenuItemClickListener(item -> {
                        // Test completo del pattern
                        new Thread(() -> {
                            String results = WorkScheduleDiagnosticTestSuite.runDiagnostics(activity.getApplicationContext());
                            android.util.Log.v("WORKSCHEDULEDIAG", results);
                        }).start();
                        return true;
                    });
        }
    }


    /**
     * Initialize diagnostic test suite
     */
    public WorkScheduleDiagnosticTestSuite(@NonNull Context context) {
        this.mContext = context;
        this.mServiceProvider = new ServiceProviderImpl(context);
        this.mWorkScheduleRepository = mServiceProvider.getWorkScheduleService();

        // Initialize use case if available
        try {
            CalendarServiceProvider calendarServiceProvider = mServiceProvider.getCalendarServiceProvider();
            this.mGenerateUserScheduleUseCase = calendarServiceProvider.getUseCaseFactory().getUserScheduleUseCase();
        } catch (Exception e) {
            logError("Failed to initialize GenerateUserScheduleUseCase: " + e.getMessage());
        }

        logInfo("=== WORKSCHEDULE DIAGNOSTIC TEST SUITE INITIALIZED ===");
    }

    /**
     * Run all diagnostic tests in sequence
     */
    public String runAllDiagnosticTests() {
        logInfo("Starting comprehensive WorkSchedule diagnostic tests...");

        // Level 1: Basic Configuration Tests
        testLevel1_BasicConfiguration();

        // Level 2: Database Integrity Tests
        testLevel2_DatabaseIntegrity();

        // Level 3: Repository Operation Tests
        testLevel3_RepositoryOperations();

        // Level 4: Use Case Execution Tests
        testLevel4_UseCaseExecution();

        // Level 5: End-to-End Data Flow Tests
        testLevel5_EndToEndDataFlow();

        // Generate final report
        return generateDiagnosticReport();
    }

    // ==================== LEVEL 1: BASIC CONFIGURATION TESTS ====================

    private void testLevel1_BasicConfiguration() {
        logInfo(">>> LEVEL 1: Basic Configuration Tests");

        testPreferencesAccess();
        testSchemeStartDate();
        testDefaultConstants();
        testServiceProviderInitialization();
    }

    private void testPreferencesAccess() {
        try {
            // Test basic preferences reading
            boolean showStops = Preferences.getSharedPreference(mContext,
                    Preferences.KEY_SHOW_STOPS, Preferences.VALUE_SHOW_STOPS);

            String userTeam = Preferences.getSharedPreference(mContext,
                    Preferences.KEY_USER_TEAM, "0");

            logPass("✅ Preferences accessible - showStops: " + showStops + ", userTeam: " + userTeam);

        } catch (Exception e) {
            logFail("❌ Preferences access failed: " + e.getMessage());
        }
    }

    private void testSchemeStartDate() {
        try {
            // Test scheme start date reading
            LocalDate schemeDate = Preferences.getSchemeStartDate(mContext);

            // Test components
            int day = Preferences.getSharedPreference(mContext,
                    Preferences.KEY_SCHEME_START_DAY, Costants.QD_SCHEME_START_DAY);
            int month = Preferences.getSharedPreference(mContext,
                    Preferences.KEY_SCHEME_START_MONTH, Costants.QD_SCHEME_START_MONTH);
            int year = Preferences.getSharedPreference(mContext,
                    Preferences.KEY_SCHEME_START_YEAR, Costants.QD_SCHEME_START_YEAR);

            logPass("✅ Scheme start date: " + schemeDate + " (components: " + day + "/" + month + "/" + year + ")");

            // Validate against constants
            if (day == Costants.QD_SCHEME_START_DAY &&
                    month == Costants.QD_SCHEME_START_MONTH &&
                    year == Costants.QD_SCHEME_START_YEAR) {
                logPass("✅ Scheme date matches default constants (7/11/2018)");
            } else {
                logWarn("⚠️ Scheme date differs from constants - using user-defined: " + schemeDate);
            }

        } catch (Exception e) {
            logFail("❌ Scheme start date reading failed: " + e.getMessage());
        }
    }

    private void testDefaultConstants() {
        try {
            logInfo("📋 Default Constants Check:");
            logInfo("  QD_SCHEME_START_DAY: " + Costants.QD_SCHEME_START_DAY);
            logInfo("  QD_SCHEME_START_MONTH: " + Costants.QD_SCHEME_START_MONTH);
            logInfo("  QD_SCHEME_START_YEAR: " + Costants.QD_SCHEME_START_YEAR);
            logInfo("  QD_SHIFTS_PER_DAY: " + Costants.QD_SHIFTS_PER_DAY);
            logInfo("  QD_TEAMS: " + Costants.QD_TEAMS);

            logPass("✅ Constants accessible");

        } catch (Exception e) {
            logFail("❌ Constants access failed: " + e.getMessage());
        }
    }

    private void testServiceProviderInitialization() {
        try {
            boolean servicesReady = mServiceProvider.areServicesReady();

            logInfo("📋 ServiceProvider Status:");
            logInfo("  Services Ready: " + servicesReady);
            logInfo("  WorkScheduleRepository: " + (mWorkScheduleRepository != null ? "✅" : "❌"));
            logInfo("  GenerateUserScheduleUseCase: " + (mGenerateUserScheduleUseCase != null ? "✅" : "❌"));

            if (servicesReady && mWorkScheduleRepository != null) {
                logPass("✅ ServiceProvider initialized correctly");
            } else {
                logFail("❌ ServiceProvider initialization incomplete");
            }

        } catch (Exception e) {
            logFail("❌ ServiceProvider test failed: " + e.getMessage());
        }
    }

    // ==================== LEVEL 2: DATABASE INTEGRITY TESTS ====================

    private void testLevel2_DatabaseIntegrity() {
        logInfo(">>> LEVEL 2: Database Integrity Tests");

        testDatabaseConnection();
        testShiftTypesExistence();
        testTeamsExistence();
        testUserAssignments();
    }

    private void testDatabaseConnection() {
        try {
            // Test repository readiness
            CompletableFuture<OperationResult<Boolean>> readyFuture =
                    mWorkScheduleRepository.isRepositoryReady();

            OperationResult<Boolean> readyResult = readyFuture.join();

            if (readyResult.isSuccess() && readyResult.getData()) {
                logPass("✅ Database connection successful");
            } else {
                logFail("❌ Database connection failed: " + readyResult.getErrorMessage());
            }

        } catch (Exception e) {
            logFail("❌ Database connection test failed: " + e.getMessage());
        }
    }

    private void testShiftTypesExistence() {
        try {
            CompletableFuture<OperationResult<java.util.List<net.calvuz.qdue.domain.calendar.models.Shift>>>
                    shiftsFuture = mWorkScheduleRepository.getAllShifts();

            OperationResult<java.util.List<net.calvuz.qdue.domain.calendar.models.Shift>>
                    shiftsResult = shiftsFuture.join();

            if (shiftsResult.isSuccess() && shiftsResult.getData() != null) {
                int shiftCount = shiftsResult.getData().size();
                logPass("✅ Found " + shiftCount + " shift types in database");

                // List shift details
                for (var shift : shiftsResult.getData()) {
                    logInfo("  Shift: " + shift.getName() + " (" + shift.getId() + ")");
                }

                if (shiftCount < 3) {
                    logWarn("⚠️ Expected at least 3 shifts (Morning, Afternoon, Night), found: " + shiftCount);
                }

            } else {
                logFail("❌ No shift types found in database: " + shiftsResult.getErrorMessage());
            }

        } catch (Exception e) {
            logFail("❌ Shift types test failed: " + e.getMessage());
        }
    }

    private void testTeamsExistence() {
        try {
            CompletableFuture<OperationResult<java.util.List<net.calvuz.qdue.domain.calendar.models.Team>>>
                    teamsFuture = mWorkScheduleRepository.getAllTeams();

            OperationResult<java.util.List<net.calvuz.qdue.domain.calendar.models.Team>>
                    teamsResult = teamsFuture.join();

            if (teamsResult.isSuccess() && teamsResult.getData() != null) {
                int teamCount = teamsResult.getData().size();
                logPass("✅ Found " + teamCount + " teams in database");

                // List team details
                for (var team : teamsResult.getData()) {
                    logInfo("  Team: " + team.getName() + " (" + team.getId() + ")");
                }

                if (teamCount < 9) {
                    logWarn("⚠️ Expected 9 QuattroDue teams (A-I), found: " + teamCount);
                }

            } else {
                logFail("❌ No teams found in database: " + teamsResult.getErrorMessage());
            }

        } catch (Exception e) {
            logFail("❌ Teams test failed: " + e.getMessage());
        }
    }

    private void testUserAssignments() {
        try {
            CompletableFuture<OperationResult<net.calvuz.qdue.domain.calendar.models.Team>>
                    userTeamFuture = mWorkScheduleRepository.getTeamForUser(TEST_USER_ID);

            OperationResult<net.calvuz.qdue.domain.calendar.models.Team>
                    userTeamResult = userTeamFuture.join();

            if (userTeamResult.isSuccess() && userTeamResult.getData() != null) {
                var team = userTeamResult.getData();
                logPass("✅ User " + TEST_USER_ID + " assigned to team: " + team.getName() + " (" + team.getId() + ")");
            } else {
                logWarn("⚠️ User " + TEST_USER_ID + " has no team assignment: " + userTeamResult.getErrorMessage());
                logInfo("  This could be the root cause of empty calendar!");
            }

        } catch (Exception e) {
            logFail("❌ User assignment test failed: " + e.getMessage());
        }
    }

    // ==================== LEVEL 3: REPOSITORY OPERATION TESTS ====================

    private void testLevel3_RepositoryOperations() {
        logInfo(">>> LEVEL 3: Repository Operation Tests");

        testSchemeStartDateFromRepository();
        testDayInCycleCalculation();
        testSingleDateScheduleGeneration();
    }

    private void testSchemeStartDateFromRepository() {
        try {
            CompletableFuture<OperationResult<LocalDate>> schemeFuture =
                    mWorkScheduleRepository.getSchemeStartDate();

            OperationResult<LocalDate> schemeResult = schemeFuture.join();

            if (schemeResult.isSuccess() && schemeResult.getData() != null) {
                LocalDate repositorySchemeDate = schemeResult.getData();
                LocalDate preferencesSchemeDate = Preferences.getSchemeStartDate(mContext);

                logPass("✅ Repository scheme start date: " + repositorySchemeDate);
                logInfo("  Preferences scheme start date: " + preferencesSchemeDate);

                if (repositorySchemeDate.equals(preferencesSchemeDate)) {
                    logPass("✅ Repository and preferences scheme dates match");
                } else {
                    logFail("❌ SCHEME DATE MISMATCH! Repository: " + repositorySchemeDate +
                            ", Preferences: " + preferencesSchemeDate);
                }

            } else {
                logFail("❌ Repository scheme start date failed: " + schemeResult.getErrorMessage());
            }

        } catch (Exception e) {
            logFail("❌ Repository scheme date test failed: " + e.getMessage());
        }
    }

    private void testDayInCycleCalculation() {
        try {
            CompletableFuture<OperationResult<Integer>> dayInCycleFuture =
                    mWorkScheduleRepository.getDayInCycle(TEST_DATE);

            OperationResult<Integer> dayInCycleResult = dayInCycleFuture.join();

            if (dayInCycleResult.isSuccess() && dayInCycleResult.getData() != null) {
                int dayInCycle = dayInCycleResult.getData();
                logPass("✅ Day in cycle for " + TEST_DATE + ": " + dayInCycle + " (0-17 range)");

                if (dayInCycle >= 0 && dayInCycle <= 17) {
                    logPass("✅ Day in cycle is within valid range");
                } else {
                    logFail("❌ Day in cycle out of range: " + dayInCycle);
                }

            } else {
                logFail("❌ Day in cycle calculation failed: " + dayInCycleResult.getErrorMessage());
            }

        } catch (Exception e) {
            logFail("❌ Day in cycle test failed: " + e.getMessage());
        }
    }

    private void testSingleDateScheduleGeneration() {
        try {
            CompletableFuture<OperationResult<WorkScheduleDay>> scheduleFuture =
                    mWorkScheduleRepository.getWorkScheduleForDate(TEST_DATE, TEST_USER_ID);

            OperationResult<WorkScheduleDay> scheduleResult = scheduleFuture.join();

            if (scheduleResult.isSuccess() && scheduleResult.getData() != null) {
                WorkScheduleDay schedule = scheduleResult.getData();
                int shiftCount = schedule.getShifts().size();

                if (shiftCount > 0) {
                    logPass("✅ Generated schedule for " + TEST_DATE + " with " + shiftCount + " shifts");

                    // List shifts
                    for (var shift : schedule.getShifts()) {
                        logInfo("  Shift: " + shift.getShift().getName() + " with " +
                                shift.getTeams().size() + " teams");
                    }
                } else {
                    logFail("❌ EMPTY SCHEDULE! No shifts generated for " + TEST_DATE);
                    logInfo("  This is the smoking gun! Schedule generation is failing.");
                }

            } else {
                logFail("❌ Schedule generation failed: " + scheduleResult.getErrorMessage());
            }

        } catch (Exception e) {
            logFail("❌ Single date schedule test failed: " + e.getMessage());
        }
    }

    // ==================== LEVEL 4: USE CASE EXECUTION TESTS ====================

    private void testLevel4_UseCaseExecution() {
        logInfo(">>> LEVEL 4: Use Case Execution Tests");

        if (mGenerateUserScheduleUseCase == null) {
            logFail("❌ GenerateUserScheduleUseCase not available - skipping Level 4 tests");
            return;
        }

        testUseCaseForSingleDate();
        testUseCaseForMonth();
    }

    private void testUseCaseForSingleDate() {
        try {
            CompletableFuture<OperationResult<WorkScheduleDay>> useCaseFuture =
                    mGenerateUserScheduleUseCase.executeForDate(TEST_USER_ID, TEST_DATE);

            OperationResult<WorkScheduleDay> useCaseResult = useCaseFuture.join();

            if (useCaseResult.isSuccess() && useCaseResult.getData() != null) {
                WorkScheduleDay schedule = useCaseResult.getData();
                int shiftCount = schedule.getShifts().size();

                logPass("✅ Use case generated schedule for " + TEST_DATE + " with " + shiftCount + " shifts");
            } else {
                logFail("❌ Use case failed for single date: " + useCaseResult.getErrorMessage());
            }

        } catch (Exception e) {
            logFail("❌ Use case single date test failed: " + e.getMessage());
        }
    }

    private void testUseCaseForMonth() {
        try {
            CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> useCaseFuture =
                    mGenerateUserScheduleUseCase.executeForMonth(TEST_USER_ID, TEST_MONTH);

            OperationResult<Map<LocalDate, WorkScheduleDay>> useCaseResult = useCaseFuture.join();

            if (useCaseResult.isSuccess() && useCaseResult.getData() != null) {
                Map<LocalDate, WorkScheduleDay> monthSchedule = useCaseResult.getData();
                int daysWithShifts = 0;
                int totalShifts = 0;

                for (WorkScheduleDay daySchedule : monthSchedule.values()) {
                    if (!daySchedule.getShifts().isEmpty()) {
                        daysWithShifts++;
                        totalShifts += daySchedule.getShifts().size();
                    }
                }

                logPass("✅ Use case generated month schedule: " + monthSchedule.size() + " days, " +
                        daysWithShifts + " days with shifts, " + totalShifts + " total shifts");

                if (daysWithShifts == 0) {
                    logFail("❌ CRITICAL: Use case generated month with NO working days!");
                }

            } else {
                logFail("❌ Use case failed for month: " + useCaseResult.getErrorMessage());
            }

        } catch (Exception e) {
            logFail("❌ Use case month test failed: " + e.getMessage());
        }
    }

    // ==================== LEVEL 5: END-TO-END DATA FLOW TESTS ====================

    private void testLevel5_EndToEndDataFlow() {
        logInfo(">>> LEVEL 5: End-to-End Data Flow Tests");

        testSwipeCalendarDataLoader();
        testCompleteWorkflow();
    }

    private void testSwipeCalendarDataLoader() {
        logInfo("🔄 Testing SwipeCalendar data loading workflow...");

        // Simulate SwipeCalendarModule.AsyncDataLoader.loadWorkScheduleForMonth()
        try {
            if (mGenerateUserScheduleUseCase != null) {
                CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> future =
                        mGenerateUserScheduleUseCase.executeForMonth(TEST_USER_ID, TEST_MONTH);

                OperationResult<Map<LocalDate, WorkScheduleDay>> result = future.join();

                if (result.isSuccess() && result.getData() != null) {
                    Map<LocalDate, WorkScheduleDay> scheduleMap = result.getData();
                    logPass("✅ SwipeCalendar data loading simulation: " + scheduleMap.size() + " days loaded");

                    // Check if any days have shifts
                    long daysWithShifts = scheduleMap.values().stream()
                            .mapToLong(day -> day.getShifts().isEmpty() ? 0 : 1)
                            .sum();

                    if (daysWithShifts > 0) {
                        logPass("✅ Found " + daysWithShifts + " days with shifts");
                    } else {
                        logFail("❌ CALENDAR EMPTY: No days with shifts found in month data!");
                    }

                } else {
                    logFail("❌ SwipeCalendar data loading failed: " + result.getErrorMessage());
                }
            } else {
                logFail("❌ Cannot test SwipeCalendar data loading - UseCase not available");
            }

        } catch (Exception e) {
            logFail("❌ SwipeCalendar data loading test failed: " + e.getMessage());
        }
    }

    private void testCompleteWorkflow() {
        logInfo("🔄 Testing complete workflow: Preferences → Repository → UseCase → UI Data");

        try {
            // Step 1: Preferences
            LocalDate prefSchemeDate = Preferences.getSchemeStartDate(mContext);
            logInfo("  Step 1 - Preferences scheme date: " + prefSchemeDate);

            // Step 2: Repository scheme date
            LocalDate repoSchemeDate = mWorkScheduleRepository.getSchemeStartDate().join().getData();
            logInfo("  Step 2 - Repository scheme date: " + repoSchemeDate);

            // Step 3: Day in cycle calculation
            int dayInCycle = mWorkScheduleRepository.getDayInCycle(TEST_DATE).join().getData();
            logInfo("  Step 3 - Day in cycle for " + TEST_DATE + ": " + dayInCycle);

            // Step 4: Repository schedule generation
            WorkScheduleDay repoSchedule = mWorkScheduleRepository.getWorkScheduleForDate(TEST_DATE, TEST_USER_ID).join().getData();
            int repoShiftCount = repoSchedule != null ? repoSchedule.getShifts().size() : 0;
            logInfo("  Step 4 - Repository generated " + repoShiftCount + " shifts");

            // Step 5: UseCase schedule generation (if available)
            if (mGenerateUserScheduleUseCase != null) {
                WorkScheduleDay useCaseSchedule = mGenerateUserScheduleUseCase.executeForDate(TEST_USER_ID, TEST_DATE).join().getData();
                int useCaseShiftCount = useCaseSchedule != null ? useCaseSchedule.getShifts().size() : 0;
                logInfo("  Step 5 - UseCase generated " + useCaseShiftCount + " shifts");

                if (repoShiftCount == useCaseShiftCount && repoShiftCount > 0) {
                    logPass("✅ Complete workflow successful: " + repoShiftCount + " shifts generated consistently");
                } else if (repoShiftCount == 0 && useCaseShiftCount == 0) {
                    logFail("❌ Complete workflow failed: Both Repository and UseCase generated 0 shifts");
                } else {
                    logWarn("⚠️ Inconsistent results: Repository=" + repoShiftCount + ", UseCase=" + useCaseShiftCount);
                }
            } else {
                if (repoShiftCount > 0) {
                    logPass("✅ Repository workflow successful: " + repoShiftCount + " shifts generated");
                } else {
                    logFail("❌ Repository workflow failed: 0 shifts generated");
                }
            }

        } catch (Exception e) {
            logFail("❌ Complete workflow test failed: " + e.getMessage());
        }
    }

    // ==================== UTILITY METHODS ====================

    private void logPass(String message) {
        mTestsPassed++;
        mTestResults.append("✅ ").append(message).append("\n");
        Log.d(TAG, message);
    }

    private void logFail(String message) {
        mTestsFailed++;
        mTestResults.append("❌ ").append(message).append("\n");
        Log.e(TAG, message);
    }

    private void logWarn(String message) {
        mTestResults.append("⚠️ ").append(message).append("\n");
        Log.w(TAG, message);
    }

    private void logInfo(String message) {
        mTestResults.append("ℹ️ ").append(message).append("\n");
        Log.i(TAG, message);
    }

    private void logError(String message) {
        mTestResults.append("💥 ").append(message).append("\n");
        Log.e(TAG, message);
    }

    private String generateDiagnosticReport() {
        StringBuilder report = new StringBuilder();

        report.append("=".repeat(60)).append("\n");
        report.append("WORKSCHEDULE DIAGNOSTIC TEST RESULTS\n");
        report.append("=".repeat(60)).append("\n");
        report.append("Tests Passed: ").append(mTestsPassed).append("\n");
        report.append("Tests Failed: ").append(mTestsFailed).append("\n");
        report.append("Overall Status: ").append(mTestsFailed == 0 ? "✅ PASS" : "❌ FAIL").append("\n");
        report.append("=".repeat(60)).append("\n\n");

        report.append("DETAILED RESULTS:\n");
        report.append("-".repeat(40)).append("\n");
        report.append(mTestResults.toString());

        report.append("\n").append("=".repeat(60)).append("\n");
        report.append("END OF DIAGNOSTIC REPORT\n");
        report.append("=".repeat(60)).append("\n");

        String fullReport = report.toString();
        Log.i(TAG, fullReport);

        return fullReport;
    }

    /**
     * Static method to run diagnostics easily from anywhere
     */
    public static String runDiagnostics(@NonNull Context context) {
        WorkScheduleDiagnosticTestSuite suite = new WorkScheduleDiagnosticTestSuite(context);
        return suite.runAllDiagnosticTests();
    }
}