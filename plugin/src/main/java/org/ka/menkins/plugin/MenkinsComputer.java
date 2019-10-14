package org.ka.menkins.plugin;

import hudson.model.Slave;
import hudson.slaves.SlaveComputer;

public class MenkinsComputer extends SlaveComputer {
    public MenkinsComputer(Slave slave) {
        super(slave);
    }
}
