pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
    }
}

val characters = listOf(
    file("../mod-characters"),
    file("../../mods/mod-characters"),
    file("mod-characters")
).firstOrNull { it.isDirectory }
if (characters != null) {
    includeBuild(characters)
}

rootProject.name = "mod-headdb"
