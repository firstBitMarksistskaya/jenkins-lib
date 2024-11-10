package ru.pulsar.jenkins.library.edt

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration

interface IEdtCliEngine {
    void edtToDesignerTransformConfiguration(IStepExecutor steps, JobConfiguration config);
    void edtToDesignerTransformExtensions(IStepExecutor steps, JobConfiguration config);
    void designerToEdtTransform(IStepExecutor steps, JobConfiguration config);
    void edtValidate(IStepExecutor steps, JobConfiguration config, String projectList);
}
