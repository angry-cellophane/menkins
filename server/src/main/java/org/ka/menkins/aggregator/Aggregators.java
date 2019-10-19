package org.ka.menkins.aggregator;

import org.ka.menkins.NanoTimer;
import org.ka.menkins.storage.Storage;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class Aggregators {

    public interface Aggregator {
        Runnable runner();
        Runnable finalizer();
    }

    public static Aggregator newAggregator(Storage.StorageManager storage) {
        var storageConfig = Storage.StorageConfiguration.builder()
                .bufferSize(5)
                .build();

        var global = storage.createNodeRequests();
        var aggregated = storage.aggregatedCreateNodeRequests();


        var stopped = new AtomicBoolean(false);
        var aggregator = RequestsAggregator.newInstance(storageConfig, stopped, global, aggregated, NanoTimer.systemClock(), Runner.newInfiniteLoop());
        return new Aggregator() {
            @Override
            public Runnable runner() {
                return () -> {
                    var aggregatorPool = Executors.newSingleThreadExecutor(runnable -> {
                        var t = new Thread(runnable);
                        t.setDaemon(true);
                        t.setName("menkins-aggregator-thread");
                        return t;
                    });
                    aggregatorPool.execute(aggregator);
                };
            }

            @Override
            public Runnable finalizer() {
                return () -> stopped.set(true);
            }
        };
    }
}
