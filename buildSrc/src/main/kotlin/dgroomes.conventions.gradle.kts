import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    java
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(19))
    }
}

tasks {

    /**
     * Configure Java tasks to enable Java language "Preview Features"
     */
    withType(JavaCompile::class.java) {
        options.compilerArgs.addAll(arrayOf("--enable-preview"))
    }

    test {
        jvmArgs = listOf("--enable-preview")
        useJUnitPlatform()
        testLogging {
            // showStandardStreams = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}
