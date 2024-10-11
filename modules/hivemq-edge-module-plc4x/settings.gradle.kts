rootProject.name = "hivemq-edge-module-plc4x"

pluginManagement {
    includeBuild("../../edge-plugins")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}
