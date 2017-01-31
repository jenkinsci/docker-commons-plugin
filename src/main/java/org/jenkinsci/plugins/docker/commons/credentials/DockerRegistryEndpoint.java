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

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;

import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.remoting.VirtualChannel;
import hudson.util.ListBoxModel;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import jenkins.model.Jenkins;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.*;
import hudson.AbortException;
import hudson.Launcher;
import hudson.model.TaskListener;

/**
 * Encapsulates the endpoint of DockerHub and how to interact with it.
 *
 * <p>
 * As {@link Describable} it comes with pre-baked configuration form that you can use in
 * your builders/publishers/etc that interact with Docker daemon.
 *
 * @author Kohsuke Kawaguchi
 */
public class DockerRegistryEndpoint extends AbstractDescribableImpl<DockerRegistryEndpoint> {

    /**
     * Some regex magic to parse docker registry:port/namespace/name:tag into parts, using the same constraints as
     * docker push.
     * 
     * registry must not contain / and must contain a .
     * 
     * everything but name is optional
     */
    private static final Pattern DOCKER_REGISTRY_PATTERN = Pattern
            .compile("(([^/]+\\.[^/]+)/)?(([a-z0-9_]+)/)?([a-zA-Z0-9-_\\.]+)(:([a-z0-9-_\\.]+))?");

    /**
     * Null if this is on the public docker hub.
     */
    private final String url;
    private final @CheckForNull String credentialsId;

    @DataBoundConstructor
    public DockerRegistryEndpoint(String url, String credentialsId) {
        this.url = Util.fixEmpty(url);
        this.credentialsId = Util.fixEmpty(credentialsId);
    }

    /**
     * Parse the registry endpoint out of a registry:port/namespace/name:tag string as created by
     * {@link #imageName(String)}. Credentials are set to the id passed. The url is built from registry:port into
     * https://registry:port, the same way docker push does.
     * 
     * @param s
     * @param credentialsId
     *            passed to the constructor, can be null
     * @throws IllegalArgumentException
     *             if string can't be parsed
     * @return The DockerRegistryEndpoint corresponding to the registry:port part of the string
     */
    public static DockerRegistryEndpoint fromImageName(String s, @CheckForNull String credentialsId) {
        Matcher matcher = DOCKER_REGISTRY_PATTERN.matcher(s);
        if (!matcher.matches() || matcher.groupCount() < 7) {
            throw new IllegalArgumentException(s + " does not match regex " + DOCKER_REGISTRY_PATTERN);
        }
        String url;
        try {
            // docker push always uses https
            url = matcher.group(2) == null ? null : new URL("https://" + matcher.group(2)).toString();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(s + " can not be parsed as URL: " + e.getMessage());
        }
        // not used, but could be
        /*String namespace = matcher.group(4);
        String repoName = matcher.group(5);
        String tag = matcher.group(7);*/
        return new DockerRegistryEndpoint(url, credentialsId);
    }

    /**
     * Gets the endpoint URL, such as "https://index.docker.io/v1/"
     */
    public @Nonnull URL getEffectiveUrl() throws IOException {
        if (url != null) {
            return new URL(url);
        } else {
            return new URL("https://index.docker.io/v1/");
        }
    }

    /**
     * For stapler.
     */
    public @Nullable String getUrl() {
        return url;
    }

    /**
     * {@linkplain IdCredentials#getId() ID} of the credentials used to talk to this endpoint.
     */
    public @Nullable String getCredentialsId() {
        return credentialsId;
    }

    /**
     * Plugins that want to refer to a {@link IdCredentials} should do so via ID string,
     * and use this method to resolve it and convert to {@link DockerRegistryToken}.
     *
     * @param context
     *       If you are a build step trying to access DockerHub in the context of a build/job,
     *       specify that job. Otherwise null. If you are scoped to something else, you might
     *       have to interact with {@link CredentialsProvider} directly.
     */
    public @CheckForNull
    DockerRegistryToken getToken(Item context) {
        if (credentialsId == null) {
            return null;
        }

        // as a build step, your access to credentials are constrained by what the build
        // can access, hence Jenkins.getAuthentication()

        List<DomainRequirement> requirements = Collections.emptyList();
        try {
            requirements = Collections.<DomainRequirement>singletonList(new HostnameRequirement(getEffectiveUrl().getHost()));
        } catch (IOException e) {
            // shrug off this error and move on. We are matching with ID anyway.
        }

        // look for subtypes that know how to create a token, such as Google Container Registry
        return AuthenticationTokens.convert(DockerRegistryToken.class, firstOrNull(CredentialsProvider.lookupCredentials(
                IdCredentials.class, context, Jenkins.getAuthentication(),requirements),
                allOf(AuthenticationTokens.matcher(DockerRegistryToken.class), withId(credentialsId))));
    }

    /**
     * @deprecated Call {@link #newKeyMaterialFactory(Item, VirtualChannel, Launcher, TaskListener)}
     */
    @Deprecated
    public KeyMaterialFactory newKeyMaterialFactory(@Nonnull AbstractBuild build) throws IOException, InterruptedException {
        final FilePath workspace = build.getWorkspace();
        if (workspace == null) {
            throw new IllegalStateException("Requires workspace.");
        }
        return newKeyMaterialFactory(build.getParent(), workspace.getChannel());
    }

    /**
     * @deprecated Call {@link #newKeyMaterialFactory(Item, VirtualChannel, Launcher, TaskListener)}
     */
    @Deprecated
    public KeyMaterialFactory newKeyMaterialFactory(Item context, @Nonnull VirtualChannel target) throws IOException, InterruptedException {
        return newKeyMaterialFactory(context, target, null, TaskListener.NULL);
    }

    /**
     * Makes the credentials available locally and returns {@link KeyMaterialFactory} that gives you the parameters
     * needed to access it.
     */
    public KeyMaterialFactory newKeyMaterialFactory(@CheckForNull Item context, @Nonnull VirtualChannel target, @CheckForNull Launcher launcher, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        if (credentialsId == null) {
            return KeyMaterialFactory.NULL; // nothing needed to be done
        }
        DockerRegistryToken token = getToken(context);
        if (token == null) {
            throw new AbortException("Could not find credentials matching " + credentialsId);
        }
        return token.newKeyMaterialFactory(getEffectiveUrl(), target, launcher, listener);
    }

    /**
     * Decorates the repository ID namespace/name (ie. "jenkinsci/workflow-demo") with registry prefix
     * (docker.acme.com:80/jenkinsci/workflow-demo).
     * 
     * @param userAndRepo
     *            the namespace/name part to append to the registry
     * @return the full registry:port/namespace/name string
     * @throws IOException
     */
    public String imageName(@Nonnull String userAndRepo) throws IOException {
        if (userAndRepo == null) {
            throw new IllegalArgumentException("Image name cannot be null.");
        }
        if (url == null) {
            return userAndRepo;
        }
        URL effectiveUrl = getEffectiveUrl();

        StringBuilder s = new StringBuilder(effectiveUrl.getHost());
        if (effectiveUrl.getPort() > 0 ) {
            s.append(':').append(effectiveUrl.getPort());
        }
        if (userAndRepo.startsWith(String.valueOf(s))) {
            return userAndRepo;
        }
        return s.append('/').append(userAndRepo).toString();
    }

    @Override public String toString() {
        return "DockerRegistryEndpoint[" + url + ";credentialsId=" + credentialsId + "]";
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + (this.url != null ? this.url.hashCode() : 0);
        hash = 31 * hash + (this.credentialsId != null ? this.credentialsId.hashCode() : 0);
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
        final DockerRegistryEndpoint other = (DockerRegistryEndpoint) obj;
        if ((this.url == null) ? (other.url != null) : !this.url.equals(other.url)) {
            return false;
        }
        if ((this.credentialsId == null) ? (other.credentialsId != null) : !this.credentialsId.equals(other.credentialsId)) {
            return false;
        }
        return true;
    }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<DockerRegistryEndpoint> {
        @Override
        public String getDisplayName() {
            return "Docker Hub";
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item) {
            // TODO may also need to specify a specific authentication and domain requirements
            return new StandardListBoxModel()
                    .withEmptySelection()
                    .withMatching(AuthenticationTokens.matcher(DockerRegistryToken.class),
                            CredentialsProvider.lookupCredentials(
                                    StandardCredentials.class,
                                    item,
                                    null,
                                    Collections.<DomainRequirement>emptyList()
                            )
                    );
        }

    }

}
