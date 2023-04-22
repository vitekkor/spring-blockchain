import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.springframework.boot") version "2.7.6"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("plugin.spring") version "1.8.20"
    kotlin("plugin.serialization") version "1.8.20"
    jacoco
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-Beta")
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.8.0")
    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}
springBoot {
    mainClass.set("com.vitekkor.blockchain.BlockchainApplicationKt")
}


tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        csv.required.set(true)
    }
}
