package org.ka.menkins.mesos;

import io.prometheus.client.Counter;
import lombok.extern.slf4j.Slf4j;
import org.apache.mesos.Protos;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
class MenkinsScheduler implements Scheduler {

    static class Metrics {
        static final Counter receivedOffers = Counter.build()
                .name("menkins_mesos_offers_received")
                .help("numbere of received offers from mesos master")
                .register();
    }

    Consumer<List<Protos.Offer>> offersProcessor;
    AtomicReference<DriverState> stateRef;

    MenkinsScheduler(AtomicReference<DriverState> stateRef, Consumer<List<Protos.Offer>> offersProcessor) {
        this.offersProcessor = offersProcessor;
        this.stateRef = stateRef;
    }

    @Override
    public void registered(SchedulerDriver driver, Protos.FrameworkID frameworkId, Protos.MasterInfo masterInfo) {
        log.info("framework registered with id " + frameworkId.getValue() + " to mesos master " + masterInfo);
        DriverState.update(stateRef, old -> old.withFrameworkID(frameworkId)
                .withDriver(driver)
                .withRunning(true)
                .withSuppressed(false)
        );
    }

    @Override
    public void reregistered(SchedulerDriver driver, Protos.MasterInfo masterInfo) {
        DriverState.update(stateRef, old -> old.withDriver(driver)
                .withRunning(true)
                .withSuppressed(false)
        );

        log.info("framework reregistered with id " + getFrameworkId() + " to mesos master " + masterInfo);
    }

    private String getFrameworkId() {
        var state = stateRef.get();
        var frameworkId = state.getFrameworkID();

        return frameworkId != null ? frameworkId.getValue() : null;
    }

    @Override
    public void resourceOffers(SchedulerDriver driver, List<Protos.Offer> offers) {
        Metrics.receivedOffers.inc(offers.size());
        log.info("Framework " + getFrameworkId() + " : received " + offers.size() + " offers");
        offersProcessor.accept(offers);
    }

    @Override
    public void offerRescinded(SchedulerDriver driver, Protos.OfferID offerId) {
        log.info("Framework " + getFrameworkId() + " -> offer rescinded " + offerId.getValue());
    }

    @Override
    public void statusUpdate(SchedulerDriver driver, Protos.TaskStatus status) {
        log.info("Framework " + getFrameworkId() + " -> status update " + status);
    }

    @Override
    public void frameworkMessage(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, byte[] data) {
        log.info("Framework " + getFrameworkId() + " -> message from executor " + executorId.getValue() + " slave " + slaveId.getValue() + " message " + new String(data));
    }

    @Override
    public void disconnected(SchedulerDriver driver) {
        DriverState.update(stateRef, old -> old.withDriver(driver)
                .withRunning(false)
                .withSuppressed(false)
        );
        log.info("Framework " + getFrameworkId() + " -> disconnected");
    }

    @Override
    public void slaveLost(SchedulerDriver driver, Protos.SlaveID slaveId) {
        log.info("Framework " + getFrameworkId() + " -> slave lost " + slaveId.getValue());
    }

    @Override
    public void executorLost(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, int status) {
        log.info("Framework " + getFrameworkId() + " -> executor lost " + executorId.getValue() + ", slaveId = " + slaveId.getValue() + " status = " + status);
    }

    @Override
    public void error(SchedulerDriver driver, String message) {
        log.info("Framework " + getFrameworkId() + " -> error message: " + message);
    }
}
