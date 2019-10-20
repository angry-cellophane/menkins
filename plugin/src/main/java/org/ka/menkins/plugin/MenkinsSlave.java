package org.ka.menkins.plugin;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Slave;
import hudson.slaves.ComputerLauncher;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MenkinsSlave extends Slave  {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MenkinsSlave.class.getName());

    private final AtomicBoolean pendingDelete;

    public MenkinsSlave(String name, String labels, long nodeTimeoutInNs) throws Descriptor.FormException, IOException {
        super(name,
                labels,  // node description
                "jenkins",
                "1", // number of executors
                Mode.EXCLUSIVE,
                labels,
                new MenkinsComputerLauncher(name, labels, nodeTimeoutInNs),
                new MenkinsRetentionStrategy(3),
                Collections.emptyList());

        this.pendingDelete = new AtomicBoolean();
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

    public boolean isPendingDelete() {
        return pendingDelete.get();
    }

    public void setPendingDelete(boolean value) {
        pendingDelete.set(value);
    }

    @Override
    public Computer createComputer() {
        return new MenkinsComputer(this);
    }

    @Extension
    public static class DescriptorImpl extends SlaveDescriptor {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "Menkins slave";
        }

        @Override
        public boolean isInstantiable() {
            return false;
        }
    }
}
