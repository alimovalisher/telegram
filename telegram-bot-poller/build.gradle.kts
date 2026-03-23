dependencies {
    implementation(project(":telegram-bot-core"))


    implementation(libs.slf4j)

    testImplementation(libs.junitJupiterApi)
    testImplementation(libs.junitJupiterEngine)
    testImplementation(libs.junitPlatformLauncher)
    testImplementation(libs.mockitoCore)
    testImplementation(libs.mockitoJunitJupiter)
    testImplementation(libs.projectReactorTest)
    testImplementation(libs.mockserverNetty)
    testImplementation(libs.mockserverClient)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
