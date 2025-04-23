import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "2.1.0"
  kotlin("plugin.spring") version "2.1.0"
  id("java")
  id("org.springframework.boot") version "3.4.2"
  id("io.spring.dependency-management") version "1.1.7"
  id("org.graalvm.buildtools.native") version "0.10.4"
  id("org.openapi.generator") version "7.11.0"
  id("com.diffplug.spotless") version "7.0.2"
  id("com.dipien.semantic-version") version "2.0.0" apply false
  id("org.sonarqube") version "6.0.1.5171"
  jacoco
}

group = "it.pagopa.checkout.authservice"

version = "0.12.5"

description = "pagopa-checkout-auth-service"

sourceSets {
  main { java.srcDirs("src/main/java", layout.buildDirectory.dir("generated/src/main/java")) }
}

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

springBoot {
  mainClass.set("it.pagopa.checkout.authservice.PagopaCheckoutAuthserviceApplicationKt")
  buildInfo { properties { additional.set(mapOf("description" to project.description)) } }
}

object Deps {
  const val ecsLoggingVersion = "1.5.0"
  const val openTelemetryVersion = "1.48.0"
  const val openTelemetryInstrumentationVersion = "2.14.0-alpha"
  const val springBootVersion = "3.4.2"
  const val jsonWebTokenVersion = "0.11.5"
}

repositories { mavenCentral() }

dependencyManagement {
  imports {
    mavenBom("org.springframework.boot:spring-boot-dependencies:${Deps.springBootVersion}")
  }
  // otel BOM
  imports {
    mavenBom(
      "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${Deps.openTelemetryInstrumentationVersion}"
    )
  }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.openapitools:jackson-databind-nullable:0.2.6")
  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("io.arrow-kt:arrow-core:2.0.1")
  implementation("io.swagger.core.v3:swagger-annotations:2.2.28")

  // otel api
  implementation("io.opentelemetry.instrumentation:opentelemetry-reactor-3.1")
  implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")

  // ECS logback encoder
  implementation("co.elastic.logging:logback-ecs-encoder:${Deps.ecsLoggingVersion}")

  // io json web token (JWT) library
  implementation("io.jsonwebtoken:jjwt-api:${Deps.jsonWebTokenVersion}")
  implementation("io.jsonwebtoken:jjwt-impl:${Deps.jsonWebTokenVersion}")
  implementation("io.jsonwebtoken:jjwt-jackson:${Deps.jsonWebTokenVersion}")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin { compilerOptions { freeCompilerArgs.addAll("-Xjsr305=strict") } }

tasks
  .register("applySemanticVersionPlugin") { dependsOn("prepareKotlinBuildScriptModel") }
  .apply { apply(plugin = "com.dipien.semantic-version") }

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  kotlin {
    toggleOffOn()
    targetExclude("build/**/*")
    ktfmt().kotlinlangStyle()
  }
  kotlinGradle {
    toggleOffOn()
    targetExclude("build/**/*.kts")
    ktfmt().googleStyle()
  }
  java {
    target("**/*.java")
    targetExclude("build/**/*")
    eclipse().configFile("eclipse-style.xml")
    toggleOffOn()
    removeUnusedImports()
    trimTrailingWhitespace()
    endWithNewline()
  }
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("auth-v1") {
  generatorName.set("spring")
  inputSpec.set("$rootDir/api-spec/v1/openapi.yaml")
  outputDir.set(layout.buildDirectory.get().dir("generated").asFile.toString())
  apiPackage.set("it.pagopa.generated.checkout.authservice.v1.api")
  modelPackage.set("it.pagopa.generated.checkout.authservice.v1.model")
  generateApiDocumentation.set(false)
  generateApiTests.set(false)
  generateModelTests.set(false)
  library.set("spring-boot")
  modelNameSuffix.set("Dto")
  configOptions.set(
    mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "true",
      "interfaceOnly" to "true",
      "hideGenerationTimestamp" to "true",
      "skipDefaultInterface" to "true",
      "useSwaggerUI" to "false",
      "reactive" to "true",
      "useSpringBoot3" to "true",
      "oas3" to "true",
      "generateSupportingFiles" to "true",
      "enumPropertyNaming" to "MACRO_CASE",
    )
  )
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("oneidentity") {
  description = "Generates the OneIdentity classes for this project."
  group = "openapi-generation"
  generatorName.set("java")
  inputSpec.set("$rootDir/api-spec/oneidentity/openapi.yaml")
  outputDir.set(layout.buildDirectory.get().dir("generated").asFile.toString())
  apiPackage.set("it.pagopa.generated.checkout.oneidentity.api")
  modelPackage.set("it.pagopa.generated.checkout.oneidentity.model")
  generateApiDocumentation.set(false)
  generateApiTests.set(false)
  generateModelTests.set(false)
  library.set("webclient")
  modelNameSuffix.set("Dto")
  configOptions.set(
    mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "true",
      "interfaceOnly" to "true",
      "hideGenerationTimestamp" to "true",
      "skipDefaultInterface" to "true",
      "useSwaggerUI" to "false",
      "reactive" to "true",
      "useSpringBoot3" to "true",
      "oas3" to "true",
      "generateSupportingFiles" to "false",
      "useJakartaEe" to "true",
    )
  )
}

tasks.withType<KotlinCompile> {
  dependsOn("auth-v1", "oneidentity")
  compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
}

tasks.test {
  useJUnitPlatform()
  finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
  dependsOn(tasks.test) // tests are required to run before generating the report
  classDirectories.setFrom(
    files(
      classDirectories.files.map {
        fileTree(it).matching {
          exclude("it/pagopa/checkout/authservice/PagopaCheckoutAuthserviceApplicationKt.class")
        }
      }
    )
  )
  reports { xml.required.set(true) }
}

tasks.processResources { filesMatching("application.properties") { expand(project.properties) } }

graalvmNative {
  toolchainDetection = true

  binaries {
    named("main") {
      javaLauncher =
        javaToolchains.launcherFor {
          languageVersion = JavaLanguageVersion.of(21)
          vendor.set(JvmVendorSpec.GRAAL_VM)
        }
    }
  }

  metadataRepository {
    enabled.set(true)
    version.set("0.3.16")
  }
}
