package ru.pulsar.jenkins.library.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.pulsar.jenkins.library.IStepExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CredentialSelectionTest {
    private final IStepExecutor steps = TestUtils.getMockedStepExecutor();

    @BeforeEach
    void setUp() {
        TestUtils.setupMockedContext(steps);
    }

    @Test
    void explicitIdSkipsLookupEvenWhenStandardIsAvailable() {
        when(steps.credentialExists("CLUSTER_ADMIN")).thenReturn(true);
        assertThat(CredentialSelection.resolve(steps, "custom", "CLUSTER_ADMIN")).isEqualTo("custom");
        verify(steps, never()).credentialExists(anyString());
    }

    @Test
    void unsetIdSelectsStandardOnlyWhenAvailable() {
        for (String configured : new String[]{null, "", "UNKNOWN_ID"}) {
            when(steps.credentialExists("CLUSTER_ADMIN")).thenReturn(true);
            assertThat(CredentialSelection.resolve(steps, configured, "CLUSTER_ADMIN")).isEqualTo("CLUSTER_ADMIN");
            when(steps.credentialExists("CLUSTER_ADMIN")).thenReturn(false);
            assertThat(CredentialSelection.resolve(steps, configured, "CLUSTER_ADMIN")).isNull();
        }
    }

    @Test
    void lookupFailurePropagates() {
        RuntimeException failure = new RuntimeException("lookup failed");
        when(steps.credentialExists(anyString())).thenThrow(failure);
        assertThat(catchThrowable(() -> CredentialSelection.resolve(steps, null, "CLUSTER_ADMIN"))).isSameAs(failure);
    }

    @Test
    void secretsAreSelectedIndependently() {
        when(steps.credentialExists("DEBUG_SERVER_PASSWORD")).thenReturn(true);
        assertThat(CredentialSelection.resolve(steps, "custom-admin", "CLUSTER_ADMIN")).isEqualTo("custom-admin");
        assertThat(CredentialSelection.resolve(steps, null, "DEBUG_SERVER_PASSWORD")).isEqualTo("DEBUG_SERVER_PASSWORD");
        verify(steps, never()).credentialExists("CLUSTER_ADMIN");
    }
}
