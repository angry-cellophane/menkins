package org.ka.menkins.mesos;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.SchedulerDriver;
import org.ka.menkins.app.AppConfig;
import org.ka.menkins.queue.NodeRequestWithResources;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class Schedulers {

    @Value
    static class State {
        BlockingQueue<List<NodeRequestWithResources>> localQueue;
        AtomicBoolean suppress;
        AtomicReference<SchedulerDriver> driver;
    }

    private Schedulers() {}

    public static Runnable newInitializer(AppConfig config, BlockingQueue<NodeRequestWithResources> globalQueue) {
        return () -> {
            log.info("Initializing driver");

            var state = new State(new ArrayBlockingQueue<>(5),
                    new AtomicBoolean(false),
                    new AtomicReference<>(null));

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                var driver = state.getDriver().get();
                if (driver != null) {
                    log.info("stopping driver");
                    driver.stop(false);
                    log.info("driver stopped");
                }
            }));

            var offersProcessor = new OffersProcessor(config.getMesos(), state);
            var scheduler = new MenkinsScheduler(offersProcessor);

            var driver = new MesosSchedulerDriver(scheduler, newFrameworkInfo(config), config.getMesos().getMesosMasterUrl());
            state.driver.set(driver);
            startDriver(driver);

            startBufferThread(state, globalQueue);

            log.info("driver initialized");
        };
    }

    private static Protos.FrameworkInfo newFrameworkInfo(AppConfig config) {
        return Protos.FrameworkInfo.newBuilder()
                .setUser(config.getMesos().getSlaveUser())
                .setName(config.getMesos().getFrameworkName())
                .setRole(config.getMesos().getRole())
                .setCheckpoint(config.getMesos().isCheckpoint())
                .setWebuiUrl(config.getUrl())
                .build();
    }

    private static void startDriver(SchedulerDriver driver) {
        var thread = new Thread(() -> {
            var status = driver.run();
            if (status != Protos.Status.DRIVER_STOPPED) {
                log.error("driver was aborted, status " + status.getNumber());
            } else {
                log.info("driver stopped");
            }
        });
        thread.setName("driver-status-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private static void startBufferThread(State state, BlockingQueue<NodeRequestWithResources> globalQueue) {
        var BUFFER_SIZE = 5;
        var TIME_LIMIT = TimeUnit.SECONDS.toMillis(5);
        var thread = new Thread(() -> {
            var local = new ArrayList<NodeRequestWithResources>(BUFFER_SIZE);
            var newGroupCreated = System.currentTimeMillis();
            for (;;) {
                try {
                    var value = globalQueue.poll(1, TimeUnit.SECONDS);
                    if (value != null) local.add(value);

                    var size = local.size();
                    if (size > 0 && (size >= BUFFER_SIZE || (System.currentTimeMillis() - newGroupCreated > TIME_LIMIT))) {
                        state.localQueue.add(local);
                        local = new ArrayList<>(BUFFER_SIZE);
                        newGroupCreated = System.currentTimeMillis();

                        if (state.suppress.get()) {
                            state.suppress.set(false);
                            state.driver.get().reviveOffers();
                        }
                    }
                } catch (Exception e) {
                    log.error("error in global queue to local thread", e);
                }
            }
        });
        thread.setName("global-to-local-queue");
        thread.setDaemon(true);
        thread.start();
    }
}
