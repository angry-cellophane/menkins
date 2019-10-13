package org.ka.menkins.queue;

import org.apache.mesos.Protos;
import org.ka.menkins.mesos.DockerConfig;

public class ResourcesByLabelsLookup {

    public static final MesosResources DEFAULT_RESOURCES = new MesosResources("*", 0.2, 128);
    public static final DockerConfig DEFAULT_IMAGE = new DockerConfig(DEFAULT_RESOURCES,
            "jenkins/jnlp-slave",
            false,
            false,
            Protos.ContainerInfo.DockerInfo.Network.BRIDGE);

    public static DockerConfig lookup(String labels) {
        return DEFAULT_IMAGE;
    }

    private ResourcesByLabelsLookup() {}
}
