package net.calvuz.qdue.ui.features.schedulepattern.integration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import net.calvuz.qdue.domain.calendar.engines.extensions.RecurrenceCalculatorExtensions;
import net.calvuz.qdue.domain.calendar.extensions.RecurrenceRuleExtensions;
import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.domain.calendar.models.Shift;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.common.enums.Priority;
import net.calvuz.qdue.ui.features.schedulepattern.models.PatternDay;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * CustomPatternIntegrationTest - Integration tests for custom pattern system
 *
 * <p>Tests the complete integration between PatternDay, RecurrenceRuleExtensions,
 * and RecurrenceCalculatorExtensions to ensure the custom pattern system
 * works end-to-end correctly.</p>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Integration Test Implementation
 * @since Clean Architecture Phase 2
 */
@RunWith (MockitoJUnitRunner.class)
public class CustomPatternIntegrationTest {

    @Mock
    private Shift mockMorningShift;

    @Mock
    private Shift mockNightShift;

    private LocalDate testStartDate;
    private String userID;

    @Before
    public void setUp() {
        // Setup userID
        userID = UUID.randomUUID().toString();

        // Setup test date
        testStartDate = LocalDate.of( 2024, 1, 1 );

        // Setup mock shifts
        when( mockMorningShift.getId() ).thenReturn( "morning_shift_id" );
        when( mockMorningShift.getName() ).thenReturn( "Morning Shift" );
        when( mockMorningShift.getStartTime() ).thenReturn( LocalTime.of( 6, 0 ) );
        when( mockMorningShift.getEndTime() ).thenReturn( LocalTime.of( 14, 0 ) );

        when( mockNightShift.getId() ).thenReturn( "night_shift_id" );
        when( mockNightShift.getName() ).thenReturn( "Night Shift" );
        when( mockNightShift.getStartTime() ).thenReturn( LocalTime.of( 22, 0 ) );
        when( mockNightShift.getEndTime() ).thenReturn( LocalTime.of( 6, 0 ) );
    }

    // ==================== FULL CYCLE INTEGRATION TESTS ====================

    @Test
    public void testFullCycle_PatternDayToRecurrenceRuleAndBack() {
        // Create original pattern: 2 work days, 1 rest day
        List<PatternDay> originalPattern = Arrays.asList(
                new PatternDay( 1, mockMorningShift ),
                new PatternDay( 2, mockNightShift ),
                new PatternDay( 3, null ) // Rest day
        );

        // Convert to RecurrenceRule
        RecurrenceRule recurrenceRule = RecurrenceRuleExtensions.createCustomPattern(
                "Test Pattern", "Test description", originalPattern, testStartDate );

        // Verify RecurrenceRule properties
        assertNotNull( recurrenceRule );
        assertEquals( "Test Pattern", recurrenceRule.getName() );
        assertEquals( RecurrenceRule.Frequency.QUATTRODUE_CYCLE, recurrenceRule.getFrequency() );
        assertEquals( 3, recurrenceRule.getCycleLength().intValue() );
        assertEquals( testStartDate, recurrenceRule.getStartDate() );
        assertTrue( RecurrenceRuleExtensions.isCustomPattern( recurrenceRule ) );

        // Extract back to PatternDays
        List<PatternDay> extractedPattern = RecurrenceRuleExtensions.extractPatternDays( recurrenceRule );

        // Verify extracted pattern matches original
        assertEquals( originalPattern.size(), extractedPattern.size() );

        for (int i = 0; i < originalPattern.size(); i++) {
            PatternDay original = originalPattern.get( i );
            PatternDay extracted = extractedPattern.get( i );

            assertEquals( original.getDayNumber(), extracted.getDayNumber() );
            assertEquals( original.isRestDay(), extracted.isRestDay() );
            assertEquals( original.isWorkDay(), extracted.isWorkDay() );

            if (original.isWorkDay()) {
                assertNotNull( extracted.getShift() );
                assertEquals( original.getShift().getId(), extracted.getShift().getId() );
                assertEquals( original.getShift().getName(), extracted.getShift().getName() );
            } else {
                assertNull( extracted.getShift() );
            }
        }
    }

    @Test
    public void testScheduleGeneration_ForSingleDate() {
        // Create 4-day pattern: work, work, rest, work
        List<PatternDay> pattern = Arrays.asList(
                new PatternDay( 1, mockMorningShift ),  // Day 1: Morning
                new PatternDay( 2, mockNightShift ),    // Day 2: Night
                new PatternDay( 3, null ),              // Day 3: Rest
                new PatternDay( 4, mockMorningShift )   // Day 4: Morning
        );

        // Create RecurrenceRule
        RecurrenceRule recurrenceRule = RecurrenceRuleExtensions.createCustomPattern(
                "4-Day Pattern", pattern, testStartDate );

        // Create UserScheduleAssignment
        UserScheduleAssignment assignment = UserScheduleAssignment.builder()
                .id( "test_assignment" )
                .userId( userID )
                .teamId( "test_team" )
                .recurrenceRuleId( recurrenceRule.getId() )
                .startDate( testStartDate )
                .priority( Priority.NORMAL )
                .build();

        // Test different dates in the cycle

        // Day 1 (pattern day 1): Should have morning shift
        LocalDate day1 = testStartDate;
        List<Shift> day1Shifts = RecurrenceCalculatorExtensions.calculateCustomPatternShiftsForDate(
                day1, recurrenceRule, assignment );
        assertEquals( 1, day1Shifts.size() );
        assertEquals( "morning_shift_id", day1Shifts.get( 0 ).getId() );

        // Day 2 (pattern day 2): Should have night shift
        LocalDate day2 = testStartDate.plusDays( 1 );
        List<Shift> day2Shifts = RecurrenceCalculatorExtensions.calculateCustomPatternShiftsForDate(
                day2, recurrenceRule, assignment );
        assertEquals( 1, day2Shifts.size() );
        assertEquals( "night_shift_id", day2Shifts.get( 0 ).getId() );

        // Day 3 (pattern day 3): Should be rest (no shifts)
        LocalDate day3 = testStartDate.plusDays( 2 );
        List<Shift> day3Shifts = RecurrenceCalculatorExtensions.calculateCustomPatternShiftsForDate(
                day3, recurrenceRule, assignment );
        assertEquals( 0, day3Shifts.size() );

        // Day 4 (pattern day 4): Should have morning shift
        LocalDate day4 = testStartDate.plusDays( 3 );
        List<Shift> day4Shifts = RecurrenceCalculatorExtensions.calculateCustomPatternShiftsForDate(
                day4, recurrenceRule, assignment );
        assertEquals( 1, day4Shifts.size() );
        assertEquals( "morning_shift_id", day4Shifts.get( 0 ).getId() );

        // Day 5 (pattern repeats, back to day 1): Should have morning shift
        LocalDate day5 = testStartDate.plusDays( 4 );
        List<Shift> day5Shifts = RecurrenceCalculatorExtensions.calculateCustomPatternShiftsForDate(
                day5, recurrenceRule, assignment );
        assertEquals( 1, day5Shifts.size() );
        assertEquals( "morning_shift_id", day5Shifts.get( 0 ).getId() );
    }

    @Test
    public void testScheduleGeneration_ForDateRange() {
        // Create simple 2-day pattern: work, rest
        List<PatternDay> pattern = Arrays.asList(
                new PatternDay( 1, mockMorningShift ),
                new PatternDay( 2, null )
        );

        RecurrenceRule recurrenceRule = RecurrenceRuleExtensions.createCustomPattern(
                "Work-Rest Pattern", pattern, testStartDate );

        UserScheduleAssignment assignment = UserScheduleAssignment.builder()
                .id( "test_assignment" )
                .userId( userID )
                .teamId( "test_team" )
                .recurrenceRuleId( recurrenceRule.getId() )
                .startDate( testStartDate )
                .priority( Priority.NORMAL )
                .build();

        // Generate 6 days (3 full cycles)
        LocalDate endDate = testStartDate.plusDays( 5 );
        List<WorkScheduleDay> workScheduleDays = RecurrenceCalculatorExtensions
                .generateCustomPatternWorkScheduleDays( testStartDate, endDate, recurrenceRule, assignment );

        assertEquals( 6, workScheduleDays.size() );

        // Verify pattern: work, rest, work, rest, work, rest
        for (int i = 0; i < workScheduleDays.size(); i++) {
            WorkScheduleDay day = workScheduleDays.get( i );
            assertEquals( testStartDate.plusDays( i ), day.getDate() );

            if (i % 2 == 0) {
                // Even days (0, 2, 4): Work days
                assertEquals( 1, day.getShifts().size() );
                assertEquals( "morning_shift_id", day.getShifts().get( 0 ).getShift().getId() );
            } else {
                // Odd days (1, 3, 5): Rest days
                assertEquals( 0, day.getShifts().size() );
            }
        }
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    public void testValidation_ValidCustomPattern() {
        List<PatternDay> validPattern = Arrays.asList(
                new PatternDay( 1, mockMorningShift ),
                new PatternDay( 2, null ),
                new PatternDay( 3, mockNightShift )
        );

        RecurrenceRuleExtensions.ValidationResult result =
                RecurrenceRuleExtensions.validateCustomPattern( validPattern );

        assertTrue( result.isValid );
    }

    @Test
    public void testValidation_EmptyPattern() {
        List<PatternDay> emptyPattern = List.of();

        RecurrenceRuleExtensions.ValidationResult result =
                RecurrenceRuleExtensions.validateCustomPattern( emptyPattern );

        assertFalse( result.isValid );
        assertTrue( result.message.contains( "empty" ) );
    }

    @Test
    public void testValidation_InvalidDayNumbering() {
        List<PatternDay> invalidPattern = Arrays.asList(
                new PatternDay( 1, mockMorningShift ),
                new PatternDay( 3, mockNightShift ), // Missing day 2
                new PatternDay( 4, null )
        );

        RecurrenceRuleExtensions.ValidationResult result =
                RecurrenceRuleExtensions.validateCustomPattern( invalidPattern );

        assertFalse( result.isValid );
        assertTrue( result.message.contains( "sequential" ) );
    }

    // ==================== STATISTICS TESTS ====================

    @Test
    public void testPatternStatistics() {
        List<PatternDay> pattern = Arrays.asList(
                new PatternDay( 1, mockMorningShift ),  // Work
                new PatternDay( 2, mockNightShift ),    // Work
                new PatternDay( 3, null ),              // Rest
                new PatternDay( 4, null ),              // Rest
                new PatternDay( 5, mockMorningShift )   // Work
        );

        RecurrenceRule recurrenceRule = RecurrenceRuleExtensions.createCustomPattern(
                "Mixed Pattern", pattern, testStartDate );

        RecurrenceCalculatorExtensions.PatternStatistics stats =
                RecurrenceCalculatorExtensions.calculateCustomPatternStatistics( recurrenceRule );

        assertEquals( 5, stats.totalDays );
        assertEquals( 3, stats.workDays );
        assertEquals( 2, stats.restDays );
        assertEquals( 60.0, stats.workPercentage, 0.1 );
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    public void testCalculation_WithNonCustomPattern() {
        // Create a standard recurrence rule (not custom)
        RecurrenceRule standardRule = RecurrenceRule.builder()
                .frequency( RecurrenceRule.Frequency.DAILY )
                .interval( 1 )
                .startDate( testStartDate )
                .build();

        UserScheduleAssignment assignment = UserScheduleAssignment.builder()
                .id( "test_assignment" )
                .userId( userID )
                .teamId( "test_team" )
                .recurrenceRuleId( standardRule.getId() )
                .startDate( testStartDate )
                .priority( Priority.NORMAL )
                .build();

        // Should return empty shifts for non-custom pattern
        List<Shift> shifts = RecurrenceCalculatorExtensions.calculateCustomPatternShiftsForDate(
                testStartDate, standardRule, assignment );

        assertTrue( shifts.isEmpty() );
    }

    @Test
    public void testCalculation_WithDateBeforePatternStart() {
        List<PatternDay> pattern = Arrays.asList(
                new PatternDay( 1, mockMorningShift )
        );

        RecurrenceRule recurrenceRule = RecurrenceRuleExtensions.createCustomPattern(
                "Test Pattern", pattern, testStartDate );

        UserScheduleAssignment assignment = UserScheduleAssignment.builder()
                .id( "test_assignment" )
                .userId( userID )
                .teamId( "test_team" )
                .recurrenceRuleId( recurrenceRule.getId() )
                .startDate( testStartDate )
                .priority( Priority.NORMAL )
                .build();

        // Try to calculate for date before pattern start
        LocalDate dateBeforeStart = testStartDate.minusDays( 1 );
        List<Shift> shifts = RecurrenceCalculatorExtensions.calculateCustomPatternShiftsForDate(
                dateBeforeStart, recurrenceRule, assignment );

        // Should return empty shifts for dates before pattern start
        assertTrue( shifts.isEmpty() );
    }
}