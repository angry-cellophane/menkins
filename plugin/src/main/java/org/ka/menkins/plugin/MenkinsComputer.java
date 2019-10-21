package org.ka.menkins.plugin;

import hudson.model.Slave;
import hudson.remoting.VirtualChannel;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;
import jenkins.slaves.EncryptedSlaveAgentJnlpFile;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebMethod;

import javax.annotation.CheckForNull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Logger;

public class MenkinsComputer extends SlaveComputer {

    private static final Logger LOGGER = Logger.getLogger(MenkinsComputer.class.getName());

    public MenkinsComputer(Slave slave) {
        super(slave);
    }

    @CheckForNull
    @Override
    public MenkinsSlave getNode() {
        return (MenkinsSlave) super.getNode();
    }

    public void deleteSlave() throws IOException {
        LOGGER.info("Terminating " + getName() + " slave");
        MenkinsSlave slave = getNode();

        // Slave already deleted
        if (slave == null) return;

        VirtualChannel channel = slave.getChannel();
        if (channel != null) {
            channel.close();
        }
        slave.terminate();
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            throw new IllegalStateException("jenkins.instance == null. Cannot remove slave " + this.getName());
        }
        jenkins.removeNode(this.getNode());
    }

    @Override
    @WebMethod(name="slave-agent.jnlp")
    public HttpResponse doSlaveAgentJnlp(StaplerRequest req, StaplerResponse res) throws IOException, ServletException {
        return new EncryptedSlaveAgentJnlpFile(this, "menkins-slave-agent.jnlp.jelly", getName(), CONNECT);
    }
}
