import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent
import com.google.protobuf.gradle.*


group = "no.nav"

object Versions {
    const val json = "20171018"
    const val kafka = "2.3.0"
    const val konfig = "1.6.10.0"
    const val kotlinlogging = "1.7.6"
    const val kotlinCoroutines = "1.3.2"
    const val kotlin = "1.3.50"
    const val ktor = "1.2.5"
    const val logback = "1.2.3"
    const val logstashEncoder = "6.2"
    const val prometheus = "0.8.0"
    const val smCommonVersion = "1.bba46d9"
    const val vaultDriver = "3.1.0"
    const val vaultJdbcVersion = "1.3.1"
    const val guava = "11.0.2"
    const val grpc = "1.26.0"
    
    // test dependencies
    const val spek = "2.0.8"
    const val kluent = "1.56"
    const val kafkaEmbedded = "2.4.0"

}

val mainClass = "no.nav.brreg.underenheter.Bootstrap"

plugins {
    kotlin("jvm") version "1.3.50"
    idea
    id("org.jmailen.kotlinter") version "2.1.2"
    id("com.github.ben-manes.versions") version "0.27.0"
    id("com.github.johnrengelman.shadow") version "5.1.0"
    id("com.google.protobuf") version "0.8.11"
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

val githubUser: String by project
val githubPassword: String by project

repositories {
    maven(url="https://dl.bintray.com/kotlin/ktor")
    maven(url="https://kotlin.bintray.com/kotlinx")
    maven(url="http://packages.confluent.io/maven/")
    mavenCentral()
    jcenter()
    maven {
        setUrl("https://maven.pkg.github.com/navikt/syfosm-common")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}

dependencies {
    compile(kotlin("stdlib"))
    compile("org.json:json:${Versions.json}")

    compile("io.ktor:ktor-server-netty:${Versions.ktor}")
    compile("io.ktor:ktor-auth:${Versions.ktor}")
    compile("io.ktor:ktor-auth-jwt:${Versions.ktor}")
    compile("io.ktor:ktor-jackson:${Versions.ktor}")

    compile("org.apache.kafka:kafka-clients:${Versions.kafka}")

    compile("com.natpryce:konfig:${Versions.konfig}")

    compile("com.google.guava:guava:${Versions.guava}")

    compile("io.grpc:grpc-stub:${Versions.grpc}")
    compile("io.grpc:grpc-protobuf:${Versions.grpc}")

    compile("io.prometheus:simpleclient_common:${Versions.prometheus}")
    compile("io.prometheus:simpleclient_hotspot:${Versions.prometheus}")

    compile("io.github.microutils:kotlin-logging:${Versions.kotlinlogging}")
    compile("net.logstash.logback:logstash-logback-encoder:${Versions.logstashEncoder}")


    compile("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${Versions.kotlinCoroutines}")

    compile("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${Versions.kotlinCoroutines}")

    runtimeOnly("ch.qos.logback:logback-classic:${Versions.logback}")

    testImplementation("no.nav:kafka-embedded-env:${Versions.kafkaEmbedded}")
    testImplementation("org.amshove.kluent:kluent:${Versions.kluent}")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:${Versions.spek}") {
        exclude(group = "org.jetbrains.kotlin")
    }
    testRuntimeOnly ("org.spekframework.spek2:spek-runner-junit5:${Versions.spek}") {
        exclude(group = "org.jetbrains.kotlin")
    }
    testImplementation("io.ktor:ktor-server-test-host:${Versions.ktor}") {
        exclude(group = "org.eclipse.jetty") // conflicts with WireMock
    }
}

tasks {
    withType<ShadowJar> {
        archiveBaseName.set("app")
        archiveClassifier.set("")
        manifest {
            attributes(
                mapOf("Main-Class" to mainClass)
            )
        }
    }
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
    }
    withType<Wrapper> {
        gradleVersion = "5.6"
        distributionType = Wrapper.DistributionType.BIN
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlinter {
    disabledRules = arrayOf("max-line-length")
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.11.0"
    }
    plugins {
        // Optional: an artifact spec for a protoc plugin, with "grpc" as
        // the identifier, which can be referred to in the "plugins"
        // container of the "generateProtoTasks" closure.
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${Versions.grpc}"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                // Apply the "grpc" plugin whose spec is defined above, without options.
                id("grpc")
            }
        }
    }
}