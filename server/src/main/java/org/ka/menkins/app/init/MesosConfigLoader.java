package org.ka.menkins.app.init;

import org.ka.menkins.app.init.PropertiesHolder.PropertyNames;

import java.util.UUID;
import java.util.function.Function;

final class MesosConfigLoader {

    static final PropertyNames LIB_PATH = PropertiesHolder.name("MESOS_LIBRARY", "mesos.lib.path");
    static final PropertyNames NODES_CONFIG_PATH = PropertiesHolder.name("MESOS_NODES_CONFIG_FILE", "mesos.nodes.config.file");
    static final PropertyNames ROLE = PropertiesHolder.name("MESOS_ROLE", "mesos.role");
    static final PropertyNames SLAVE_USER = PropertiesHolder.name("MESOS_SLAVE_USER", "mesos.slave.user");
    static final PropertyNames FRAMEWORK_ID = PropertiesHolder.name("MESOS_FRAMEWORK_ID", "mesos.framework.id");
    static final PropertyNames MASTER_URL = PropertiesHolder.name("MESOS_MASTER", "mesos.master");
    static final PropertyNames CHECKPOINT_ON = PropertiesHolder.name("MESOS_CHECKPOINT_ON", "mesos.checkpoint.on");
    static final PropertyNames WEBUI_URL = PropertiesHolder.name("MESOS_WEBUI_URL", "mesos.webui.url");
    static final PropertyNames PRINCIPLE = PropertiesHolder.name("MESOS_PRINCIPLE", "mesos.principle");
    static final PropertyNames SECRET = PropertiesHolder.name("MESOS_SECRET", "mesos.secret");


    static Function<AppConfig.AppConfigBuilder, AppConfig.AppConfigBuilder> load(PropertiesHolder properties) {
        return builder -> {
            var mesos = AppConfig.Mesos.builder();

            var path = properties.getValue(LIB_PATH);
            var nodeConfigFilePath = properties.getValue(NODES_CONFIG_PATH, () -> "mesos_nodes.yaml");
            var role = properties.getValue(ROLE);
            var id = properties.getValue(FRAMEWORK_ID, () -> UUID.randomUUID().toString());
            var slaveUser = properties.getValue(SLAVE_USER);
            var mesosMaster = properties.getValue(MASTER_URL);
            var checkpoint = Boolean.getBoolean(properties.getValue(CHECKPOINT_ON, () -> "true"));
            var webui = properties.getValue(WEBUI_URL, () -> id);
            var principle = properties.getValue(PRINCIPLE, () -> "jenkins");
            var secret = properties.getValue(SECRET, () -> "");

            return builder.mesos(mesos
                    .pathToMesosLib(path)
                    .role(role)
                    .slaveUser(slaveUser)
                    .frameworkName("menkins-" + id)
                    .mesosMasterUrl(mesosMaster)
                    .checkpoint(checkpoint)
                    .webUiUrl(webui)
                    .principal(principle)
                    .secret(secret)
                    .nodeConfigPath(nodeConfigFilePath)
                    .build());
        };
    }

    private MesosConfigLoader() {}
}
