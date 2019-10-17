package org.ka.menkins.app.init;

import java.util.Objects;

public class BuildAppConfig {

    public static AppConfig loadFrom(PropertiesHolder properties) {
        Objects.requireNonNull(properties);

        return LoadConfig.from(properties)
                .apply(AppConfig.builder())
                .build();


//        return AppConfig.builder()
//                .mesos(
//                        AppConfig.Mesos.builder()
//                                .pathToMesosLib("/home/aleksandr/src/3rd/mesos/mesos-1.9.0/build/src/.libs/libmesos.so")
//                                .role("*")
//                                .slaveUser("nobody")
//                                .frameworkName("menkins-" + id)
//                                .mesosMasterUrl("172.28.128.16:5050")
//                                .checkpoint(true)
//                                .build()
//                )
//                .port(5678)
//                .url("http://localhost:5678")
//                .hazelcast(new Config())
//                .build();
    }

    private BuildAppConfig() {}
}
