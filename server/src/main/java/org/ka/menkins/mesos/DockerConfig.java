package org.ka.menkins.mesos;

import lombok.Value;
import lombok.With;
import org.apache.mesos.Protos;
import org.ka.menkins.storage.MesosResources;

@Value
@With
public class DockerConfig {
    MesosResources resources;
    String dockerImage;
    boolean forcePull;
    boolean privileged;
    Protos.ContainerInfo.DockerInfo.Network networking;
}
