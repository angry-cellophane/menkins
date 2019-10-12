package org.ka.menkins.mesos;

import lombok.AllArgsConstructor;
import org.apache.mesos.Protos;
import org.apache.mesos.SchedulerDriver;
import org.ka.menkins.queue.BuilderNodeRequest;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

@AllArgsConstructor
public class OffersProcessor implements Consumer<List<Protos.Offer>> {

    Supplier<SchedulerDriver> driverHolder;
    BlockingQueue<BuilderNodeRequest> requests;

    @Override
    public void accept(List<Protos.Offer> offers) {

    }
}
