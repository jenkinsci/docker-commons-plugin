package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.remoting.VirtualChannel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.docker.commons.impl.CompositeKeyMaterialFactory;
import org.jenkinsci.plugins.docker.commons.impl.ServerHostKeyMaterialFactory;
import org.jenkinsci.plugins.docker.commons.impl.ServerKeyMaterialFactory;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;

import static hudson.Util.*;
import hudson.util.ListBoxModel;
import javax.annotation.CheckForNull;
import org.kohsuke.stapler.AncestorInPath;

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
    private final @CheckForNull String credentialsId;

    @DataBoundConstructor
    public DockerServerEndpoint(String uri, String credentialsId) {
        this.uri = fixEmpty(uri);
        this.credentialsId = fixEmpty(credentialsId);
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

    /**
     * Makes the key materials available locally for the on-going build
     * and returns {@link KeyMaterialFactory} that gives you the parameters needed to access it.
     */
    public KeyMaterialFactory newKeyMaterialFactory(AbstractBuild build) throws IOException, InterruptedException {
        return newKeyMaterialFactory(build.getParent(), build.getWorkspace().getChannel());
    }

    /**
     * Makes the key materials available locally and returns {@link KeyMaterialFactory} that gives you the parameters
     * needed to access it.
     */
    public KeyMaterialFactory newKeyMaterialFactory(Item context, VirtualChannel target) throws IOException, InterruptedException {
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
        // TODO if creds == null, or for other reasons dir is not passed to ServerKeyMaterialImpl, this creates a temp dir which is never deleted
        FilePath dotDocker = FilePath.getHomeDirectory(target).child(".docker");
        dotDocker.mkdirs();
        FilePath baseDir = dotDocker.createTempDir("keys",null);

        return newKeyMaterialFactory(baseDir, creds);
    }

    /**
     * Create a {@link KeyMaterialFactory} for connecting to the docker server/host. 
     */
    public KeyMaterialFactory newKeyMaterialFactory(FilePath dir, @Nullable DockerServerCredentials credentials) throws IOException, InterruptedException {
        return new CompositeKeyMaterialFactory(new ServerHostKeyMaterialFactory(getUri()), new ServerKeyMaterialFactory(credentials, dir));
    }

    @Override public String toString() {
        return "DockerServerEndpoint[" + uri + ";credentialsId=" + credentialsId + "]";
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<DockerServerEndpoint> {
        @Override
        public String getDisplayName() {
            return "Docker Daemon";
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item) {
            // TODO may also need to specify a specific authentication and domain requirements
            return new StandardListBoxModel().withEmptySelection().withAll(CredentialsProvider.lookupCredentials(DockerServerCredentials.class, item, null, Collections.<DomainRequirement>emptyList()));
        }

    }

}
