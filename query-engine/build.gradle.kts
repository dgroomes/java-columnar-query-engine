plugins {
    id("dgroomes.conventions")
    `java-library`
}

dependencies {
    implementation(project(":util"))
    api(project(":query-api"))

    testImplementation(libs.assertj)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
