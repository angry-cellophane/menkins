package org.ka.menkins.app;

import org.ka.menkins.storage.NodeRequest;
import org.ka.menkins.storage.NodeRequestWithResources;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

public class SubmitTestRequests {
    public static void doSubmit(BlockingQueue<NodeRequestWithResources> queue) {
        List.of(node("agent1"), node("agent2")).forEach(node -> queue.add(node));
    }

    static NodeRequestWithResources node(String name) {
        var id = UUID.randomUUID().toString();
        var nodeName = ("menkins-task-" + name + "-" + id);
        return NodeRequestWithResources.from(NodeRequest.builder()
                .id(id)
                .jenkinsUrl("http://172.28.128.1:8080/")
                .labels("agent")
                .nodeName(nodeName.substring(0, Math.min(nodeName.length(), 64)))
                .jnlpArgs("-noReconnect")
                .jnlpSecret("")
                .jnlpUrl("http://172.28.128.1:8080/computer/agent1/slave-agent.jnlp")
                .slaveJarUrl("http://172.28.128.1:8080/jnlpJars/slave.jar")
                .build()
        );
    }
}
