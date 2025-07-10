
plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.justlime"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigotmc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://jitpack.io")
}
dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21-R0.1-SNAPSHOT")
    compileOnly("com.github.ItzYashvardhan:RedeemCodeX-API:1.0.2")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.ktor:ktor-server-core:3.2.1")
    implementation("io.ktor:ktor-server-netty:3.2.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.2.1")
}

val targetJavaVersion = 8
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}
tasks.shadowJar {
    mergeServiceFiles()
    minimize {
        exclude(dependency("io.ktor:ktor-serialization-kotlinx-json"))
    }
}
tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

// Task to copy the jar to the server plugins folder
tasks.register<Copy>("copyToServerPlugins") {
    dependsOn("shadowJar")  // Ensure shadowJar completes before copying
    from(layout.buildDirectory.dir("libs/${project.name}-${project.version}-all.jar"))  // Use layout.buildDirectory
    into("E:/Minecraft/servers/Plugin-Maker/plugins")  // Destination folder
}

// Combined task to build and copy
tasks.register("buildAndCopy") {
    dependsOn("build", "copyToServerPlugins")
}
