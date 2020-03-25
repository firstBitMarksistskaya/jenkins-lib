import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication

plugins {
    java
    id("com.mkobit.jenkins.pipelines.shared-library") version "0.10.1"
    id("com.github.ben-manes.versions") version "0.21.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    val spock = "org.spockframework:spock-core:1.2-groovy-2.4"
    testImplementation(spock)
    testImplementation("org.assertj:assertj-core:3.12.2")
    integrationTestImplementation(spock)
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
        dependency("org.jenkins-ci.plugins", "pipeline-build-step", "2.9")
        dependency("org.6wind.jenkins", "lockable-resources", "2.5")
        val declarativePluginsVersion = "1.3.9"
        dependency("org.jenkinsci.plugins", "pipeline-model-api", declarativePluginsVersion)
        dependency("org.jenkinsci.plugins", "pipeline-model-declarative-agent", "1.1.1")
        dependency("org.jenkinsci.plugins", "pipeline-model-definition", declarativePluginsVersion)
        dependency("org.jenkinsci.plugins", "pipeline-model-extensions", declarativePluginsVersion)
    }
}
