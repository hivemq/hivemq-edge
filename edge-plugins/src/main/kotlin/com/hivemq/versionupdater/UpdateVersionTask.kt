package com.hivemq.versionupdater

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class UpdateVersionTask : DefaultTask() {

    @TaskAction
    protected fun run() {
        val newVersion = project.version.toString()

        // If the new version is not a SNAPSHOT version, we have to update all versions in the extra-files
        if ("-SNAPSHOT" !in newVersion) {
            var previousPatchVersion = project.properties["prevVersion"]

            if (previousPatchVersion == null) {
                previousPatchVersion = previousPatchVersion()
            }

            if (previousPatchVersion == null) {
                error("Cannot infer previous version for version ${newVersion}. Please specify it with '-PprevVersion=x.x.x'.")
            }

            updateExtraFiles(newVersion, previousPatchVersion.toString())
        }

        updateGradleProperties(newVersion)
    }

    private fun updateGradleProperties(newVersion: String) {
        val gradleProperties = project.file("gradle.properties")
        replaceInFile(gradleProperties, """^version=.+""".toRegex(), "version=${newVersion}")
    }

    private fun updateExtraFiles(newVersion: String, previousVersion: String) {
        val filesToUpdate = project.properties[PROPERTIES_FILES_KEY]
        if ((filesToUpdate != null) && (filesToUpdate is Array<*>)) {
            filesToUpdate.forEach {
                val file = project.file(it.toString())
                replaceInFile(file, "${previousVersion}(-SNAPSHOT)?".toRegex(), newVersion)
            }
        }
    }

    private fun replaceInFile(file: File, find: Regex, replace: String) {
        require(file.exists()) { "File ${file.absolutePath} does not exist" }
        require(file.isFile) { "File ${file.absolutePath} is not a file" }

        val text = file.readText()
        val replacedText = text.replace(find, replace)
        file.writeText(replacedText)
    }

    private fun previousPatchVersion(): String? {
        val currentVersion = project.version.toString()
        val patchVersion = currentVersion.substringAfterLast('.').toInt()

        return if (patchVersion == 0) {
            null
        } else {
            currentVersion.substringBefore('.') + // Major version
                    ".${currentVersion.substringAfter('.').substringBefore('.')}." + // Minor version
                    (patchVersion - 1)
        }
    }
}
