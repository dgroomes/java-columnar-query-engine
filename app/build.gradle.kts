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
    implementation(project(":data-system-serial-indices-arrays"))

    testImplementation(libs.assertj)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

application {
    mainClass.set("dgroomes.app.Runner")
}
