plugins {
    id("net.neoforged.moddev") version "2.0.141"
}

val modId = rootProject.property("mod_id") as String
val modVersion = rootProject.property("mod_version") as String
val minecraftVersion = rootProject.property("minecraft_version") as String
val neoforgeVersion = rootProject.property("neoforge_version") as String

base {
    archivesName.set("${rootProject.property("archives_base_prefix")}-neoforge")
}

neoForge {
    version = neoforgeVersion
}

dependencies {
    compileOnly("com.google.code.gson:gson:2.11.0")
    compileOnly("org.slf4j:slf4j-api:2.0.13")
}

tasks.processResources {
    inputs.property("mod_id", modId)
    inputs.property("mod_version", modVersion)
    inputs.property("minecraft_version", minecraftVersion)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(
            mapOf(
                "mod_id" to modId,
                "mod_version" to modVersion,
                "minecraft_version" to minecraftVersion
            )
        )
    }
}
