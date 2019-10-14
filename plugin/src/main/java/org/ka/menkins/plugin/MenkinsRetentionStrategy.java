package org.ka.menkins.plugin;

import hudson.slaves.RetentionStrategy;

import javax.annotation.Nonnull;

public class MenkinsRetentionStrategy extends RetentionStrategy<MenkinsComputer> {
    @Override
    public long check(@Nonnull MenkinsComputer c) {
        return 5;
    }
}
