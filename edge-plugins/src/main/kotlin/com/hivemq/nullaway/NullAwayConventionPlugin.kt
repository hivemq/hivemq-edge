package com.hivemq.nullaway

import net.ltgt.gradle.errorprone.ErrorPronePlugin
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

/**
 * Convention plugin that applies the NullAway null-safety checker on top of ErrorProne.
 * This centralizes the NullAway configuration to avoid duplication across build files.
 *
 * Usage: Apply this plugin in any module's build.gradle.kts:
 *   plugins {
 *       id("com.hivemq.errorprone-convention")
 *       id("com.hivemq.nullaway-convention")
 *   }
 */
class NullAwayConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(ErrorPronePlugin::class)

        val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
        val nullaway = libs.findLibrary("nullaway").orElseThrow {
            IllegalStateException("Library 'nullaway' not found in version catalog 'libs'")
        }

        project.dependencies {
            add("errorprone", nullaway)
        }

        project.tasks.withType<JavaCompile>().configureEach {
            options.errorprone {
                warn("NullAway")
                option("NullAway:AnnotatedPackages", "com.hivemq")
                option("NullAway:UnannotatedSubPackages", "com.hivemq.edge.api")
                option("NullAway:TreatGeneratedAsUnannotated", "true")
                option("NullAway:ExcludedFieldAnnotations", "dagger.Lazy")
            }
        }
    }
}
