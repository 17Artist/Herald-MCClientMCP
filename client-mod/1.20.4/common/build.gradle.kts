plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    `maven-publish`
}

val modId = rootProject.property("mod_id") as String
val modVersion = rootProject.property("mod_version") as String
val archivesBasePrefix = rootProject.property("archives_base_prefix") as String
val minecraftVersion = rootProject.property("minecraft_version") as String
val fabricLoaderVersion = rootProject.property("fabric_loader_version") as String

architectury {
    common("fabric", "forge")
}

base {
    archivesName.set("$archivesBasePrefix-common")
}

loom {
    silentMojangMappingsLicense()
    accessWidenerPath.set(file("src/main/resources/herald-common.accesswidener"))
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")

    // @ExpectPlatform annotation classes are auto-added by architectury-plugin.

    // Gson + SLF4J are shipped by Minecraft at runtime; compileOnly here.
    compileOnly("com.google.code.gson:gson:2.11.0")
    compileOnly("org.slf4j:slf4j-api:2.0.13")

    // Pure-Java unit tests (no MC runtime required)
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("com.google.code.gson:gson:2.11.0")
    testImplementation("org.slf4j:slf4j-simple:2.0.13")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("herald.test.mode", "true")
}

tasks.processResources {
    inputs.property("mod_id", modId)
    inputs.property("mod_version", modVersion)
    inputs.property("minecraft_version", minecraftVersion)

    filesMatching(
        listOf(
            "herald.properties",
            "META-INF/mods.toml",
            "fabric.mod.json"
        )
    ) {
        expand(
            mapOf(
                "mod_id" to modId,
                "mod_version" to modVersion,
                "minecraft_version" to minecraftVersion
            )
        )
    }
}
