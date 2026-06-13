plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `maven-publish`
}

val modId = rootProject.property("mod_id") as String
val modVersion = rootProject.property("mod_version") as String
val archivesBasePrefix = rootProject.property("archives_base_prefix") as String
val minecraftVersion = rootProject.property("minecraft_version") as String
val forgeVersion = rootProject.property("forge_version") as String
val architecturyApiVersion = rootProject.property("architectury_api_version") as String

architectury {
    platformSetupLoomIde()
    forge()
}

base {
    archivesName.set("$archivesBasePrefix-forge")
}

loom {
    silentMojangMappingsLicense()
    forge {
        convertAccessWideners.set(true)
    }
}

val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating

configurations {
    compileClasspath { extendsFrom(common) }
    runtimeClasspath { extendsFrom(common) }
    named("developmentForge") { extendsFrom(common) }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())

    "forge"("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")
    modImplementation("dev.architectury:architectury-forge:$architecturyApiVersion")

    common(project(":common", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", configuration = "transformProductionForge")) { isTransitive = false }
}

tasks.processResources {
    inputs.property("mod_id", modId)
    inputs.property("mod_version", modVersion)
    inputs.property("minecraft_version", minecraftVersion)
    filesMatching("META-INF/mods.toml") {
        expand(
            mapOf(
                "mod_id" to modId,
                "mod_version" to modVersion,
                "minecraft_version" to minecraftVersion
            )
        )
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    exclude("fabric.mod.json")
    exclude("architectury.common.json")
    configurations = listOf(shadowCommon)
    archiveClassifier.set("dev-shadow")
}

tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    inputFile.set(tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar").get().archiveFile)
    dependsOn("shadowJar")
    archiveClassifier.set("")
}

// Output contract: client-mod/forge/build/libs/herald-client-forge-<version>.jar
