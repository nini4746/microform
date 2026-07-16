plugins {
    java
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.microform"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Data
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    runtimeOnly("org.postgresql:postgresql")

    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Observability
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // Rate limiting
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // JSON
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // CSV
    implementation("org.apache.commons:commons-csv:1.11.0")

    // API Docs
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // DB Migration
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

tasks.withType<Test> {
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

// Default suite: everything except the heavy Docker streaming guard.
tasks.named<Test>("test") {
    useJUnitPlatform { excludeTags("streaming") }
}

// CSV streaming/OOM guard: real Postgres via Testcontainers, heap capped at
// 128m so a buffering regression OOMs instead of silently passing. Needs Docker.
tasks.register<Test>("csvStreamingTest") {
    description = "Runs the CSV streaming/OOM guard under a 128m heap (needs Docker)."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { includeTags("streaming") }
    maxHeapSize = "128m"
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    // Forward the external-Postgres coordinates (the local Docker Engine is too
    // new for the bundled docker-java, so this test uses a manually-started DB).
    listOf("mf.test.pg.url", "mf.test.pg.user", "mf.test.pg.pass").forEach { key ->
        System.getProperty(key)?.let { systemProperty(key, it) }
    }
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("microform.jar")
}
