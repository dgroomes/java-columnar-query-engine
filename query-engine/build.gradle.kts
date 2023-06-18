plugins {
    id("dgroomes.conventions")
    `java-library`
}

dependencies {
    implementation(project(":util"))
    api(project(":query-api"))
    api(project(":data-model-api"))
    implementation(project(":data-model-in-memory"))

    testImplementation(libs.assertj)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
