package org.ka.menkins.app;

import lombok.AllArgsConstructor;
import org.apache.mesos.MesosNativeLibrary;
import org.ka.menkins.aggregator.Aggregators;
import org.ka.menkins.app.init.AppConfig;
import org.ka.menkins.app.init.LoadConfig;
import org.ka.menkins.mesos.MesosSchedulers;
import org.ka.menkins.storage.Storage;

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
        Aggregators.run(storage);
        MesosSchedulers.newInitializer(config, aggregatedCreateRequests, terminateTaskRequests).run();
    }

    private void validate() {
        MesosNativeLibrary.load(config.getMesos().getPathToMesosLib());
    }

}
