/**
 * Shift Generation Algorithms
 * <p>
 * Contains sophisticated algorithms for generating shift schedules,
 * parsing recurrence rules, and validating continuous cycle patterns.
 * These components form the mathematical core of the SmartShifts system.
 *
 * <h2>Core Algorithms</h2>
 * <ul>
 *   <li><strong>ShiftGeneratorEngine</strong> - Advanced shift generation engine</li>
 *   <li><strong>RecurrenceRuleParser</strong> - JSON to domain object parser</li>
 *   <li><strong>PatternJsonGenerator</strong> - Pattern to JSON converter</li>
 *   <li><strong>ContinuousCycleValidator</strong> - Continuous pattern validation</li>
 *   <li><strong>ShiftTimeValidator</strong> - Shift timing validation + conflicts</li>
 * </ul>
 *
 * <h2>Capabilities</h2>
 * <ul>
 *   <li>Generate shifts for multiple users with cycle offsets</li>
 *   <li>Handle complex recurrence patterns (4-2, 3-2, 5-2, 6-1)</li>
 *   <li>Validate 24-hour continuous coverage</li>
 *   <li>Detect scheduling conflicts and gaps</li>
 *   <li>Support custom pattern creation</li>
 * </ul>
 *
 * <h2>Mathematical Foundation</h2>
 * Algorithms are based on modular arithmetic for cycle calculations
 * and set theory for coverage validation, ensuring mathematical
 * correctness and performance optimization.
 *
 * @author SmartShifts Team
 * @since Phase 2 - Business Logic
 */
package net.calvuz.qdue.smartshifts.domain.generators;