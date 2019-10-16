package org.ka.menkins.mesos;

import com.hazelcast.core.MessageListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.mesos.Protos;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class TerminateTask {

    public static MessageListener<String> newKiller(AtomicReference<Schedulers.DriverState> stateRef) {
        return message -> {
            var taskId = message.getMessageObject();
            log.info("received request to terminate task " + taskId);
            try {
                var state = stateRef.get();
                if (state == null) {
                    log.error("State object is null. Cannot terminate task " + taskId);
                    return;
                }

                var driver = state.getDriver();
                if (driver == null) {
                    log.error("Driver is null. Cannot terminate task " + taskId);
                    return;
                }

                var status = driver.killTask(Protos.TaskID.newBuilder().setValue(taskId).build());
                log.info("Killed task " + taskId + ", driver status = " + status);
            } catch (Exception e) {
                log.error("error while trying to terminate task " + taskId, e);
            }
        };
    }

    private TerminateTask() {}
}
