package org.ka.menkins.app.init;

import java.util.function.Function;

final class K8sConfigLoader {

    static Function<AppConfig.AppConfigBuilder, AppConfig.AppConfigBuilder> load(PropertiesHolder properties) {
        return builder -> builder;
    }

    private K8sConfigLoader() {}
}
