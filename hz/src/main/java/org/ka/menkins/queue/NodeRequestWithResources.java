package org.ka.menkins.queue;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.ka.menkins.mesos.DockerConfig;

@Value
@AllArgsConstructor(access = AccessLevel.MODULE)
public class NodeRequestWithResources {
    NodeRequest request;
    DockerConfig image;
}
