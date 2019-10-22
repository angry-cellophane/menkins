package org.ka.menkins.mesos;

import org.ka.menkins.app.init.AppConfig;
import org.ka.menkins.storage.DockerConfig;
import org.ka.menkins.storage.MesosResources;
import org.yaml.snakeyaml.Yaml;
import spark.utils.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MesosBuilderNodesBuilder {
    public static Function<String, DockerConfig> create(AppConfig.Mesos config) {
        var configFile = config.getNodeConfigPath();
        var file = new File(configFile);
        if (!file.exists()) {
            throw new RuntimeException("mesos config file " + configFile + " doesn't exist");
        }

        try (var is = new FileInputStream(file)) {
            var mapping = new Yaml().loadAs(is, BuilderNodesMappingDefinition.class);

            Map<String, DockerConfig> configByLabels = new HashMap<>();
            for (BuilderNodeDefinition builder : mapping.getBuilders()) {
                var docker = toDockerConfig(config, builder);
                var labelBits = Arrays.stream(builder.getLabels().split("\t"))
                        .filter(StringUtils::isNotBlank)
                        .sorted()
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    private static DockerConfig toDockerConfig(AppConfig.Mesos config, BuilderNodeDefinition builder) {
        var resources = new MesosResources(config.getRole(), builder.getCpu(), builder.getMemory());
        return new DockerConfig(resources, builder.getDockerImageUrl(), builder.isForcePull(), builder.isPrivileged(), builder.getNetwork());
    }
}
