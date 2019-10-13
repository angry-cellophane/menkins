package org.ka.menkins.queue;

import lombok.Value;
import lombok.With;

@Value
@With
public class MesosResources {
    String role;
    double cpus;
    double mem;

    public boolean greaterThan(MesosResources other) {
        return this.mem > other.mem
                && this.cpus > other.cpus;
    }

    public MesosResources substract(MesosResources other) {
        return new MesosResources(this.role, this.cpus - other.cpus, this.mem - other.mem);
    }
}
