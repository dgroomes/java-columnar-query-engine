plugins {
    id("dgroomes.conventions")
    `java-library`
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)
    api(project(":geography"))
    implementation(project(":util"))
}
