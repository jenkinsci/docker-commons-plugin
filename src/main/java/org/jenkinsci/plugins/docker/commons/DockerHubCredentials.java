package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

import javax.annotation.Nonnull;

/**
 * Factory of {@link DockerHubToken}, which is normally a secret itself (such as OAuth refresh token.)
 *
 * @author Kohsuke Kawaguchi
 * @see DockerHubEndpoint
 */
public abstract class DockerHubCredentials extends BaseStandardCredentials {

    /**
     * Gets the token value to be used for authenticating access to DockerHub.
     * This is what gets stored in {@code ~/.dockercfg}.
     */
    @Nonnull
    public abstract DockerHubToken getToken();

    protected DockerHubCredentials(CredentialsScope scope, String id, String description) {
        super(scope, id, description);
    }
}
