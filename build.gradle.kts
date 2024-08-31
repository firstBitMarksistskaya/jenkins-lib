import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication

plugins {
    java
    groovy
    jacoco
    id("com.mkobit.jenkins.pipelines.shared-library") version "0.10.1"
    id("com.github.ben-manes.versions") version "0.51.0"
    id("org.jenkins-ci.jpi") version "0.52.0-rc.1" apply false
}

repositories {
    mavenCentral()
}

tasks {

    register<org.jenkinsci.gradle.plugins.jpi.TestDependenciesTask>("resolveIntegrationTestDependencies") {
        into {
            val javaConvention = project.convention.getPlugin<JavaPluginConvention>()
            File("${javaConvention.sourceSets.integrationTest.get().output.resourcesDir}/test-dependencies")
        }
        configuration = configurations.integrationTestRuntimeClasspath.get()
    }
    processIntegrationTestResources {
        dependsOn("resolveIntegrationTestDependencies")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val junitVersion = "5.11.0"
val spockVersion = "1.3-groovy-2.4"
val groovyVersion = "2.4.21"
val slf4jVersion = "2.0.16"
val jsonschemaVersion = "4.36.0"

dependencies {
    implementation("org.codehaus.groovy", "groovy-all", groovyVersion)

    // jsonschema-generator
    implementation("com.github.victools", "jsonschema-generator", jsonschemaVersion)
    implementation("com.github.victools", "jsonschema-module-jackson", jsonschemaVersion)

    // unit-tests
    testImplementation("org.junit.jupiter", "junit-jupiter-api", junitVersion)
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", junitVersion)

    testImplementation("org.assertj", "assertj-core", "3.26.3")
    testImplementation("org.mockito", "mockito-core", "5.13.0")

    testImplementation("org.slf4j", "slf4j-api", slf4jVersion)
    testImplementation("org.slf4j", "slf4j-simple", slf4jVersion)
    
    // integration-tests
    integrationTestImplementation("org.jenkins-ci.main", "jenkins-test-harness", "2254.vcff7a_d4969e5")

    integrationTestImplementation("org.spockframework", "spock-core", spockVersion)
    integrationTestImplementation("org.codehaus.groovy", "groovy-all", groovyVersion)

    integrationTestImplementation("org.springframework.security", "spring-security-core", "5.1.13.RELEASE")

    integrationTestImplementation("org.slf4j", "slf4j-api", slf4jVersion)
    integrationTestImplementation("org.slf4j", "slf4j-simple", slf4jVersion)

}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
    }

    reports {
        html.isEnabled = true
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestReport)
    dependsOn(tasks.integrationTest)
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        xml.destination = File("$buildDir/reports/jacoco/test/jacoco.xml")
    }
}

jenkinsIntegration {
    baseUrl.set(uri("http://localhost:5050").toURL())
    authentication.set(providers.provider { AnonymousAuthentication })
    downloadDirectory.set(layout.projectDirectory.dir("jenkinsResources"))
}

sharedLibrary {
    // TODO: this will need to be altered when auto-mapping functionality is complete
    coreVersion.set(jenkinsIntegration.downloadDirectory.file("core-version.txt").map { it.asFile.readText().trim() })
    // TODO: retrieve downloaded plugin resource
    pluginDependencies {
        dependency("org.jenkins-ci.plugins", "pipeline-build-step", "540.vb_e8849e1a_b_d8")
        dependency("org.jenkins-ci.plugins", "pipeline-utility-steps", "2.17.0")
        dependency("org.jenkins-ci.plugins", "git", "5.2.2")
        dependency("org.jenkins-ci.plugins", "http_request", "1.19")
        dependency("org.jenkins-ci.plugins", "timestamper", "1.27")
        dependency("org.jenkins-ci.plugins", "credentials", "1371.vfee6b_095f0a_3")
        dependency("org.jenkins-ci.plugins", "token-macro", "400.v35420b_922dcb_")
        dependency("org.jenkins-ci.plugins.workflow", "workflow-step-api", "678.v3ee58b_469476")

        dependency("org.jenkins-ci.modules", "sshd", "3.329.v668e35efc720")

        dependency("org.6wind.jenkins", "lockable-resources", "1255.vf48745da_35d0")
        dependency("ru.yandex.qatools.allure", "allure-jenkins-plugin", "2.31.1")
        dependency("io.jenkins.blueocean", "blueocean-pipeline-api-impl", "1.27.14")
        dependency("sp.sd", "file-operations", "266.v9d4e1eb_235b_a_")

        val declarativePluginsVersion = "2.2214.vb_b_34b_2ea_9b_83"

        dependency("org.jenkinsci.plugins", "pipeline-model-api", declarativePluginsVersion)
        dependency("org.jenkinsci.plugins", "pipeline-model-definition", declarativePluginsVersion)
        dependency("org.jenkinsci.plugins", "pipeline-model-extensions", declarativePluginsVersion)
        dependency("org.jenkinsci.plugins", "pipeline-model-declarative-agent", "1.1.1")
    }
}
