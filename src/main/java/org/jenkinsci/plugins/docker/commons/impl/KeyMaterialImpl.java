package org.jenkinsci.plugins.docker.commons.impl;

import hudson.EnvVars;
import hudson.FilePath;
import org.jenkinsci.plugins.docker.commons.KeyMaterial;

import java.io.IOException;
import java.io.Serializable;

/**
 * Key/certificates have to be laid out in a specific file names in this directory
 * to make docker(1) happy.
 *
 * <p>
 * Marked serializale for persistence, although such environment has to handle {@link FilePath} correctly, such as workflow.
 *
 * @author Kohsuke Kawaguchi
 */
public class KeyMaterialImpl implements KeyMaterial, Serializable {
    private FilePath dir;
    private final String host;

    public KeyMaterialImpl(String host, FilePath dir) {
        this.host = host;
        this.dir = dir;
    }

    @Override
    public EnvVars builEnvironments() {
        EnvVars e = new EnvVars();
        if (host!=null)
            e.put("DOCKER_CERT_PATH",host);
        if (dir!=null) {
            e.put("DOCKER_TLS_VERIFY", "1");
            e.put("DOCKER_CERT_PATH",dir.getRemote());
        }
        
        return e;
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            if (dir!=null) {
                dir.deleteRecursive();
            }
            dir = null;
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private static final long serialVersionUID = 1L;
}
