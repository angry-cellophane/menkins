package org.ka.menkins.mesos;

import com.hazelcast.core.IQueue;
import org.ka.menkins.queue.BuilderNodeRequest;

public class Schedulers {

    private Schedulers() {}

    public static Runnable newInitializer(IQueue<BuilderNodeRequest> queue) {
        return () -> {

        };
    }
}
