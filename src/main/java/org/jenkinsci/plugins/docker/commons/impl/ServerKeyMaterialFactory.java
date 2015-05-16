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

import hudson.EnvVars;
import hudson.FilePath;
import org.jenkinsci.plugins.docker.commons.DockerServerCredentials;
import org.jenkinsci.plugins.docker.commons.KeyMaterial;
import org.jenkinsci.plugins.docker.commons.KeyMaterialFactory;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.UUID;

/**
 * {@link org.jenkinsci.plugins.docker.commons.KeyMaterialFactory} for talking to docker daemon.
 *
 * <p>
 * Key/certificates have to be laid out in a specific file names in this directory
 * to make docker(1) happy.
 *
 * @author Kohsuke Kawaguchi
 */
@Restricted(NoExternalUse.class)
public class ServerKeyMaterialFactory extends KeyMaterialFactory {
    
    @CheckForNull 
    private final String key;
    @CheckForNull 
    private final String cert;
    @CheckForNull 
    private final String ca;

    public ServerKeyMaterialFactory(@CheckForNull final DockerServerCredentials credentials) {
        key = credentials.getClientKey();
        cert = credentials.getClientCertificate();
        ca = credentials.getServerCaCertificate();
    }

    public ServerKeyMaterialFactory(@CheckForNull String key, @CheckForNull String cert, @CheckForNull String ca) {
        this.key = key;
        this.cert = cert;
        this.ca = ca;
    }

    @Override
    public KeyMaterial materialize() throws IOException, InterruptedException {
        
        EnvVars e = new EnvVars();

        if (key != null && cert != null && ca != null) {
            final FilePath tempCredsDir = new FilePath(getContext().getBaseDir(), UUID.randomUUID().toString());

            // protect this information from prying eyes
            tempCredsDir.chmod(0600);

            // these file names are defined by convention by docker
            copyInto(tempCredsDir, "key.pem", key);
            copyInto(tempCredsDir,"cert.pem", cert);
            copyInto(tempCredsDir,"ca.pem", ca);

            e.put("DOCKER_TLS_VERIFY", "1");
            e.put("DOCKER_CERT_PATH", tempCredsDir.getRemote());
            return new ServerKeyMaterial(e, tempCredsDir);
        }

        return new ServerKeyMaterial(e);
    }

    private void copyInto(FilePath dir, String fileName, String content) throws IOException, InterruptedException {
        if (content==null)      return;
        dir.child(fileName).write(content,"UTF-8");
    }

    private static final long serialVersionUID = 1L;
    
    private static final class ServerKeyMaterial extends KeyMaterial {

        private final FilePath[] tempDirs;

        protected ServerKeyMaterial(EnvVars envVars, FilePath... temporaryDirectories) {
            super(envVars);
            this.tempDirs = temporaryDirectories;
        }

        @Override
        public void close() throws IOException {
            Throwable first = null;
            if (tempDirs != null) {
                for (FilePath tempDir : tempDirs) {
                    try {
                        tempDir.deleteRecursive();
                    } catch (Throwable e) {
                        first = first == null ? e : first;
                    }
                }
            }
            if (first != null) {
                if (first instanceof IOException) {
                    throw (IOException) first;
                } else if (first instanceof InterruptedException) {
                    throw new IOException(first);
                } else if (first instanceof RuntimeException) {
                    throw (RuntimeException) first;
                } else {
                    throw new IOException("Error closing credentials.", first);
                }
            }
        }
    }
}
