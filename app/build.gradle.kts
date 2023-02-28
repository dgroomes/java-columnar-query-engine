plugins {
    id("dgroomes.conventions")
    application
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)
    implementation(project(":geography-loader"))
    implementation(project(":util"))
//    implementation(project(":query-engine"))
//    implementation(project(":geography-query"))

    testImplementation(libs.assertj)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

application {
    mainClass.set("dgroomes.app.Runner")
}

tasks {

    withType<JavaExec> {
        jvmArgs = listOf(
            // Apache Arrow accesses internal Java modules reflectively. These modules need to be "opened" during
            // runtime.
            //
            // See https://arrow.apache.org/docs/java/install.html#java-compatibility
            "--add-opens=java.base/java.nio=ALL-UNNAMED",
            "--enable-preview"
        )
    }

    test {
        jvmArgs = listOf(
            "--add-opens=java.base/java.nio=ALL-UNNAMED",
            "--enable-preview"
        )
    }
}
