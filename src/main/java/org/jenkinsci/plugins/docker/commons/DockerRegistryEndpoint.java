package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.remoting.VirtualChannel;
import hudson.util.ListBoxModel;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.*;

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
     * Makes the credentials available locally for the on-going build
     * and returns {@link KeyMaterial} that gives you the parameters needed to access it.
     */
    public KeyMaterial materialize(AbstractBuild build) throws IOException, InterruptedException {
        return materialize(build.getParent(),build.getWorkspace().getChannel());
    }

    /**
     * Makes the credentials available locally and returns {@link KeyMaterial} that gives you the parameters
     * needed to access it.
     */
    public KeyMaterial materialize(Item context,VirtualChannel target) throws IOException, InterruptedException {
        DockerRegistryToken token = getToken(context);
        if (token==null)    return KeyMaterial.NULL;    // nothing needed to be done

        return token.materialize(getEffectiveUrl(),target);
    }

    /**
     * Decorates the repository ID like "jenkinsci/workflow-demo" with repository prefix.
     */
    public String imageName(String userAndRepo) throws IOException {
        if (url==null)    return userAndRepo;
        URL effectiveUrl = getEffectiveUrl();

        StringBuilder s = new StringBuilder(effectiveUrl.getHost());
        if (effectiveUrl.getPort() > 0 && effectiveUrl.getDefaultPort() != effectiveUrl.getPort()) {
            s.append(':').append(effectiveUrl.getPort());
        }
        return s.append('/').append(userAndRepo).toString();
    }

    @Override public String toString() {
        return "DockerRegistryEndpoint[" + url + ";credentialsId=" + credentialsId + "]";
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
