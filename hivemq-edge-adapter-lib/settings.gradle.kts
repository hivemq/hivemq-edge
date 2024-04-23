rootProject.name = "hivemq-edge-adapter-lib"

if (file("../../hivemq-extension-sdk").exists()) {
    includeBuild("../../hivemq-extension-sdk")
} else {
    logger.warn(
        """
        ######################################################################################################
        You can not use the latest changes of or modify the hivemq-extension-sdk.
        Please checkout the hivemq-extension-sdk repository next to the hivemq-edge repository.
        Execute the following command from your project directory:
        git clone https://github.com/hivemq/hivemq-extension-sdk.git ../hivemq-extension-sdk
        You can also clone your fork:
        git clone https://github.com/<replace-with-your-fork>/hivemq-extension-sdk.git ../hivemq-extension-sdk
        ######################################################################################################
        """.trimIndent()
    )
}


if (file("../../hivemq-edge-extension-sdk").exists()) {
    includeBuild("../../hivemq-edge-extension-sdk")
} else {
    logger.warn(
        """
        ######################################################################################################
        You can not use the latest changes of or modify the hivemq-edge-extension-sdk.
        Please checkout the hivemq-edge-extension-sdk repository next to the hivemq-edge repository.
        Execute the following command from your project directory:
        git clone https://github.com/hivemq/hivemq-edge-extension-sdk.git ../hivemq-edge-extension-sdk
        You can also clone your fork:
        git clone https://github.com/<replace-with-your-fork>/hivemq-edge-extension-sdk.git ../hivemq-edge-extension-sdk
        ######################################################################################################
        """.trimIndent()
    )
}
