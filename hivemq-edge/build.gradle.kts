import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins.withId("com.hivemq.edge-version-updater") {
    project.ext.set(
        "versionUpdaterFiles",
        arrayOf("src/main/resources/hivemq-edge-configuration.json", "gradle.properties")
    )
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.openapitools:openapi-generator-gradle-plugin")
    }
}

plugins {
    java
    `java-library`
    `maven-publish`
    signing
    alias(libs.plugins.shadow)
    alias(libs.plugins.defaults)
    alias(libs.plugins.metadata)
    alias(libs.plugins.javadoclinks)
    alias(libs.plugins.githubrelease)
    alias(libs.plugins.hivemq.license)
    alias(libs.plugins.versions)

    // Code Quality Plugins
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.forbiddenapis)

    alias(libs.plugins.openapi.generator)

    id("pmd")
    id("com.hivemq.edge-version-updater")
    id("com.hivemq.repository-convention")
    id("com.hivemq.jacoco-convention")
    id("com.hivemq.spotless-convention")
    id("com.hivemq.errorprone-convention")
    id("com.hivemq.nullaway-convention")
}

group = "com.hivemq"
description = "HiveMQ Edge is a Gateway exposing MQTT and Edge services"

metadata {
    readableName.set("HiveMQ Edge")
    organization {
        name.set("HiveMQ GmbH")
        url.set("https://www.hivemq.com/")
    }
    license {
        apache2()
    }
    developers {
        register("cschaebe") {
            fullName.set("Christoph Schaebel")
            email.set("christoph.schaebel@hivemq.com")
        }
        register("simon622") {
            fullName.set("Simon Johnson")
            email.set("simon.johnson@hivemq.com")
        }
        register("lbrandl") {
            fullName.set("Lukas Brandl")
            email.set("lukas.brandl@hivemq.com")
        }
        register("flimpoeck") {
            fullName.set("Florian Limpoeck")
            email.set("florian.limpoeck@hivemq.com")
        }
        register("sauroter") {
            fullName.set("Georg Held")
            email.set("georg.held@hivemq.com")
        }
        register("SgtSilvio") {
            fullName.set("Silvio Giebl")
            email.set("silvio.giebl@hivemq.com")
        }
    }
    github {
        org.set("hivemq")
        repo.set("hivemq-edge")
        issues()
    }
}

// Create a configuration for javadocLinks that excludes dependencies without javadoc jars
val javadocLinksClasspath: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    extendsFrom(configurations.compileClasspath.get())
    // jitpack.io doesn't provide a javadoc jar for json-schema-inferrer
    exclude(group = "com.github.saasquatch", module = "json-schema-inferrer")
    // netty-codec is a metadata module without its own javadoc
    exclude(group = "io.netty", module = "netty-codec")
    // Copy attributes from compileClasspath to ensure proper resolution
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_API))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
        attribute(
            LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
            objects.named(LibraryElements::class.java, LibraryElements.JAR)
        )
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class.java, Bundling.EXTERNAL))
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
    }
}

tasks.javadocLinks {
    useConfiguration(javadocLinksClasspath)
}

// ******************** java ********************

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withJavadocJar()
    withSourcesJar()
}

// ******************** dependencies ********************

// Runtime stuffs
dependencies {

    // HiveMQ
    api(libs.hivemq.extensionsdk)
    api(libs.hivemq.edge.extensionsdk)
    api(libs.hivemq.edge.adaptersdk)

    // netty
    implementation(libs.netty.buffer)
    implementation(libs.netty.codec)
    implementation(libs.netty.codec.http)
    implementation(libs.netty.commons)
    implementation(libs.netty.handler)
    implementation(libs.netty.transport)

    // logging
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.jultoslf4j)
    implementation(libs.logback.classic)

    // security
    implementation(libs.bouncycastle.prov)
    implementation(libs.bouncycastle.pkix)

    // override transitive dependencies that have security vulnerabilities
    implementation(platform(libs.kotlin.bom))
    constraints {
        implementation(libs.apache.commons.compress)
    }

    // config
    implementation(libs.jaxb2.impl)
    implementation(libs.jaxb4.impl)
    implementation(libs.jaxb4.bind)

    // metrics
    implementation(libs.dropwizard.metrics)
    implementation(libs.dropwizard.metrics.jmx)
    runtimeOnly(libs.dropwizard.metrics.logback)
    implementation(libs.dropwizard.metrics.jvm)

    // dependency injection
    implementation(libs.dagger)
    annotationProcessor(libs.dagger.compiler)

    implementation(libs.jakarta.annotation.api)

    // CSV
    implementation(libs.apache.commons.csv)

    // common
    implementation(libs.apache.commons.io)
    implementation(libs.apache.commons.lang)
    implementation(libs.guava) {
        exclude("org.checkerframework", "checker-qual")
        exclude("com.google.errorprone", "error_prone_annotations")
    }
    implementation(libs.javacrumbs.futureconverter)

    // com.google.code.findbugs:jsr305 (transitive dependency of com.google.guava:guava) is used in imports
    implementation(libs.zeroallocationhashing)
    implementation(libs.jctools)

    // mqtt-sn codec
    implementation(libs.mqtt.sn.codec)
    implementation(libs.hivemq.mqtt.client)

    // JAX-RS + Http Connector + Serializers
    implementation(libs.jersey.container.jdk.http)
    implementation(libs.jersey.hk2)
    implementation(libs.jersey.media.json.jackson)
    implementation(libs.jersey.media.multipart)

    // Jackson
    implementation(libs.jackson.jaxrs.json.provider)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.datatype.jdk8)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.databind.nullable)
    implementation(libs.jackson.dataformat.xml)
    implementation(libs.jackson.dataformat.yaml)

    // Open API
    implementation(libs.swagger.annotations)
    implementation(libs.swagger.jaxrs)

    // JWT
    implementation(libs.jose4j)

    // LDAP
    implementation(libs.unboundid.ldap.sdk)

    // json schema
    implementation(libs.json.schema.validator)
    implementation(libs.victools.jsonschema.generator)
    implementation(libs.victools.jsonschema.jackson)
    implementation(libs.json.schema.inferrer)

    // json path
    implementation(libs.json.path)

    // i18n
    implementation(libs.freemarker)

    // Edge modules
    compileOnly("com.hivemq:hivemq-edge-module-etherip")
    compileOnly("com.hivemq:hivemq-edge-module-plc4x")
    compileOnly("com.hivemq:hivemq-edge-module-http")
    compileOnly("com.hivemq:hivemq-edge-module-modbus")
    compileOnly("com.hivemq:hivemq-edge-module-mtconnect")
    compileOnly("com.hivemq:hivemq-edge-module-databases")
    compileOnly("com.hivemq:hivemq-edge-module-file")
    // hivemq-edge-module-opcua: NOT listed here because opcua has compileOnly on core (for browse types),
    // and bidirectional compileOnly creates a Gradle task cycle. OPC UA module is still included in the
    // distribution — the dependency direction is opcua -> core, not core -> opcua.

    // FIXME: should be in module instead
    // we need better module isolation for that as the modules pick up Netty from the app class loader
    implementation(libs.protobuf)
}

configurations.all {
    resolutionStrategy {
        /*
         * https://nvd.nist.gov/vuln/detail/CVE-2024-57699
         * A security issue was found in Netplex Json-smart 2.5.0 through 2.5.1.
         * When loading a specially crafted JSON input, containing a large number of ’{’,
         * a stack exhaustion can be trigger, which could allow an attacker to cause a Denial of Service (DoS).
         * This issue exists because of an incomplete fix for CVE-2023-1370.
         */
        force(libs.json.smart)
    }
}

// ******************** test ********************

dependencies {
    testAnnotationProcessor(libs.dagger.compiler)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation(libs.mockito.junit.jupiter)

    testImplementation(libs.equalsverifier)
    testImplementation(libs.concurrentunit)
    testImplementation(libs.shrinkwrap.api)
    testRuntimeOnly(libs.shrinkwrap.impl)
    testImplementation(libs.bytebuddy)
    testImplementation(libs.wiremock.jre8.standalone)
    testImplementation(libs.javassist)
    testImplementation(libs.awaitility)
    testImplementation(libs.assertj)
    testImplementation(libs.systemstubs)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
    minHeapSize = "128m"
    maxHeapSize = "2048m"
    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Duser.language=en",
        "-Duser.region=US",
        "--add-opens",
        "java.base/java.lang=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.nio=ALL-UNNAMED",
        "--add-opens",
        "java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.util=ALL-UNNAMED",
        "--add-opens",
        "java.xml/javax.xml.namespace=ALL-UNNAMED",
        "--add-opens",
        "jdk.management/com.sun.management.internal=ALL-UNNAMED",
        "--add-exports",
        "java.base/jdk.internal.misc=ALL-UNNAMED"
    )

    val inclusions = rootDir.resolve("inclusions.txt")
    val exclusions = rootDir.resolve("exclusions.txt")
    if (inclusions.exists()) {
        include(inclusions.readLines())
    } else if (exclusions.exists()) {
        exclude(exclusions.readLines())
    }

    testLogging {
        events = setOf(TestLogEvent.STARTED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

// ******************** OpenAPI ********************
val buildDirectory = layout.buildDirectory.get()
tasks.register<GenerateTask>("genJaxRs") {
    inputSpec.set("$projectDir/../ext/hivemq-edge-openapi.yaml")
    outputDir.set("$buildDirectory/generated/openapi")
    templateDir.set("$projectDir/../hivemq-edge-openapi/openapi/templates/Java")
    generatorName.set("jaxrs-spec")
    apiPackage.set("com.hivemq.edge.api")
    modelPackage.set("com.hivemq.edge.api.model")
    invokerPackage.set("com.hivemq.edge.api")
    generateApiTests.set(false)
    configOptions.set(
        hashMapOf(
            "useJakartaEe" to "true",
            "dateLibrary" to "java8",
            "generateBuilders" to "true",
            "generatePom" to "false",
            "interfaceOnly" to "true",
            "useTags" to "true",
            "returnResponse" to "true",
            "openApiNullable" to "false"
        )
    )
}

sourceSets {
    main {
        java {
            srcDirs("$buildDirectory/generated/openapi/src/gen/java")
            srcDirs("$buildDirectory/generated/openapi/src/main/java")
        }
    }
}

tasks.processResources {
    from("$projectDir/../ext") {
        include("remote-endpoints.txt")
        into("ext")
    }
}

// ******************** distribution ********************

tasks.jar {
    manifest.attributes(
        "Implementation-Title" to "HiveMQ",
        "Implementation-Vendor" to
            metadata.organization
                .get()
                .name
                .get(),
        "Implementation-Version" to project.version,
        "HiveMQ-Version" to project.version,
        "HiveMQ-Edge-Version" to project.version,
        "Main-Class" to "com.hivemq.HiveMQEdgeMain"
    )
}

tasks.compileJava {
    dependsOn(tasks.named("genJaxRs"))
}

tasks.named("sourcesJar") {
    dependsOn(tasks.named("genJaxRs"))
}

tasks.shadowJar {
    mergeServiceFiles()
    from(frontendBinary) {
        into("httpd")
    }
}

val hivemqZip by tasks.registering(Zip::class) {
    group = "distribution"
    dependsOn(tasks.named("updateThirdPartyLicenses"))

    val name = "hivemq-edge-${project.version}"

    archiveFileName.set("$name.zip")

    from("src/distribution") { exclude("**/.gitkeep") }
    from("src/main/resources/config.xml") { into("conf") }
    from(tasks.shadowJar) { into("bin").rename { "hivemq.jar" } }
    into(name)

    filesMatching(listOf("**/bin/run.sh", "**/bin/init-script/hivemq")) {
        permissions { unix("rwxr-xr-x") }
    }
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).addStringOption("-html5")

    include("com/hivemq/embedded/*")

    val javadocCleanerResult =
        providers
            .javaexec {
                classpath(layout.projectDirectory.file("../gradle/tools/javadoc-cleaner-1.0.jar"))
            }.result
    doLast {
        javadocCleanerResult.get()
    }
}

// ******************** checks ********************

pmd {
    toolVersion = libs.versions.pmd.get()
    sourceSets = listOf(project.sourceSets.main.get())
    isIgnoreFailures = true
    rulesMinimumPriority.set(3)
}

spotbugs {
    toolVersion.set(libs.versions.spotBugs.get())
    ignoreFailures.set(true)
    reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
}

dependencies {
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.14.0")
}

forbiddenApis {
    bundledSignatures = setOf("jdk-system-out")
}

tasks.forbiddenApisMain {
    exclude("**/BatchedException.class")
    exclude("**/LoggingBootstrap.class")
    exclude("**/CreateAdapterBlueprint.class")
}

tasks.forbiddenApisTest { enabled = false }

// ******************** compliance ********************

hivemqLicense {
    projectName.set("HiveMQ Edge")
    thirdPartyLicenseDirectory.set(layout.projectDirectory.dir("src/distribution/third-party-licenses"))
    ignoredGroupPrefixes.addAll("com.hivemq", "com.github.saasquatch")
}

/*** artifacts ***/

val frontend: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = false
}

val frontendBinary: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("binary"))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("release"))
    }
    extendsFrom(frontend)
}

dependencies {
    frontend("com.hivemq:hivemq-edge-frontend")
}

val releaseBinary: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("binary"))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("release"))
    }
}

val releaseJar: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("jar"))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("release"))
    }
}

val thirdPartyLicenses: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("third-party-licenses"))
    }
}

artifacts {
    add(releaseBinary.name, hivemqZip)
    add(releaseJar.name, tasks.shadowJar)
    add(
        thirdPartyLicenses.name,
        tasks.updateThirdPartyLicenses.flatMap { it.outputDirectory }
    )
}
