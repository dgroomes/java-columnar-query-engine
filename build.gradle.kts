import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    java
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)
    implementation(platform(libs.jackson.bom))

    implementation(libs.arrow.vector)
    implementation(libs.arrow.algorithm)
    implementation(libs.arrow.memory.netty)
    implementation(libs.jackson.databind)

    testImplementation(libs.assertj)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

application {
    mainClass.set("dgroomes.Runner")
}

tasks {

    withType<JavaExec> {
        jvmArgs = listOf(
            // Apache Arrow accesses internal Java modules reflectively. These modules need to be "opened" during
            // runtime.
            //
            // See https://arrow.apache.org/docs/java/install.html#java-compatibility
            "--add-opens=java.base/java.nio=ALL-UNNAMED",
        )
    }

    test {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
        }

        jvmArgs = listOf(
            "--add-opens=java.base/java.nio=ALL-UNNAMED",
        )
    }
}
