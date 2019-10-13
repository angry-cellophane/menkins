package org.ka.menkins.queue;

import lombok.Value;

@Value
public class NodeRequest {
    String id;
    String nodeName;
    String labels;
    String jenkinsUrl;
    String jnlpUrl;
    String jnlpJarUrl;
    String jnlpSecret;
    String jnlpArgs;

    public void validate() {
        if (nodeName == null) {
            throw new RuntimeException("node name is null in request from " + jnlpUrl);
        }

        if (labels == null) {
            throw new RuntimeException("labels is null in request from " + jnlpUrl);
        }

        if (jnlpSecret == null) {
            throw new RuntimeException("jnlp secret is null in request from " + jnlpUrl);
        }

        if (jnlpUrl == null) {
            throw new RuntimeException("jnlp url is null in builder node request");
        }
    }

    public NodeRequestWithResources toWithResources() {
        var docker = ResourcesByLabelsLookup.lookup(this.labels);
        return new NodeRequestWithResources(this, docker);
    }
}
