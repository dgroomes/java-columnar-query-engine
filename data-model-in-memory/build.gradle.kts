plugins {
    id("dgroomes.conventions")
    `java-library`
}

dependencies {
    implementation(project(":util"))
    api(project(":data-system"))
}
