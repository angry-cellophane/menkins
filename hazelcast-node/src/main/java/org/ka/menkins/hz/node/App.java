package org.ka.menkins.hz.node;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;

public class App {
    public static void main(String[] args) {
        var config1 = new Config();
        config1.getNetworkConfig().setPort(5701);
        var hz1 = Hazelcast.newHazelcastInstance(config1);

        var config2 = new Config();
        config2.getNetworkConfig().setPort(5702);

        var joinConfig = new JoinConfig();
        joinConfig.getTcpIpConfig().setEnabled(true);
        joinConfig.getTcpIpConfig().addMember("localhost:5701");
        joinConfig.getMulticastConfig().setEnabled(false);
        config2.getNetworkConfig().setJoin(joinConfig);

        var hz2 = Hazelcast.newHazelcastInstance(config1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            hz1.getLifecycleService().shutdown();
            hz2.getLifecycleService().shutdown();
        }));
    }
}
