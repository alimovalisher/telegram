include(
    "telegram-bot-client",
    "telegram-bot-core",
    "telegram-bot-poller",
    "telegram-bot-worker",
    "telegram-bot-queue-pulsar"
)

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}
