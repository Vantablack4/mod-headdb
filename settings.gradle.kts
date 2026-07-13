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

rootProject.name = "mod-headdb"
