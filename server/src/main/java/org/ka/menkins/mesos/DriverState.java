package org.ka.menkins.mesos;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;
import org.apache.mesos.Protos;
import org.apache.mesos.SchedulerDriver;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Value
@With
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DriverState {
    SchedulerDriver driver;
    Protos.FrameworkID frameworkID;
    boolean running;
    boolean suppressed;

    public static DriverState newState() {
        return new DriverState(null, null, false, false);
    }

    public static void update(AtomicReference<DriverState> stateRef, Function<DriverState, DriverState> update) {
        boolean updated = false;
        while (!updated) {
            var old = stateRef.get();
            var newState = update.apply(old);
            updated = stateRef.compareAndSet(old, newState);
        }
    }
}
