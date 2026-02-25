package com.hivemq.errorprone

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
                disable("MissingSummary")
                disable("EmptyBlockTag")
                disable("InvalidBlockTag")
                disable("EffectivelyPrivate")
                disable("ImmutableEnumChecker")
            }
            options.compilerArgs.addAll(listOf("-Xmaxwarns", "9999", "-Xmaxerrs", "9999"))

            val reportFile = project.file("${project.layout.buildDirectory.get().asFile}/reports/errorprone/${name}.log")
            doFirst {
                reportFile.parentFile.mkdirs()
                reportFile.delete()
            }
            logging.addStandardErrorListener { msg ->
                reportFile.appendText(msg.toString())
            }
        }
    }
}
