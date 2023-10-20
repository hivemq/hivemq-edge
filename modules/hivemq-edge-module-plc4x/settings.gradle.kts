rootProject.name = "hivemq-edge-module-plc4x"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../libs.versions.toml"))
        }
    }
}

includeBuild("../../hivemq-edge")
