package org.ka.menkins.storage;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.hazelcast.monitor.LocalTopicStats;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
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
        ITopic<String> terminateTaskRequestsTopic();
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

            @Override
            public ITopic<String> terminateTaskRequestsTopic() {
                return hz.getTopic("terminate-task-topic");
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

            @Override
            public ITopic<String> terminateTaskRequestsTopic() {
                return new ITopic<>() {

                    final List<MessageListener<String>> listeners = new ArrayList<>();

                    @Override
                    public String getName() {
                        return "terminate-task-topic";
                    }

                    @Override
                    public synchronized void publish(String message) {
                        listeners.forEach(l -> l.onMessage(new Message<>("terminate-task-topic", message, 0, null)));
                    }

                    @Override
                    public synchronized String addMessageListener(MessageListener<String> listener) {
                        listeners.add(listener);
                        return "id";
                    }

                    @Override
                    public boolean removeMessageListener(String registrationId) {
                        return false;
                    }

                    @Override
                    public LocalTopicStats getLocalTopicStats() {
                        return null;
                    }

                    @Override
                    public String getPartitionKey() {
                        return null;
                    }

                    @Override
                    public String getServiceName() {
                        return null;
                    }

                    @Override
                    public void destroy() {

                    }
                };
            }
        };
    }
}
