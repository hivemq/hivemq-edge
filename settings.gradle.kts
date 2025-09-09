rootProject.name = "hivemq-edge-build"

pluginManagement {
    plugins {
        includeBuild("./edge-plugins")
    }
}

plugins {
    id("com.gradle.develocity") version "3.19.2"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "2.1"
}

includeBuild("./hivemq-edge")
includeBuild("./hivemq-edge-frontend")

// ** module-deps ** //

includeBuild("./modules/hivemq-edge-module-etherip")
includeBuild("./modules/hivemq-edge-module-plc4x")
includeBuild("./modules/hivemq-edge-module-http")
includeBuild("./modules/hivemq-edge-module-modbus")
includeBuild("./modules/hivemq-edge-module-mtconnect")
includeBuild("./modules/hivemq-edge-module-databases")
includeBuild("./modules/hivemq-edge-module-opcua")
includeBuild("./modules/hivemq-edge-module-file")
