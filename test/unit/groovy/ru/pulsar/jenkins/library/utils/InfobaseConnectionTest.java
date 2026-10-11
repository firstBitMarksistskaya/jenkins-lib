package ru.pulsar.jenkins.library.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InfobaseConnectionTest {

    @Test
    void isServerRecognizesServerConnectionInAnyCase() {
        assertThat(InfobaseConnection.isServer("/Ssrv1c\\test_base")).isTrue();
        assertThat(InfobaseConnection.isServer("/sserver\\base")).isTrue();
        assertThat(InfobaseConnection.isServer(" /Ssrv1c:1541\\test_base ")).isTrue();
    }

    @Test
    void isServerIsFalseForFileConnection() {
        assertThat(InfobaseConnection.isServer("/F./build/ib")).isFalse();
        assertThat(InfobaseConnection.isServer("/FC:\\bases\\test")).isFalse();
        assertThat(InfobaseConnection.isServer(null)).isFalse();
    }

    @Test
    void serverBaseNameIsTakenAfterServer() {
        assertThat(InfobaseConnection.serverBaseName("/Ssrv1c\\test_base")).isEqualTo("test_base");
        assertThat(InfobaseConnection.serverBaseName("/Ssrv1c:1541\\test_base")).isEqualTo("test_base");
        assertThat(InfobaseConnection.serverBaseName("/S\"srv1c\\test_base\"")).isEqualTo("test_base");
    }

    @Test
    void serverPortIsTakenFromServerPart() {
        assertThat(InfobaseConnection.serverPort("/Ssrv1c:1641\\test_base")).isEqualTo("1641");
        assertThat(InfobaseConnection.serverPort("/S\"srv1c:1641\\test_base\"")).isEqualTo("1641");
        assertThat(InfobaseConnection.serverPort("/Ssrv1c:1641,srv2c:1641\\test_base")).isEqualTo("1641");
    }

    @Test
    void serverPortIsEmptyWhenNotSpecified() {
        assertThat(InfobaseConnection.serverPort("/Ssrv1c\\test_base")).isEmpty();
        assertThat(InfobaseConnection.serverPort("/Ssrv1c,srv2c\\test_base")).isEmpty();
    }

    @Test
    void lockResourceIsTheSameForDifferentNotationsOfServerBase() {
        String expected = "/Ssrv1c:1541\\test_base";
        assertThat(InfobaseConnection.lockResource("/Ssrv1c\\test_base")).isEqualTo(expected);
        assertThat(InfobaseConnection.lockResource("/SSrv1C\\Test_Base")).isEqualTo(expected);
        assertThat(InfobaseConnection.lockResource("/s\"srv1c:1541\\test_base\"")).isEqualTo(expected);
        assertThat(InfobaseConnection.lockResource(" /Ssrv1c/test_base ")).isEqualTo(expected);
    }

    @Test
    void lockResourceKeepsNonDefaultClusterPort() {
        assertThat(InfobaseConnection.lockResource("/Ssrv1c:1641\\test_base")).isEqualTo("/Ssrv1c:1641\\test_base");
        assertThat(InfobaseConnection.lockResource("/Ssrv1c,Srv2c:1641\\test_base"))
            .isEqualTo("/Ssrv1c:1541,srv2c:1641\\test_base");
    }

    @Test
    void lockResourceIsTheSameForDifferentNotationsOfAbsoluteFileBase() {
        assertThat(InfobaseConnection.lockResource("/FC:\\Bases\\Test")).isEqualTo("/Fc:/bases/test");
        assertThat(InfobaseConnection.lockResource("/F\"c:/bases/test/\"")).isEqualTo("/Fc:/bases/test");
        assertThat(InfobaseConnection.lockResource("/F/var/1c/Test_Base/")).isEqualTo("/F/var/1c/test_base");
        assertThat(InfobaseConnection.lockResource("/F\\\\fileserver\\bases\\test")).isEqualTo("/F//fileserver/bases/test");
    }

    @Test
    void lockResourceIsNullForRelativeFileBase() {
        // относительный путь на разных агентах означает разные базы
        assertThat(InfobaseConnection.lockResource("/F./build/ib")).isNull();
        assertThat(InfobaseConnection.lockResource("/Fbases/test")).isNull();
    }

    @Test
    void fileBaseDirIsTakenWithoutQuotes() {
        assertThat(InfobaseConnection.fileBaseDir("/FC:\\bases\\test")).isEqualTo("C:\\bases\\test");
        assertThat(InfobaseConnection.fileBaseDir("/F\"C:\\my bases\\test\"")).isEqualTo("C:\\my bases\\test");
        assertThat(InfobaseConnection.fileBaseDir("/F./build/ib")).isEqualTo("./build/ib");
    }
}
