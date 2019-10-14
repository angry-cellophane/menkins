package org.ka.menkins.plugin;

import hudson.model.Descriptor;
import hudson.model.Slave;
import hudson.slaves.ComputerLauncher;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MenkinsSlave extends Slave  {
    private static final Logger LOGGER = Logger.getLogger(MenkinsSlave.class.getName());

    public MenkinsSlave(String name, String labels) throws Descriptor.FormException, IOException {
        super(name,
                labels,  // node description
                "jenkins",
                "1", // number of executors
                Mode.EXCLUSIVE,
                labels,
                new MenkinsComputerLauncher(name, labels),
                new MenkinsRetentionStrategy(),
                Collections.emptyList());
    }

    public String getUuid() {
        return ((MenkinsComputerLauncher)getLauncher()).getUuid();
    }

    public void terminate() {
        LOGGER.info("terminating menkins slave " + this.getNodeName());
        try {
            Jenkins.getInstance().removeNode(this);

            ComputerLauncher launcher = getLauncher();

            if (launcher instanceof MenkinsComputerLauncher) {
                ((MenkinsComputerLauncher) launcher).terminate();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to terminate Menkins instance: " + getNodeName(), e);
        }
    }
}
