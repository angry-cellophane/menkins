package org.ka.menkins.queue;

import lombok.Value;

@Value
public class BuilderNodeRequest {
    String nodeName;
    String labels;
    String jenkinsUrl;
    String jnlpUrl;
    String jnlpSecret;

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
}
