package org.ka.menkins.mesos;

import lombok.Value;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.SchedulerDriver;
import org.ka.menkins.app.AppConfig;
import org.ka.menkins.queue.NodeRequestWithResources;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
public class Schedulers {

    @Value
    @With
    public static class DriverState {
        SchedulerDriver driver;
        boolean suppressed;
    }

    public static Runnable newInitializer(AppConfig config,
                                          BlockingQueue<List<NodeRequestWithResources>> aggregatedCreateRequestsQueue,
                                          Consumer<AtomicReference<DriverState>> aggregatorInitializer) {
        return () -> {
            log.info("Initializing driver");

            var stateRef = new AtomicReference<>(new DriverState(null, false));

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                var driver = stateRef.get().driver;
                if (driver != null) {
                    log.info("stopping driver");
                    driver.stop(false);
                    log.info("driver stopped");
                }
            }));

            var offersProcessor = new OffersProcessor(config.getMesos(), aggregatedCreateRequestsQueue, stateRef);
            var scheduler = new MenkinsScheduler(offersProcessor);

            var driver = new MesosSchedulerDriver(scheduler, newFrameworkInfo(config), config.getMesos().getMesosMasterUrl());
            stateRef.set(stateRef.get().withDriver(driver));
            startDriver(driver);

            aggregatorInitializer.accept(stateRef);

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

    private Schedulers() {}
}
