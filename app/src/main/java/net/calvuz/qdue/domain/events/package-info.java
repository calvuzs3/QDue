/**
 * LocalEvents Domain Layer Package
 *
 * <p>This package contains the domain layer implementation for LocalEvents following
 * Clean Architecture principles. The domain layer encapsulates business logic,
 * entities, repositories, and use cases while maintaining independence from
 * external frameworks and infrastructure concerns.</p>
 *
 * <h3>Architecture Overview:</h3>
 * <pre>
 * Domain Layer (net.calvuz.qdue.domain.events.local)
 * ├── repository/              # Repository interfaces and implementations
 * │   ├── LocalEventRepository.java        # Repository interface
 * │   └── impl/
 * │       └── LocalEventRepositoryImpl.java  # Repository implementation
 * ├── usecases/               # Business use cases
 * │   └── LocalEventsUseCases.java        # Use cases collection
 * └── models/                 # Domain-specific models (if any additional ones needed)
 * </pre>
 *
 * <h3>Key Components:</h3>
 *
 * <h4>Repository Layer ({@link net.calvuz.qdue.domain.events.local.repository}):</h4>
 * <ul>
 *   <li><strong>{@link net.calvuz.qdue.domain.events.local.repository.LocalEventRepository}</strong>:
 *       Repository interface defining data access operations for LocalEvent entities</li>
 *   <li><strong>{@link net.calvuz.qdue.domain.events.local.repository.impl.LocalEventRepositoryImpl}</strong>:
 *       Concrete implementation providing thread-safe data access with error handling</li>
 * </ul>
 *
 * <h4>Use Cases Layer ({@link net.calvuz.qdue.domain.events.local.usecases}):</h4>
 * <ul>
 *   <li><strong>{@link net.calvuz.qdue.domain.events.local.usecases.LocalEventsUseCases}</strong>:
 *       Comprehensive collection of business use cases for LocalEvent operations</li>
 * </ul>
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li><strong>Dependency Inversion</strong>: Domain layer defines interfaces, implementations are injected</li>
 *   <li><strong>Single Responsibility</strong>: Each class has one reason to change</li>
 *   <li><strong>Business Logic Encapsulation</strong>: All business rules contained within domain layer</li>
 *   <li><strong>Framework Independence</strong>: No dependencies on Android or external frameworks</li>
 *   <li><strong>Testability</strong>: All components designed for easy unit testing</li>
 * </ul>
 *
 * <h3>Integration with Existing Architecture:</h3>
 * <ul>
 *   <li><strong>Entity Models</strong>: Uses existing {@link net.calvuz.qdue.domain.events.models.LocalEvent}</li>
 *   <li><strong>Data Layer</strong>: Integrates with {@link net.calvuz.qdue.data.events.dao.LocalEventDao}</li>
 *   <li><strong>Service Layer</strong>: Consumed by {@link net.calvuz.qdue.data.services.LocalEventsService}</li>
 *   <li><strong>Common Models</strong>: Uses {@link net.calvuz.qdue.core.services.models.OperationResult}</li>
 * </ul>
 *
 * <h3>Async Operations:</h3>
 * <p>All repository and use case operations return {@link java.util.concurrent.CompletableFuture}
 * for non-blocking execution. Error handling is managed through the {@link net.calvuz.qdue.core.services.models.OperationResult}
 * pattern providing consistent success/failure handling throughout the application.</p>
 *
 * <h3>Usage Examples:</h3>
 * <pre>
 * // Repository usage
 * LocalEventRepository repository = new LocalEventRepositoryImpl(dao);
 * CompletableFuture&lt;OperationResult&lt;LocalEvent&gt;&gt; result =
 *     repository.getLocalEventById("event-123");
 *
 * // Use cases usage
 * LocalEventsUseCases useCases = new LocalEventsUseCases(repository);
 * CompletableFuture&lt;OperationResult&lt;List&lt;LocalEvent&gt;&gt;&gt; events =
 *     useCases.getEventsForMonth(YearMonth.now());
 * </pre>
 *
 * <h3>Error Handling:</h3>
 * <p>All operations use the {@link net.calvuz.qdue.core.services.models.OperationResult} pattern:</p>
 * <ul>
 *   <li><strong>Success</strong>: Contains data and success message</li>
 *   <li><strong>Failure</strong>: Contains error message and operation type</li>
 *   <li><strong>Validation</strong>: Business rule validation results</li>
 * </ul>
 *
 * <h3>Thread Safety:</h3>
 * <p>All domain components are designed to be thread-safe:</p>
 * <ul>
 *   <li>Repository operations execute on background threads</li>
 *   <li>Use cases coordinate multiple repository calls safely</li>
 *   <li>Immutable objects used where possible</li>
 *   <li>Concurrent collections for thread-safe operations</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since LocalEvents MVVM Implementation
 * @see net.calvuz.qdue.domain.events.models.LocalEvent
 * @see net.calvuz.qdue.core.services.models.OperationResult
 * @see net.calvuz.qdue.data.services.LocalEventsService
 */
package net.calvuz.qdue.domain.events;