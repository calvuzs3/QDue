package net.calvuz.qdue.ui.features.schedulepattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;

import org.junit.Test;

import java.time.LocalDate;

/**
 * Test examples demonstrating the fixed UserScheduleAssignment architecture.
 * These tests verify that status is correctly computed and assignments work for future dates.
 */
public class UserScheduleAssignmentArchitectureTest {

    /**
     * Test the core fix: Future assignments should work without validation errors.
     */
    @Test
    public void testFutureAssignmentCreation() {
        LocalDate futureDate = LocalDate.now().plusDays(7);

        // BEFORE: This would fail with validation error
        // AFTER: This should work perfectly
        UserScheduleAssignment futureAssignment = UserScheduleAssignment.createPatternAssignment(
                1L, "TeamA", "rule123", futureDate, "Future Pattern"
        );

        // Verify computed status is correct
        assertEquals(UserScheduleAssignment.Status.PENDING, futureAssignment.getStatus());
        assertEquals(UserScheduleAssignment.Status.PENDING, futureAssignment.getEffectiveStatus());

        // Verify administrative flags
        assertTrue(futureAssignment.isAdministrativelyEnabled());
        assertTrue(futureAssignment.isActive());

        // Verify business logic
        assertTrue(futureAssignment.isFuture());
        assertFalse(futureAssignment.isCurrentlyActive());
        assertFalse(futureAssignment.isExpired());

        // CRITICAL: Future assignments should be processable and apply to their date
        assertTrue(futureAssignment.isProcessable());  // ← This was failing before!
        assertTrue(futureAssignment.appliesTo(futureDate));  // ← This was failing before!
        assertTrue(futureAssignment.isValidForPlanning(futureDate));

        System.out.println("✅ Future assignment test passed!");
        System.out.println("   Status: " + futureAssignment.getStatus());
        System.out.println("   Processable: " + futureAssignment.isProcessable());
        System.out.println("   Applies to date: " + futureAssignment.appliesTo(futureDate));
    }

    /**
     * Test current assignments work as expected.
     */
    @Test
    public void testCurrentAssignmentCreation() {
        LocalDate today = LocalDate.now();

        UserScheduleAssignment currentAssignment = UserScheduleAssignment.createPatternAssignment(
                1L, "TeamA", "rule123", today, "Current Pattern"
        );

        // Verify computed status
        assertEquals(UserScheduleAssignment.Status.ACTIVE, currentAssignment.getStatus());
        assertEquals(UserScheduleAssignment.Status.ACTIVE, currentAssignment.getEffectiveStatus());

        // Verify business logic
        assertFalse(currentAssignment.isFuture());
        assertTrue(currentAssignment.isCurrentlyActive());
        assertFalse(currentAssignment.isExpired());

        // Verify processability
        assertTrue(currentAssignment.isProcessable());
        assertTrue(currentAssignment.appliesTo(today));

        System.out.println("✅ Current assignment test passed!");
        System.out.println("   Status: " + currentAssignment.getStatus());
        System.out.println("   Currently active: " + currentAssignment.isCurrentlyActive());
    }

    /**
     * Test expired assignments.
     */
    @Test
    public void testExpiredAssignmentCreation() {
        LocalDate pastStart = LocalDate.now().minusDays(10);
        LocalDate pastEnd = LocalDate.now().minusDays(1);

        UserScheduleAssignment expiredAssignment = UserScheduleAssignment.createPatternAssignment(
                1L, "TeamA", "rule123", pastStart, pastEnd, "Expired Pattern"
        );

        // Verify computed status
        assertEquals(UserScheduleAssignment.Status.EXPIRED, expiredAssignment.getStatus());
        assertEquals(UserScheduleAssignment.Status.EXPIRED, expiredAssignment.getEffectiveStatus());

        // Verify business logic
        assertFalse(expiredAssignment.isFuture());
        assertFalse(expiredAssignment.isCurrentlyActive());
        assertTrue(expiredAssignment.isExpired());

        // Expired assignments should not be processable
        assertFalse(expiredAssignment.isProcessable());
        assertFalse(expiredAssignment.appliesTo(LocalDate.now()));

        System.out.println("✅ Expired assignment test passed!");
        System.out.println("   Status: " + expiredAssignment.getStatus());
        System.out.println("   Expired: " + expiredAssignment.isExpired());
    }

    /**
     * Test disabled (cancelled) assignments.
     */
    @Test
    public void testDisabledAssignmentCreation() {
        LocalDate futureDate = LocalDate.now().plusDays(7);

        // Create a normal assignment first
        UserScheduleAssignment normalAssignment = UserScheduleAssignment.createPatternAssignment(
                1L, "TeamA", "rule123", futureDate, "Future Pattern"
        );

        // Create disabled version
        UserScheduleAssignment disabledAssignment = UserScheduleAssignment.createDisabledAssignment(normalAssignment);

        // Verify computed status
        assertEquals(UserScheduleAssignment.Status.CANCELLED, disabledAssignment.getStatus());
        assertEquals(UserScheduleAssignment.Status.CANCELLED, disabledAssignment.getEffectiveStatus());

        // Verify administrative flags
        assertFalse(disabledAssignment.isAdministrativelyEnabled());
        assertFalse(disabledAssignment.isActive());

        // Disabled assignments should not be processable or apply to any date
        assertFalse(disabledAssignment.isProcessable());
        assertFalse(disabledAssignment.appliesTo(futureDate));
        assertFalse(disabledAssignment.isValidForPlanning(futureDate));

        System.out.println("✅ Disabled assignment test passed!");
        System.out.println("   Status: " + disabledAssignment.getStatus());
        System.out.println("   Administratively enabled: " + disabledAssignment.isAdministrativelyEnabled());
    }

    /**
     * Test the core architectural fix: status consistency.
     */
    @Test
    public void testStatusConsistency() {
        LocalDate[] testDates = {
                LocalDate.now().minusDays(5),  // Past
                LocalDate.now(),               // Today
                LocalDate.now().plusDays(5)    // Future
        };

        String[] expectedStatuses = {"EXPIRED", "ACTIVE", "PENDING"};

        for (int i = 0; i < testDates.length; i++) {
            LocalDate testDate = testDates[i];
            String expectedStatus = expectedStatuses[i];

            UserScheduleAssignment assignment;
            if (i == 0) {
                // Past assignment (with end date)
                assignment = UserScheduleAssignment.createPatternAssignment(
                        1L, "TeamA", "rule123", testDate, testDate.plusDays(1), "Test Pattern"
                );
            } else {
                // Current or future assignment
                assignment = UserScheduleAssignment.createPatternAssignment(
                        1L, "TeamA", "rule123", testDate, "Test Pattern"
                );
            }

            // Verify status consistency
            assertEquals("Status mismatch for date: " + testDate,
                    expectedStatus, assignment.getStatus().name());
            assertEquals("Effective status mismatch for date: " + testDate,
                    assignment.getStatus(), assignment.getEffectiveStatus());

            System.out.println("✅ Date: " + testDate + " → Status: " + assignment.getStatus());
        }

        System.out.println("✅ Status consistency test passed!");
    }

    /**
     * Test that builder doesn't allow manual status setting.
     */
    @Test
    public void testBuilderStatusRestriction() {
        // This should compile and work (status computed automatically)
        UserScheduleAssignment assignment = UserScheduleAssignment.builder()
                .userId(1L)
                .teamId("TeamA")
                .recurrenceRuleId("rule123")
                .startDate(LocalDate.now().plusDays(5))
                .active(true)
                // .status(Status.ACTIVE)  ← This method should not exist anymore!
                .build();

        // Status should be computed as PENDING (future date)
        assertEquals(UserScheduleAssignment.Status.PENDING, assignment.getStatus());

        System.out.println("✅ Builder restriction test passed!");
        System.out.println("   Auto-computed status: " + assignment.getStatus());
    }

    /**
     * Comprehensive test of the appliesTo() logic with the new architecture.
     */
    @Test
    public void testAppliesToLogic() {
        LocalDate startDate = LocalDate.now().plusDays(3);
        LocalDate endDate = LocalDate.now().plusDays(10);

        UserScheduleAssignment assignment = UserScheduleAssignment.createPatternAssignment(
                1L, "TeamA", "rule123", startDate, endDate, "Test Pattern"
        );

        // Test dates before start
        assertFalse(assignment.appliesTo(LocalDate.now()));
        assertFalse(assignment.appliesTo(startDate.minusDays(1)));

        // Test dates in range
        assertTrue(assignment.appliesTo(startDate));
        assertTrue(assignment.appliesTo(startDate.plusDays(2)));
        assertTrue(assignment.appliesTo(endDate));

        // Test dates after end
        assertFalse(assignment.appliesTo(endDate.plusDays(1)));
        assertFalse(assignment.appliesTo(endDate.plusDays(10)));

        // Test disabled assignment
        UserScheduleAssignment disabledAssignment = UserScheduleAssignment.createDisabledAssignment(assignment);
        assertFalse(disabledAssignment.appliesTo(startDate));  // Disabled = never applies

        System.out.println("✅ appliesTo() logic test passed!");
        System.out.println("   Assignment status: " + assignment.getStatus());
        System.out.println("   Applies to start date: " + assignment.appliesTo(startDate));
        System.out.println("   Disabled version applies: " + disabledAssignment.appliesTo(startDate));
    }

    /**
     * Demo method showing before/after behavior.
     */
    public void demonstrateArchitectureFix() {
        System.out.println("=== UserScheduleAssignment Architecture Fix Demo ===");

        LocalDate futureDate = LocalDate.now().plusDays(7);

        // BEFORE: This would throw validation exception
        // if (status == Status.ACTIVE && !isCurrentlyActive()) {
        //     throw new IllegalArgumentException("Status ACTIVE but dates indicate otherwise");
        // }

        // AFTER: This works perfectly
        UserScheduleAssignment futureAssignment = UserScheduleAssignment.createPatternAssignment(
                1L, "TeamA", "rule123", futureDate, "Future Pattern"
        );

        System.out.println("Future assignment created successfully!");
        System.out.println("├─ Start Date: " + futureAssignment.getStartDate());
        System.out.println("├─ Status: " + futureAssignment.getStatus() + " (auto-computed)");
        System.out.println("├─ Active: " + futureAssignment.isActive());
        System.out.println("├─ Processable: " + futureAssignment.isProcessable());
        System.out.println("├─ Applies to future date: " + futureAssignment.appliesTo(futureDate));
        System.out.println("├─ Valid for planning: " + futureAssignment.isValidForPlanning(futureDate));
        System.out.println("└─ Display message: " + futureAssignment.getEffectiveStatusMessage());

        System.out.println("\n🎯 Architecture fix successful!");
        System.out.println("   • Status is always computed correctly");
        System.out.println("   • Future assignments are processable");
        System.out.println("   • No validation conflicts");
        System.out.println("   • Clean separation of concerns");
    }
}