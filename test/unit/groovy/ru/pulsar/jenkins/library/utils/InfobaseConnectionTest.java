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
    void fileBaseDirIsTakenWithoutQuotes() {
        assertThat(InfobaseConnection.fileBaseDir("/FC:\\bases\\test")).isEqualTo("C:\\bases\\test");
        assertThat(InfobaseConnection.fileBaseDir("/F\"C:\\my bases\\test\"")).isEqualTo("C:\\my bases\\test");
        assertThat(InfobaseConnection.fileBaseDir("/F./build/ib")).isEqualTo("./build/ib");
    }
}
