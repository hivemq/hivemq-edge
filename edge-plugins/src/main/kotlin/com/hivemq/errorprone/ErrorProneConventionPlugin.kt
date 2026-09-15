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

            val reportFileProvider = project.layout.buildDirectory.file("reports/errorprone/${name}.log")
            doFirst {
                val reportFile = reportFileProvider.get().asFile
                reportFile.parentFile.mkdirs()
                reportFile.delete()
            }
            logging.addStandardErrorListener { msg ->
                val reportFile = reportFileProvider.get().asFile
                // The listener can fire before doFirst runs (or when the task is
                // UP-TO-DATE / FROM-CACHE and doFirst is skipped), so ensure the
                // parent directory exists here rather than relying on doFirst. An
                // I/O failure in a logging listener runs on a Gradle worker thread
                // and would otherwise wedge the whole build, so it must never escape.
                try {
                    reportFile.parentFile.mkdirs()
                    reportFile.appendText(msg.toString())
                } catch (_: Exception) {
                    // Best-effort logging; never fail the build from here.
                }
            }
        }
    }
}
