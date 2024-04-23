plugins {
    id("java")
}

group = "com.hivemq"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // TODO
    compileOnly("com.hivemq:hivemq-edge-extension-sdk:${property("hivemq-edge-extension-sdk.version")}")


    compileOnly(libs.guava)
    compileOnly(libs.jackson.jaxrs.jsonProvider)
    compileOnly(libs.networkNT)
    compileOnly(libs.swagger.annotations)
    compileOnly(libs.apache.commonsLang)

    compileOnly("com.github.victools:jsonschema-generator:4.35.0")
    compileOnly("com.github.victools:jsonschema-module-jackson:4.35.0")

}


dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
