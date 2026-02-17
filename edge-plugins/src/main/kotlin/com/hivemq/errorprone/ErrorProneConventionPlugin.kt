package com.hivemq.errorprone

import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.ErrorPronePlugin
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * Convention plugin that applies the ErrorProne static analysis tool.
 * This centralizes the ErrorProne configuration to avoid duplication across build files.
 *
 * Usage: Apply this plugin in any module's build.gradle.kts:
 *   plugins {
 *       id("com.hivemq.errorprone-convention")
 *   }
 */
class ErrorProneConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(ErrorPronePlugin::class)

        project.dependencies {
            add("errorprone", "com.google.errorprone:error_prone_core:2.45.0")
        }

        project.tasks.withType<JavaCompile>().configureEach {
            options.errorprone {
                disableWarningsInGeneratedCode.set(true)

                // High-volume stylistic checks disabled for initial rollout.
                // Enable these incrementally in follow-up PRs.
                check("MissingSummary", CheckSeverity.OFF)
                check("MissingOverride", CheckSeverity.OFF)
                check("PatternMatchingInstanceof", CheckSeverity.OFF)
                check("EqualsGetClass", CheckSeverity.OFF)
                check("StatementSwitchToExpressionSwitch", CheckSeverity.OFF)
                check("EffectivelyPrivate", CheckSeverity.OFF)
                check("EmptyBlockTag", CheckSeverity.OFF)
                check("UnnecessaryParentheses", CheckSeverity.OFF)
                check("FormatStringShouldUsePlaceholders", CheckSeverity.OFF)
            }
            options.compilerArgs.add("-Xmaxwarns")
            options.compilerArgs.add("9999")
        }
    }
}
