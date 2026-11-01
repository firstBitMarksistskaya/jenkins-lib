package ru.pulsar.jenkins.library.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.ConfigurationReader;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

class InfobaseExtensionsTest {

    private static final String BOM = String.valueOf((char) 0xFEFF);

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor();

    @BeforeEach
    void setUp() {
        TestUtils.setupMockedContext(steps);
    }

    @Test
    void namesSkipBomAndEmptyLines() {
        assertThat(InfobaseExtensions.names(BOM + "YAXUnit\r\nТесты\r\n\r\n")).containsExactly("YAXUnit", "Тесты");
        // для базы без расширений файл пустой
        assertThat(InfobaseExtensions.names(BOM)).isEmpty();
        assertThat(InfobaseExtensions.names("")).isEmpty();
    }

    @Test
    void containsIgnoresCase() {
        assertThat(InfobaseExtensions.contains(List.of("YAXUnit"), "yaxunit")).isTrue();
        assertThat(InfobaseExtensions.contains(List.of("YAXUnit"), "Tests")).isFalse();
    }

    @Test
    void listDumpsExtensionsToOwnFileAndReadsIt() {
        // given
        JobConfiguration jobConfiguration = ConfigurationReader.create("{ \"basePath\": \"/F/var/1c/test_base\" }");
        doReturn(BOM + "YAXUnit\r\n").when(steps).readFile(InfobaseExtensions.LIST_FILE, "UTF-8");
        List<String> commands = new ArrayList<>();

        // when
        List<String> names;
        try (MockedStatic<VRunner> vrunner = Mockito.mockStatic(VRunner.class)) {
            vrunner.when(VRunner::getVRunnerPath).thenReturn("vrunner");
            vrunner.when(() -> VRunner.exec(anyString(), anyBoolean())).thenAnswer(invocation -> {
                commands.add(invocation.getArgument(0));
                return 0;
            });
            names = InfobaseExtensions.list(jobConfiguration, " --settings ./tools/vrunner.json");
        }

        // then
        assertThat(names).containsExactly("YAXUnit");
        assertThat(commands).containsExactly("vrunner designer --additional \"/DumpDBCfgList -AllExtensions /Out "
            + InfobaseExtensions.LIST_FILE + "\" --ibconnection \"/F/var/1c/test_base\" --settings ./tools/vrunner.json");
        // старый список не прочитается, если конфигуратор не запишет новый
        var order = inOrder(steps);
        order.verify(steps).writeFile(InfobaseExtensions.LIST_FILE, "", "UTF-8");
        order.verify(steps).readFile(InfobaseExtensions.LIST_FILE, "UTF-8");
    }

    @Test
    void listReportsConfiguratorOutputOnError() {
        // given
        // сообщение конфигуратора попадает в файл списка, vanessa-runner его не выводит
        JobConfiguration jobConfiguration = ConfigurationReader.create("{ \"basePath\": \"/F/var/1c/test_base\" }");
        doReturn(BOM + "Infobase user is not identified\r\n").when(steps).readFile(InfobaseExtensions.LIST_FILE, "UTF-8");

        // when
        try (MockedStatic<VRunner> vrunner = Mockito.mockStatic(VRunner.class)) {
            vrunner.when(VRunner::getVRunnerPath).thenReturn("vrunner");
            vrunner.when(() -> VRunner.exec(anyString(), anyBoolean())).thenReturn(1);
            InfobaseExtensions.list(jobConfiguration, "");
        }

        // then
        // текст сверяется латиницей: Java-тесты на Windows компилируются не в UTF-8
        verify(steps).error(contains(" 1. "));
        verify(steps).error(contains(": Infobase user is not identified"));
    }

    @Test
    void deleteCommandDeletesExtensionByName() {
        // given
        JobConfiguration jobConfiguration = ConfigurationReader.create("{ \"basePath\": \"/F/var/1c/test_base\" }");

        // when
        String command;
        try (MockedStatic<VRunner> vrunner = Mockito.mockStatic(VRunner.class)) {
            vrunner.when(VRunner::getVRunnerPath).thenReturn("vrunner");
            command = InfobaseExtensions.deleteCommand(jobConfiguration, "YAXUnit", "");
        }

        // then
        assertThat(command).isEqualTo(
            "vrunner designer --additional \"/DeleteCfg -Extension YAXUnit\" --ibconnection \"/F/var/1c/test_base\"");
    }
}
