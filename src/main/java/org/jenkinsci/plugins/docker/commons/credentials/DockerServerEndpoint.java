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
package org.jenkinsci.plugins.docker.commons.credentials;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.remoting.VirtualChannel;
import hudson.util.ListBoxModel;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.docker.commons.impl.ServerHostKeyMaterialFactory;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static hudson.Util.fixEmpty;

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
    // TODO once we have a base type to migrate DockerServerCredentials replace with the corresponding interface
    private static final Class<DockerServerCredentials> BASE_CREDENTIAL_TYPE = DockerServerCredentials.class;
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
    public KeyMaterialFactory newKeyMaterialFactory(@Nonnull AbstractBuild build) throws IOException, InterruptedException {
        final FilePath workspace = build.getWorkspace();
        if (workspace == null) {
            throw new IllegalStateException("Build has no workspace");
        }
        return newKeyMaterialFactory(build.getParent(), workspace.getChannel());
    }

    /**
     * Makes the key materials available locally and returns {@link KeyMaterialFactory} that gives you the parameters
     * needed to access it.
     */
    public KeyMaterialFactory newKeyMaterialFactory(@Nonnull Item context, @Nonnull VirtualChannel target) throws IOException, InterruptedException {
        // as a build step, your access to credentials are constrained by what the build
        // can access, hence Jenkins.getAuthentication()
        DockerServerCredentials creds=null;
        if (credentialsId!=null) {
            List<DomainRequirement> domainRequirements = URIRequirementBuilder.fromUri(getUri()).build();
            domainRequirements.add(new DockerServerDomainRequirement());
            creds = CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(
                            DockerServerCredentials.class, context, Jenkins.getAuthentication(),
                            domainRequirements),
                    CredentialsMatchers.withId(credentialsId)
            );
        }

        // the directory needs to be outside workspace to avoid prying eyes
        FilePath dotDocker = dotDocker(target);
        dotDocker.mkdirs();
        // ServerKeyMaterialFactory.materialize creates a random subdir if one is needed:
        return newKeyMaterialFactory(dotDocker, creds);
    }

    static FilePath dotDocker(@Nonnull VirtualChannel target) throws IOException, InterruptedException {
        return FilePath.getHomeDirectory(target).child(".docker");
    }

    /**
     * Create a {@link KeyMaterialFactory} for connecting to the docker server/host. 
     */
    public KeyMaterialFactory newKeyMaterialFactory(FilePath dir, @Nullable DockerServerCredentials credentials) throws IOException, InterruptedException {
        return (uri == null ? KeyMaterialFactory.NULL : new ServerHostKeyMaterialFactory(uri))
                .plus(AuthenticationTokens.convert(KeyMaterialFactory.class, credentials))
                .contextualize(new KeyMaterialContext(dir));
    }

    @Override public String toString() {
        return "DockerServerEndpoint[" + uri + ";credentialsId=" + credentialsId + "]";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + (this.uri != null ? this.uri.hashCode() : 0);
        hash = 13 * hash + (this.credentialsId != null ? this.credentialsId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DockerServerEndpoint other = (DockerServerEndpoint) obj;
        if ((this.uri == null) ? (other.uri != null) : !this.uri.equals(other.uri)) {
            return false;
        }
        if ((this.credentialsId == null) ? (other.credentialsId != null) : !this.credentialsId.equals(other.credentialsId)) {
            return false;
        }
        return true;
    }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<DockerServerEndpoint> {
        @Override
        public String getDisplayName() {
            return "Docker Daemon";
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String uri) {
            List<DomainRequirement> domainRequirements = URIRequirementBuilder.fromUri(uri).build();
            domainRequirements.add(new DockerServerDomainRequirement());
            return new StandardListBoxModel()
                    .withEmptySelection()
                    .withMatching(
                            AuthenticationTokens.matcher(KeyMaterialFactory.class),
                            CredentialsProvider
                                    .lookupCredentials(BASE_CREDENTIAL_TYPE, item, null, domainRequirements)
                    );
        }

    }

}
