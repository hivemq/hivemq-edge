# Contributing

Welcome to the HiveMQ Community!
Glad to see your interest in contributing to HiveMQ Edge.
Please checkout our [Contribution Guide](https://github.com/hivemq/hivemq-community/blob/master/CONTRIBUTING.adoc) to make sure your contribution will be accepted by the HiveMQ team.

For information on how the HiveMQ Community is organized and how contributions will be accepted please have a look at our [HiveMQ Community Repo](https://github.com/hivemq/hivemq-community).

## Prerequisites

We recommend that you use the [IntelliJ](https://www.jetbrains.com/idea/download/) IDE for all development on HiveMQ CE.
It will automate your process as much as possible.
Java version 11 is required to build and run HiveMQ Edge (for example [Azul Zulu JDK](https://www.azul.com/downloads/)).
You can check your installed Java version by entering `java -version` on the command line.

First you need to [fork](https://help.github.com/en/articles/fork-a-repo) HiveMQ Edge [repository](https://github.com/hivemq/hivemq-edge).

Then you can clone the repository:

```shell
git clone https://github.com/<your user name>/hivemq-edge.git
```

*Open* the HiveMQ Edge project folder in IntelliJ.
Choose to sync the gradle project, if so prompted by the IDE.
After setting the gradle and project SDK (Java 11), you are good to go.

### Checking out the HiveMQ Extension SDK

HiveMQ Edge uses the HiveMQ Extension SDK which resides in [its own repository](https://github.com/hivemq/hivemq-extension-sdk) . And the HiveMQ Edge Extension SDK which can be found [here](https://github.com/hivemq/hivemq-edge-extension-sdk).
By default, you can not use the latest changes of or modify the Extension SDK.
Please checkout the `hivemq-extension-sdk` repository next to the `hivemq-edge` repository.
Gradle and IntelliJ will then automatically wire the two projects.

If you only want to use the Extension SDK, execute the following command from the `hivemq-edge` project directory:

```shell
git clone https://github.com/hivemq/hivemq-extension-sdk.git ../hivemq-extension-sdk
git clone https://github.com/hivemq/hivemq-edge-extension-sdk.git ../hivemq-edge-extension-sdk
```

If you also want to make changes to the Extension SDK, please fork the `hivemq-extension-sdk` repository and clone your fork:

```shell
git clone https://github.com/<your user name>/hivemq-extension-sdk.git ../hivemq-extension-sdk
git clone https://github.com/<your user name>/hivemq-edge-extension-sdk.git ../hivemq-edge-extension-sdk
```

## Code formatting
HiveMQ Edge uses Spotless to automatically format the code.
Please make sure to run `./gradlew spotlessApply` before committing your changes.

During development, you can also make use of the [Spotless IntelliJ Plugin](https://plugins.jetbrains.com/plugin/13149-spotless) to automatically format your code on save.

## 🚀 Thank you for taking the time to contribute to HiveMQ Edge! 🚀

We truly appreciate and value your time and work. ❤️
