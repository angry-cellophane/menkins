package org.ka.menkins.queue;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IQueue;

public class RequestsQueue {

    private static final String QUEUE_NAME = "menkins-requests-queue";

    public static IQueue<BuilderNodeRequest> getQueue(Config config) {
        var hz = Hazelcast.newHazelcastInstance(config);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> hz.getLifecycleService().shutdown()));

        return hz.getQueue(QUEUE_NAME);
    }
}
