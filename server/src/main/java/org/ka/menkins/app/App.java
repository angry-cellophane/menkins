package org.ka.menkins.app;

import com.hazelcast.config.Config;
import lombok.AllArgsConstructor;
import org.apache.mesos.MesosNativeLibrary;
import org.ka.menkins.mesos.Schedulers;
import org.ka.menkins.storage.Storage;

import java.util.concurrent.Executors;

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
                                .mesosMasterUrl("172.28.128.16:5050")
                                .checkpoint(true)
                                .build()
                )
                .port(5678)
                .url("http://localhost:5678")
                .hazelcast(new Config())
                .build();

        new App(config).run();
    }

    void run() {
        validate();
        Metrics.Requests.total.get();

        var storage = Storage.newLocalStorageManager();
        var createRequests = storage.createNodeRequests();
        var aggregatedCreateRequests = storage.aggregatedCreateNodeRequests();
        var terminateTaskRequests = storage.terminateTaskRequestsTopic();

        HttpServer.newInitializer(config, createRequests, terminateTaskRequests).run();

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
        Schedulers.newInitializer(config, aggregatedCreateRequests, aggregator, terminateTaskRequests).run();
    }

    private void validate() {
        MesosNativeLibrary.load(config.getMesos().getPathToMesosLib());
    }

}
