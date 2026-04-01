rootProject.name = "hivemq-edge-compiler"

pluginManagement {
    includeBuild("../edge-plugins")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
