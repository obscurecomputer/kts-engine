plugins {
    kotlin("jvm") version "2.3.0"
}

group = "me.znotchill"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.znotchill.me/repository/maven-releases/")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:2.3.0")
    implementation("org.jetbrains.kotlin:kotlin-scripting-common:2.3.0")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:2.3.0")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.3.0")
    implementation("org.ow2.asm:asm:9.8")
    implementation("org.ow2.asm:asm-util:9.8")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

    implementation("net.minestom:minestom:2026.04.13-1.21.11")
    implementation("me.znotchill:blossom:1.5.9")
}

tasks.test {
    useJUnitPlatform()
}