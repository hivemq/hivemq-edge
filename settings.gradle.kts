rootProject.name = "hivemq-edge-build"

pluginManagement {
    plugins {
        id("com.gradle.enterprise") version "3.12.3"
        id("com.gradle.common-custom-user-data-gradle-plugin") version "1.8.2"
        includeBuild("./edge-plugins")
    }
}
includeBuild("./hivemq-edge")

// ** module-deps ** //

includeBuild("./modules/hivemq-edge-module-etherip")
includeBuild("./modules/hivemq-edge-module-plc4x")
includeBuild("./modules/hivemq-edge-module-http")
includeBuild("./modules/hivemq-edge-module-modbus")
includeBuild("./modules/hivemq-edge-module-opcua")
includeBuild("./modules/hivemq-edge-module-file")
includeBuild("./modules/hivemq-edge-module-s7")
