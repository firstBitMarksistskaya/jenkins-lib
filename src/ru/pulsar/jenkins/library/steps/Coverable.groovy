package ru.pulsar.jenkins.library.steps

interface Coverable {

    String getStageSlug();
    String getCoverageStashPath();
    String getCoverageStashName();
    String getCoveragePidsPath();

}