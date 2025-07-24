/**
 * Initial Setup Wizard
 * <p>
 * Contains the first-time setup wizard that guides users through
 * the initial configuration of SmartShifts. The wizard uses a
 * 4-step process to collect necessary information and create
 * the user's first shift assignment.
 *
 * <h2>Components</h2>
 * <ul>
 *   <li><strong>ShiftSetupWizardActivity</strong> - Main wizard activity</li>
 *   <li><strong>SetupWizardViewModel</strong> - Setup flow business logic</li>
 *   <li><strong>4 Step Fragments</strong> - Individual wizard steps</li>
 * </ul>
 *
 * <h2>Wizard Steps</h2>
 * <ol>
 *   <li><strong>Welcome</strong> - Introduction and branding</li>
 *   <li><strong>Pattern Selection</strong> - Choose predefined or custom pattern</li>
 *   <li><strong>Start Date</strong> - Select cycle start date</li>
 *   <li><strong>Confirmation</strong> - Review and confirm setup</li>
 * </ol>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Step-by-step validation</li>
 *   <li>Progress indication</li>
 *   <li>Back/forward navigation</li>
 *   <li>Real-time pattern preview</li>
 * </ul>
 *
 * @author SmartShifts Team
 * @since Phase 3 - UI Foundation
 */
package net.calvuz.qdue.smartshifts.ui.setup;