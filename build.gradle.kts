plugins {
    id("java")
    id("idea")
    id("com.vanniktech.maven.publish") version "0.36.0"
}

java {
    version = JavaVersion.VERSION_25
}

repositories {
    mavenLocal()
    mavenCentral()
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "idea")
    apply(plugin = "com.vanniktech.maven.publish")

    tasks.matching { it.name == "generateMetadataFileForMavenPublication" }.configureEach {
        dependsOn(tasks.matching { it.name == "plainJavadocJar" })
    }

    idea {
        module {
            setDownloadJavadoc(true)
            setDownloadSources(true)
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }

    java {
        withSourcesJar()
    }

    mavenPublishing {
        publishToMavenCentral(automaticRelease = true)

        signAllPublications()

        pom {
            name.set("Telegram Bot Client")
            description.set("Reactive Java client for the Telegram Bot API built on Spring WebFlux")
            url.set("https://github.com/alimovalisher/telegram")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }

            developers {
                developer {
                    id.set("alimov")
                    name.set("Alisher Alimov")
                    url.set("https://alimov.dev")
                }
            }

            scm {
                connection.set("scm:git:git://github.com:alimovalisher/telegram.git")
                developerConnection.set("scm:git:ssh://github.com:alimovalisher/telegram.git")
                url.set("https://github.com:alimovalisher/telegram")
            }
        }
    }



    tasks.named<Test>("test") {
        useJUnitPlatform()
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
