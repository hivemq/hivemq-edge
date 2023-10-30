rootProject.name = "hivemq-edge-module-modbus"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../libs.versions.toml"))
        }
    }
}

includeBuild("../../hivemq-edge")
