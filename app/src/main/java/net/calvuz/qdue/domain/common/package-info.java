/**
 * Domain Common - Universal Infrastructure for Clean Architecture Domain Layer.
 *
 * <p>This package provides core infrastructure components that enable clean, testable,
 * and localizable domain models while maintaining strict clean architecture principles.
 * All components are designed to be framework-agnostic and fully testable in isolation.</p>
 *
 * <h2>🏗️ Architecture Overview</h2>
 *
 * <p>The domain common package implements a universal pattern for domain models that need
 * localization support without violating clean architecture principles:</p>
 *
 * <ul>
 *   <li><strong>Zero Framework Dependencies</strong>: No Android or UI framework imports</li>
 *   <li><strong>Optional Localization</strong>: Domain models work with or without i18n</li>
 *   <li><strong>Dependency Injection Compatible</strong>: Integrates with existing ServiceProvider pattern</li>
 *   <li><strong>Consistent API</strong>: Uniform pattern across all domain models</li>
 *   <li><strong>Full Testability</strong>: Complete isolation for unit testing</li>
 * </ul>
 *
 * <h2>🎯 Core Components</h2>
 *
 * <h3>DomainLocalizer Interface</h3>
 * <p>The primary localization interface that provides string resolution with fallback support:</p>
 * <pre>
 * // Inject via dependency injection
 * DomainLocalizer localizer = serviceProvider.getDomainLocalizer();
 *
 * // Use for localization with fallback
 * String localized = localizer.localize("recurrence.frequency.daily", "Daily");
 *
 * // Create scoped localizer for specific domain
 * DomainLocalizer scopedLocalizer = localizer.scope("recurrence");
 * </pre>
 *
 * <h3>Localizable Interface</h3>
 * <p>Marker interface for domain models that support optional localization:</p>
 * <pre>
 * public class MyDomainModel implements Localizable {
 *
 *     // Check if localization is available
 *     if (hasLocalizationSupport()) {
 *         String displayName = getLocalizedDisplayName();
 *     }
 *
 *     // Add localization to existing instance
 *     MyDomainModel localized = (MyDomainModel) withLocalizer(localizer);
 * }
 * </pre>
 *
 * <h3>LocalizableDomainModel Abstract Base Class</h3>
 * <p>Base class that provides complete localization infrastructure for domain models:</p>
 * <pre>
 * public class RecurrenceRule extends LocalizableDomainModel {
 *
 *     private static final String LOCALIZATION_SCOPE = "recurrence";
 *
 *     private RecurrenceRule(Builder builder) {
 *         super(builder.mLocalizer, LOCALIZATION_SCOPE);
 *         // ... field assignments
 *     }
 *
 *     &#64;NonNull
 *     public String getDisplayName() {
 *         // Localize with automatic fallback
 *         return localize("frequency." + frequency.name().toLowerCase(),
 *                        frequency.name(), // fallback
 *                        frequency.name());
 *     }
 * }
 * </pre>
 *
 * <h3>LocalizableBuilder Abstract Base Class</h3>
 * <p>Base builder class that provides localization support in the builder pattern:</p>
 * <pre>
 * public static class Builder extends LocalizableBuilder&lt;RecurrenceRule, Builder&gt; {
 *
 *     // Builder methods...
 *
 *     &#64;NonNull
 *     public Builder copyFrom(RecurrenceRule source) {
 *         // Copy domain fields
 *         this.frequency = source.frequency;
 *         // Copy localization support
 *         return copyLocalizableFrom(source);
 *     }
 *
 *     &#64;Override
 *     &#64;NonNull
 *     public RecurrenceRule build() {
 *         return new RecurrenceRule(this);
 *     }
 * }
 * </pre>
 *
 * <h2>🚀 Implementation Pattern</h2>
 *
 * <h3>1. Domain Model Implementation</h3>
 * <pre>
 * public class YourDomainModel extends LocalizableDomainModel {
 *
 *     private static final String LOCALIZATION_SCOPE = "your_scope";
 *
 *     private YourDomainModel(Builder builder) {
 *         super(builder.mLocalizer, LOCALIZATION_SCOPE);
 *         // ... field assignments
 *     }
 *
 *     // Localized getters
 *     &#64;NonNull
 *     public String getDisplayName() {
 *         return localize("display.name", defaultName, params...);
 *     }
 *
 *     // Factory methods with optional localizer
 *     &#64;NonNull
 *     public static YourDomainModel create(Params params) {
 *         return create(params, null);
 *     }
 *
 *     &#64;NonNull
 *     public static YourDomainModel create(Params params,
 *                                         &#64;Nullable DomainLocalizer localizer) {
 *         return builder().params(params).localizer(localizer).build();
 *     }
 *
 *     // Localizable implementation
 *     &#64;Override
 *     &#64;NonNull
 *     public YourDomainModel withLocalizer(&#64;NonNull DomainLocalizer localizer) {
 *         return builder().copyFrom(this).localizer(localizer).build();
 *     }
 *
 *     // Builder implementation
 *     public static class Builder extends LocalizableBuilder&lt;YourDomainModel, Builder&gt; {
 *         // ... builder implementation
 *     }
 * }
 * </pre>
 *
 * <h3>2. Dependency Injection Integration</h3>
 * <pre>
 * // ServiceProvider extension
 * public interface DomainServiceProvider extends ServiceProvider {
 *     &#64;NonNull
 *     DomainLocalizer getDomainLocalizer();
 *
 *     &#64;NonNull
 *     default DomainLocalizer getDomainLocalizer(String scope) {
 *         return getDomainLocalizer().scope(scope);
 *     }
 * }
 *
 * // UI Module usage
 * public class FeatureModule {
 *     private final DomainLocalizer mLocalizer;
 *
 *     public FeatureModule(Context context) {
 *         this.mLocalizer = serviceProvider.getDomainLocalizer();
 *     }
 *
 *     public YourDomainModel createLocalizedModel(Params params) {
 *         return YourDomainModel.create(params, mLocalizer);
 *     }
 * }
 * </pre>
 *
 * <h3>3. Testing Strategy</h3>
 * <pre>
 * // Unit tests without localization
 * &#64;Test
 * public void testDomainLogicWithoutLocalizer() {
 *     YourDomainModel model = YourDomainModel.create(params);
 *
 *     // Domain logic works without localization
 *     assertTrue(model.isValid());
 *     assertEquals("DEFAULT_NAME", model.getDisplayName()); // fallback
 * }
 *
 * // Integration tests with mock localizer
 * &#64;Test
 * public void testDomainWithMockLocalizer() {
 *     DomainLocalizer mockLocalizer = mock(DomainLocalizer.class);
 *     when(mockLocalizer.localize("display.name")).thenReturn("Localized Name");
 *
 *     YourDomainModel model = YourDomainModel.create(params, mockLocalizer);
 *
 *     assertEquals("Localized Name", model.getDisplayName());
 * }
 * </pre>
 *
 * <h2>🔑 Key Benefits</h2>
 *
 * <ul>
 *   <li><strong>Clean Architecture Compliance</strong>: Domain layer remains independent</li>
 *   <li><strong>Optional Localization</strong>: Domain works with or without i18n support</li>
 *   <li><strong>Testability</strong>: Complete isolation for unit testing</li>
 *   <li><strong>Consistency</strong>: Uniform pattern across all domain models</li>
 *   <li><strong>Performance</strong>: Lazy loading and scoped localizers</li>
 *   <li><strong>Maintainability</strong>: Centralized localization infrastructure</li>
 * </ul>
 *
 * <h2>📋 Usage Guidelines</h2>
 *
 * <ol>
 *   <li><strong>Domain Models</strong>: Extend LocalizableDomainModel for i18n support</li>
 *   <li><strong>Builders</strong>: Extend LocalizableBuilder for consistent API</li>
 *   <li><strong>Factory Methods</strong>: Provide both parameterized and non-parameterized versions</li>
 *   <li><strong>Scoping</strong>: Use meaningful scope names for localization keys</li>
 *   <li><strong>Fallbacks</strong>: Always provide sensible fallback values</li>
 *   <li><strong>Testing</strong>: Test both with and without localization</li>
 * </ol>
 *
 * <h2>🎯 Localization Key Conventions</h2>
 *
 * <pre>
 * // Pattern: {scope}.{category}.{item}
 * "recurrence.frequency.daily"        // → "Giornaliero"
 * "recurrence.frequency.weekly"       // → "Settimanale"
 * "recurrence.end.never"              // → "Mai"
 * "shift_exception.type.vacation"     // → "Ferie"
 * "shift_exception.status.pending"    // → "In attesa"
 * "team.role.manager"                 // → "Manager"
 * </pre>
 *
 * <h2>⚡ Performance Considerations</h2>
 *
 * <ul>
 *   <li><strong>Scoped Localizers</strong>: Automatic key prefixing reduces lookup overhead</li>
 *   <li><strong>Optional Injection</strong>: No performance cost when localization not needed</li>
 *   <li><strong>Lazy Loading</strong>: Localization only occurs when accessed</li>
 *   <li><strong>Fallback Strategy</strong>: Fast fallback to default values</li>
 * </ul>
 *
 * <h2>🔄 Integration with Existing Architecture</h2>
 *
 * <p>This package integrates seamlessly with the existing QDue architecture:</p>
 * <ul>
 *   <li><strong>ServiceProvider Pattern</strong>: Extends existing DI infrastructure</li>
 *   <li><strong>LocaleManager</strong>: Leverages existing core localization</li>
 *   <li><strong>OperationResult</strong>: Compatible with service layer patterns</li>
 *   <li><strong>Builder Pattern</strong>: Extends existing domain model builders</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Universal Domain Localization Pattern
 * @since Clean Architecture Phase 2
 */
package net.calvuz.qdue.domain.common;