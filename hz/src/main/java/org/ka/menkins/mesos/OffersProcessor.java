package org.ka.menkins.mesos;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mesos.Protos;
import org.apache.mesos.SchedulerDriver;
import org.ka.menkins.app.AppConfig;
import org.ka.menkins.queue.NodeRequestWithResources;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class OffersProcessor implements Consumer<List<Protos.Offer>> {

    AppConfig config;
    Schedulers.State state;

    @Override
    public void accept(List<Protos.Offer> offers) {
        var driver = state.getDriver().get();
        var declineOffer = declineOfferClosure(driver);

        var requests = state.getLocalQueue().poll();
        if (requests == null) {
            offers.stream().map(Protos.Offer::getId).forEach(declineOffer);
            return;
        }
        var matchers = offers.stream().map(OfferRequestMatcher::from)
                .collect(Collectors.toList());

        matchByOffer(requests, matchers);

        // decline unused offers
        matchers.stream().filter(matcher -> matcher.getAcceptedRequests().isEmpty())
                .map(matcher -> matcher.getOffer().getId())
                .forEach(declineOffer);

        var matched = matchers.stream().filter(matcher -> !matcher.getAcceptedRequests().isEmpty()).collect(Collectors.toList());

        var taskBuilder = TaskBuilder.newTaskBuilder(config.getMesos());
        var tasks = matched.stream()
                .flatMap(matcher -> taskBuilder.apply(matcher).stream())
                .collect(Collectors.toList());

        if (!tasks.isEmpty()) {
            driver.launchTasks(
                    matched.stream().map(o -> o.getOffer().getId()).collect(Collectors.toList()),
                    tasks
            );
        }
    }

    private void matchByOffer(List<NodeRequestWithResources> requests, List<OfferRequestMatcher> offers) {
        for (NodeRequestWithResources request : requests) {
            for (OfferRequestMatcher offer : offers) {
                if (!offer.canFit(request)) continue;

                offer.accept(request);
                break; // found a suitable offer, move to the next request
            }
        }
    }

    private Consumer<Protos.OfferID> declineOfferClosure(SchedulerDriver driver) {
        var filters = Protos.Filters.newBuilder().setRefuseSeconds(config.getMesos().getRefuseInSeconds()).build();
        return offer -> driver.declineOffer(offer, filters);
    }
}
