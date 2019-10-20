package org.ka.menkins.app

import com.hazelcast.client.config.ClientConfig
import com.hazelcast.config.Config
import com.hazelcast.core.ITopic
import org.ka.menkins.app.init.AppConfig
import org.ka.menkins.mesos.MesosHelpers
import org.ka.menkins.storage.NodeRequest
import org.ka.menkins.storage.NodeRequestWithResources
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class HttpServerTest extends Specification implements MesosHelpers {

    static final BlockingQueue<NodeRequestWithResources> queue = new ArrayBlockingQueue<>(1)

    void setupSpec() {
        AppConfig config = AppConfig.builder()
                .mesos(getMESOS_CONFIG())
                .http(AppConfig.Http.builder().port(5678).build())
                .hazelcast(new ClientConfig())
                .storageType(AppConfig.StorageType.LOCAL)
                .build()

        HttpServer.newInitializer(config, queue, Mock(ITopic)).run()
    }

    void cleanupSpec() {
        HttpServer.finalizeHttp().run()
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
                .properties(Collections.emptyMap())
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

    void 'returns 500 when request was not added in global queue'() {
        given:
        queue.add(request {})
        def request = NodeRequest.builder()
                .id("id#1")
                .jenkinsUrl("url")
                .jnlpSecret("secret")
                .jnlpArgs("args")
                .slaveJarUrl("slave-url")
                .nodeName("node")
                .labels("labels")
                .properties(Collections.emptyMap())
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
        queue.poll()

        then:
        response.statusCode() == 500
        queue.isEmpty() == true
    }
}
