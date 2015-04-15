package org.jenkinsci.plugins.docker.commons.impl;

import hudson.EnvVars;
import org.jenkinsci.plugins.docker.commons.KeyMaterial;
import org.jenkinsci.plugins.docker.commons.KeyMaterialFactory;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.IOException;
import java.io.Serializable;

/**
 * Composes two {@link org.jenkinsci.plugins.docker.commons.KeyMaterialFactory}s into one.
 *
 * @author Kohsuke Kawaguchi
 */
@Restricted(NoExternalUse.class)
public class CompositeKeyMaterialFactory extends KeyMaterialFactory {
    private final KeyMaterialFactory lhs;
    private final KeyMaterialFactory rhs;

    public CompositeKeyMaterialFactory(KeyMaterialFactory lhs, KeyMaterialFactory rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public KeyMaterial materialize() throws IOException, InterruptedException {
        KeyMaterial lhsMaterialized = lhs.materialize();
        KeyMaterial rhsMaterialized = null;
        
        try {
            rhsMaterialized = rhs.materialize();
            EnvVars env = lhsMaterialized.env();
            env.putAll(rhsMaterialized.env());
    
            return new CompositeKeyMaterial(env, lhsMaterialized, rhsMaterialized);
        } catch (Exception e) {
            try {
                lhsMaterialized.close();

                // TODO Java 7+ use chained exceptions
                if (e instanceof IOException) {
                    throw (IOException) e;
                } else if (e instanceof InterruptedException) {
                    throw (InterruptedException) e;
                } else {
                    throw new IOException("Error materializing credentials.", e);
                }
            } finally {
                if (rhsMaterialized != null) {
                    rhsMaterialized.close();
                }
            }
        }
    }
    
    private static final class CompositeKeyMaterial extends KeyMaterial implements Serializable {
        
        private static final long serialVersionUID = 1L;

        private final KeyMaterial lhsMaterialized;
        private final KeyMaterial rhsMaterialized;

        protected CompositeKeyMaterial(EnvVars envVars, KeyMaterial lhsMaterialized, KeyMaterial rhsMaterialized) {
            super(envVars);
            this.lhsMaterialized = lhsMaterialized;
            this.rhsMaterialized = rhsMaterialized;
        }

        @Override
        public void close() throws IOException {
            try {
                lhsMaterialized.close();
            } finally {
                rhsMaterialized.close();
            }
        }
    }
}
