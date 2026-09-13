buildscript {
    repositories {
        maven("https://repo.spongepowered.org/maven")
        mavenCentral()
    }
}

plugins {
    java
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

repositories {
    maven("https://repo.spongepowered.org/maven")
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-tree:9.6")
    implementation("org.spongepowered:mixin:0.8.5")
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "8"
    targetCompatibility = "8"
}

sourceSets.main {
    java {
        srcDir("../common/java")
    }
}