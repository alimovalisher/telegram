dependencies {
    implementation(libs.springBootStarterWebflux)

    implementation(libs.slf4j)

    // Micrometer Prometheus registry for metrics scraping
    implementation(libs.micrometerRegistryPrometheus)

    implementation(libs.jetbrainsAnnotations)

    // Guava for hashing utilities
    implementation(libs.guava)
    implementation(libs.lang3)

    // Adding RSocket support
    testImplementation(libs.projectReactorTest)
    testImplementation(libs.springBootStarterTest)

    testImplementation(libs.junitJupiterEngine)
    testImplementation(libs.junitJupiterApi)

    // Add mockito dependencies explicitly
    testImplementation(libs.mockitoCore)
    testImplementation(libs.mockitoJunitJupiter)
    testImplementation(libs.junitPlatformLauncher)

    testImplementation(libs.mockserverNetty)
    testImplementation(libs.mockserverClient)
}



tasks.withType<Test> {
    useJUnitPlatform()
}