plugins {
    id("net.fabricmc.fabric-loom") version "1.16-SNAPSHOT"
}

val minecraftVersion = property("minecraft_version").toString()
val javaVersion = property("java_version").toString()
val fabricLoaderVersion = property("fabric_loader_version").toString()
val fabricApiVersion = property("fabric_api_version").toString()
val modVersion = property("mod_version").toString()
val mavenGroup = property("maven_group").toString()
val archivesBaseName = property("archives_base_name").toString()
val zstdVersion = property("zstd_version").toString()

group = mavenGroup
version = providers.gradleProperty("publishVersion").orElse(modVersion).get()

base {
    archivesName.set(archivesBaseName)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
    withSourcesJar()
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src/main/java", "headdb-api/src/main/java", "headdb-core/src/main/java"))
    }
    test {
        java.setSrcDirs(listOf("src/test/java", "headdb-api/src/test/java", "headdb-core/src/test/java"))
    }
}

loom {
    splitEnvironmentSourceSets()
    mods {
        create("mod_headdb") {
            sourceSet(sourceSets["main"])
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    implementation(include("com.github.luben:zstd-jni:$zstdVersion")!!)
    compileOnly("org.jetbrains:annotations:24.0.0")
    testCompileOnly("org.jetbrains:annotations:24.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.1")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.1")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion.toInt())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.jar {
    from("LICENSE")
    from("LICENSES") {
        into("LICENSES")
    }
}
