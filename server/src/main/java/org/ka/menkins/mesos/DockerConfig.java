package org.ka.menkins.mesos;

import lombok.Value;
import lombok.With;
import org.apache.mesos.Protos;
import org.ka.menkins.storage.MesosResources;

import java.io.Serializable;

@Value
@With
public class DockerConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    MesosResources resources;
    String dockerImage;
    boolean forcePull;
    boolean privileged;
    Protos.ContainerInfo.DockerInfo.Network networking;
}
