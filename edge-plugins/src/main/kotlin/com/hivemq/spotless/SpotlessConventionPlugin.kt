package com.hivemq.spotless

import com.diffplug.gradle.spotless.JavaExtension
import com.diffplug.gradle.spotless.KotlinGradleExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaBasePlugin
import java.io.File

/**
 * Convention plugin that applies and configures Spotless for consistent code formatting.
 *
 * This plugin configures:
 * - Java formatting with Palantir Java Format
 * - Import ordering matching the project's .editorconfig
 * - License header enforcement
 * - Kotlin formatting with ktlint (for Gradle build scripts)
 *
 * The spotlessCheck task is automatically run as part of the check task.
 */
class SpotlessConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("com.diffplug.spotless")

        val licenseHeaderFile = findLicenseHeaderFile(project)

        val spotless = project.extensions.getByType(SpotlessExtension::class.java)

        spotless.encoding("UTF-8")

        // Java formatting
        spotless.java(Action<JavaExtension> {
            target("src/*/java/**/*.java")

            // License header
            if (licenseHeaderFile != null) {
                licenseHeaderFile(licenseHeaderFile)
            }

            // Import ordering matching editorconfig: *, |, javax.**, java.**, |, $*
            // This means: all other imports, blank line, javax/java imports, blank line, static imports
            importOrder("", "javax", "java", "\\#")
            removeUnusedImports()

            // Use Palantir Java Format (closer to IntelliJ style than google-java-format)
            palantirJavaFormat("2.87.0")

            // Basic cleanup
            trimTrailingWhitespace()
            endWithNewline()

            // Respect @formatter:off / @formatter:on directives
            toggleOffOn("@formatter:off", "@formatter:on")
        })

        // Kotlin Gradle DSL formatting
        spotless.kotlinGradle(Action<KotlinGradleExtension> {
            target("*.gradle.kts")
            ktlint("1.5.0").editorConfigOverride(
                mapOf(
                    "ktlint_code_style" to "ktlint_official",
                    "indent_size" to "4",
                    "max_line_length" to "120"
                )
            )
            trimTrailingWhitespace()
            endWithNewline()
        })

        // Hook spotlessCheck into the check task

        //TODO deactivated for now until spotlessApply has been run at least once to avoid breaking the build for everyone
//        project.plugins.withType(JavaBasePlugin::class.java, Action<JavaBasePlugin> {
//            project.tasks.named("check", Task::class.java, Action<Task> {
//                dependsOn("spotlessCheck")
//            })
//        })
    }

    /**
     * Finds the license-header.txt file by traversing up the project hierarchy.
     */
    private fun findLicenseHeaderFile(project: Project): File? {
        var currentDir: File? = project.projectDir
        val rootDir = project.rootProject.projectDir.parentFile ?: project.rootProject.projectDir

        while (currentDir != null && currentDir.absolutePath.startsWith(rootDir.absolutePath)) {
            val licenseFile = currentDir.resolve("license-header.txt")
            if (licenseFile.exists()) {
                return licenseFile
            }
            currentDir = currentDir.parentFile
        }

        // Also check the hivemq-edge directory specifically for composite builds
        val hivemqEdgeDir = project.rootProject.projectDir.parentFile?.resolve("hivemq-edge")
        if (hivemqEdgeDir != null && hivemqEdgeDir.exists()) {
            val licenseFile = hivemqEdgeDir.resolve("license-header.txt")
            if (licenseFile.exists()) {
                return licenseFile
            }
        }

        return null
    }
}
