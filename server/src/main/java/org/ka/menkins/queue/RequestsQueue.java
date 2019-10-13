package org.ka.menkins.queue;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RequestsQueue {

    private static final String QUEUE_NAME = "menkins-requests-queue";

    public static BlockingQueue<NodeRequestWithResources> getQueue(Config config) {
        // tmp hack to run locally faster
        if (true) {
            return new LinkedBlockingQueue<>();
        }

        var hz = Hazelcast.newHazelcastInstance(config);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> hz.getLifecycleService().shutdown()));

        return hz.getQueue(QUEUE_NAME);
    }
}
