/**
 * Common Domain Components
 * <p>
 * Contains shared domain components used across multiple domain
 * classes. These components provide consistent patterns and
 * utilities for domain layer operations.
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li><strong>UnifiedOperationResult</strong> - Unified result handling system</li>
 * </ul>
 *
 * <h2>UnifiedOperationResult System</h2>
 * Provides consistent result handling across all domain operations:
 * <ul>
 *   <li>Type-safe success/failure states</li>
 *   <li>Progress tracking capabilities</li>
 *   <li>Consistent error messaging</li>
 *   <li>Operation metadata (type, timestamp, etc.)</li>
 * </ul>
 *
 * <h2>Usage Pattern</h2>
 * All Use Cases and major domain operations return UnifiedOperationResult
 * objects to provide consistent error handling and progress tracking
 * throughout the application.
 *
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features
 */
package net.calvuz.qdue.smartshifts.domain.common;