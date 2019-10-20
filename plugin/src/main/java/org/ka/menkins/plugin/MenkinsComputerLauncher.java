package org.ka.menkins.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.TaskListener;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.SlaveComputer;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.ka.menkins.storage.NodeRequest;

import java.util.Collections;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MenkinsComputerLauncher extends JNLPLauncher {

    private static final Logger LOGGER = Logger.getLogger(MenkinsComputerLauncher.class.getName());

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String URL = "http://localhost:5678/api/v1/node";
    private static final CloseableHttpClient HTTP = HttpClientBuilder.create().build();

    private final String uuid;
    private final String name;
    private final String labels;
    private final String menkinsUrl;
    private final long nodeTimeoutInNs;

    public MenkinsComputerLauncher(String name, String labels, String menkinsUrl, long nodeTimeoutInNs) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.labels = labels;
        this.menkinsUrl = menkinsUrl;
        this.nodeTimeoutInNs = nodeTimeoutInNs;
    }

    public String getUuid() {
        return this.uuid;
    }

    @Override
    public void launch(SlaveComputer computer, TaskListener listener) {
        LOGGER.info("Launching menkins node " + this.name);

        MenkinsComputer menkinsComputer = (MenkinsComputer) computer;
        MenkinsSlave node = menkinsComputer.getNode();

        NodeRequest request = NodeRequest.builder()
                .id(this.uuid)
                .jenkinsUrl("http://172.28.128.1:8080/")
                .labels(this.labels)
                .nodeName(this.name)
                .jnlpArgs("-noReconnect")
                .jnlpSecret("")
                .jnlpUrl("http://172.28.128.1:8080/computer/" + this.name + "/slave-agent.jnlp")
                .slaveJarUrl("http://172.28.128.1:8080/jnlpJars/slave.jar")
                .properties(Collections.emptyMap())
                .build();

        LOGGER.info("Request to menkins " + request);

        try {
            byte[] data = MAPPER.writeValueAsBytes(request);

            HttpPost post = new HttpPost(createNodeUrl());
            post.addHeader("Content-Type", "application/json");
            post.setEntity(new ByteArrayEntity(data));

            try (CloseableHttpResponse response = HTTP.execute(post)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    LOGGER.warning("Launching of " + this.name + " failed. Server returned " + response.getStatusLine().getStatusCode() + " " + IOUtils.toString(response.getEntity().getContent()));
                    if (node != null) node.setPendingDelete(true);
                    return;
                } else {
                    LOGGER.info("Successfully submitted request to spin up " + this.name);
                }
            }

            long start = System.nanoTime();
            while ((System.nanoTime() - start) < nodeTimeoutInNs && computer.isOffline() && computer.isConnecting()) {
                try {
                    LOGGER.info("Waiting for slave computer connection " + name);
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) { return; }
            }
            if (computer.isOnline()) {
                LOGGER.info("menkins node connected " + name);
            } else {
                LOGGER.warning("menkins node not connected " + name);
                if (node != null) node.setPendingDelete(true);
            }
        } catch (Exception e) {
            LOGGER.warning("error when trying to launch " + name);
        }
    }

    public void terminate() {
        LOGGER.info("Sending request to terminate task " + this.uuid + ", node " + this.name);

        HttpDelete request = new HttpDelete(terminateNodeUrl());
        try {
            try (CloseableHttpResponse response = HTTP.execute(request)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    LOGGER.warning("Terminating node " + this.name + " failed. Server returned " + response.getStatusLine().getStatusCode() + " " + IOUtils.toString(response.getEntity().getContent()));
                } else {
                    LOGGER.info("Successfully terminated node " + this.name);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while trying to delete task " + this.uuid + ", node " + this.name, e);
        }
    }

    private String createNodeUrl() {
        return this.menkinsUrl;
    }

    private String terminateNodeUrl() {
        return this.menkinsUrl + "/" + this.uuid;
    }
}
