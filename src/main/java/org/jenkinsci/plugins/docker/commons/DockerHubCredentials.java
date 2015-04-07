package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.model.Item;
import jenkins.model.Jenkins;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.net.URL;
import java.util.Collections;

/**
 * Represents the endpoint and the credential to access DockerHub, which is a triplet
 * of API endpoint URL, secret token, and (unused) email.
 * 
 * <p>
 * See your ~/.dockercfg for the structure.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class DockerHubCredentials extends BaseStandardCredentials {

    /**
     * Gets the token value to be used for authenticating access to DockerHub.
     * This is what gets stored in {@code ~/.dockercfg} as "auth". 
     */
    @Nonnull
    public abstract String getToken();

    /**
     * {@code .dockercfg} also has the "email" field that is unused, but we need some value in there.
     * Defining this as a property to allow subtypes to expose this in the future.
     */
    @Nonnull
    public String getEmail() {
        return getId();
    }

    /**
     * The DockerHub endpoint that this credential represents to, such as
     * {@code https://index.docker.io/v1/}
     */
    @Nonnull
    public abstract URL getEndpoint();
    
    protected DockerHubCredentials(CredentialsScope scope, String id, String description) {
        super(scope, id, description);
    }

    /**
     * Plugins that want to refer to a {@link DockerHubCredentials} should do so via ID string,
     * and use this method to resolve it to {@link DockerHubCredentials}.
     * 
     * @param context     
     *       If you are a build step trying to access DockerHub in the context of a build/job,
     *       specify that job. Otherwise null. If you are scoped to something else, you might
     *       have to interact with {@link CredentialsProvider} directly.
     */
    public static @CheckForNull DockerHubCredentials get(String id, Item context) {
        // as a build step, your access to credentials are constrained by what the build
        // can access, hence Jenkins.getAuthentication()
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        DockerHubCredentials.class, context, Jenkins.getAuthentication(),
                        Collections.<DomainRequirement>emptyList()),
                CredentialsMatchers.withId(id)
        );
    }
}
