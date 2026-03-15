dependencies {
    api(project(":telegram-bot-client"))

    implementation(libs.slf4j)

    testImplementation(libs.junitJupiterApi)
    testImplementation(libs.junitJupiterEngine)
    testImplementation(libs.junitPlatformLauncher)
    testImplementation(libs.projectReactorTest)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
