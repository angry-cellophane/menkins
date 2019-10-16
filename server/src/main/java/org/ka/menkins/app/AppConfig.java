package org.ka.menkins.app;

import com.hazelcast.config.Config;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class AppConfig {

    @Value
    @Builder
    public static class Mesos {
        @NonNull String pathToMesosLib;
        @NonNull String role;
        @NonNull String slaveUser;
        @NonNull String mesosMasterUrl;
        @NonNull String frameworkName;
        double refuseInSeconds;
        boolean checkpoint;
    }

    @NonNull Mesos mesos;
    int port;
    @NonNull String url;
    @NonNull Config hazelcast;
}
