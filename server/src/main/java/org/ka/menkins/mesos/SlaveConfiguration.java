package org.ka.menkins.mesos;

import lombok.Value;

import java.io.Serializable;

@Value
public class SlaveConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final SlaveConfiguration DEFAULT = new SlaveConfiguration(64,
            "-Xms16m -XX:+UseConcMarkSweepGC -Djava.net.preferIPv4Stack=true");

    int mem;
    String jvmArgs;
}
