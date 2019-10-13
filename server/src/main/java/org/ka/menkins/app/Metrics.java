package org.ka.menkins.app;

import io.prometheus.client.Counter;

public class Metrics {

    public static class Requests {
        public static final Counter total = Counter.build()
                .name("menkins_node_requests_total")
                .help("number of node requests")
                .register();
    }

    private Metrics() {}
}
