package org.ka.menkins.app;

import lombok.AllArgsConstructor;
import org.apache.mesos.MesosNativeLibrary;
import org.ka.menkins.app.init.AppConfig;
import org.ka.menkins.app.init.LoadConfig;
import org.ka.menkins.mesos.MesosSchedulers;
import org.ka.menkins.storage.Storage;

import java.util.concurrent.Executors;

@AllArgsConstructor
public class App {

    AppConfig config;

    public static void main(String[] args) {
        var config = LoadConfig.fromJvm();
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
        MesosSchedulers.newInitializer(config, aggregatedCreateRequests, aggregator, terminateTaskRequests).run();
    }

    private void validate() {
        MesosNativeLibrary.load(config.getMesos().getPathToMesosLib());
    }

}
