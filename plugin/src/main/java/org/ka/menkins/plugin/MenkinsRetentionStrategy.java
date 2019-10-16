package org.ka.menkins.plugin;

import hudson.model.Descriptor;
import hudson.slaves.RetentionStrategy;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;

import static hudson.util.TimeUnit2.MINUTES;

public class MenkinsRetentionStrategy extends RetentionStrategy<MenkinsComputer> {

    private static final Logger LOGGER = Logger.getLogger(MenkinsRetentionStrategy.class.getName());

    private final long idleMinutes;

    public MenkinsRetentionStrategy(long idleMinutes) {
        this.idleMinutes = idleMinutes;
    }

    @Override
    public long check(@Nonnull MenkinsComputer c) {
        MenkinsSlave computerNode = c.getNode();
        if (c.isIdle() && computerNode != null) {
            final long idleMilliseconds = System.currentTimeMillis() - c.getIdleStartMilliseconds();
            if (idleMilliseconds > MINUTES.toMillis(idleMinutes)) {
                LOGGER.log(Level.INFO, "Disconnecting {0}", c.getName());
                computerNode.terminate();
            }
        }

        return 1;
    }

    @Override
    public void start(@Nonnull MenkinsComputer c) {
        c.connect(false);
    }

    public static class DescriptorImpl extends Descriptor<RetentionStrategy<?>> {
        @Override
        public String getDisplayName() {
            return "MENKINS";
        }
    }
}
