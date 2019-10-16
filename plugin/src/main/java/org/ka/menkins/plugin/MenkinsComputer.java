package org.ka.menkins.plugin;

import hudson.model.Slave;
import hudson.slaves.SlaveComputer;

import javax.annotation.CheckForNull;

public class MenkinsComputer extends SlaveComputer {
    public MenkinsComputer(Slave slave) {
        super(slave);
    }

    @CheckForNull
    @Override
    public MenkinsSlave getNode() {
        return (MenkinsSlave) super.getNode();
    }
}
