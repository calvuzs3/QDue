package net.calvuz.qdue.ui.features.schedulepattern.models;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import net.calvuz.qdue.domain.calendar.models.Shift;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalTime;

/**
 * PatternDayTest - Unit tests for PatternDay model
 *
 * <p>Comprehensive unit tests for the PatternDay model class covering
 * all business logic, validation, and edge cases.</p>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Test Implementation
 * @since Clean Architecture Phase 2
 */
@RunWith(MockitoJUnitRunner.class)
public class PatternDayTest {

    @Mock
    private Shift mockMorningShift;

    @Mock
    private Shift mockNightShift;

    @Before
    public void setUp() {
        // Setup mock shifts
        when(mockMorningShift.getId()).thenReturn("morning_shift_id");
        when(mockMorningShift.getName()).thenReturn("Morning Shift");
        when(mockMorningShift.getStartTime()).thenReturn(LocalTime.of(6, 0));
        when(mockMorningShift.getEndTime()).thenReturn(LocalTime.of(14, 0));

        when(mockNightShift.getId()).thenReturn("night_shift_id");
        when(mockNightShift.getName()).thenReturn("Night Shift");
        when(mockNightShift.getStartTime()).thenReturn(LocalTime.of(22, 0));
        when(mockNightShift.getEndTime()).thenReturn(LocalTime.of(6, 0));
    }

    // ==================== CONSTRUCTOR TESTS ====================

    @Test
    public void testConstructor_WorkDay() {
        PatternDay workDay = new PatternDay(1, mockMorningShift);

        assertEquals(1, workDay.getDayNumber());
        assertEquals(mockMorningShift, workDay.getShift());
        assertTrue(workDay.isEditable());
        assertFalse(workDay.isRestDay());
        assertTrue(workDay.isWorkDay());
    }

    @Test
    public void testConstructor_RestDay() {
        PatternDay restDay = new PatternDay(2, null);

        assertEquals(2, restDay.getDayNumber());
        assertNull(restDay.getShift());
        assertTrue(restDay.isEditable());
        assertTrue(restDay.isRestDay());
        assertFalse(restDay.isWorkDay());
    }

    @Test
    public void testConstructor_WithEditability() {
        PatternDay nonEditableDay = new PatternDay(3, mockMorningShift, false);

        assertEquals(3, nonEditableDay.getDayNumber());
        assertEquals(mockMorningShift, nonEditableDay.getShift());
        assertFalse(nonEditableDay.isEditable());
    }

    // ==================== BUSINESS LOGIC TESTS ====================

    @Test
    public void testIsRestDay_NullShift() {
        PatternDay day = new PatternDay(1, null);
        assertTrue(day.isRestDay());
        assertFalse(day.isWorkDay());
    }

    @Test
    public void testIsWorkDay_WithShift() {
        PatternDay day = new PatternDay(1, mockMorningShift);
        assertTrue(day.isWorkDay());
        assertFalse(day.isRestDay());
    }

    @Test
    public void testGetDisplayName_WorkDay() {
        PatternDay workDay = new PatternDay(1, mockMorningShift);
        assertEquals("Morning Shift", workDay.getDisplayName());
    }

    @Test
    public void testGetDisplayName_RestDay() {
        PatternDay restDay = new PatternDay(1, null);
        assertEquals("Rest", restDay.getDisplayName());
    }

    @Test
    public void testGetDisplayDescription_WorkDay() {
        PatternDay workDay = new PatternDay(1, mockMorningShift);
        String description = workDay.getDisplayDescription();

        assertTrue(description.contains("Morning Shift"));
        assertTrue(description.contains("05:00"));
        assertTrue(description.contains("13:00"));
    }

    @Test
    public void testGetDisplayDescription_RestDay() {
        PatternDay restDay = new PatternDay(1, null);
        String description = restDay.getDisplayDescription();

        assertTrue(description.contains("Rest day"));
        assertTrue(description.contains("no shifts"));
    }

    @Test
    public void testGetType_WorkDay() {
        PatternDay workDay = new PatternDay(1, mockMorningShift);
        assertEquals(PatternDay.PatternDayType.WORK_DAY, workDay.getType());
    }

    @Test
    public void testGetType_RestDay() {
        PatternDay restDay = new PatternDay(1, null);
        assertEquals(PatternDay.PatternDayType.REST_DAY, restDay.getType());
    }

    // ==================== MODIFICATION TESTS ====================

    @Test
    public void testSetDayNumber() {
        PatternDay day = new PatternDay(1, mockMorningShift);
        day.setDayNumber(5);
        assertEquals(5, day.getDayNumber());
    }

    @Test
    public void testSetShift_ToRestDay() {
        PatternDay day = new PatternDay(1, mockMorningShift);
        day.setShift(null);

        assertTrue(day.isRestDay());
        assertFalse(day.isWorkDay());
        assertNull(day.getShift());
    }

    @Test
    public void testSetShift_ToWorkDay() {
        PatternDay day = new PatternDay(1, null);
        day.setShift(mockNightShift);

        assertFalse(day.isRestDay());
        assertTrue(day.isWorkDay());
        assertEquals(mockNightShift, day.getShift());
    }

    @Test
    public void testSetEditable() {
        PatternDay day = new PatternDay(1, mockMorningShift);
        assertTrue(day.isEditable());

        day.setEditable(false);
        assertFalse(day.isEditable());

        day.setEditable(true);
        assertTrue(day.isEditable());
    }

    // ==================== COPY TESTS ====================

    @Test
    public void testCopy() {
        PatternDay original = new PatternDay(2, mockMorningShift, false);
        PatternDay copy = original.copy();

        assertEquals(original.getDayNumber(), copy.getDayNumber());
        assertEquals(original.getShift(), copy.getShift());
        assertEquals(original.isEditable(), copy.isEditable());

        // Ensure it's a different object
        assertNotSame(original, copy);
    }

    @Test
    public void testCopyWithDayNumber() {
        PatternDay original = new PatternDay(2, mockMorningShift, false);
        PatternDay copy = original.copyWithDayNumber(10);

        assertEquals(10, copy.getDayNumber());
        assertEquals(original.getShift(), copy.getShift());
        assertEquals(original.isEditable(), copy.isEditable());
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    public void testIsValid_ValidWorkDay() {
        PatternDay workDay = new PatternDay(1, mockMorningShift);
        assertTrue(workDay.isValid());
    }

    @Test
    public void testIsValid_ValidRestDay() {
        PatternDay restDay = new PatternDay(1, null);
        assertTrue(restDay.isValid());
    }

    @Test
    public void testIsValid_InvalidDayNumber() {
        PatternDay invalidDay = new PatternDay(-1, mockMorningShift);
        assertFalse(invalidDay.isValid());

        PatternDay zeroDayNumber = new PatternDay(0, mockMorningShift);
        assertFalse(zeroDayNumber.isValid());
    }

    @Test
    public void testIsValid_InvalidShift() {
        Shift invalidShift = mock(Shift.class);
        when(invalidShift.getId()).thenReturn(null);

        PatternDay dayWithInvalidShift = new PatternDay(1, invalidShift);
        assertFalse(dayWithInvalidShift.isValid());

        when(invalidShift.getId()).thenReturn("");
        assertFalse(dayWithInvalidShift.isValid());

        when(invalidShift.getId()).thenReturn("  ");
        assertFalse(dayWithInvalidShift.isValid());
    }

    // ==================== EQUALS AND HASHCODE TESTS ====================

    @Test
    public void testEquals_SameValues() {
        PatternDay day1 = new PatternDay(1, mockMorningShift, true);
        PatternDay day2 = new PatternDay(1, mockMorningShift, true);

        assertEquals(day1, day2);
        assertEquals(day1.hashCode(), day2.hashCode());
    }

    @Test
    public void testEquals_DifferentDayNumbers() {
        PatternDay day1 = new PatternDay(1, mockMorningShift);
        PatternDay day2 = new PatternDay(2, mockMorningShift);

        assertNotEquals(day1, day2);
    }

    @Test
    public void testEquals_DifferentShifts() {
        PatternDay day1 = new PatternDay(1, mockMorningShift);
        PatternDay day2 = new PatternDay(1, mockNightShift);

        assertNotEquals(day1, day2);
    }

    @Test
    public void testEquals_DifferentEditability() {
        PatternDay day1 = new PatternDay(1, mockMorningShift, true);
        PatternDay day2 = new PatternDay(1, mockMorningShift, false);

        assertNotEquals(day1, day2);
    }

    @Test
    public void testEquals_SameObject() {
        PatternDay day = new PatternDay(1, mockMorningShift);
        assertEquals(day, day);
    }

    @Test
    public void testEquals_NullObject() {
        PatternDay day = new PatternDay(1, mockMorningShift);
        assertNotEquals(day, null);
    }

    @Test
    public void testEquals_DifferentClass() {
        PatternDay day = new PatternDay(1, mockMorningShift);
        assertNotEquals(day, "not a PatternDay");
    }

    // ==================== TOSTRING TESTS ====================

    @Test
    public void testToString_WorkDay() {
        PatternDay workDay = new PatternDay(1, mockMorningShift);
        String toString = workDay.toString();

        assertTrue(toString.contains("PatternDay{"));
        assertTrue(toString.contains("dayNumber=1"));
        assertTrue(toString.contains("shift=Morning Shift"));
        assertTrue(toString.contains("editable=true"));
    }

    @Test
    public void testToString_RestDay() {
        PatternDay restDay = new PatternDay(2, null);
        String toString = restDay.toString();

        assertTrue(toString.contains("PatternDay{"));
        assertTrue(toString.contains("dayNumber=2"));
        assertTrue(toString.contains("shift=REST"));
        assertTrue(toString.contains("editable=true"));
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    public void testDisplayDescription_ShiftWithNullTimes() {
        Shift shiftWithNullTimes = mock(Shift.class);
        when(shiftWithNullTimes.getName()).thenReturn("Flexible Shift");
        when(shiftWithNullTimes.getStartTime()).thenReturn(null);
        when(shiftWithNullTimes.getEndTime()).thenReturn(null);

        PatternDay day = new PatternDay(1, shiftWithNullTimes);
        String description = day.getDisplayDescription();

        assertTrue(description.contains("Flexible Shift"));
        assertTrue(description.contains("?"));
    }

    @Test
    public void testDisplayDescription_ShiftWithPartialTimes() {
        Shift shiftWithPartialTimes = mock(Shift.class);
        when(shiftWithPartialTimes.getName()).thenReturn("Open Shift");
        when(shiftWithPartialTimes.getStartTime()).thenReturn(LocalTime.of(8, 0));
        when(shiftWithPartialTimes.getEndTime()).thenReturn(null);

        PatternDay day = new PatternDay(1, shiftWithPartialTimes);
        String description = day.getDisplayDescription();

        assertTrue(description.contains("Open Shift"));
        assertTrue(description.contains("08:00"));
        assertTrue(description.contains("?"));
    }
}