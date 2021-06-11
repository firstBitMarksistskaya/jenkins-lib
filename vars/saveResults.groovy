import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.PublishAllure

def call(JobConfiguration config) {
    ContextRegistry.registerDefaultContext(this)

    def publishAllure = new PublishAllure(config)
    publishAllure.run()

//    step([
//        $class: 'CucumberReportPublisher',
//        fileIncludePattern: '*.json',
//        jsonReportDirectory: 'build/out/cucumber'
//    ])
//
//    step([
//        $class: 'CukedoctorPublisher',
//        featuresDir: 'build/out/cucumber',
//        format: 'HTML',
//        hideFeaturesSection: false,
//        hideScenarioKeyword: false,
//        hideStepTime: false,
//        hideSummary: false,
//        hideTags: false,
//        numbered: true,
//        sectAnchors: true,
//        title: 'Living Documentation',
//        toc: 'LEFT'
//    ])

}