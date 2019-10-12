package org.ka.menkins.app;

import com.hazelcast.config.Config;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AppConfig {
    int port;
    String pathToMesosLib;
    String role;
    String slaveUser;
    String mesosMasterUrl;
    String frameworkName;
    Config hazelcast;
}
