plugins {
    kotlin("jvm")
    id("maven-publish")
    id("application")
    id("com.github.jk1.tcdeps")
    id("com.jaredsburrows.license")
    id("kotlin-language-server.publishing-conventions")
    id("kotlin-language-server.distribution-conventions")
    id("kotlin-language-server.kotlin-conventions")
}

val debugPort = 8000
val debugArgs = "-agentlib:jdwp=transport=dt_socket,server=y,address=$debugPort,suspend=n,quiet=y"

val serverMainClassName = "org.javacs.kt.MainKt"
val applicationName = "kotlin-language-server"

application {
    mainClass.set(serverMainClassName)
    description = "Code completions, diagnostics and more for Kotlin"
    applicationDefaultJvmArgs = listOf("-DkotlinLanguageServer.version=$version")
}

repositories {
    maven(url = "https://repo.gradle.org/gradle/libs-releases")
    maven(url = "https://jitpack.io")
    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    mavenCentral()
}

dependencies {
    // Implementation dependencies: libraries required for your application
    implementation(platform(project(":platform")))
    implementation(project(":shared"))
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc")
    implementation(kotlin("compiler"))
    implementation(kotlin("scripting-compiler"))
    implementation(kotlin("scripting-jvm-host-unshaded"))
    implementation(kotlin("sam-with-receiver-compiler-plugin"))
    implementation(kotlin("reflect"))
    implementation("com.jetbrains.intellij.java:java-decompiler-engine")
    implementation("org.jetbrains.exposed:exposed-core")
    implementation("org.jetbrains.exposed:exposed-dao")
    implementation("org.jetbrains.exposed:exposed-jdbc")
    implementation("com.h2database:h2")
    implementation("com.github.fwcd.ktfmt:ktfmt")
    implementation("com.beust:jcommander")
    implementation("org.xerial:sqlite-jdbc")

    // Test dependencies: libraries required for testing
    testImplementation("org.hamcrest:hamcrest-all")
    testImplementation("junit:junit")
    testImplementation("org.openjdk.jmh:jmh-core")

    // Compile-only dependencies: libraries needed at compile time only
    // See https://github.com/JetBrains/kotlin/blob/65b0a5f90328f4b9addd3a10c6f24f3037482276/libraries/examples/scripting/jvm-embeddable-host/build.gradle.kts#L8
    compileOnly(kotlin("scripting-jvm-host"))

    // Test compile-only dependencies
    testCompileOnly(kotlin("scripting-jvm-host"))

    // Annotation processors: libraries used for annotation processing
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess")
}


configurations.forEach { config ->
    config.resolutionStrategy {
        preferProjectModules()
    }
}

tasks.startScripts {
    applicationName = "kotlin-language-server"
}

tasks.register<Exec>("fixFilePermissions") {
    group = "Distribution"
    description = "Fix file permissions for the start script on macOS or Linux."

    onlyIf { !System.getProperty("os.name").lowercase().contains("windows") }
    commandLine("chmod", "+x", "${tasks.installDist.get().destinationDir}/bin/kotlin-language-server")
}

tasks.register<JavaExec>("debugRun") {
    group = "Application"
    description = "Run the application with debugging enabled."

    mainClass.set(serverMainClassName)
    classpath(sourceSets.main.get().runtimeClasspath)
    standardInput = System.`in`

    jvmArgs(debugArgs)
    doLast {
        println("Using debug port $debugPort")
    }
}

tasks.register<CreateStartScripts>("debugStartScripts") {
    group = "Distribution"
    description = "Create start scripts with debug options for the application."

    applicationName = "kotlin-language-server"
    mainClass.set(serverMainClassName)
    outputDir = tasks.installDist.get().destinationDir.toPath().resolve("bin").toFile()
    classpath = tasks.startScripts.get().classpath
    defaultJvmOpts = listOf(debugArgs)
}

tasks.register<Sync>("installDebugDist") {
    group = "Distribution"
    description = "Install the debug distribution and create debug start scripts."

    dependsOn("installDist")
    finalizedBy("debugStartScripts")
}

tasks.withType<Test>() {
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.installDist {
    finalizedBy("fixFilePermissions")
}

tasks.build {
    finalizedBy("installDist")
}
