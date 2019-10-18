package org.ka.menkins.aggregator;

import io.prometheus.client.Counter;
import lombok.extern.slf4j.Slf4j;
import org.ka.menkins.NanoTimer;
import org.ka.menkins.storage.NodeRequestWithResources;
import org.ka.menkins.storage.Storage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class RequestsAggregator {

    static class Metrics {
        static final Counter polls = Counter.build()
                .name("menkins_aggregator_polls_count")
                .help("number of poll requests to global queue from aggregator")
                .register();

        static final Counter failedPolls = Counter.build()
                .name("menkins_aggregator_polls_failed_count")
                .help("number of failed poll requests to global queue from aggregator")
                .register();

        static final Counter failedPush = Counter.build()
                .name("menkins_aggregator_push_failed_count")
                .help("number of failed attempts to add aggregated node requests to queue")
                .register();

        static final Counter failedPushBack = Counter.build()
                .name("menkins_aggregator_push_failed_back_count")
                .help("number of failed attempts to add aggregated node requests back to global queue")
                .register();
    }

    @SuppressWarnings("unchecked")
    public static Runnable newInstance(Storage.StorageConfiguration config,
                                       BlockingQueue<NodeRequestWithResources> globalQueue,
                                       BlockingQueue<List<NodeRequestWithResources>> aggregated,
                                       NanoTimer timer,
                                       Runner runner) {
        var FLUSH_INTERVAL = TimeUnit.SECONDS.toNanos(5);
        var KEEP_ON_NODE = FLUSH_INTERVAL * 3;
        var FETCH_TIMEOUT = TimeUnit.SECONDS.toNanos(1);
        var BUFFER_SIZE = config.getBufferSize();

        List<NodeRequestWithResources>[] buffer = (List<NodeRequestWithResources>[]) new List[]{new ArrayList<NodeRequestWithResources>(BUFFER_SIZE)};
        long[] firstAdded = new long[]{timer.nanoTime()};
        return () -> {
            runner.run(() -> {
                try {
                    int size = buffer[0].size();
                    long timePast = timer.nanoTime() - firstAdded[0];
                    if (size > 0 && (size >= BUFFER_SIZE || timePast > FLUSH_INTERVAL)) {
                        boolean added = false;
                        try {
                            added = aggregated.add(buffer[0]);
                        } catch (Exception e) {}

                        if (added) {
                            buffer[0] = new ArrayList<>(BUFFER_SIZE);
                        } else {
                            Metrics.failedPush.inc();
                            if (timePast > KEEP_ON_NODE) {
                                buffer[0] = buffer[0].stream().filter(n -> !globalQueue.add(n)).collect(Collectors.toList());
                                if (buffer[0].size() > 0) Metrics.failedPushBack.inc();
                                return;
                            }
                        }
                    }

                    NodeRequestWithResources request = null;
                    try {
                        Metrics.polls.inc();
                        request = globalQueue.poll(FETCH_TIMEOUT, TimeUnit.NANOSECONDS);
                    } catch (Exception e) {
                        Metrics.failedPolls.inc();
                    }
                    if (request != null) {
                        if (buffer[0].isEmpty()) {
                            firstAdded[0] = timer.nanoTime();
                        }
                        buffer[0].add(request);
                    }
                } catch (Exception e) {
                    log.error("error in aggregator", e);
                }
            });
        };
    }
}
