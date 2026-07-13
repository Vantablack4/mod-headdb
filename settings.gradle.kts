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

val permissions = listOf(file("../mod-permissions"), file("mod-permissions")).firstOrNull { it.isDirectory }
if (permissions != null) {
    includeBuild(permissions)
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
