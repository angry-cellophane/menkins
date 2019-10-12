package org.ka.menkins.mesos;

import lombok.extern.slf4j.Slf4j;
import org.apache.mesos.Protos;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
class MenkinsScheduler implements Scheduler {

    AtomicReference<String> frameworkId;
    Consumer<List<Protos.Offer>> offersProcessor;

    MenkinsScheduler(Consumer<List<Protos.Offer>> offersProcessor) {
        this.offersProcessor = offersProcessor;
        this.frameworkId = new AtomicReference<>(null);
    }

    @Override
    public void registered(SchedulerDriver driver, Protos.FrameworkID frameworkId, Protos.MasterInfo masterInfo) {
        log.info("framework registered with id " + frameworkId.getValue() + " to mesos master " + masterInfo);
        this.frameworkId.set(frameworkId.getValue());
    }

    @Override
    public void reregistered(SchedulerDriver driver, Protos.MasterInfo masterInfo) {
        log.info("framework reregistered with id " + frameworkId.get() + " to mesos master " + masterInfo);
    }

    @Override
    public void resourceOffers(SchedulerDriver driver, List<Protos.Offer> offers) {
        log.info("Framework " + frameworkId.get() + " : received " + offers.size() + " offers");
        offersProcessor.accept(offers);
    }

    @Override
    public void offerRescinded(SchedulerDriver driver, Protos.OfferID offerId) {
        log.info("Framework " + frameworkId.get() + " -> offer rescinded " + offerId.getValue());
    }

    @Override
    public void statusUpdate(SchedulerDriver driver, Protos.TaskStatus status) {
        log.info("Framework " + frameworkId.get() + " -> status update " + status);
    }

    @Override
    public void frameworkMessage(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, byte[] data) {
        log.info("Framework " + frameworkId.get() + " -> message from executor " + executorId.getValue() + " slave " + slaveId.getValue() + " message " + new String(data));
    }

    @Override
    public void disconnected(SchedulerDriver driver) {
        log.info("Framework " + frameworkId.get() + " -> disconnected");
    }

    @Override
    public void slaveLost(SchedulerDriver driver, Protos.SlaveID slaveId) {
        log.info("Framework " + frameworkId.get() + " -> slave lost " + slaveId.getValue());
    }

    @Override
    public void executorLost(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, int status) {
        log.info("Framework " + frameworkId.get() + " -> executor lost " + executorId.getValue() + ", slaveId = " + slaveId.getValue() + " status = " + status);
    }

    @Override
    public void error(SchedulerDriver driver, String message) {
        log.info("Framework " + frameworkId.get() + " -> error message: " + message);
    }
}
