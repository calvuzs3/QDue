/**
 * SmartShifts Domain Layer
 * <p>
 * Contains all business logic components including domain models,
 * use cases, shift generation algorithms, and export/import system.
 * This layer is independent of external frameworks and represents
 * the core business rules of the SmartShifts system.
 *
 * <h2>Architecture</h2>
 * <pre>
 * domain/
 * ├── models/        → Domain models and DTOs
 * ├── usecases/      → Business logic use cases (5 complete)
 * ├── generators/    → Shift generation algorithms
 * ├── exportimport/  → Export/Import system (Phase 4D)
 * └── common/        → Common domain components
 * </pre>
 *
 * <h2>Key Algorithms</h2>
 * <ul>
 *   <li><strong>ShiftGeneratorEngine</strong> - Complex shift calculation algorithms</li>
 *   <li><strong>RecurrenceRuleParser</strong> - JSON pattern interpretation</li>
 *   <li><strong>ContinuousCycleValidator</strong> - Pattern continuity validation</li>
 *   <li><strong>ExportImportManager</strong> - Multi-format data exchange</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li>Framework independence - pure Java business logic</li>
 *   <li>Single Responsibility Principle for each use case</li>
 *   <li>Dependency Inversion - depends on abstractions</li>
 *   <li>Testable design with clear interfaces</li>
 * </ul>
 *
 * @author SmartShifts Team
 * @since Phase 2 - Business Logic
 */
package net.calvuz.qdue.smartshifts.domain;