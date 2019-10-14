package org.ka.menkins.queue;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Storage {

    @Value
    @Builder
    public static class StorageConfiguration {
        int bufferSize;
    }

    public interface StorageManager {
        BlockingQueue<NodeRequestWithResources> createNodeRequests();
        BlockingQueue<List<NodeRequestWithResources>> aggregatedCreateNodeRequests();
        Runnable onShutDown();
    }

    public static StorageManager newStorageManager(Config config) {
        var hz = Hazelcast.newHazelcastInstance(config);
        return new StorageManager() {
            @Override
            public BlockingQueue<NodeRequestWithResources> createNodeRequests() {
                return hz.getQueue("incoming-requests");
            }

            @Override
            public BlockingQueue<List<NodeRequestWithResources>> aggregatedCreateNodeRequests() {
                return hz.getQueue("aggregated");
            }

            @Override
            public Runnable onShutDown() {
                return () -> hz.getLifecycleService().shutdown();
            }
        };
    }

    public static StorageManager newLocalStorageManager() {
        BlockingQueue<NodeRequestWithResources> incoming = new LinkedBlockingQueue<>();
        BlockingQueue<List<NodeRequestWithResources>> aggregated = new LinkedBlockingQueue<>();
        return new StorageManager() {
            @Override
            public BlockingQueue<NodeRequestWithResources> createNodeRequests() {
                return incoming;
            }

            @Override
            public BlockingQueue<List<NodeRequestWithResources>> aggregatedCreateNodeRequests() {
                return aggregated;
            }

            @Override
            public Runnable onShutDown() {
                return () -> { /* noop */ };
            }
        };
    }
}
