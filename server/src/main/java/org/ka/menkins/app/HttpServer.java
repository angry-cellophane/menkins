package org.ka.menkins.app;

import com.hazelcast.core.ITopic;
import io.prometheus.client.exporter.MetricsServlet;
import org.ka.menkins.storage.NodeRequest;
import org.ka.menkins.storage.NodeRequestWithResources;

import java.util.concurrent.BlockingQueue;

import static spark.Spark.awaitInitialization;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.path;
import static spark.Spark.port;
import static spark.Spark.post;

public class HttpServer {

    public static Runnable newInitializer(AppConfig config, BlockingQueue<NodeRequestWithResources> queue,
                                          ITopic<String> terminateTaskRequests) {
        return () -> {
            port(config.getPort());
            path("/api/v1", () -> {
                post("/node", "application/json", ((request, response) -> {
                    Metrics.Requests.total.inc();

                    var node = Json.from(request.bodyAsBytes(), NodeRequest.class);
                    node.validate();
                    queue.add(NodeRequestWithResources.from(node));

                    return "";
                }));
                delete("/node/:id", (request, response) -> {
                    Metrics.Requests.total.inc();

                    var id = request.params(":id");
                    terminateTaskRequests.publish(id);
                    return "accepted";
                });
            });
            get("/health", ((request, response) -> "up"));

            var metrics = new MetricsServlet();
            get("/prometheus", ((request, response) -> {
                metrics.service(request.raw(), response.raw());
                return "";
            }));

            awaitInitialization();
        };
    }

    private HttpServer() {}
}
