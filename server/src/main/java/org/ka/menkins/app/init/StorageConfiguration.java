package org.ka.menkins.app.init;

import com.hazelcast.config.Config;
import org.ka.menkins.app.init.PropertiesHolder.PropertyNames;

import java.util.function.Function;

final class StorageConfiguration {

    static final PropertyNames TYPE = PropertiesHolder.name("STORAGE_TYPE", "storage.type");

    static Function<AppConfig.AppConfigBuilder, AppConfig.AppConfigBuilder> load(PropertiesHolder properties) {
        return builder -> {
            var type = AppConfig.StorageType.valueOf(properties.getValue(TYPE, () -> "LOCAL").toUpperCase());

            builder.storageType(type);
            builder.hazelcast(new Config());
            return builder;
        };
    }

    private StorageConfiguration() {}
}
