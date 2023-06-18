plugins {
    id("dgroomes.conventions")
    application
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)
    implementation(project(":geography-loader"))
    implementation(project(":util"))
    implementation(project(":data-model-in-memory"))
    implementation(project(":query-engine"))

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
            "--enable-preview"
        )
    }

    test {
        jvmArgs = listOf(
            "--enable-preview"
        )
    }
}
