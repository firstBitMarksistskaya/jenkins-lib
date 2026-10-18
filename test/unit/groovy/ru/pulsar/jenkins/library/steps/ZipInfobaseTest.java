package ru.pulsar.jenkins.library.steps;

import groovy.lang.Closure;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.ConfigurationReader;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;
import ru.pulsar.jenkins.library.utils.TestUtils;
import ru.pulsar.jenkins.library.utils.VRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZipInfobaseTest {

    private static final String SAVE_ALWAYS = """
        "initInfobase": { "archiveInfobase": { "onAlways": true } },
        "bdd": { "archiveInfobase": { "onAlways": true } }
        """;

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor();

    @BeforeEach
    void setUp() throws Exception {
        RunWrapper currentBuild = mock(RunWrapper.class);
        when(currentBuild.getCurrentResult()).thenReturn("SUCCESS");
        when(steps.currentBuild()).thenReturn(currentBuild);
        when(steps.withCredentials(anyList(), any(Closure.class))).thenAnswer(invocation -> {
            Closure<?> body = invocation.getArgument(1);
            return body.call();
        });
        when(steps.fileExists("./tools/vrunner.json")).thenReturn(true);
        TestUtils.setupMockedContext(steps);
    }

    @Test
    void runArchivesLocalBaseForTestStages() {
        // when
        List<String> commands = run("", "", "", "initInfoBase");

        // then
        // тестовые стадии получают локальную базу из stash, артефакт сохраняется только по настройке
        verify(steps).zip("build/ib", "1Cv8.1CD.zip", "1Cv8.1CD", false);
        verify(steps).stash("1Cv8.1CD.zip", "1Cv8.1CD.zip", false);
        assertThat(commands).isEmpty();
    }

    @Test
    void runDoesNothingForExternalBaseWithoutSaving() {
        // when
        List<String> commands = run("/Ssrv1c\\\\ssl", "srv1c:1545", "", "initInfoBase");

        // then
        assertThat(commands).isEmpty();
        verify(steps, never()).zip(anyString(), anyString(), anyString(), anyBoolean());
        verify(steps, never()).stash(anyString(), anyString(), anyBoolean());
        verify(steps, never()).archiveArtifacts(anyString());
    }

    @Test
    void runDumpsServerBaseWithExclusiveAccess() {
        // when
        List<String> commands = run("/Ssrv1c\\\\ssl", "srv1c:1545", SAVE_ALWAYS, "bdd");

        // then
        // выгрузка идет под пользователем этапа инициализации между завершением сеансов и снятием запрета
        assertThat(commands).hasSize(3);
        assertThat(commands.get(0)).startsWith("vrunner session kill --db \"ssl\"");
        assertThat(commands.get(1))
            .startsWith("vrunner dump 1Cv8.bdd.dt --ibconnection \"/Ssrv1c\\ssl\" --settings ./tools/vrunner.json")
            .contains(" --uccode ");
        assertThat(commands.get(2)).startsWith("vrunner session unlock --db \"ssl\"");
        verify(steps).archiveArtifacts("1Cv8.bdd.dt");
        // тестовые стадии работают с базой по адресу, stash не нужен
        verify(steps, never()).stash(anyString(), anyString(), anyBoolean());
    }

    @Test
    void runArchivesExternalFileBaseFromItsDirectory() {
        // when
        List<String> commands = run("/F/var/1c/ssl", "", SAVE_ALWAYS, "initInfoBase");

        // then
        assertThat(commands).isEmpty();
        verify(steps).zip("/var/1c/ssl", "1Cv8.1CD.zip", "1Cv8.1CD", true);
        verify(steps, never()).stash(anyString(), anyString(), anyBoolean());
    }

    private List<String> run(String basePath, String rasAddress, String options, String stage) {
        String config = """
            {
              %s
              "basePath": "%s",
              "rasAddress": "%s"
            }
            """.formatted(options.isBlank() ? "" : options.strip() + ",", basePath, rasAddress);
        JobConfiguration jobConfiguration = ConfigurationReader.create(config);
        List<String> commands = new ArrayList<>();

        try (MockedStatic<VRunner> vrunner = Mockito.mockStatic(VRunner.class)) {
            vrunner.when(VRunner::getVRunnerPath).thenReturn("vrunner");
            vrunner.when(() -> VRunner.exec(anyString())).thenAnswer(invocation -> {
                commands.add(invocation.getArgument(0));
                return 0;
            });

            new ZipInfobase(jobConfiguration, stage).run();
        }

        return commands;
    }
}
