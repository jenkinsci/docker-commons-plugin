package org.jenkinsci.plugins.docker.commons.impl;

import hudson.EnvVars;
import org.jenkinsci.plugins.docker.commons.KeyMaterial;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * {@link KeyMaterial} that does nothing.
 *
 * @see KeyMaterial#NULL
 * @author Kohsuke Kawaguchi
 */
@Restricted(NoExternalUse.class)
public final class NullKeyMaterial extends KeyMaterial {

    private Object readResolve() {
        return NULL;
    }

    @Override
    public EnvVars env() {
        return new EnvVars();
    }

    @Override
    public void close() {
        // noop
    }

    private static final long serialVersionUID = 1L;
}
