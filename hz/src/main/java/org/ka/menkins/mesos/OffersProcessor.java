package org.ka.menkins.mesos;

import lombok.AllArgsConstructor;
import org.apache.mesos.Protos;
import org.ka.menkins.queue.BuilderNodeRequest;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

@AllArgsConstructor
public class OffersProcessor implements Consumer<List<Protos.Offer>> {

    Schedulers.State state;
    BlockingQueue<BuilderNodeRequest> requests;

    @Override
    public void accept(List<Protos.Offer> offers) {
    }
}
