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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import org.jenkinsci.plugins.docker.commons.credentials.KeyMaterial;
import org.jenkinsci.plugins.docker.commons.credentials.KeyMaterialFactory;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.IOException;

/**
 * A {@link KeyMaterial} that maintains information about the host.
 * 
 * @author Stephen Connolly
 */
@Restricted(NoExternalUse.class)
public class ServerHostKeyMaterialFactory extends KeyMaterialFactory{

    /**
     * The host.
     */
    @NonNull
    private final String host;

    public ServerHostKeyMaterialFactory(@NonNull String host) {
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
