plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.spring") version "2.2.21" apply false
    kotlin("plugin.jpa") version "2.2.21" apply false
    id("org.springframework.boot") version "4.0.3" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

group = "com.test"
version = "0.0.1-SNAPSHOT"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    repositories {
        mavenCentral()
    }

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
        }
        jvmToolchain(21)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
