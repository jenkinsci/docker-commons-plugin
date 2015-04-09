package org.jenkinsci.plugins.docker.commons.impl;

import hudson.EnvVars;
import org.jenkinsci.plugins.docker.commons.KeyMaterial;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.IOException;

/**
 * Composes two {@link KeyMaterial}s into one.
 *
 * @author Kohsuke Kawaguchi
 */
@Restricted(NoExternalUse.class)
public class CompositeKeyMaterial extends KeyMaterial {
    private final KeyMaterial lhs;
    private final KeyMaterial rhs;

    public CompositeKeyMaterial(KeyMaterial lhs, KeyMaterial rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public EnvVars env() {
        EnvVars env = lhs.env();
        env.putAll(rhs.env());
        return env;
    }

    @Override
    public void close() throws IOException {
        lhs.close();
        rhs.close();
    }
}
