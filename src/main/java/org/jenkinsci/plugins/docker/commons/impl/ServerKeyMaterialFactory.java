package org.jenkinsci.plugins.docker.commons.impl;

import hudson.EnvVars;
import hudson.FilePath;
import org.jenkinsci.plugins.docker.commons.DockerServerCredentials;
import org.jenkinsci.plugins.docker.commons.KeyMaterial;
import org.jenkinsci.plugins.docker.commons.KeyMaterialFactory;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
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
    
    private final String host;
    private final DockerServerCredentials credentials;
    private final FilePath baseDir;

    public ServerKeyMaterialFactory(@Nonnull final String host, @CheckForNull final DockerServerCredentials credentials, @Nonnull final FilePath baseDir) {
        this.host = host;
        this.credentials = credentials;
        this.baseDir = baseDir;
    }

    @Override
    public KeyMaterial materialize() throws IOException, InterruptedException {
        final FilePath tempCredsDir;
        
        if (credentials != null) {
            String key = credentials.getClientSecretKeyInPEM();
            String cert = credentials.getClientCertificateInPEM();
            String ca = credentials.getServerCaCertificateInPEM();

            if (key != null && cert != null && ca != null) {
                tempCredsDir = new FilePath(baseDir, UUID.randomUUID().toString());
                // these file names are defined by convention by docker
                copyInto(tempCredsDir,"key.pem", key);
                copyInto(tempCredsDir,"cert.pem", cert);
                copyInto(tempCredsDir,"ca.pem", ca);

                // protect this information from prying eyes
                tempCredsDir.chmod(0600);
            } else {
                tempCredsDir = null;
            }
        } else {
            tempCredsDir = null;
        }

        EnvVars e = new EnvVars();
        if (host!=null) {
            e.put("DOCKER_HOST",host);
        }
        if (tempCredsDir !=null) {
            e.put("DOCKER_TLS_VERIFY", "1");
            e.put("DOCKER_CERT_PATH", tempCredsDir.getRemote());
        }

        return new ServerKeyMaterial(e, tempCredsDir);
    }

    private void copyInto(FilePath dir, String fileName, String content) throws IOException, InterruptedException {
        if (content==null)      return;
        dir.child(fileName).write(content,"UTF-8");
    }

    private static final long serialVersionUID = 1L;
    
    private static final class ServerKeyMaterial extends KeyMaterial {

        private final FilePath tempCredsDir;

        protected ServerKeyMaterial(EnvVars envVars, FilePath tempCredsDir) {
            super(envVars);
            this.tempCredsDir = tempCredsDir;
        }

        @Override
        public void close() throws IOException {
            try {
                if (tempCredsDir !=null) {
                    tempCredsDir.deleteRecursive();
                }
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        }
    }
}
