plugins {
    java
}

group = "com.extantape"
version = "0.10.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
}

// Built against the oldest supported Paper, so nothing newer than 26.1 sneaks in.
val paperApi = "io.papermc.paper:paper-api:26.1.2.build.74-stable"
// Optional: Wranglin' as an AuraSkills skill. Reached only when AuraSkills is installed.
val auraSkillsApi = "dev.aurelium:auraskills-api-bukkit:2.4.0"
// Optional: placeholders for quest conditions. Reached only when PlaceholderAPI is installed.
val placeholderApi = "me.clip:placeholderapi:2.12.3"

dependencies {
    compileOnly(paperApi)
    compileOnly(auraSkillsApi)
    compileOnly(placeholderApi)

    testImplementation(paperApi)
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:removal"))
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("paper-plugin.yml") { expand(props) }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName.set("MobSquad")
}
