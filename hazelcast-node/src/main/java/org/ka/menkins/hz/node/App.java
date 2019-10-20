package org.ka.menkins.hz.node;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;

public class App {
    public static void main(String[] args) {
        var config1 = new Config();
        config1.getNetworkConfig().setPort(5701);
        config1.getGroupConfig().setName("menkins");

        var joinConfig1 = new JoinConfig();
        joinConfig1.getTcpIpConfig().setEnabled(true);
        joinConfig1.getMulticastConfig().setEnabled(false);
        config1.getNetworkConfig().setJoin(joinConfig1);

        var hz1 = Hazelcast.newHazelcastInstance(config1);

        var config2 = new Config();
        config2.getGroupConfig().setName("menkins");
        config2.getNetworkConfig().setPort(5702);

        var joinConfig2 = new JoinConfig();
        joinConfig2.getTcpIpConfig().setEnabled(true);
        joinConfig2.getTcpIpConfig().addMember("172.28.128.1:5701");
        joinConfig2.getMulticastConfig().setEnabled(false);
        config2.getNetworkConfig().setJoin(joinConfig2);

        var hz2 = Hazelcast.newHazelcastInstance(config2);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            hz1.getLifecycleService().shutdown();
            hz2.getLifecycleService().shutdown();
        }));
    }
}
