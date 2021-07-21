import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.4.4"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.4.21"
    kotlin("plugin.spring") version "1.4.21"
}

group = "dev.turkdogan.aws.sns"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
    mavenLocal()
}

val ktlint by configurations.creating

dependencies {
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-json")

    implementation("com.amazonaws:aws-java-sdk-core:1.11.970")
    implementation("com.amazonaws:aws-java-sdk-s3:1.11.970")
    implementation("com.amazonaws:aws-java-sdk-sqs:1.11.970")
    implementation("com.amazonaws:aws-java-sdk-sns:1.11.970")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

sourceSets {
    val main by getting
    main.java.srcDirs("src/main/java", "src/main/kotlin")
}
