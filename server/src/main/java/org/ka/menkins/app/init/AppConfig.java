package org.ka.menkins.app.init;

import com.hazelcast.config.Config;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class AppConfig {

    public static enum StorageType {
        LOCAL, HAZELCAST
    }

    @Value
    @Builder
    public static class Http {
        int port;
    }

    @Value
    @Builder
    public static class Mesos {
        @NonNull String pathToMesosLib;
        @NonNull String role;
        @NonNull String slaveUser;
        @NonNull String mesosMasterUrl;
        @NonNull String frameworkName;
        @NonNull String webUiUrl;
        double refuseInSeconds;
        boolean checkpoint;
    }

    @NonNull Mesos mesos;
    @NonNull Http http;
    @NonNull StorageType storageType;
    @NonNull Config hazelcast;
}
