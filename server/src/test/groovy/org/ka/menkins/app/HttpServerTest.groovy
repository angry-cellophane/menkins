package org.ka.menkins.app

import com.hazelcast.config.Config
import org.ka.menkins.app.init.AppConfig
import org.ka.menkins.mesos.MesosHelpers
import org.ka.menkins.storage.NodeRequest
import org.ka.menkins.storage.NodeRequestWithResources
import spark.Spark
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class HttpServerTest extends Specification implements MesosHelpers {

    static final BlockingQueue<NodeRequestWithResources> queue = new LinkedBlockingQueue<NodeRequestWithResources>()

    void setupSpec() {
        AppConfig config = AppConfig.builder()
                .mesos(getMESOS_CONFIG())
                .port(5678)
                .url("http://localhost:5678")
                .hazelcast(new Config())
                .build()

        HttpServer.newInitializer(config, queue).run()
    }

    void cleanupSpec() {
        Spark.awaitStop()
    }

    void 'parse incoming json'() {
        given:
        def request = NodeRequest.builder()
                .id("id#1")
                .jenkinsUrl("url")
                .jnlpSecret("secret")
                .jnlpArgs("args")
                .slaveJarUrl("slave-url")
                .nodeName("node")
                .labels("labels")
                .build()

        when:
        def http = HttpClient.newHttpClient()
        def response = http.send(
                HttpRequest.newBuilder().uri("http://localhost:5678/api/v1/node".toURI())
                        .POST(HttpRequest.BodyPublishers.ofByteArray(Json.toBytes(request)))
                        .header("Content-Type", "application/json")
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        )

        then:
        response.statusCode() == 200
        queue.poll()?.getRequest()?.id == 'id#1'
    }
}
