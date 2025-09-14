/**
 * LocalEvents Presentation Layer Package (MVVM)
 *
 * <p>This package contains the presentation layer implementation for LocalEvents following
 * the Model-View-ViewModel (MVVM) architectural pattern. This layer provides reactive UI
 * components, state management, and clean separation between UI logic and business logic.</p>
 *
 * <h3>MVVM Architecture Overview:</h3>
 * <pre>
 * Presentation Layer (net.calvuz.qdue.ui.features.events.local)
 * ├── viewmodels/             # ViewModels (Business Logic + State Management)
 * │   ├── BaseViewModel.java                    # Base class with observable pattern
 * │   ├── LocalEventsViewModel.java             # Main events management ViewModel
 * │   └── LocalEventsFileOperationsViewModel.java  # File operations ViewModel
 * ├── models/                 # UI State Models
 * │   └── LocalEventsUiState.java               # Immutable UI state classes
 * ├── presentation/           # Activities and Fragments (Views)
 * │   ├── LocalEventsActivity.java              # Main MVVM Activity
 * │   └── fragments/                            # Fragment implementations
 * └── di/                     # Dependency Injection
 *     └── LocalEventsModule.java                # DI module for all components
 * </pre>
 *
 * <h3>MVVM Components:</h3>
 *
 * <h4>Model:</h4>
 * <ul>
 *   <li><strong>Domain Models</strong>: {@link net.calvuz.qdue.domain.events.models.LocalEvent}</li>
 *   <li><strong>UI State Models</strong>: {@link net.calvuz.qdue.ui.features.events.local.models.LocalEventsUiState}</li>
 *   <li><strong>Service Models</strong>: {@link net.calvuz.qdue.core.services.models.OperationResult}</li>
 * </ul>
 *
 * <h4>View:</h4>
 * <ul>
 *   <li><strong>{@link net.calvuz.qdue.ui.features.events.local.presentation.LocalEventsActivity}</strong>:
 *       Main Activity coordinating ViewModels and UI navigation</li>
 *   <li><strong>Fragments</strong>: UI components for specific features (list, detail, etc.)</li>
 * </ul>
 *
 * <h4>ViewModel:</h4>
 * <ul>
 *   <li><strong>{@link net.calvuz.qdue.ui.features.events.local.viewmodels.BaseViewModel}</strong>:
 *       Base class providing observable state management pattern</li>
 *   <li><strong>{@link net.calvuz.qdue.ui.features.events.local.viewmodels.LocalEventsViewModel}</strong>:
 *       Main ViewModel managing events data, filtering, selection, and navigation</li>
 *   <li><strong>{@link net.calvuz.qdue.ui.features.events.local.viewmodels.LocalEventsFileOperationsViewModel}</strong>:
 *       Specialized ViewModel for file import/export operations</li>
 * </ul>
 *
 * <h3>State Management Architecture:</h3>
 *
 * <h4>Observable Pattern:</h4>
 * <ul>
 *   <li><strong>State Changes</strong>: Observable via listener pattern for specific state keys</li>
 *   <li><strong>Events</strong>: One-time events for navigation and UI actions</li>
 *   <li><strong>Loading States</strong>: Operation-specific loading state tracking</li>
 *   <li><strong>Error States</strong>: Centralized error state management</li>
 * </ul>
 *
 * <h4>Immutable State:</h4>
 * <ul>
 *   <li><strong>UI State Classes</strong>: Immutable state objects with builder pattern</li>
 *   <li><strong>Thread Safety</strong>: Concurrent collections for multi-threaded access</li>
 *   <li><strong>Defensive Copying</strong>: Collections copied to prevent external modification</li>
 * </ul>
 *
 * <h3>ViewModel State Management:</h3>
 * <pre>
 * // State observation example
 * viewModel.addStateChangeListener("events", (key, value) -> updateEventsUI(value));
 * viewModel.addLoadingStateListener((operation, loading) -> showLoading(operation, loading));
 * viewModel.addEventListener(event -> handleNavigationEvent(event));
 *
 * // State updates from ViewModel
 * viewModel.setState("events", newEventsList);
 * viewModel.setLoading("loadEvents", true);
 * viewModel.emitEvent(new NavigationEvent("event_detail", args));
 * </pre>
 *
 * <h3>Navigation Architecture:</h3>
 * <ul>
 *   <li><strong>Event-Driven</strong>: ViewModels emit navigation events</li>
 *   <li><strong>Activity Coordination</strong>: Activity handles navigation implementation</li>
 *   <li><strong>Arguments Passing</strong>: Type-safe argument passing through events</li>
 *   <li><strong>Navigation Component</strong>: Integration with Android Navigation Component</li>
 * </ul>
 *
 * <h3>Dependency Injection:</h3>
 * <ul>
 *   <li><strong>{@link net.calvuz.qdue.ui.features.events.local.di.LocalEventsModule}</strong>:
 *       Centralized DI module managing all component dependencies</li>
 *   <li><strong>Service Integration</strong>: Integration with existing {@link net.calvuz.qdue.data.di.CalendarServiceProvider}</li>
 *   <li><strong>Lifecycle Management</strong>: Proper initialization and cleanup coordination</li>
 * </ul>
 *
 * <h3>File Operations Integration:</h3>
 * <ul>
 *   <li><strong>Separated Concerns</strong>: File operations isolated in dedicated ViewModel</li>
 *   <li><strong>Progress Tracking</strong>: Real-time progress reporting for long operations</li>
 *   <li><strong>Error Handling</strong>: Comprehensive error states and user feedback</li>
 *   <li><strong>Activity Result Handling</strong>: Clean integration with Android file selection</li>
 * </ul>
 *
 * <h3>MVVM Benefits:</h3>
 * <ul>
 *   <li><strong>Testability</strong>: ViewModels easily unit testable without Android dependencies</li>
 *   <li><strong>Separation of Concerns</strong>: Clear boundaries between UI and business logic</li>
 *   <li><strong>Reactive UI</strong>: Automatic UI updates through observable state changes</li>
 *   <li><strong>Maintainability</strong>: Structured architecture for easy feature additions</li>
 *   <li><strong>Reusability</strong>: ViewModels can be reused across different UI implementations</li>
 * </ul>
 *
 * <h3>Comparison with Original EventsActivity:</h3>
 * <table border="1">
 * <tr><th>Aspect</th><th>Original EventsActivity</th><th>MVVM LocalEventsActivity</th></tr>
 * <tr><td>Business Logic</td><td>Mixed with UI in Activity</td><td>Separated in ViewModels</td></tr>
 * <tr><td>State Management</td><td>Manual UI updates</td><td>Observable state changes</td></tr>
 * <tr><td>File Operations</td><td>Embedded in Activity</td><td>Dedicated ViewModel</td></tr>
 * <tr><td>Error Handling</td><td>Scattered throughout Activity</td><td>Centralized in ViewModels</td></tr>
 * <tr><td>Testability</td><td>Requires Android testing</td><td>Unit testable ViewModels</td></tr>
 * <tr><td>Dependencies</td><td>Direct service instantiation</td><td>Dependency injection</td></tr>
 * </table>
 *
 * <h3>Usage Examples:</h3>
 * <pre>
 * // Activity setup
 * public class LocalEventsActivity extends AppCompatActivity implements Injectable {
 *     private LocalEventsViewModel mEventsViewModel;
 *     private LocalEventsModule mModule;
 *
 *     protected void onCreate(Bundle savedInstanceState) {
 *         // Initialize module and ViewModels
 *         mModule = new LocalEventsModule(this, calendarServiceProvider);
 *         mEventsViewModel = mModule.getLocalEventsViewModel();
 *
 *         // Setup observers
 *         mEventsViewModel.addStateChangeListener("events", this::updateEventsUI);
 *         mEventsViewModel.addEventListener(this::handleNavigationEvent);
 *
 *         // Initialize
 *         mEventsViewModel.initialize();
 *     }
 * }
 *
 * // ViewModel operations
 * mEventsViewModel.loadEvents();                    // Load data
 * mEventsViewModel.searchEvents("query");           // Search
 * mEventsViewModel.navigateToEventDetail(eventId);  // Navigate
 * mEventsViewModel.toggleSelectionMode();           // UI state change
 * </pre>
 *
 * <h3>Thread Safety and Performance:</h3>
 * <ul>
 *   <li><strong>Background Operations</strong>: All data operations executed on background threads</li>
 *   <li><strong>UI Thread Updates</strong>: State changes delivered on UI thread</li>
 *   <li><strong>Concurrent Collections</strong>: Thread-safe collections for listener management</li>
 *   <li><strong>Efficient Updates</strong>: Only changed state triggers UI updates</li>
 * </ul>
 *
 * <h3>Error Handling Strategy:</h3>
 * <ul>
 *   <li><strong>Operation-Specific Errors</strong>: Errors associated with specific operations</li>
 *   <li><strong>User-Friendly Messages</strong>: Localized error messages for users</li>
 *   <li><strong>Graceful Degradation</strong>: Partial functionality when some features fail</li>
 *   <li><strong>Retry Mechanisms</strong>: Built-in retry for transient failures</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since LocalEvents MVVM Implementation
 * @see net.calvuz.qdue.ui.features.events.local.viewmodels.LocalEventsViewModel
 * @see net.calvuz.qdue.ui.features.events.local.presentation.LocalEventsActivity
 * @see net.calvuz.qdue.ui.features.events.local.di.LocalEventsModule
 */
package net.calvuz.qdue.ui.features.events.local;