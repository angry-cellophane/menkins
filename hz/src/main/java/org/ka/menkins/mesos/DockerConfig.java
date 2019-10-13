package org.ka.menkins.mesos;

import lombok.Value;
import org.apache.mesos.Protos;
import org.ka.menkins.queue.MesosResources;

@Value
public class DockerConfig {
    MesosResources resources;
    String dockerImage;
    boolean forcePull;
    boolean privileged;
    Protos.ContainerInfo.DockerInfo.Network networking;
}
