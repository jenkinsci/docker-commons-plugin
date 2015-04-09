package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Run;
import hudson.remoting.VirtualChannel;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.docker.commons.impl.KeyMaterialImpl;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static hudson.Util.*;

/**
 * Encapsulates the endpoint of Docker daemon and how to interact with it.
 *
 * <p>
 * As {@link Describable} it comes with pre-baked configuration form that you can use in
 * your builders/publishers/etc that interact with Docker daemon.
 *
 * @author Kohsuke Kawaguchi
 */
public class DockerServerEndpoint extends AbstractDescribableImpl<DockerServerEndpoint> {
    private final String uri;
    private final String credentialsId;

    @DataBoundConstructor
    public DockerServerEndpoint(String uri, String credentialsId) {
        this.uri = fixEmpty(uri);
        this.credentialsId = credentialsId;
    }

    /**
     * Gets the endpoint in URI, such as "unix:///var/run/docker.sock".
     *
     * <p>
     * Null to indicate whatever Docker picks by default.
     */
    public @Nullable String getUri() {
        return uri;
    }

    /**
     * {@linkplain IdCredentials#getId() ID} of the credentials used to talk to this endpoint.
     */
    public @Nullable String getCredentialsId() {
        return credentialsId;
    }

    public KeyMaterial materialize(AbstractBuild build) throws IOException, InterruptedException {
        return materialize(build.getParent(),build.getWorkspace().getChannel());
    }

    /**
     * Makes the key materials available locally and returns {@link KeyMaterial} that gives you the parameters
     * needed to access it.
     */
    public KeyMaterial materialize(Item context,VirtualChannel target) throws IOException, InterruptedException {
        // as a build step, your access to credentials are constrained by what the build
        // can access, hence Jenkins.getAuthentication()
        DockerServerCredentials creds=null;
        if (credentialsId!=null) {
            creds = CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(
                            DockerServerCredentials.class, context, Jenkins.getAuthentication(),
                            Collections.<DomainRequirement>emptyList()),
                    CredentialsMatchers.withId(credentialsId)
            );
        }

        // the directory needs to be outside workspace to avoid prying eyes
        FilePath baseDir = FilePath.getHomeDirectory(target).child(".docker");

        return materialize(baseDir, creds);
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
