package org.ka.menkins.mesos;

import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.SchedulerDriver;
import org.ka.menkins.app.AppConfig;
import org.ka.menkins.queue.BuilderNodeRequest;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class Schedulers {

    private Schedulers() {}

    public static Runnable newInitializer(AppConfig config, BlockingQueue<BuilderNodeRequest> queue) {
        return () -> {
            var driverHolder = new AtomicReference<SchedulerDriver>();
            var offersProcessor = new OffersProcessor(() -> driverHolder.get(), queue);

            var scheduler = new MenkinsScheduler(offersProcessor);

//            var driver = new MesosSchedulerDriver(scheduler, )
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
}
