package org.ka.menkins.mesos;

import lombok.Builder;
import lombok.Value;
import org.apache.mesos.Protos;

@Value
@Builder
public class BuilderNodeDefinition {
    String labels;
    String dockerImageUrl;
    double cpu;
    double memory;
    boolean forcePull;
    boolean privileged;
    Protos.ContainerInfo.DockerInfo.Network network;
    String customShellCommand;
}
