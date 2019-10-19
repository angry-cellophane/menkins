package org.ka.menkins.mesos;

import com.hazelcast.core.ITopic;
import lombok.extern.slf4j.Slf4j;
import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.SchedulerDriver;
import org.ka.menkins.app.init.AppConfig;
import org.ka.menkins.storage.NodeRequestWithResources;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class MesosSchedulers {

    public static Runnable newInitializer(AppConfig config,
                                          BlockingQueue<List<NodeRequestWithResources>> aggregatedCreateRequestsQueue,
                                          ITopic<String> terminateTasksTopic) {
        return () -> {
            log.info("Initializing driver");

            var stateRef = new AtomicReference<>(DriverState.newState());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                var driver = stateRef.get().getDriver();
                if (driver != null) {
                    log.info("stopping driver");
                    driver.stop(false);
                    log.info("driver stopped");
                }
            }));

            var offersProcessor = new OffersProcessor(config.getMesos(), aggregatedCreateRequestsQueue, stateRef);
            var scheduler = new MenkinsScheduler(stateRef, offersProcessor);

            var driver = new MesosSchedulerDriver(scheduler, newFrameworkInfo(config), config.getMesos().getMesosMasterUrl());
            stateRef.set(stateRef.get().withDriver(driver));
            startDriver(driver);

            terminateTasksTopic.addMessageListener(TerminateTask.newKiller(stateRef));

            log.info("driver initialized");
        };
    }

    private static Protos.FrameworkInfo newFrameworkInfo(AppConfig config) {
        var mesos = config.getMesos();
        return Protos.FrameworkInfo.newBuilder()
                .setUser(mesos.getSlaveUser())
                .setName(mesos.getFrameworkName())
                .setRole(mesos.getRole())
                .setCheckpoint(mesos.isCheckpoint())
                .setWebuiUrl(mesos.getWebUiUrl())
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

    private MesosSchedulers() {}
}
