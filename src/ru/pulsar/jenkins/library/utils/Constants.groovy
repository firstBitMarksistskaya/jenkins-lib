package ru.pulsar.jenkins.library.utils
import ru.pulsar.jenkins.library.configuration.JobConfiguration

final class Constants {
    Constants(JobConfiguration config) {
        static this.DEFAULT_RING_OPTS = ("RING_OPTS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF8 -Dosgi.nl=ru -Duser.language=ru -Xmx" + config.ringMemory);
    }   
    public static final String DEFAULT_RING_OPTS;    
}
