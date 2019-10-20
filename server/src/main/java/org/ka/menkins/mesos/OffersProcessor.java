package org.ka.menkins.mesos;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mesos.Protos;
import org.apache.mesos.SchedulerDriver;
import org.ka.menkins.app.init.AppConfig;
import org.ka.menkins.storage.NodeRequestWithResources;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class OffersProcessor implements Consumer<List<Protos.Offer>> {

    static final Protos.Offer NO_MATCH = Protos.Offer.newBuilder()
            .setId(Protos.OfferID.newBuilder().setValue("NO_MATCH").build())
            .setFrameworkId(Protos.FrameworkID.newBuilder().setValue("fake_no_match").build())
            .setSlaveId(Protos.SlaveID.newBuilder().setValue("fake_slave_id").build())
            .setHostname("fake_host")
            .addResources(Protos.Resource.newBuilder()
                    .setName("cpus")
                    .setRole("*")
                    .setType(Protos.Value.Type.SCALAR)
                    .setScalar(Protos.Value.Scalar.newBuilder().setValue(10000000000.0d).build())
                    .build())
            .addResources(Protos.Resource.newBuilder()
                    .setName("mem")
                    .setRole("*")
                    .setType(Protos.Value.Type.SCALAR)
                    .setScalar(Protos.Value.Scalar.newBuilder().setValue(10000000000.0d).build())
                    .build())
            .build();

    AppConfig.Mesos config;
    BlockingQueue<List<NodeRequestWithResources>> queue;
    AtomicReference<DriverState> stateRef;

    @Override
    public void accept(List<Protos.Offer> offers) {
        log.info("processing " + offers.size() + " offers");
        var state = stateRef.get();
        var driver = state.getDriver();
        if (driver == null) {
            throw new IllegalStateException("driver cannot be null");
        }
        var declineOffer = declineOfferClosure(driver);

        var requests = queue.poll();
        log.info("found " + (requests == null ? 0 : requests.size()) + " requests");
        if (requests == null) {
            offers.stream().map(Protos.Offer::getId).forEach(declineOffer);
            driver.suppressOffers();
            stateRef.set(state.withSuppressed(true));
            log.info("framework suppressed as there were no builder node requests");
            return;
        }

        var matchers = matchByOffer(requests, offers);

        // decline unused offers
        matchers.stream().filter(matcher -> matcher.getAcceptedRequests().isEmpty())
                .filter(matcher -> matcher.getOffer() != NO_MATCH)
                .map(matcher -> matcher.getOffer().getId())
                .forEach(declineOffer);

        var matched = matchers.stream().filter(matcher -> !matcher.getAcceptedRequests().isEmpty()).collect(Collectors.toList());

        var taskBuilder = TaskBuilder.newTaskBuilder(config);
        matched.forEach(matcher -> {
            if (matcher.getOffer() == NO_MATCH) {
                log.info("no match found for requests " + matcher.getAcceptedRequests().stream().map(r -> r.getRequest().getId()).collect(Collectors.joining(", ")));
                queue.add(matcher.getAcceptedRequests());
            } else {
                var tasks = taskBuilder.apply(matcher);
                if (!tasks.isEmpty()) {
                    log.info("offer " + matcher.getOffer().getId().getValue() + " used to start tasks " + tasks.stream().map(t -> t.getTaskId().getValue()).collect(Collectors.joining(", ")));
                    driver.launchTasks(Collections.singletonList(matcher.getOffer().getId()), tasks);
                }
            }
        });
    }

    private List<OfferRequestMatcher> matchByOffer(List<NodeRequestWithResources> requests, List<Protos.Offer> offers) {
        var matchers = offers.stream().map(OfferRequestMatcher::from)
                .collect(Collectors.toList());
        matchers.add(OfferRequestMatcher.from(NO_MATCH));

        for (NodeRequestWithResources request : requests) {
            for (OfferRequestMatcher matcher : matchers) {
                if (!matcher.canFit(request)) continue;

                matcher.accept(request);
                break; // found a suitable offer, move to the next request
            }
        }

        return matchers;
    }

    private Consumer<Protos.OfferID> declineOfferClosure(SchedulerDriver driver) {
        var filters = Protos.Filters.newBuilder().setRefuseSeconds(config.getRefuseInSeconds()).build();
        return offer -> driver.declineOffer(offer, filters);
    }
}
