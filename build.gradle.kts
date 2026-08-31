
plugins {
    `maven-publish`
    kotlin("jvm") version "2.4.10"
}
val jvmVersion = "2.4.10"

group = "computer.obscure"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.znotchill.me/repository/maven-releases/")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:$jvmVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-common:$jvmVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:$jvmVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:$jvmVersion")
    implementation("org.ow2.asm:asm:9.8")
    implementation("org.ow2.asm:asm-util:9.8")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            group
            artifactId = "endergine"
            version
        }
    }

    repositories {
        maven {
            name = "obscurerepo"
            url = uri("https://repo.obscure.computer/repository/maven-releases/")
            credentials {
                username = findProperty("obscureUsername") as String? ?: System.getenv("OBSCURE_MAVEN_USER")
                password = findProperty("obscurePassword") as String? ?: System.getenv("OBSCURE_MAVEN_PASS")
            }
        }

        mavenLocal()
    }
}