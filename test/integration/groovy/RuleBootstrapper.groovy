import com.mkobit.jenkins.pipelines.codegen.LocalLibraryRetriever
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration
import org.jenkinsci.plugins.workflow.libs.LibraryRetriever
import org.jvnet.hudson.test.JenkinsRule

final class RuleBootstrapper {
    private RuleBootstrapper() {
    }

    /**
     * This demonstrates how you can can configure the {@link JenkinsRule} to use the local source code
     * as a {@link LibraryConfiguration}. In this example we are making it implicitly loaded.
     */
    static void setup(JenkinsRule rule) {
        rule.timeout = 30
        final LibraryRetriever retriever = new LocalLibraryRetriever()
        final LibraryConfiguration localLibrary =
                new LibraryConfiguration('testLibrary', retriever)
        localLibrary.implicit = true
        localLibrary.defaultVersion = 'unused'
        localLibrary.allowVersionOverride = false
        GlobalLibraries.get().libraries = [localLibrary]
    }
}