package org.ka.menkins.aggregator;

import org.ka.menkins.NanoTimer;
import org.ka.menkins.storage.Storage;

import java.util.concurrent.Executors;

public class Aggregators {
    public static void run(Storage.StorageManager storage) {
        var storageConfig = Storage.StorageConfiguration.builder()
                .bufferSize(5)
                .build();

        var global = storage.createNodeRequests();
        var aggregated = storage.aggregatedCreateNodeRequests();

        var aggregatorPool = Executors.newSingleThreadExecutor(runnable -> {
            var t = new Thread(runnable);
            t.setDaemon(true);
            t.setName("menkins-aggregator-thread");
            return t;
        });

        var aggregator = RequestsAggregator.newInstance(storageConfig, global, aggregated, NanoTimer.systemClock(), Runner.newInfiniteLoop());
        aggregatorPool.execute(aggregator);
    }
}
