package org.ka.menkins.storage;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.ka.menkins.mesos.DockerConfig;

import java.io.Serializable;

@Value
@AllArgsConstructor(access = AccessLevel.MODULE)
public class NodeRequestWithResources implements Serializable {
    private static final long serialVersionUID = 1L;

    NodeRequest request;
    DockerConfig image;

    public static NodeRequestWithResources from(NodeRequest request) {
        var docker = ResourcesByLabelsLookup.lookup(request.getLabels());
        return new NodeRequestWithResources(request, docker);
    }
}
