package org.ka.menkins.app;

import lombok.AllArgsConstructor;
import org.apache.mesos.MesosNativeLibrary;
import org.ka.menkins.aggregator.Aggregators;
import org.ka.menkins.app.init.AppConfig;
import org.ka.menkins.app.init.LoadConfig;
import org.ka.menkins.mesos.DriverState;
import org.ka.menkins.mesos.MesosSchedulers;
import org.ka.menkins.storage.Storage;

import java.util.concurrent.atomic.AtomicReference;

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

        var stateRef = new AtomicReference<>(DriverState.newState());
        var storage = Storage.newLocalStorageManager();

        var aggregator = Aggregators.newAggregator(storage);

        var finalizeHttp = HttpServer.finalizeHttp();
        var finalizeScheduler = MesosSchedulers.newFinalizer(stateRef);
        var finalizeStorage = storage.onShutDown();
        var finalizeAggregator = aggregator.finalizer();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            finalizeAggregator.run();
            finalizeScheduler.run();
            finalizeHttp.run();
            finalizeStorage.run();
        }));

        var createRequests = storage.createNodeRequests();
        var aggregatedCreateRequests = storage.aggregatedCreateNodeRequests();
        var terminateTaskRequests = storage.terminateTaskRequestsTopic();

        aggregator.runner().run();
        HttpServer.newInitializer(config, createRequests, terminateTaskRequests).run();
        MesosSchedulers.newInitializer(config, stateRef, aggregatedCreateRequests, terminateTaskRequests).run();
    }

    private void validate() {
        MesosNativeLibrary.load(config.getMesos().getPathToMesosLib());
    }

}
