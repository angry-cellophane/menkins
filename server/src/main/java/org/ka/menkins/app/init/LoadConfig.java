package org.ka.menkins.app.init;

import java.util.function.Function;

public final class LoadConfig {

    static Function<AppConfig.AppConfigBuilder, AppConfig.AppConfigBuilder> from(PropertiesHolder properties) {
        return StorageConfiguration.load(properties)
                .andThen(MesosConfigLoader.load(properties))
                .andThen(K8sConfigLoader.load(properties))
                .andThen(HttpConfigLoader.load(properties));
    }

    public static AppConfig fromJvm() {
        return from(new PropertiesHolder(System.getenv(), System.getProperties()))
                .apply(AppConfig.builder())
                .build();
    }

    private LoadConfig() {}
}
