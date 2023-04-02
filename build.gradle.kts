import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    application
}

group = "me.michael"
version = "1.2"

repositories {
    mavenCentral()
    maven {
        url = uri("https://m2.dv8tion.net/releases")
    }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("net.dv8tion:JDA:5.0.0-beta.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    // https://mvnrepository.com/artifact/javax.annotation/javax.annotation-api
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    // https://mvnrepository.com/artifact/org.reflections/reflections
    implementation("org.reflections:reflections:0.10.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.20")
    implementation("com.sedmelluq:lavaplayer:1.3.78")
    // https://mvnrepository.com/artifact/mysql/mysql-connector-java
    implementation("mysql:mysql-connector-java:8.0.31")
    // https://mvnrepository.com/artifact/com.opencsv/opencsv
    implementation("com.opencsv:opencsv:5.7.1")
    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("ch.qos.logback:logback-classic:1.4.4")
    // https://mvnrepository.com/artifact/ch.qos.logback/logback-core
    implementation("ch.qos.logback:logback-core:1.4.4")
    // https://mvnrepository.com/artifact/club.minnced/discord-webhooks
    implementation("club.minnced:discord-webhooks:0.8.2")

//    for the YouTube api
//     https://mvnrepository.com/artifact/com.google.api-client/google-api-client
    implementation("com.google.api-client:google-api-client:1.15")
//     https://mvnrepository.com/artifact/com.google.oauth-client/google-oauth-client-jetty
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
//     https://mvnrepository.com/artifact/com.google.apis/google-api-services-youtube
    implementation("com.google.apis:google-api-services-youtube:v3-rev222-1.25.0")


//     https://mvnrepository.com/artifact/net.sourceforge.htmlunit/htmlunit
    implementation("net.sourceforge.htmlunit:htmlunit:2.67.0")
    // https://mvnrepository.com/artifact/xerces/xercesImpl
    implementation("xerces:xercesImpl:2.11.0")


}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}

tasks.register<Copy>("copyStartScripts") {
    val win = startBatchFile()
    val linux = startUnixShellScriptFile()
    from(win, linux)
    into(layout.buildDirectory)
}

fun startBatchFile(): File {
    val file = File(layout.projectDirectory.asFile, "src/dist/start.bat")
    file.createNewFile()
    file.writeText("%cd%\\bin\\${application.applicationName}.bat")
    return file
}

fun startUnixShellScriptFile(): File {
    val file = File(layout.projectDirectory.asFile, "src/dist/start")
    file.createNewFile()
    file.writeText("#! /bin/bash\n$(pwd)/bin/${application.applicationName}")
    return file
}

tasks.startScripts {
    doLast {
        val winScriptFile = windowsScript
        var winFileText = winScriptFile.readText()
        winFileText = winFileText.replace(
            "set CLASSPATH=.*".toRegex(),
            "rem original CLASSPATH declaration replaced by:\nset CLASSPATH=%APP_HOME%\\\\lib\\\\\\*"
        )
        winScriptFile.writeText(winFileText)
    }
}

application {
    mainClass.set("ApplicationKt")
}