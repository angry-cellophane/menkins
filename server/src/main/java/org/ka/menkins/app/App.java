package org.ka.menkins.app;

import io.prometheus.client.exporter.MetricsServlet;
import lombok.AllArgsConstructor;
import org.apache.mesos.MesosNativeLibrary;
import org.ka.menkins.mesos.Schedulers;
import org.ka.menkins.queue.NodeRequest;
import org.ka.menkins.queue.NodeRequestWithResources;
import org.ka.menkins.queue.Storage;

import java.util.concurrent.Executors;

import static spark.Spark.awaitInitialization;
import static spark.Spark.get;
import static spark.Spark.path;
import static spark.Spark.port;
import static spark.Spark.post;

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
                                .mesosMasterUrl("172.28.128.13:5050")
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

        var storage = Storage.newLocalStorageManager();
        var createRequests = storage.createNodeRequests();
        var aggregatedCreateRequests = storage.aggregatedCreateNodeRequests();

        port(config.getPort());
        path("/api/v1", () -> {
            post("/node", "application/json", ((request, response) -> {
                Metrics.Requests.total.inc();

                var node = Json.from(request.bodyAsBytes(), NodeRequest.class);
                node.validate();
                createRequests.add(NodeRequestWithResources.from(node));

                return "";
            }));
        });
        get("/health", ((request, response) -> "up"));

        var metrics = new MetricsServlet();
        get("/prometheus", ((request, response) -> {
            metrics.service(request.raw(), response.raw());
            return "";
        }));

        var aggregatorPool = Executors.newSingleThreadExecutor(runnable -> {
            var t = new Thread(runnable);
            t.setDaemon(true);
            t.setName("menkins-aggregator-thread");
            return t;
        });

        var storageConfig = Storage.StorageConfiguration.builder()
                .bufferSize(5)
                .build();

        var aggregator = RequestsAggregator.newAggregatorInitializer(aggregatorPool, storageConfig, storage);
        Schedulers.newInitializer(config, aggregatedCreateRequests, aggregator).run();

        SubmitTestRequests.doSubmit(createRequests);

        awaitInitialization();
    }

    private void validate() {
        MesosNativeLibrary.load(config.getMesos().getPathToMesosLib());
    }

}
