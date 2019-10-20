package org.ka.menkins.mesos;

import com.hazelcast.core.ITopic;
import lombok.extern.slf4j.Slf4j;
import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.SchedulerDriver;
import org.ka.menkins.app.init.AppConfig;
import org.ka.menkins.storage.NodeRequestWithResources;
import spark.utils.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class MesosSchedulers {

    public static Runnable newInitializer(AppConfig config,
                                          AtomicReference<DriverState> stateRef,
                                          BlockingQueue<List<NodeRequestWithResources>> aggregatedCreateRequestsQueue,
                                          ITopic<String> terminateTasksTopic) {
        return () -> {
            log.info("Initializing driver");

            var offersProcessor = new OffersProcessor(config.getMesos(), aggregatedCreateRequestsQueue, stateRef);
            var scheduler = new MenkinsScheduler(stateRef, offersProcessor);

            var frameworkInfo = newFrameworkInfo(config);
            var mesosUrl = config.getMesos().getMesosMasterUrl();

            var principle = config.getMesos().getPrincipal();
            var driver = Optional.ofNullable(config.getMesos().getSecret())
                    .filter(StringUtils::isNotBlank)
                    .map(s -> Protos.Credential.newBuilder().setPrincipal(principle).setSecret(s).build())
                    .map(secret -> new MesosSchedulerDriver(scheduler, frameworkInfo, mesosUrl, secret))
                    .orElseGet(() -> new MesosSchedulerDriver(scheduler, frameworkInfo, mesosUrl));

            DriverState.update(stateRef, old -> old.withDriver(driver));
            startDriver(driver);

            terminateTasksTopic.addMessageListener(TerminateTask.newKiller(stateRef));

            log.info("driver initialized");
        };
    }

    public static Runnable newFinalizer(AtomicReference<DriverState> stateRef) {
        return () -> {
            var state = stateRef.get();
            var driver = state.getDriver();
            if (driver != null) {
                log.info("stopping driver");
                driver.stop(false);
                log.info("driver stopped");
            }
            if (state.isRunning()) {
                DriverState.update(stateRef, old -> old.withRunning(false));
            }
        };
    }

    private static Protos.FrameworkInfo newFrameworkInfo(AppConfig config) {
        var mesos = config.getMesos();
        return Protos.FrameworkInfo.newBuilder()
                .setUser(mesos.getSlaveUser())
                .setName(mesos.getFrameworkName())
                .setRole(mesos.getRole())
                .setCheckpoint(mesos.isCheckpoint())
                .setPrincipal(mesos.getPrincipal())
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
