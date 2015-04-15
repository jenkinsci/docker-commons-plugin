package org.jenkinsci.plugins.docker.commons.impl;

import hudson.EnvVars;
import org.jenkinsci.plugins.docker.commons.KeyMaterial;
import org.jenkinsci.plugins.docker.commons.KeyMaterialFactory;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.IOException;

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
        final KeyMaterial lhsMaterialized = lhs.materialize();
        final KeyMaterial rhsMaterialized;
        
        try {
            rhsMaterialized = rhs.materialize();
        } catch (Exception e) {
            lhsMaterialized.close();
            if (e instanceof IOException) {
                throw (IOException) e;
            } else if (e instanceof InterruptedException) {
                throw (InterruptedException) e;
            } else {
                throw new IOException("Error materializing credentials.", e);
            }
        }

        EnvVars env = lhsMaterialized.env();
        env.putAll(rhsMaterialized.env());

        return new KeyMaterial(env) {
            @Override
            public void close() throws IOException {
                try {
                    lhsMaterialized.close();
                } finally {
                    rhsMaterialized.close();
                }
            }
        };
    }
}
