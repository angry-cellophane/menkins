package org.ka.menkins.plugin;

import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner.PlannedNode;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MenkinsCloud extends Cloud {

    private static final Logger LOGGER = Logger.getLogger(MenkinsCloud.class.getName());
    private static final int MAX_HOSTNAME_LENGTH = 63;

    private static final long NODE_TIMEOUT_NS = TimeUnit.MINUTES.toNanos(10);

    private String menkinsUrl;
    private long nodeTimeoutSec;

    @DataBoundConstructor
    public MenkinsCloud() {
        super("MenkinsCloud");
    }

    @Initializer(after= InitMilestone.JOB_LOADED)
    public static void init() {
        Jenkins.getInstance().getNodes().stream()
                .filter(n -> n instanceof MenkinsSlave)
                .map(n -> (MenkinsSlave) n)
                .forEach(n -> n.terminate() );
    }

    @Override
    public Collection<PlannedNode> provision(Label label, int excessWorkload) {
        LOGGER.info(String.format("Received request to provision %d executors for label %s", excessWorkload, label));
        List<PlannedNode> nodes = new ArrayList<>();
        try {
            while (excessWorkload > 0) {
                excessWorkload--;
                String labels = label.getExpression().replaceAll("&", "");
                String nodeName = buildNodeName(labels);
                MenkinsSlave slave = new MenkinsSlave(nodeName, labels, this.menkinsUrl, TimeUnit.SECONDS.toNanos(this.nodeTimeoutSec));
                nodes.add(new PlannedNode(this.getDisplayName(), Computer.threadPoolForRemoting
                        .submit(() -> {
                            // We do not need to explicitly add the Node here because that is handled by
                            // hudson.slaves.NodeProvisioner::update() that checks the result from the
                            // Future and adds the node. Though there is duplicate node addition check
                            // because of this early addition there is difference in job scheduling and
                            // best to avoid it.
                            LOGGER.info(String.format("Slave %s pulled by thread.", slave.getUuid()));
                            return slave;
                        }), 1));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create instances on Mesos", e);
        }
        return nodes;
    }

    @Override
    public boolean canProvision(Label label) {
        return true;
    }

    private static String buildNodeName(String label) {
        String suffix;
        if (label == null) {
            suffix = StringUtils.EMPTY;
        } else {
            suffix = StringUtils.remove("-" + label, " ").replace("&", "");
        }
        return StringUtils.left("menkins-jenkins-" + StringUtils.remove(UUID.randomUUID().toString(), '-') + suffix, MAX_HOSTNAME_LENGTH);
    }

    public void setMenkinsUrl(String menkinsUrl) {
        this.menkinsUrl = menkinsUrl;
    }

    public void setNodeTimeoutSec(long nodeTimeoutSec) {
        this.nodeTimeoutSec = nodeTimeoutSec;
    }

    public String getMenkinsUrl() {
        return menkinsUrl;
    }

    public long getNodeTimeoutSec() {
        return nodeTimeoutSec;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Cloud> {

        public DescriptorImpl() {
            super(MenkinsCloud.class);
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Menkins Cloud";
        }
    }
}
