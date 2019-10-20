package org.ka.menkins.app;

import com.hazelcast.core.ITopic;
import io.prometheus.client.exporter.MetricsServlet;
import lombok.extern.slf4j.Slf4j;
import org.ka.menkins.app.init.AppConfig;
import org.ka.menkins.storage.NodeRequest;
import org.ka.menkins.storage.NodeRequestWithResources;
import spark.Spark;

import java.util.concurrent.BlockingQueue;

import static spark.Spark.awaitInitialization;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.path;
import static spark.Spark.port;
import static spark.Spark.post;

@Slf4j
public class HttpServer {

    public static Runnable newInitializer(AppConfig config, BlockingQueue<NodeRequestWithResources> queue,
                                          ITopic<String> terminateTaskRequests) {
        return () -> {
            log.info("Starting http server");
            port(config.getHttp().getPort());
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
            log.info("Http server started");
        };
    }

    public static Runnable finalizeHttp() {
        return () -> {
            log.info("stopping http server");
            Spark.awaitStop();
            log.info("http server stopped");
        };
    }

    private HttpServer() {}
}
