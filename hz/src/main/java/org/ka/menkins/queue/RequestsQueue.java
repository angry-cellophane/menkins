package org.ka.menkins.queue;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;

import java.util.concurrent.BlockingQueue;

public class RequestsQueue {

    private static final String QUEUE_NAME = "menkins-requests-queue";

    public static BlockingQueue<NodeRequestWithResources> getQueue(Config config) {
        var hz = Hazelcast.newHazelcastInstance(config);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> hz.getLifecycleService().shutdown()));

        return hz.getQueue(QUEUE_NAME);
    }
}
