plugins {
    id("dgroomes.conventions")
    `java-library`
}

dependencies {
    implementation(project(":util"))
    api(project(":query-api"))
    api(project(":data-model-api"))

    // This dependency needs to go away. The 'query-engine' module should code to the interface of the 'data-model-api'
    // module. If part of the query engine is implementation specific, then that's fine but that needs to go in a
    // different module.
    implementation(project(":data-model-in-memory"))

    testImplementation(libs.assertj)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
