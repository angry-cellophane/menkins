package org.ka.menkins.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.util.Map;

@Value
@With
@AllArgsConstructor
@Builder
public class NodeRequest {
    String id;
    String nodeName;
    String labels;
    String jenkinsUrl;
    String jnlpSecret;
    String jnlpArgs;
    String slaveJarUrl;
    String jnlpUrl;
    Map<String, String> properties;

    public void validate() {
        if (nodeName == null) {
            throw new RuntimeException("node name is null in request from " + jenkinsUrl);
        }

        if (labels == null) {
            throw new RuntimeException("labels is null in request from " + jenkinsUrl);
        }

        if (jnlpSecret == null) {
            throw new RuntimeException("jnlp secret is null in request from " + jenkinsUrl);
        }

        if (slaveJarUrl == null) {
            throw new RuntimeException("slaveJarUrl url is null in request from " + jenkinsUrl);
        }

        if (jenkinsUrl == null) {
            throw new RuntimeException("slaveJarUrl url is null in request " + nodeName);
        }
    }
}
