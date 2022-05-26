package ru.pulsar.jenkins.library.utils

import com.cloudbees.groovy.cps.NonCPS

class RepoUtils implements Serializable {

    private final static REPO_SLUG_REGEXP = ~/(?m)^(?:[^:\/?#\n]+:)?(?:\/+[^\/?#\n]*)?\/?([^?\n]*)/
    private static String REPO_SLUG;

    @NonCPS
    static void computeRepoSlug(String text) {
        def matcher = text =~ REPO_SLUG_REGEXP
        String repoSlug = matcher != null && matcher.getCount() == 1 ? matcher[0][1] : ""
        if (repoSlug.endsWith(".git")) {
            repoSlug = repoSlug[0..-5]
        }

        REPO_SLUG = repoSlug.replace('/', '_')
    }

    static String getRepoSlug() {
        return REPO_SLUG
    }
}
