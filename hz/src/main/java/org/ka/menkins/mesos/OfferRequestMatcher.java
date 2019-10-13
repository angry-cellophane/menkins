package org.ka.menkins.mesos;

import lombok.Getter;
import org.apache.mesos.Protos;
import org.ka.menkins.queue.MesosResources;
import org.ka.menkins.queue.NodeRequestWithResources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OfferRequestMatcher {

    private static final String ALL_ROLES = "*";

    @Getter
    Protos.Offer offer;
    @Getter SlaveConfiguration slaveConfiguration;

    List<NodeRequestWithResources> acceptedRequests;
    Map<String, MesosResources> resourcesByRole;


    public OfferRequestMatcher(Protos.Offer offer, Collection<MesosResources> resources, SlaveConfiguration slaveConfiguration) {
        this.offer = offer;
        this.slaveConfiguration = slaveConfiguration;
        this.resourcesByRole = new HashMap<>();
        this.acceptedRequests = new ArrayList<>();

        for (MesosResources resource: resources) {
            if (resourcesByRole.containsKey(resource.getRole())) {
                throw new RuntimeException("Offer " + offer.getId().getValue() + " has two resources with role " + resource.getRole());
            }

            resourcesByRole.put(resource.getRole(), resource);
        }
    }

    public boolean canFit(NodeRequestWithResources request) {
        return List.of(request.getImage().getResources().getRole(), ALL_ROLES).stream()
                .anyMatch(role -> {
                    var offerResource = resourcesByRole.get(role);
                    return offerResource.greaterThan(request.getImage().getResources());
                });
    }

    public void accept(NodeRequestWithResources request) {
        if (!canFit(request)) {
            String message = "cannot accept request " + request.getRequest().getId() + " from " + request.getRequest().getJenkinsUrl() + ", not enough resources to fit";
            throw new RuntimeException(message);
        }

        for (var role: List.of(request.getImage().getResources().getRole(), ALL_ROLES)) {
            var resources = this.resourcesByRole.get(role);
            if (resources == null) continue;

            if (resources.greaterThan(request.getImage().getResources())) {
                this.resourcesByRole.put(role, resources.substract(request.getImage().getResources()));
                this.acceptedRequests.add(request);
                break;
            }
        }
    }

    public List<NodeRequestWithResources> getAcceptedRequests() {
        return Collections.unmodifiableList(this.acceptedRequests);
    }


    public static OfferRequestMatcher from(Protos.Offer offer) {
        var byRole = offer.getResourcesList().stream().reduce(
                new HashMap<String, MesosResources>(),
                (all, resource) -> {
                    var accum = all.computeIfAbsent(resource.getRole(), r -> new MesosResources(r, 0.0, 0.0));
                    if (resource.getName().equals("cpus")) {
                        var cpus = resource.getScalar().getValue();
                        accum = accum.withCpus(cpus);
                    }

                    if (resource.getName().equals("mem")) {
                        var mem = resource.getScalar().getValue();
                        accum = accum.withMem(mem);
                    }
                    all.put(resource.getRole(), accum);

                    return all;
                },
                (map1, map2) -> {
                    map1.putAll(map2);
                    return map1;
                }
        );
        return new OfferRequestMatcher(offer, byRole.values(), SlaveConfiguration.DEFAULT);
    }
}
