package org.ka.menkins.app.init;

import org.ka.menkins.app.init.PropertiesHolder.PropertyNames;

import java.util.function.Function;

final class AppConfigLoader {

    static final PropertyNames PORT = PropertiesHolder.name("APP_PORT", "app.port");

    static Function<AppConfig.AppConfigBuilder, AppConfig.AppConfigBuilder> load(PropertiesHolder properties) {
        return builder -> {
            var port = Integer.valueOf(properties.getValue(PORT, () -> "5678"));

            return builder.port(port);
        };
    }

    private AppConfigLoader() {}
}
