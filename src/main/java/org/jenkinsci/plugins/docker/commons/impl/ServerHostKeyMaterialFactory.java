package org.jenkinsci.plugins.docker.commons.impl;

import hudson.EnvVars;
import org.jenkinsci.plugins.docker.commons.KeyMaterial;
import org.jenkinsci.plugins.docker.commons.KeyMaterialFactory;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * A {@link KeyMaterial} that maintains information about the host.
 * 
 * @author Stephen Connolly
 */
@Restricted(NoExternalUse.class)
public class ServerHostKeyMaterialFactory extends KeyMaterialFactory{
    /**
     * Standardize serialization
     */
    private static final long serialVersionUID = 1L;

    /**
     * The host.
     */
    @Nonnull
    private final String host;

    public ServerHostKeyMaterialFactory(@Nonnull String host) {
        this.host = host;
    }

    /** {@inheritDoc} */
    @Override
    public KeyMaterial materialize() throws IOException, InterruptedException {
        EnvVars env = new EnvVars();
        env.put("DOCKER_HOST", host);
        return new KeyMaterialImpl(env);
    }

    /**
     * Our implementation.
     */
    private static class KeyMaterialImpl extends KeyMaterial {
        /**
         * Standardize serialization
         */
        private static final long serialVersionUID = 1L;

        /** {@inheritDoc} */
        private KeyMaterialImpl(EnvVars envVars) {
            super(envVars);
        }

        /** {@inheritDoc} */
        @Override
        public void close() throws IOException {
            
        }
    }
}
