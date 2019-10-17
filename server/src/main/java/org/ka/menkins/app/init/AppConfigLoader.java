package org.ka.menkins.app.init;

import org.ka.menkins.app.init.PropertiesHolder.PropertyNames;

import java.util.function.Function;

final class AppConfigLoader {

    static final PropertyNames PORT = PropertiesHolder.name("APP_PORT", "app.port");
    static final PropertyNames URL = PropertiesHolder.name("APP_URL", "app.url");

    static Function<AppConfig.AppConfigBuilder, AppConfig.AppConfigBuilder> load(PropertiesHolder properties) {
        return builder -> {

//                .port(5678)
//                .url("http://localhost:5678")
            return builder;
        };
    }

    private AppConfigLoader() {}
}
