package org.ka.menkins.app;

import lombok.extern.slf4j.Slf4j;
import org.ka.menkins.mesos.Schedulers;
import org.ka.menkins.queue.NodeRequestWithResources;
import org.ka.menkins.queue.Storage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
public class RequestsAggregator {
    public static Consumer<AtomicReference<Schedulers.DriverState>> newAggregatorInitializer(ExecutorService pool,
                                                                                             Storage.StorageConfiguration config,
                                                                                             Storage.StorageManager storageManager) {
        return stateRef -> {
            var FLUSH_INTERVAL = TimeUnit.SECONDS.toNanos(5);
            var FETCH_TIMEOUT = TimeUnit.SECONDS.toNanos(1);
            var BUFFER_SIZE = config.getBufferSize();

            var all = storageManager.createNodeRequests();
            var aggregated = storageManager.aggregatedCreateNodeRequests();

            pool.execute(() -> {
                NodeRequestWithResources request = null;
                List<NodeRequestWithResources> buffer = new ArrayList<>(BUFFER_SIZE);
                long firstAdded = System.nanoTime();
                for (;;) {
                    try {
                        try {
                            request = all.poll(FETCH_TIMEOUT, TimeUnit.NANOSECONDS);
                        } catch (InterruptedException e) {}
                        if (request != null) {
                            if (buffer.isEmpty()) {
                                firstAdded = System.nanoTime();
                            }
                            buffer.add(request);
                        }

                        int size = buffer.size();
                        if (size > 0 && (size >= BUFFER_SIZE || System.nanoTime() - firstAdded > FLUSH_INTERVAL)) {
                            var added = aggregated.add(buffer);
                            if (added) {
                                var state = stateRef.get();
                                if (state.isSuppressed() && stateRef.compareAndSet(state, state.withSuppressed(false))) {
                                    stateRef.get().getDriver().reviveOffers();
                                }
                                buffer = new ArrayList<>(BUFFER_SIZE);
                            }
                        }
                    } catch (Exception e) {
                        log.error("error in aggregator", e);
                    }
                }
            });
        };
    }
}
