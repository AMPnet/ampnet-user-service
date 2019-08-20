import com.google.protobuf.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.3.41"
    kotlin("plugin.jpa") version kotlinVersion
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion

    id("org.springframework.boot") version "2.1.7.RELEASE"
    id("io.spring.dependency-management") version "1.0.8.RELEASE"
    id("com.google.cloud.tools.jib") version "1.5.0"
    id("org.jlleitschuh.gradle.ktlint") version "8.2.0"
    id("io.gitlab.arturbosch.detekt").version("1.0.0")
    id("org.asciidoctor.convert") version "2.3.0"
    id("com.google.protobuf") version "0.8.10"
    idea
    jacoco
}

group = "com.ampnet"
version = "0.4.0"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    val jjwtVersion = "0.10.7"
    val junitVersion = "5.3.2"

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.springframework.social:spring-social-facebook:2.0.3.RELEASE")
    implementation("com.github.spring-social:spring-social-google:1.1.3")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.flywaydb:flyway-core")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    runtimeOnly("org.postgresql:postgresql")

    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    implementation("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")
    implementation("io.github.microutils:kotlin-logging:1.7.6")
    implementation("net.logstash.logback:logstash-logback-encoder:5.3")
    implementation("io.micrometer:micrometer-registry-prometheus:1.1.5")
    implementation("net.devh:grpc-spring-boot-starter:2.5.0.RELEASE")
    implementation("org.iban4j:iban4j:3.2.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude("junit")
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

tasks.test {
    useJUnitPlatform()
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.6.1"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.20.0"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}

jib {
    val dockerUsername: String = System.getenv("DOCKER_USERNAME") ?: "DOCKER_USERNAME"
    val dockerPassword: String = System.getenv("DOCKER_PASSWORD") ?: "DOCKER_PASSWORD"
    val identyumUsername: String = System.getenv("IDENTYUM_USERNAME") ?: "IDENTYUM_USERNAME"
    val identyumPassword: String = System.getenv("IDENTYUM_PASSWORD") ?: "IDENTYUM_PASSWORD"
    val identyumKey: String = System.getenv("IDENTYUM_KEY") ?: "IDENTYUM_KEY"

    to {
        image = "ampnet/crowdfunding-user-service:$version"
        auth {
            username = dockerUsername
            password = dockerPassword
        }
        tags = setOf("latest")
    }
    container {
        creationTime = "USE_CURRENT_TIMESTAMP"
        environment = mapOf(
            "IDENTYUM_USERNAME" to identyumUsername,
            "IDENTYUM_PASSWORD" to identyumPassword,
            "IDENTYUM_KEY" to identyumKey
        )
    }
}

jacoco.toolVersion = "0.8.4"
tasks.jacocoTestReport {
    reports {
        xml.isEnabled = false
        csv.isEnabled = false
        html.destination = file("$buildDir/reports/jacoco/html")
    }
    sourceDirectories.setFrom(listOf(file("${project.projectDir}/src/main/kotlin")))
    classDirectories.setFrom(fileTree("$buildDir/classes/kotlin/main").apply {
        exclude("**/model/**", "**/pojo/**")
    })
    dependsOn(tasks.test)
}
tasks.jacocoTestCoverageVerification {
    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude("com/ampnet/*/proto/**")
        }
    )
    violationRules {
        rule {
            limit {
                minimum = "0.7".toBigDecimal()
            }
        }
    }
    mustRunAfter(tasks.jacocoTestReport)
}

detekt {
    input = files("src/main/kotlin")
}

task("qualityCheck") {
    dependsOn(tasks.ktlintCheck, tasks.jacocoTestReport, tasks.jacocoTestCoverageVerification, tasks.detekt)
}

tasks.asciidoctor {
    attributes(mapOf("snippets" to file("build/generated-snippets")))
    dependsOn(tasks.test)
}

tasks.register<Copy>("copyDocs") {
    from(file("$buildDir/asciidoc/html5"))
    into(file("src/main/resources/static/docs"))
    dependsOn(tasks.asciidoctor)
}
