package org.ka.menkins.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import org.ka.menkins.queue.NodeRequest;

import java.io.IOException;
import java.util.UUID;

public class MenkinsComputerLauncher extends ComputerLauncher {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String URL = "http://localhost:5678/api/v1/node";

    private final String uuid;

    public MenkinsComputerLauncher(String name, String labels) {
        this.uuid = UUID.randomUUID().toString();
        NodeRequest.builder()
                .id(this.uuid)
                .build();
    }

    public String getUuid() {
        return this.uuid;
    }

    @Override
    public void launch(SlaveComputer computer, TaskListener listener) throws IOException, InterruptedException {
        super.launch(computer, listener);
    }

    public void terminate() {

    }
}
