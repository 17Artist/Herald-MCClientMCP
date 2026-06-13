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
val fabricLoaderVersion = rootProject.property("fabric_loader_version") as String
val fabricApiVersion = rootProject.property("fabric_api_version") as String
val architecturyApiVersion = rootProject.property("architectury_api_version") as String

architectury {
    platformSetupLoomIde()
    fabric()
}

base {
    archivesName.set("$archivesBasePrefix-fabric")
}

loom {
    silentMojangMappingsLicense()
    runs {
        named("client") {
            // Auto-connect + stable username so the server admin can /op us.
            // Disable when running offline by clearing these via -PnoAutoConnect.
            if (!project.hasProperty("noAutoConnect")) {
                val host = (project.findProperty("mc.server.host") as String?) ?: "mc8.ytonidc.com"
                val port = (project.findProperty("mc.server.port") as String?) ?: "39304"
                val user = (project.findProperty("mc.username") as String?) ?: "HeraldBot"
                // 1.20+ uses --quickPlayMultiplayer instead of the old --server/--port pair.
                programArgs("--username", user, "--quickPlayMultiplayer", "$host:$port")
            }
        }
    }
}

val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating

configurations {
    compileClasspath { extendsFrom(common) }
    runtimeClasspath { extendsFrom(common) }
    named("developmentFabric") { extendsFrom(common) }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    modImplementation("dev.architectury:architectury-fabric:$architecturyApiVersion")

    common(project(":common", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", configuration = "transformProductionFabric")) { isTransitive = false }
}

tasks.processResources {
    inputs.property("mod_id", modId)
    inputs.property("mod_version", modVersion)
    inputs.property("minecraft_version", minecraftVersion)
    filesMatching("fabric.mod.json") {
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
    exclude("architectury.common.json")
    configurations = listOf(shadowCommon)
    archiveClassifier.set("dev-shadow")
}

tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    inputFile.set(tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar").get().archiveFile)
    dependsOn("shadowJar")
    archiveClassifier.set("")
}

// Output contract: client-mod/fabric/build/libs/herald-client-fabric-<version>.jar
