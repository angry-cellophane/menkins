package org.ka.menkins.mesos;

import org.apache.mesos.Protos;
import org.ka.menkins.app.init.AppConfig;
import org.ka.menkins.storage.NodeRequest;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TaskBuilder {
    private static final String SLAVE_COMMAND_FORMAT =
            "java -DHUDSON_HOME=jenkins -server -Xmx%dm %s -jar ${MESOS_SANDBOX-.}/slave.jar %s %s -jnlpUrl %s";

    public static Function<OfferRequestMatcher, List<Protos.TaskInfo>> newTaskBuilder(AppConfig.Mesos config) {
        return matcher -> {
            var offer = matcher.getOffer();
            var slaveConfig = matcher.getSlaveConfiguration();

            return matcher.getAcceptedRequests().stream()
                    .map(request -> {
                        var taskId = Protos.TaskID.newBuilder().setValue(request.getRequest().getId()).build();

                        var startJenkins = String.format(SLAVE_COMMAND_FORMAT,
                                slaveConfig.getMem(),
                                slaveConfig.getJvmArgs(),
                                request.getRequest().getJnlpArgs(),
                                request.getRequest().getJnlpSecret(),
                                request.getRequest().getJnlpUrl());

                        var command = Protos.CommandInfo.newBuilder()
                                .setValue(startJenkins)
                                .addUris(
                                        Protos.CommandInfo.URI.newBuilder().setValue(request.getRequest().getSlaveJarUrl())
                                                .setExecutable(false)
                                                .setExtract(false)
                                                .build()
                                )
                                .build();

                        var docker = request.getImage();
                        var resource = docker.getResources();

                        return Protos.TaskInfo.newBuilder()
                                .setTaskId(taskId)
                                .setName("task " + request.getRequest().getNodeName())
                                .setCommand(command)
                                .setSlaveId(offer.getSlaveId())
                                .addResources(
                                        Protos.Resource.newBuilder()
                                                .setName("cpus")
                                                .setType(Protos.Value.Type.SCALAR)
                                                .setRole(config.getRole())
                                                .setScalar(
                                                        Protos.Value.Scalar.newBuilder()
                                                                .setValue(resource.getCpus())
                                                                .build()
                                                )
                                                .build()
                                )
                                .addResources(
                                        Protos.Resource.newBuilder()
                                                .setName("mem")
                                                .setType(Protos.Value.Type.SCALAR)
                                                .setRole(config.getRole())
                                                .setScalar(
                                                        Protos.Value.Scalar.newBuilder()
                                                                .setValue(resource.getMem())
                                                                .build()
                                                )
                                                .build()
                                )
                                .setContainer(newContainerInfo(request.getRequest(), docker))
                                .build();
                    }).collect(Collectors.toList());
        };
    }

    static Protos.ContainerInfo newContainerInfo(NodeRequest request, DockerConfig config) {
        var builder = Protos.ContainerInfo.newBuilder()
                .setDocker(
                        Protos.ContainerInfo.DockerInfo.newBuilder()
                                .setImage(config.getDockerImage())
                                .setForcePullImage(config.isForcePull())
                                .setPrivileged(config.isPrivileged())
                                .setNetwork(config.getNetworking())
                                .build()
                )
                .setType(Protos.ContainerInfo.Type.DOCKER);

        if (config.getNetworking() == Protos.ContainerInfo.DockerInfo.Network.HOST) {
            builder.setHostname(request.getNodeName());
        }

        return builder.build();
    }

    private TaskBuilder() {}
}
