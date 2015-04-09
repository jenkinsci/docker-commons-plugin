package org.jenkinsci.plugins.docker.commons;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.docker.commons.impl.KeyMaterialImpl;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;

/**
 * Encapsulates the endpoint of Docker daemon and how to interact with it.
 *
 * @author Kohsuke Kawaguchi
 */
public class DockerServerEndpoint extends AbstractDescribableImpl<DockerServerEndpoint> {
    private final String uri;

    @DataBoundConstructor
    public DockerServerEndpoint(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    /**
     * Makes the key materials available locally and returns {@link KeyMaterial} that gives you the parameters
     * needed to access it.
     */
    public KeyMaterial materialize(DockerServerCredentials credentials) throws IOException, InterruptedException {
        return materialize(new FilePath(new File(FileUtils.getUserDirectory(),".docker")),credentials);
    }

    /**
     * Makes the key materials available as files on the given {@link FilePath} and
     * return {@link KeyMaterial} that gives you the parameters needed to access it.
     */
    public KeyMaterial materialize(FilePath dir, DockerServerCredentials credentials) throws IOException, InterruptedException {
        String endpoint = getUri();

        String key = credentials.getClientSecretKeyInPEM();
        String cert = credentials.getClientCertificateInPEM();
        String ca = credentials.getServerCaCertificateInPEM();
        if (key==null && cert==null && ca==null)
            return new KeyMaterialImpl(endpoint,null);  // no need to create temporary directory

        // protect this information from prying eyes
        dir = dir.createTempDir("docker","keys");
        dir.chmod(0600);

        // these file names are defined by convention by docker
        copyInto(dir,"key.pem", key);
        copyInto(dir,"cert.pem", cert);
        copyInto(dir,"ca.pem", ca);

        return new KeyMaterialImpl(endpoint,dir);
    }

    private void copyInto(FilePath dir, String fileName, String content) throws IOException, InterruptedException {
        if (content==null)      return;
        dir.child(fileName).write(content,"UTF-8");
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<DockerServerEndpoint> {
        @Override
        public String getDisplayName() {
            return "Docker Daemon";
        }
    }

    // TODO: write online config.jelly
}
