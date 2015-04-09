package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

import javax.annotation.Nonnull;

/**
 * Factory of {@link DockerRegistryToken}, which is normally a secret itself (such as OAuth refresh token.)
 *
 * @author Kohsuke Kawaguchi
 * @see DockerRegistryEndpoint
 */
public abstract class DockerRegistryCredentials extends BaseStandardCredentials {

    /**
     * Gets the token value to be used for authenticating access to DockerHub.
     * This is what gets stored in {@code ~/.dockercfg}.
     */
    @Nonnull
    public abstract DockerRegistryToken getToken();

    protected DockerRegistryCredentials(CredentialsScope scope, String id, String description) {
        super(scope, id, description);
    }
}
