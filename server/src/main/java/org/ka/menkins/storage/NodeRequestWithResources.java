package org.ka.menkins.storage;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.ka.menkins.mesos.DockerConfig;

@Value
@AllArgsConstructor(access = AccessLevel.MODULE)
public class NodeRequestWithResources {
    NodeRequest request;
    DockerConfig image;

    public static NodeRequestWithResources from(NodeRequest request) {
        var docker = ResourcesByLabelsLookup.lookup(request.getLabels());
        return new NodeRequestWithResources(request, docker);
    }
}
