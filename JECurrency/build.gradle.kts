import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("maven-publish")
    id("com.gradleup.shadow") version "8.3.6"
}

group = "de.jexcellence.currency"
version = "2.0.0"

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        name = "placeholderapi"
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
    maven {
        name = "sonatype-snapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
    maven {
        name = "tcoded-releases"
        url = uri("https://repo.tcoded.com/releases")
    }
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.7-R0.1-SNAPSHOT")

    implementation("com.raindropcentral.commands:RCommands:1.0.0") { isTransitive = false }
    implementation("com.raindropcentral.r18n:R18n:1.0.0") { isTransitive = false }
    implementation("com.raindropcentral.rplatform:RPlatform:1.0.0") { isTransitive = false }
    implementation("de.jexcellence.config:Evaluable:1.0.0") { isTransitive = false }
    implementation("de.jexcellence.config:GPEEE:1.0.0") { isTransitive = false }
    implementation("de.jexcellence.config:ConfigMapper:1.0.0") { isTransitive = false }
    implementation("de.jexcellence.hibernate:JEHibernate:1.0.0") { isTransitive = false }

    compileOnly("me.devnatan:inventory-framework-api:3.5.1")
    compileOnly("me.devnatan:inventory-framework-platform-bukkit:3.5.1")
    compileOnly("me.devnatan:inventory-framework-anvil-input:3.5.1")
    compileOnly("me.devnatan:inventory-framework-platform:3.5.1")
    compileOnly("me.devnatan:inventory-framework-core:3.5.1")
    compileOnly("me.devnatan:inventory-framework-platform-paper:3.5.1")

    compileOnly(platform("org.hibernate.orm:hibernate-platform:6.6.4.Final"))
    compileOnly("org.hibernate.orm:hibernate-core")
    compileOnly("jakarta.transaction:jakarta.transaction-api")
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.2.0")
    compileOnly("com.mysql:mysql-connector-j:9.2.0")
    compileOnly("com.h2database:h2:2.3.232")
    compileOnly("com.tcoded:FoliaLib:0.5.1")

    // Plugin APIs
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

sourceSets {
    val main by getting

    create("free") {
        java.srcDir("src/free/java")
        resources.srcDir("src/free/resources")
        compileClasspath += main.compileClasspath + main.output
        runtimeClasspath += main.runtimeClasspath + main.output
    }

    create("premium") {
        java.srcDir("src/premium/java")
        resources.srcDir("src/premium/resources")
        compileClasspath += main.compileClasspath + main.output
        runtimeClasspath += main.runtimeClasspath + main.output
    }
}

configurations {
    named("freeImplementation") {
        extendsFrom(configurations["implementation"])
    }
    named("premiumImplementation") {
        extendsFrom(configurations["implementation"])
    }
    named("freeRuntimeOnly") {
        extendsFrom(configurations["runtimeOnly"])
    }
    named("premiumRuntimeOnly") {
        extendsFrom(configurations["runtimeOnly"])
    }
}

fun shadowDepsFor(sourceSetName: String) =
    configurations.getByName("${sourceSetName}RuntimeClasspath")

tasks {
    val shadowFree by registering(ShadowJar::class) {
        group = "build"
        description = "Builds the Free version shadow JAR"
        archiveFileName.set("JECurrency-Free-${project.version}.jar")
        archiveClassifier.set("free")
        from(sourceSets["main"].output)
        from(sourceSets["free"].output)

        val runtimeConfig = shadowDepsFor("free")
        configurations = listOf(runtimeConfig)

        mergeServiceFiles()
        dependsOn("jar")
    }

    val shadowPremium by registering(ShadowJar::class) {
        group = "build"
        description = "Builds the Premium version shadow JAR"
        archiveFileName.set("JECurrency-Premium-${project.version}.jar")
        archiveClassifier.set("premium")
        from(sourceSets["main"].output)
        from(sourceSets["premium"].output)

        val runtimeConfig = shadowDepsFor("premium")
        configurations = listOf(runtimeConfig)

        mergeServiceFiles()
        dependsOn("jar")
    }

    build {
        dependsOn(shadowFree, shadowPremium)
    }
}