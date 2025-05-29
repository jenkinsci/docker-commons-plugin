/*
 * The MIT License
 *
 * Copyright (c) 2015, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.docker.commons.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.plugins.docker.commons.credentials.KeyMaterial;
import org.jenkinsci.plugins.docker.commons.credentials.KeyMaterial2;
import org.jenkinsci.plugins.docker.commons.credentials.KeyMaterialContext;
import org.jenkinsci.plugins.docker.commons.credentials.KeyMaterialFactory;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.Serializable;

/**
 * Composes multiple {@link org.jenkinsci.plugins.docker.commons.credentials.KeyMaterialFactory}s into one.
 *
 * @author Kohsuke Kawaguchi
 */
@Restricted(NoExternalUse.class)
public class CompositeKeyMaterialFactory extends KeyMaterialFactory {
    private final KeyMaterialFactory[] factories;

    public CompositeKeyMaterialFactory(KeyMaterialFactory... factories) {
        this.factories = factories == null || factories.length == 0
                ? new KeyMaterialFactory[]{new NullKeyMaterialFactory()}
                : factories.clone();
    }

    @Override
    public synchronized KeyMaterialFactory contextualize(@NonNull KeyMaterialContext context) {
        KeyMaterialFactory contextualized = super.contextualize(context);
        assert contextualized == this;
        for (KeyMaterialFactory factory : factories) {
            factory.contextualize(context);
        }
        return this;
    }

    @Override
    public KeyMaterial2 materialize2() throws IOException, InterruptedException {

        KeyMaterial2[] keyMaterials = new KeyMaterial2[factories.length];
        EnvVars env = new EnvVars();
        try {
            for (int index = 0; index < factories.length; index++) {
                keyMaterials[index] = factories[index].materialize2();
                env.putAll(keyMaterials[index].env());
            }
            return new CompositeKeyMaterial2(env, keyMaterials);
        } catch (Throwable e) {
            for (int index = keyMaterials.length - 1; index >= 0; index--) {
                try {
                    if (keyMaterials[index] != null) {
                        keyMaterials[index].close(getChannel());
                    }
                } catch (Throwable t) {
                    e.addSuppressed(t);
                }
            }
            if (e instanceof IOException ioe) {
                throw ioe;
            } else if (e instanceof InterruptedException ie) {
                throw ie;
            } else if (e instanceof RuntimeException re) {
                throw re;
            } else {
                throw new IOException("Error materializing credentials.", e);
            }
        }
    }

    private static final class CompositeKeyMaterial2 extends KeyMaterial2 implements Serializable {

        private static final long serialVersionUID = 1L;

        private final KeyMaterial2[] keyMaterials;

        CompositeKeyMaterial2(EnvVars envVars, KeyMaterial2... keyMaterials) {
            super(envVars);
            this.keyMaterials = keyMaterials;
        }

        @Override
        public void close(VirtualChannel channel) throws IOException, InterruptedException {
            Throwable first = null;
            for (int index = keyMaterials.length - 1; index >= 0; index--) {
                try {
                    if (keyMaterials[index] != null) {
                        keyMaterials[index].close(channel);
                    }
                } catch (Throwable e) {
                    if (first == null) {
                        first = e;
                    } else {
                        first.addSuppressed(e);
                    }
                }
            }
            if (first != null) {
                if (first instanceof IOException ioe) {
                    throw ioe;
                } else if (first instanceof InterruptedException ie) {
                    throw ie;
                } else if (first instanceof RuntimeException re) {
                    throw re;
                } else {
                    throw new IOException("Error closing credentials.", first);
                }
            }
        }
    }

    @SuppressFBWarnings(value = {"NP_UNWRITTEN_FIELD", "UWF_NULL_FIELD"})
    @Deprecated
    private static final class CompositeKeyMaterial extends KeyMaterial implements Serializable {

        private static final long serialVersionUID = 1L;

        private final KeyMaterial[] keyMaterials = null;

        private CompositeKeyMaterial() {
            super(null);
            assert false : "only deserialized";
        }

        @Override
        public void close() throws IOException {
            Throwable first = null;
            for (int index = keyMaterials.length - 1; index >= 0; index--) {
                try {
                    if (keyMaterials[index] != null) {
                        keyMaterials[index].close();
                    }
                } catch (Throwable e) {
                    first = first == null ? e : first;
                }
            }
            if (first != null) {
                if (first instanceof IOException) {
                    throw (IOException) first;
                } else if (first instanceof RuntimeException) {
                    throw (RuntimeException) first;
                } else {
                    throw new IOException("Error closing credentials.", first);
                }
            }
        }
    }
}
