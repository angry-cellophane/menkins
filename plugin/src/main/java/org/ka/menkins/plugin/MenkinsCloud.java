package org.ka.menkins.plugin;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Label;
import hudson.security.ACL;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner.PlannedNode;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.annotation.Nonnull;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MenkinsCloud extends Cloud {

    private static final Logger LOGGER = Logger.getLogger(MenkinsCloud.class.getName());
    private static final int MAX_HOSTNAME_LENGTH = 63;

    private static final long NODE_TIMEOUT_SEC = TimeUnit.MINUTES.toSeconds(10);

    private String menkinsUrl;
    private long nodeTimeoutSec;
    private String jenkinsUrl;
    private String credentialsId;

    @DataBoundConstructor
    public MenkinsCloud(
            String menkinsUrl,
            long nodeTimeoutSec,
            String jenkinsUrl,
            String credentialsId) {
        super("MenkinsCloud");

        this.menkinsUrl = menkinsUrl;
        this.nodeTimeoutSec = nodeTimeoutSec != 0 ? nodeTimeoutSec : NODE_TIMEOUT_SEC;
        this.jenkinsUrl = jenkinsUrl != null ? jenkinsUrl : Jenkins.getInstance().getRootUrl();
        this.credentialsId = credentialsId;
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
                MenkinsSlave slave = new MenkinsSlave(nodeName, labels, this.menkinsUrl, this.jenkinsUrl, TimeUnit.SECONDS.toNanos(this.nodeTimeoutSec));
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
        this.menkinsUrl = menkinsUrl.endsWith("/") ? menkinsUrl.substring(0, menkinsUrl.length() - 1) : menkinsUrl;
    }

    public void setNodeTimeoutSec(long nodeTimeoutSec) {
        this.nodeTimeoutSec = nodeTimeoutSec;
    }

    public void setJenkinsUrl(String jenkinsUrl) {
        this.jenkinsUrl = jenkinsUrl.endsWith("/") ? jenkinsUrl.substring(0, jenkinsUrl.length() - 1) : jenkinsUrl;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getMenkinsUrl() {
        return menkinsUrl;
    }

    public long getNodeTimeoutSec() {
        return nodeTimeoutSec;
    }

    public String getJenkinsUrl() {
        return jenkinsUrl;
    }

    public StandardUsernamePasswordCredentials getCredentials() {
        if (credentialsId == null) {
            return null;
        } else {
            List<DomainRequirement> domainRequirements = Collections.emptyList();
            Jenkins jenkins = Jenkins.getInstance();
            return CredentialsMatchers.firstOrNull(CredentialsProvider
                            .lookupCredentials(StandardUsernamePasswordCredentials.class, jenkins, ACL.SYSTEM, domainRequirements),
                    CredentialsMatchers.withId(credentialsId)
            );
        }
    }

    public String getCredentialsId() {
        return credentialsId;
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

        @Restricted(DoNotUse.class)
        @SuppressWarnings("unused")
        @RequirePOST
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item) {
            Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
            List<DomainRequirement> domainRequirements = Collections.emptyList();
            return new StandardListBoxModel().withEmptySelection().withMatching(
                    CredentialsMatchers.instanceOf(UsernamePasswordCredentials.class),
                    CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class, item, null, domainRequirements)
            );
        }

        public FormValidation doCheckJenkinsUrl(@QueryParameter String value) {
            return checkUrl(value);
        }

        public FormValidation doCheckMenkinsUrl(@QueryParameter String value) {
            return checkUrl(value);
        }

        private FormValidation checkUrl(@QueryParameter String value) {
            Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);

            if (value == null || StringUtils.isBlank(value)) {
                return FormValidation.error("value cannot be blank");
            }

            try {
                new URL(value);
            } catch (MalformedURLException e) {
                return FormValidation.error(e.getMessage());
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckNodeTimeoutSec(@QueryParameter String value) {
            Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);

            try {
                Long longValue = Long.valueOf(value);
                if (longValue == 0) return FormValidation.error("value cannot be 0");
            } catch (NumberFormatException e) {
                return FormValidation.error(value + " is not an integer number");
            }

            return FormValidation.ok();
        }
    }
}
