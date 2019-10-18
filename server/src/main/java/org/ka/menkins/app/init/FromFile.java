package org.ka.menkins.app.init;

import lombok.extern.slf4j.Slf4j;
import org.ka.menkins.app.init.PropertiesHolder.PropertyNames;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

@Slf4j
public class FromFile {

    static final PropertyNames CONFIG_FILE = PropertiesHolder.name("MENKINS_CONFIG_FILE", "menkins.config.file");

    static PropertiesHolder preLoadProperties(PropertiesHolder old) {
        var props = loadForFile("menkins.properties", old);

        var configFile = old.getValue(CONFIG_FILE, () -> null);
        if (configFile == null) return props;

        var file = new File((configFile));
        if (!file.exists()) {
            throw new RuntimeException("configuration file " + configFile + " doesn't exist. Cannot initialize app");
        }

        return loadForFile(configFile, props);
    }

    private static PropertiesHolder loadForFile(String configFile, PropertiesHolder old) {
        log.info("Loading from configuration file " + configFile);

        var file = new File((configFile));
        if (!file.exists())  return old;

        var newProps = new Properties();
        try {
            try (var fis = new FileInputStream(file)) {
                newProps.load(fis);

                var newHolder = new PropertiesHolder(Collections.emptyMap(), newProps);
                return old.merge(newHolder);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private FromFile() {}
}
