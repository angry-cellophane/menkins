package org.ka.menkins.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.TaskListener;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;
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

import static jenkins.slaves.JnlpSlaveAgentProtocol.*;

public class MenkinsComputerLauncher extends JNLPLauncher {

    private static final Logger LOGGER = Logger.getLogger(MenkinsComputerLauncher.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final CloseableHttpClient HTTP = HttpClientBuilder.create().build();

    private static final String JNLP_SECRET_FORMAT = "-secret %s";

    private final String uuid;
    private final String name;
    private final String labels;
    private final String menkinsUrl;
    private final String jenkinsUrl;
    private final long nodeTimeoutInNs;

    public MenkinsComputerLauncher(String name, String labels, String menkinsUrl, String jenkinsUrl, long nodeTimeoutInNs) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.labels = labels;
        this.menkinsUrl = menkinsUrl;
        this.jenkinsUrl = jenkinsUrl;
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
                .jenkinsUrl(jenkinsUrl)
                .labels(this.labels)
                .nodeName(this.name)
                .jnlpArgs("-noReconnect")
                .jnlpSecret(getJnlpSecret(this.name))
                .jnlpUrl(jenkinsUrl + "/computer/" + this.name + "/slave-agent.jnlp")
                .slaveJarUrl(jenkinsUrl + "/jnlpJars/slave.jar")
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

    private String getJnlpSecret(String slaveName) {
        String jnlpSecret = "";
        if(Jenkins.getInstance().isUseSecurity()) {
            jnlpSecret = String.format(JNLP_SECRET_FORMAT, SLAVE_SECRET.mac(slaveName));
        }
        return jnlpSecret;
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

    private String apiUrl() {
        return this.menkinsUrl + "/api/v1";
    }

    private String createNodeUrl() {
        return apiUrl() + "/node";
    }

    private String terminateNodeUrl() {
        return apiUrl() + "/node/" + this.uuid;
    }
}
