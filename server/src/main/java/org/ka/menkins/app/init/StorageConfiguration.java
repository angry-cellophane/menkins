package org.ka.menkins.app.init;

import com.hazelcast.client.config.ClientConfig;
import org.ka.menkins.app.init.PropertiesHolder.PropertyNames;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

final class StorageConfiguration {

    static final PropertyNames TYPE = PropertiesHolder.name("STORAGE_TYPE", "storage.type");
    static final PropertyNames HZ_NODES = PropertiesHolder.name("HAZELCAST_NODES", "hazelcast.nodes");
    static final PropertyNames HZ_CLUSTER_NAME = PropertiesHolder.name("HAZELCAST_CLUSTER_NAME", "hazelcast.cluster.name");

    static Function<AppConfig.AppConfigBuilder, AppConfig.AppConfigBuilder> load(PropertiesHolder properties) {
        return builder -> {
            var type = AppConfig.StorageType.valueOf(properties.getValue(TYPE, () -> "LOCAL").toUpperCase());

            var hazelcast = new ClientConfig();
            if (type == AppConfig.StorageType.REMOTE) {
                var network = hazelcast.getNetworkConfig();
                var nodes = Arrays.stream(properties.getValue(HZ_NODES).split(","))
                        .map(String::trim)
                        .collect(Collectors.toList());
                network.setAddresses(nodes);

                var clusterName = properties.getValue(HZ_CLUSTER_NAME);
                hazelcast.getGroupConfig().setName(clusterName);
            }

            builder.storageType(type);
            builder.hazelcast(hazelcast);
            return builder;
        };
    }

    private StorageConfiguration() {}
}
