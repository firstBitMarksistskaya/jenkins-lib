import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication
import org.gradle.api.tasks.Copy

plugins {
    java
    groovy
    jacoco
    id("com.mkobit.jenkins.pipelines.shared-library") version "0.10.1"
    id("com.github.ben-manes.versions") version "0.51.0"
    id("org.jenkins-ci.jpi") version "0.55.0" apply false
}

repositories {
    mavenCentral()
}

tasks {
	register<Copy>("resolveIntegrationTestDependencies") {
		// Place dependencies into the processed resources output for the integrationTest source set
		into(layout.buildDirectory.dir("resources/integrationTest/test-dependencies"))
		from(configurations.integrationTestRuntimeClasspath.get())
	}

    processIntegrationTestResources {
        dependsOn("resolveIntegrationTestDependencies")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val junitVersion = "5.13.4"
val spockVersion = "1.3-groovy-2.4"
val groovyVersion = "2.4.21"
val slf4jVersion = "2.0.17"
val jsonschemaVersion = "4.38.0"

dependencies {
    implementation("org.codehaus.groovy", "groovy-all", groovyVersion)
	implementation("jakarta.servlet", "jakarta.servlet-api", "5.0.0")


	// jsonschema-generator
    implementation("com.github.victools", "jsonschema-generator", jsonschemaVersion)
    implementation("com.github.victools", "jsonschema-module-jackson", jsonschemaVersion)

    // unit-tests
	testRuntimeOnly("org.junit.platform", "junit-platform-launcher", "1.13.4")
	testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", junitVersion)
	testImplementation("org.junit.jupiter", "junit-jupiter-api", junitVersion)

    testImplementation("org.assertj", "assertj-core", "3.26.3")
    testImplementation("org.mockito", "mockito-core", "5.19.0")

    testImplementation("org.slf4j", "slf4j-api", slf4jVersion)
    testImplementation("org.slf4j", "slf4j-simple", slf4jVersion)

    // integration-tests
    integrationTestImplementation("org.jenkins-ci.main", "jenkins-test-harness", "2488.v0c38b_4e6b_cfa_")

    integrationTestImplementation("org.spockframework", "spock-core", spockVersion)
    integrationTestImplementation("org.codehaus.groovy", "groovy-all", groovyVersion)

    integrationTestImplementation("org.springframework.security", "spring-security-core", "6.5.2")
	//integrationTestImplementation("jakarta.servlet", "jakarta.servlet-api", "5.0.0")

    integrationTestImplementation("org.slf4j", "slf4j-api", slf4jVersion)
    integrationTestImplementation("org.slf4j", "slf4j-simple", slf4jVersion)

}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
    }

    reports {
        html.required.set(true)
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestReport)
    dependsOn(tasks.integrationTest)
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        xml.outputLocation.set(File("$buildDir/reports/jacoco/test/jacoco.xml"))
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
        dependency("org.jenkins-ci.plugins", "pipeline-build-step", "571.v08a_fffd4b_0ce")
        dependency("org.jenkins-ci.plugins", "pipeline-utility-steps", "2.19.0")
        dependency("org.jenkins-ci.plugins", "git", "5.7.0")
        dependency("org.jenkins-ci.plugins", "http_request", "1.20")
        dependency("org.jenkins-ci.plugins", "timestamper", "1.30")
        dependency("org.jenkins-ci.plugins", "credentials", "1419.v2337d1ceceef")
        dependency("org.jenkins-ci.plugins", "token-macro", "477.vd4f0dc3cb_cf1")
        dependency("org.jenkins-ci.plugins.workflow", "workflow-step-api", "706.v518c5dcb_24c0")

        dependency("org.jenkins-ci.modules", "sshd", "3.374.v19b_d59ce6610")

        dependency("org.6wind.jenkins", "lockable-resources", "1412.v3f305a_fb_a_117")
        dependency("ru.yandex.qatools.allure", "allure-jenkins-plugin", "2.32.0")
        dependency("io.jenkins.blueocean", "blueocean-pipeline-api-impl", "1.27.21")
        dependency("sp.sd", "file-operations", "353.vf3b_9b_a_f1f7f7")

        val declarativePluginsVersion = "2.2265.v140e610fe9d5"

        dependency("org.jenkinsci.plugins", "pipeline-model-api", declarativePluginsVersion)
        dependency("org.jenkinsci.plugins", "pipeline-model-definition", declarativePluginsVersion)
        dependency("org.jenkinsci.plugins", "pipeline-model-extensions", declarativePluginsVersion)
        dependency("org.jenkinsci.plugins", "pipeline-model-declarative-agent", "1.1.1")
    }
}
