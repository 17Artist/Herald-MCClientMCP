plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.16-SNAPSHOT"
}

val modId = rootProject.property("mod_id") as String
val modVersion = rootProject.property("mod_version") as String
val minecraftVersion = rootProject.property("minecraft_version") as String
val fabricLoaderVersion = rootProject.property("fabric_loader_version") as String
val fabricApiVersion = rootProject.property("fabric_api_version") as String

base {
    archivesName.set("${rootProject.property("archives_base_prefix")}-fabric")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    compileOnly("com.google.code.gson:gson:2.11.0")
    compileOnly("org.slf4j:slf4j-api:2.0.13")
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
