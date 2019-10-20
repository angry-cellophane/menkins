package org.ka.menkins.app.init;

import com.hazelcast.client.config.ClientConfig;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class AppConfig {

    public static enum StorageType {
        LOCAL, REMOTE
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
        @NonNull String principal;
        @NonNull String secret;
        double refuseInSeconds;
        boolean checkpoint;
    }

    @NonNull Mesos mesos;
    @NonNull Http http;
    @NonNull StorageType storageType;
    @NonNull ClientConfig hazelcast;
}
