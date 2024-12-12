package ru.pulsar.jenkins.library.steps

interface Coverable {

    String getCoverageStashPath();
    String getCoverageStashName();
    String getCoveragePidsPath();

}