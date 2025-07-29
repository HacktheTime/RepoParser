import io.franzbecker.gradle.lombok.task.DelombokTask
import org.checkerframework.gradle.plugin.CheckerFrameworkExtension

plugins {
    id("java")
    id("org.checkerframework") version "0.6.13"
    id("io.franzbecker.gradle-lombok") version "5.0.0"
    `maven-publish`
    signing
}

group = "moe.nea"
version = "1.7.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.0")
    implementation("com.google.code.gson:gson:2.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

lombok {
    version = "1.18.32"
    sha256 = "97574674e2a25f567a313736ace00df8787d443de316407d57fc877d9f19a65d"
}

java {
    withJavadocJar()
}

configure<CheckerFrameworkExtension> {

}

val delombok by tasks.registering(DelombokTask::class) {
    mainClass.set(lombok.main)
    dependsOn(tasks.compileJava)
    val outputDir by extra { layout.buildDirectory.dir("delombok").get() }
    outputs.dir(outputDir)
    sourceSets["main"].java.srcDirs.forEach {
        inputs.dir(it)
        args(it, "-d", outputDir)
    }
    doFirst {
        delete(outputDir)
    }
}

tasks.javadoc {
    dependsOn(delombok)
    val outputDir: Directory by delombok.get().extra
    source = outputDir.asFileTree
}

val sourcesJar by tasks.creating(Jar::class) {
    dependsOn(delombok)
    from(delombok)
    archiveClassifier.set("sources")
}

tasks.withType<JavaCompile>() {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
tasks.processResources {
    filesMatching("neurepoparser.properties") {
        expand(
            "VERSION" to version
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(sourcesJar) {
                classifier = "sources"
            }
            pom {
                name.set("NEU Repo Parsing")
                description.set("A library for standardized parsing of the NotEnoughUpdates item repository")
                url.set("https://github.com/NotEnoughUpdates/RepoParser")

                licenses {
                    license {
                        name.set("BSD 2-Clause")
                        url.set("https://github.com/NotEnoughUpdates/RepoParser/blob/master/LICENSE")
                    }
                }
                developers {
                    developer {
                        name.set("Linnea Gräf")
                    }
                }
                scm {
                    url.set("https://github.com/NotEnoughUpdates/RepoParser")
                }
            }
        }
    }
}

tasks.withType<Javadoc> {
    // schaltet alle DocLint-Checks ab und unterdrückt Ausgaben
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}
//disabled task to make it jitpack compatible.
tasks.named<Test>("test") {
    enabled = false
}