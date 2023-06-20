plugins {
    id("dgroomes.conventions")
    `java-library`
}

dependencies {
    implementation(project(":util"))
    api(project(":data-system"))

    // The 'data-model-in-memory' module can be used as a test dependency but not as a main dependency.
    testImplementation(project(":data-model-in-memory"))
    testImplementation(libs.assertj)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
