import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    java
}

repositories {
    mavenCentral()
}

tasks {

    test {
        useJUnitPlatform()
        testLogging {
            // showStandardStreams = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}
