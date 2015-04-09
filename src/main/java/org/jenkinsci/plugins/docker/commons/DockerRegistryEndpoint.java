package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
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
import jenkins.model.Jenkins;
import org.apache.commons.codec.binary.Base64;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
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
    private final String urlString;
    private final String credentialsId;

    @DataBoundConstructor
    public DockerRegistryEndpoint(String urlString, String credentialsId) {
        this.urlString = Util.fixEmpty(urlString);
        this.credentialsId = credentialsId;
    }

    /**
     * Gets the endpoint URL, such as "http://index.docker.io/v1/"
     *
     * <p>
     * Null to indicate whatever Docker picks by default.
     */
    public URL getUrl() throws IOException {
        if (urlString!=null)
            return new URL(urlString);
        else
            return new URL("http://index.docker.io/v1/");
    }

    /**
     * For stapler.
     */
    public @Nullable String getUrlString() {
        return urlString;
    }

    /**
     * {@linkplain IdCredentials#getId() ID} of the credentials used to talk to this endpoint.
     */
    public @Nullable String getCredentialsId() {
        return credentialsId;
    }

    /**
     * Plugins that want to refer to a {@link DockerRegistryCredentials} should do so via ID string,
     * and use this method to resolve it to {@link DockerRegistryCredentials}.
     *
     * @param context
     *       If you are a build step trying to access DockerHub in the context of a build/job,
     *       specify that job. Otherwise null. If you are scoped to something else, you might
     *       have to interact with {@link CredentialsProvider} directly.
     */
    public @CheckForNull
    DockerRegistryToken getToken(Item context) {
        // as a build step, your access to credentials are constrained by what the build
        // can access, hence Jenkins.getAuthentication()

        List<DomainRequirement> requirements = Collections.emptyList();
        try {
            requirements = Collections.<DomainRequirement>singletonList(new HostnameRequirement(getUrl().getHost()));
        } catch (IOException e) {
            // shrug off this error and move on. We are matching with ID anyway.
        }

        // look for subtypes that know how to create a token, such as Google Container Registry
        DockerRegistryCredentials v = firstOrNull(CredentialsProvider.lookupCredentials(
                DockerRegistryCredentials.class, context, Jenkins.getAuthentication(),requirements),
            withId(credentialsId));
        if (v!=null)
            return v.getToken();

        // allow the plain username/password token and treat it like how DockerHub turns it into a token,
        UsernamePasswordCredentials w = firstOrNull(CredentialsProvider.lookupCredentials(
                UsernamePasswordCredentials.class, context, Jenkins.getAuthentication(),requirements),
            withId(credentialsId));
        if (w!=null)
            return new DockerRegistryToken(w.getUsername(),
                    Base64.encodeBase64String((w.getUsername() + ":" + w.getPassword().getPlainText()).getBytes(UTF8)));

        return null;
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

        return token.materialize(getUrl(),target);
    }

    /**
     * Decorates the repository ID like "jenkinsci/workflow-demo" with repository prefix.
     */
    public String imageName(String userAndRepo) throws IOException {
        if (urlString==null)    return userAndRepo;
        URL url = getUrl();

        StringBuilder s = new StringBuilder(url.getHost());
        if (url.getPort()>0 && url.getDefaultPort()!=url.getPort())
            s.append(':').append(url.getPort());
        return s.append('/').append(userAndRepo).toString();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<DockerRegistryEndpoint> {
        @Override
        public String getDisplayName() {
            return "Docker Hub";
        }
    }

    private static final Charset UTF8 = Charset.forName("UTF-8");
}
