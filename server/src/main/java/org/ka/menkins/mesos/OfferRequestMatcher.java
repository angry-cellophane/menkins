package org.ka.menkins.mesos;

import lombok.Getter;
import org.apache.mesos.Protos;
import org.ka.menkins.storage.MesosResources;
import org.ka.menkins.storage.NodeRequestWithResources;

import java.util.ArrayList;
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


    public OfferRequestMatcher(Protos.Offer offer, Map<String, MesosResources> resources, SlaveConfiguration slaveConfiguration) {
        this.offer = offer;
        this.slaveConfiguration = slaveConfiguration;
        this.acceptedRequests = new ArrayList<>();
        this.resourcesByRole = new HashMap<>(resources);
    }

    public boolean canFit(NodeRequestWithResources request) {
        return List.of(request.getImage().getResources().getRole(), ALL_ROLES).stream()
                .anyMatch(role -> {
                    var offerResource = resourcesByRole.get(role);
                    if (offerResource == null) return false;

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
                this.resourcesByRole.put(role, resources.subtract(request.getImage().getResources()));
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
        return new OfferRequestMatcher(offer, byRole, SlaveConfiguration.DEFAULT);
    }
}
