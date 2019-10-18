package org.ka.menkins.app.init;

import java.util.function.Function;

public final class LoadConfig {

    static Function<AppConfig.AppConfigBuilder, AppConfig.AppConfigBuilder> from(PropertiesHolder properties) {
        var allProperties = FromFile.preLoadProperties(properties);

        return StorageConfiguration.load(allProperties)
                .andThen(MesosConfigLoader.load(allProperties))
                .andThen(K8sConfigLoader.load(allProperties))
                .andThen(HttpConfigLoader.load(allProperties));
    }

    public static AppConfig fromJvm() {
        return from(new PropertiesHolder(System.getenv(), System.getProperties()))
                .apply(AppConfig.builder())
                .build();
    }

    private LoadConfig() {}
}
