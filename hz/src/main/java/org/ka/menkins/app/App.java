package org.ka.menkins.app;

import io.prometheus.client.exporter.MetricsServlet;
import lombok.AllArgsConstructor;
import org.apache.mesos.MesosNativeLibrary;
import org.ka.menkins.mesos.Schedulers;
import org.ka.menkins.queue.BuilderNodeRequest;
import org.ka.menkins.queue.RequestsQueue;

import static spark.Spark.*;

@AllArgsConstructor
public class App {

    AppConfig config;

    public static void main(String[] args) {
        var id = 1;

        var config = AppConfig.builder()
                .mesos(
                        AppConfig.Mesos.builder()
                                .pathToMesosLib("/home/aleksandr/src/3rd/mesos/mesos-1.9.0/build/src/.libs/libmesos.so")
                                .role("*")
                                .slaveUser("nobody")
                                .frameworkName("menkins-" + id)
                                .mesosMasterUrl("172.28.128.7:5050")
                                .checkpoint(true)
                                .build()
                )
                .port(5678)
                .url("http://localhost:5678")
                .hazelcast(null)
                .build();

        new App(config).run();
    }

    void run() {
        validate();
        Metrics.Requests.total.get();

        var queue = RequestsQueue.getQueue(config.getHazelcast());

        port(config.getPort());
        path("/api/v1", () -> {
            post("/node", "application/json", ((request, response) -> {
                Metrics.Requests.total.inc();

                var node = Json.from(request.bodyAsBytes(), BuilderNodeRequest.class);
                node.validate();
                queue.add(node);

                return "";
            }));
        });
        get("/health", ((request, response) -> "up"));

        var metrics = new MetricsServlet();
        get("/prometheus", ((request, response) -> {
            metrics.service(request.raw(), response.raw());
            return "";
        }));

        Schedulers.newInitializer(config, queue).run();


        awaitInitialization();
    }

    private void validate() {
        MesosNativeLibrary.load(config.getMesos().getPathToMesosLib());
    }

}
