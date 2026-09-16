plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow") version "9.6.1"
}

group = "me.yic"
version = "2.26.3-mr.26.2.3"
java.toolchain.languageVersion.set(JavaLanguageVersion.of(25))

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://repo.glaremasters.me/repository/towny/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.121-stable")
    compileOnly(files("../../PluginLibs/Jars/Vault-1.7.3.jar"))
    compileOnly("me.clip:placeholderapi:2.12.3")
    compileOnly("com.palmergames.bukkit.towny:towny:0.102.0.9")
    compileOnly(files("../../PluginLibs/Jars/Enterprise-1.7.jar"))
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("redis.clients:jedis:6.2.0")
    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation(files("../../PluginLibs/Jars/bungeecord-chat-1.21-R0.3.jar"))
}

// 按官方 Paper -> Bukkit -> Core 覆盖优先级编译源码，保留全部业务及可选集成。
val mergedSources = layout.buildDirectory.dir("paper-sources")
val preparePaperSources by tasks.registering(Sync::class) {
    into(mergedSources)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from("XConomy-Paper/src/main/java")
    from("XConomy-Bukkit/src/main/java")
    from("XConomy-Core/src/main/java") {
        exclude("**/*.kt", "**/XConomyBungee.java", "**/XConomyVelocity.java", "**/listeners/BCsync.java", "**/listeners/Vsync.java")
    }
}
sourceSets.main {
    java.setSrcDirs(listOf(mergedSources))
    resources.setSrcDirs(listOf("XConomy-Paper/src/main/resources", "XConomy-Core/src/main/resources"))
}
tasks.compileJava {
    dependsOn(preparePaperSources)
    options.encoding = "UTF-8"
    options.release.set(25)
}
tasks.processResources {
    exclude("bungee.yml")
    filesMatching("plugin.yml") {
        filter { line -> line.replace("\${project.version}", project.version.toString()).replace("api-version: 1.13", "api-version: '26.2'") }
    }
}
tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("XConomy-Paper-${project.version}.jar")
    destinationDirectory.set(layout.projectDirectory.dir("jars"))
    relocate("com.zaxxer", "me.yic.xc_libs.zaxxer")
    relocate("org.bstats", "me.yic.xc_libs.bstats")
    from("LICENSE") { into("META-INF") }
    exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA", "module-info.class")
}
publishing {
    publications { create<MavenPublication>("paper") { artifact(tasks.shadowJar) } }
    repositories { maven { name = "Shared"; url = uri("../../PluginLibs/Maven") } }
}
