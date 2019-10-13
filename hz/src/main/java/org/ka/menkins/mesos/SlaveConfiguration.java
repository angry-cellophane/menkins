package org.ka.menkins.mesos;

import lombok.Value;

@Value
public class SlaveConfiguration {

    public static final SlaveConfiguration DEFAULT = new SlaveConfiguration(64,
            "-Xms16m -XX:+UseConcMarkSweepGC -Djava.net.preferIPv4Stack=true");

    int mem;
    String jvmArgs;
}
