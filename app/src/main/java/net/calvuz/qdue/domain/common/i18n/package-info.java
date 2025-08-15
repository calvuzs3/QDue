/**
 * Domain Common i18n - Clean Architecture Internationalization Bridge
 *
 * <p>This package provides the bridge infrastructure between domain layer localization
 * needs and the core infrastructure LocaleManager. It maintains strict clean architecture
 * principles by keeping the bridge implementation in the CORE layer while the domain
 * layer depends only on abstractions.</p>
 *
 * <h2>🏗️ Clean Architecture Integration</h2>
 *
 * <p>The domain common i18n package follows strict clean architecture principles:</p>
 *
 * <ul>
 *   <li><strong>Dependency Direction</strong>: Domain → Domain Interface, Core → Domain Interface</li>
 *   <li><strong>Interface Abstraction</strong>: Domain depends only on DomainLocalizer interface</li>
 *   <li><strong>Infrastructure Bridge</strong>: DomainLocalizerImpl in CORE layer bridges to LocaleManager</li>
 *   <li><strong>Dependency Injection</strong>: Full integration with ServiceProvider pattern</li>
 * </ul>
 *
 * <h2>🔧 Implementation Architecture</h2>
 *
 * <h3>Core Components</h3>
 * <ul>
 *   <li><strong>DomainLocalizer</strong>: Abstract interface for domain localization (in DOMAIN layer)</li>
 *   <li><strong>DomainLocalizerImpl</strong>: Bridge implementation to LocaleManager (in CORE layer)</li>
 *   <li><strong>DomainServiceProvider</strong>: Extended ServiceProvider with domain i18n</li>
 * </ul>
 *
 * <h3>Clean Architecture Pattern</h3>
 * <pre>
 * Domain Layer                     Infrastructure Layer (Core)
 * ┌─────────────────────┐         ┌─────────────────────────────┐
 * │   Domain Models     │────────▶│   DomainLocalizer           │
 * │   - WorkScheduleDay │         │   (interface in domain)     │
 * │   - Team            │         └─────────────────────────────┘
 * │   - ShiftException  │                      │
 * └─────────────────────┘                      ▼
 *                                 ┌─────────────────────────────┐
 *                                 │ DomainLocalizerImpl         │
 *                                 │   (bridge in core)          │
 *                                 └─────────────────────────────┘
 *                                          │
 *                                          ▼
 *                                 ┌─────────────────────────────┐
 *                                 │   LocaleManager             │
 *                                 │ (core infrastructure)       │
 *                                 └─────────────────────────────┘
 * </pre>
 *
 * <h2>🚀 Usage Patterns</h2>
 *
 * <h3>Domain Model Integration</h3>
 * <pre>
 * public class WorkScheduleDay extends LocalizableDomainModel {
 *
 *     private static final String LOCALIZATION_SCOPE = "schedule";
 *
 *     private WorkScheduleDay(Builder builder) {
 *         super(builder.mLocalizer, LOCALIZATION_SCOPE);
 *         // ... field assignments
 *     }
 *
 *     &#64;NonNull
 *     public String getDisplayName() {
 *         return localize("day.display_name", "Schedule Day", date.toString());
 *     }
 *
 *     &#64;NonNull
 *     public String getStatusDescription() {
 *         return localize("day.status." + status.name().toLowerCase(),
 *                        status.name(), shiftsCount);
 *     }
 * }
 * </pre>
 *
 * <h3>Service Layer Integration</h3>
 * <pre>
 * public class CalendarServiceImpl implements CalendarService {
 *     private final DomainLocalizer mDomainLocalizer;
 *
 *     public CalendarServiceImpl(..., DomainServiceProvider serviceProvider) {
 *         // Get scoped localizer for calendar domain
 *         this.mDomainLocalizer = serviceProvider.getDomainLocalizer("calendar");
 *     }
 *
 *     private WorkScheduleDay createLocalizedSchedule(LocalDate date) {
 *         return WorkScheduleDay.builder(date)
 *                 .localizer(mDomainLocalizer.scope("schedule"))
 *                 .build();
 *     }
 * }
 * </pre>
 *
 * <h3>Domain Engine Integration</h3>
 * <pre>
 * public class ExceptionResolver {
 *     private final DomainLocalizer mLocalizer;
 *
 *     public static ExceptionResolver create(DomainLocalizer localizer) {
 *         return new ExceptionResolver(localizer.scope("exceptions"));
 *     }
 *
 *     private ExceptionResolver(DomainLocalizer localizer) {
 *         this.mLocalizer = localizer;
 *     }
 *
 *     public String resolveExceptionMessage(ShiftException exception) {
 *         return mLocalizer.localize("resolution." + exception.getType().name().toLowerCase(),
 *                                   "Exception resolved", exception.getDetails());
 *     }
 * }
 * </pre>
 *
 * <h2>🌐 Localization Key Conventions</h2>
 *
 * <h3>Android Resource Key Structure</h3>
 * <pre>
 * domain_{scope}_{category}_{item}_{detail}
 *
 * Examples:
 * domain_calendar_schedule_day_display_name     → "Giornata Lavorativa"
 * domain_calendar_exceptions_type_vacation      → "Ferie"
 * domain_calendar_exceptions_status_approved    → "Approvato"
 * domain_calendar_recurrence_frequency_daily    → "Giornaliero"
 * domain_calendar_validation_conflict_detected  → "Conflitto Rilevato"
 * </pre>
 *
 * <h3>Scope-Based Organization</h3>
 * <ul>
 *   <li><strong>calendar</strong>: Root calendar domain scope</li>
 *   <li><strong>calendar_schedule</strong>: Schedule-related localizations</li>
 *   <li><strong>calendar_exceptions</strong>: Exception handling localizations</li>
 *   <li><strong>calendar_recurrence</strong>: Recurrence pattern localizations</li>
 *   <li><strong>calendar_validation</strong>: Validation message localizations</li>
 * </ul>
 *
 * <h2>⚡ Performance Optimizations</h2>
 *
 * <h3>LocaleManager Integration</h3>
 * <ul>
 *   <li><strong>Android Resource System</strong>: Full integration with Android string resources</li>
 *   <li><strong>Context Localization</strong>: Proper context handling for locale switching</li>
 *   <li><strong>Fallback Strategy</strong>: Immediate fallback to provided default values</li>
 *   <li><strong>Caching</strong>: LocaleManager's built-in resource caching</li>
 * </ul>
 *
 * <h3>Scoped Localizers</h3>
 * <ul>
 *   <li><strong>Automatic Key Prefixing</strong>: Reduces key management overhead</li>
 *   <li><strong>Scope Reuse</strong>: Scoped localizers reuse base infrastructure</li>
 *   <li><strong>Lazy Loading</strong>: Localization only occurs when accessed</li>
 * </ul>
 *
 * <h2>🧪 Testing Strategy</h2>
 *
 * <h3>Unit Testing Without Localization</h3>
 * <pre>
 * &#64;Test
 * public void testDomainLogicWithoutLocalizer() {
 *     WorkScheduleDay schedule = WorkScheduleDay.builder(LocalDate.now()).build();
 *
 *     // Domain logic works without localization
 *     assertTrue(schedule.isValid());
 *     assertEquals("Schedule Day", schedule.getDisplayName()); // fallback
 * }
 * </pre>
 *
 * <h3>Integration Testing With Mock Localizer</h3>
 * <pre>
 * &#64;Test
 * public void testDomainWithMockLocalizer() {
 *     DomainLocalizer mockLocalizer = mock(DomainLocalizer.class);
 *     when(mockLocalizer.localize("day.display_name", "Schedule Day"))
 *         .thenReturn("Giornata Lavorativa");
 *
 *     WorkScheduleDay schedule = WorkScheduleDay.builder(LocalDate.now())
 *             .localizer(mockLocalizer)
 *             .build();
 *
 *     assertEquals("Giornata Lavorativa", schedule.getDisplayName());
 * }
 * </pre>
 *
 * <h3>Service Integration Testing</h3>
 * <pre>
 * &#64;Test
 * public void testServiceWithDomainLocalizer() {
 *     DomainServiceProvider serviceProvider = mock(DomainServiceProvider.class);
 *     DomainLocalizer domainLocalizer = mock(DomainLocalizer.class);
 *     when(serviceProvider.getDomainLocalizer()).thenReturn(domainLocalizer);
 *
 *     CalendarService service = new CalendarServiceImpl(..., serviceProvider);
 *
 *     // Test that service uses domain localization correctly
 *     verify(domainLocalizer).scope("calendar");
 * }
 * </pre>
 *
 * <h2>🔄 Migration Strategy</h2>
 *
 * <h3>Phase 1: Infrastructure Setup</h3>
 * <ol>
 *   <li>Create DomainLocalizer interface in domain layer</li>
 *   <li>Create DomainLocalizerImpl bridge in core layer</li>
 *   <li>Extend ServiceProvider with domain support</li>
 * </ol>
 *
 * <h3>Phase 2: Domain Model Integration</h3>
 * <ol>
 *   <li>Implement LocalizableDomainModel base class</li>
 *   <li>Convert existing domain models to use DomainLocalizer</li>
 *   <li>Update service implementations to inject DomainLocalizer</li>
 * </ol>
 *
 * <h3>Phase 3: Resource Localization</h3>
 * <ol>
 *   <li>Add domain-specific localization keys to Android string resources</li>
 *   <li>Implement comprehensive fallback values</li>
 *   <li>Test localization coverage and quality</li>
 * </ol>
 *
 * <h2>📋 Integration Checklist</h2>
 *
 * <h3>Service Implementation</h3>
 * <ul>
 *   <li>☐ Inject DomainLocalizer through DomainServiceProvider</li>
 *   <li>☐ Create scoped localizers for domain engines</li>
 *   <li>☐ Pass localizers to domain model builders</li>
 *   <li>☐ Use localized messages in error handling</li>
 * </ul>
 *
 * <h3>Domain Model Implementation</h3>
 * <ul>
 *   <li>☐ Extend LocalizableDomainModel base class</li>
 *   <li>☐ Define appropriate localization scope</li>
 *   <li>☐ Implement localized display methods</li>
 *   <li>☐ Provide sensible fallback values</li>
 * </ul>
 *
 * <h3>String Resources Implementation</h3>
 * <ul>
 *   <li>☐ Add domain keys to res/values/strings.xml</li>
 *   <li>☐ Follow Android resource naming conventions</li>
 *   <li>☐ Test with LocaleManager's getLocalizedString() method</li>
 *   <li>☐ Verify fallback behavior works correctly</li>
 * </ul>
 *
 * <h2>📦 Package Structure</h2>
 *
 * <h3>Domain Layer (Interfaces Only)</h3>
 * <pre>
 * net.calvuz.qdue.domain.common.i18n/
 * ├── DomainLocalizer.java                    # Interface only
 * └── package-info.java                       # Documentation
 * </pre>
 *
 * <h3>Core Layer (Implementation)</h3>
 * <pre>
 * net.calvuz.qdue.core.common.i18n.impl/
 * ├── DomainLocalizerImpl.java               # Bridge implementation
 * └── package-info.java                       # Implementation docs
 * </pre>
 *
 * <h3>DI Layer (Extended ServiceProvider)</h3>
 * <pre>
 * net.calvuz.qdue.core.di/
 * ├── DomainServiceProvider.java             # Extended interface
 * └── impl/
 *     └── DomainServiceProviderImpl.java     # Extended implementation
 * </pre>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture i18n Bridge
 * @since Clean Architecture Phase 2
 *
 * @see net.calvuz.qdue.domain.common.i18n.DomainLocalizer
 * @see net.calvuz.qdue.core.common.i18n.LocaleManager
 * @see net.calvuz.qdue.core.common.i18n.impl.DomainLocalizerImpl
 * @see net.calvuz.qdue.core.di.DomainServiceProvider
 */
package net.calvuz.qdue.domain.common.i18n;