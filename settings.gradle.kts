rootProject.name = "hivemq-edge-build"

includeBuild("./hivemq-edge")

// ** module-deps ** //

includeBuild("./modules/hivemq-edge-module-plc4x")
includeBuild("./modules/hivemq-edge-module-http")
includeBuild("./modules/hivemq-edge-module-modbus")
includeBuild("./modules/hivemq-edge-module-opcua")




