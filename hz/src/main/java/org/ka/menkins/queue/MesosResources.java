package org.ka.menkins.queue;

import lombok.Value;
import lombok.With;

@Value
@With
public class MesosResources {

    public static class MesosResourceOperationException extends RuntimeException {
        public MesosResourceOperationException(String message) {
            super(message);
        }
    }

    String role;
    double cpus;
    double mem;

    public boolean greaterThan(MesosResources other) {
        return this.mem >= other.mem
                && this.cpus >= other.cpus;
    }

    public MesosResources subtract(MesosResources other) {
        var cpus = this.cpus - other.cpus;
        var mem = this.mem - other.mem;

        if (cpus < 0) throw new MesosResourceOperationException("cpu < 0 when tried to subtract mesos resources");
        if (mem < 0) throw new MesosResourceOperationException("mem < 0 when tried to subtract mesos resources");

        return new MesosResources(this.role, cpus, mem);
    }
}
