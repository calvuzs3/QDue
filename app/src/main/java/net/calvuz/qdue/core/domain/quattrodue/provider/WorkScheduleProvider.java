
package net.calvuz.qdue.core.domain.quattrodue.provider;

import net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleEvent;
import net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleTemplate;

import java.time.LocalDate;
import java.util.List;

/**
 * ✅ PROVIDER INTERFACE: Work Schedule Provider
 *
 * <p>Provider interface for generating volatile work schedule events based on
 * templates and patterns. Implementations can provide different scheduling
 * algorithms (fixed patterns, custom patterns, etc.).</p>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Generate volatile events for specified date ranges</li>
 *   <li>Apply work schedule templates and patterns</li>
 *   <li>Handle team rotation and assignment logic</li>
 *   <li>Support both predefined and custom schedules</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 5
 */
public interface WorkScheduleProvider {

    /**
     * Generates work schedule events for a date range using the provider's algorithm.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param template Work schedule template to apply
     * @param userTeam User's team identifier (for filtering if needed)
     * @return List of volatile work schedule events
     */
    List<WorkScheduleEvent> generateSchedule(LocalDate startDate, LocalDate endDate,
                                             WorkScheduleTemplate template, String userTeam);

    /**
     * Generates work schedule events for a single date.
     *
     * @param date Target date
     * @param template Work schedule template to apply
     * @param userTeam User's team identifier (for filtering if needed)
     * @return List of volatile work schedule events for the day
     */
    List<WorkScheduleEvent> generateScheduleForDate(LocalDate date, WorkScheduleTemplate template, String userTeam);

    /**
     * Checks if this provider supports the given template type.
     *
     * @param template Template to check
     * @return true if this provider can handle the template
     */
    boolean supportsTemplate(WorkScheduleTemplate template);

    /**
     * Gets the provider's name/identifier.
     *
     * @return Provider name
     */
    String getProviderName();

    /**
     * Gets the provider's description.
     *
     * @return Provider description
     */
    String getProviderDescription();

    /**
     * Validates that the template is compatible with this provider.
     *
     * @param template Template to validate
     * @return true if template is valid for this provider
     */
    boolean validateTemplate(WorkScheduleTemplate template);
}
