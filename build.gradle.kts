import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    java
    jacoco
    id("org.jooq.jooq-codegen-gradle") version "3.21.7"
    id("org.openapi.generator") version "7.24.0"
    id("com.diffplug.spotless") version "8.10.1"
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "dev.portableagent"
version = "0.1.0-SNAPSHOT"

extra["tomcat.version"] = "11.0.25"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    jooqCodegen("org.jooq:jooq-meta-extensions:3.21.7")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

jooq {
    configuration {
        generator {
            database {
                name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                includes = "conversations|conversation_messages"
                properties {
                    property {
                        key = "scripts"
                        value = "src/main/resources/db/migration/*.sql"
                    }
                    property {
                        key = "sort"
                        value = "flyway"
                    }
                    property {
                        key = "defaultNameCase"
                        value = "lower"
                    }
                }
            }
            generate {
                isDeprecated = false
                isRecords = true
            }
            target {
                packageName = "dev.portableagent.conversation.db"
                directory = "build/generated-src/jooq/main"
            }
        }
    }
}

sourceSets.main {
    java.srcDir("build/generated-src/jooq/main")
    java.srcDir(layout.buildDirectory.dir("generated-src/openapi/src/main/java"))
    java.srcDir(layout.buildDirectory.dir("generated-src/agent-openapi/src/main/java"))
    java.srcDir(layout.buildDirectory.dir("generated-src/action-openapi/src/main/java"))
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$projectDir/src/main/openapi/conversation-api.yaml")
    outputDir.set(
        layout.buildDirectory
            .dir("generated-src/openapi")
            .get()
            .asFile.absolutePath,
    )
    apiPackage.set("dev.portableagent.conversation.api")
    modelPackage.set("dev.portableagent.conversation.api.model")
    configOptions.set(
        mapOf(
            "annotationLibrary" to "none",
            "documentationProvider" to "none",
            "hideGenerationTimestamp" to "true",
            "interfaceOnly" to "true",
            "openApiNullable" to "false",
            "performBeanValidation" to "true",
            "skipDefaultInterface" to "true",
            "useResponseEntity" to "true",
            "useSpringBoot4" to "true",
            "useSpringBuiltInValidation" to "true",
            "useSwaggerUI" to "false",
            "useTags" to "true",
        ),
    )
}

fun clientModels(
    taskName: String,
    specPath: String,
    output: String,
    modelPackageName: String,
) = tasks.register<GenerateTask>(taskName) {
    generatorName.set("spring")
    inputSpec.set(specPath)
    outputDir.set(
        layout.buildDirectory
            .dir(output)
            .get()
            .asFile.absolutePath,
    )
    modelPackage.set(modelPackageName)
    globalProperties.set(
        mapOf(
            "models" to "",
            "modelDocs" to "false",
            "modelTests" to "false",
        ),
    )
    typeMappings.set(mapOf("OffsetDateTime" to "String"))
    configOptions.set(
        mapOf(
            "annotationLibrary" to "none",
            "documentationProvider" to "none",
            "hideGenerationTimestamp" to "true",
            "openApiNullable" to "false",
            "performBeanValidation" to "true",
            "useJackson3" to "true",
            "useSpringBoot4" to "true",
            "useSpringBuiltInValidation" to "true",
        ),
    )
}

val prepareAgentSpec =
    tasks.register<Copy>("prepareAgentSpec") {
        from("src/main/openapi/clients/agent-runtime-api.yaml")
        into(layout.buildDirectory.dir("generated-specs"))
        filter { line -> if (line.trim() == "const: true") "" else line }
    }

val agentApiGenerate =
    clientModels(
        "agentApiGenerate",
        layout.buildDirectory
            .file("generated-specs/agent-runtime-api.yaml")
            .get()
            .asFile.absolutePath,
        "generated-src/agent-openapi",
        "dev.portableagent.conversation.agent.api.model",
    ).also { task -> task.configure { dependsOn(prepareAgentSpec) } }

val actionApiGenerate =
    clientModels(
        "actionApiGenerate",
        "$projectDir/src/main/openapi/clients/action-api.yaml",
        "generated-src/action-openapi",
        "dev.portableagent.conversation.action.api.model",
    )

spotless {
    java {
        target("src/**/*.java")
        palantirJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
    format("docs") {
        target("*.md", "docs/**/*.md", "*.yml", "*.yaml")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.compileJava {
    dependsOn(tasks.jooqCodegen, tasks.openApiGenerate, agentApiGenerate, actionApiGenerate)
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.check {
    dependsOn(tasks.spotlessCheck)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}
