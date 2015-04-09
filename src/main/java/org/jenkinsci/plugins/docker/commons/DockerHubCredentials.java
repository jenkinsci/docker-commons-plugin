package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

import javax.annotation.Nonnull;

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
     * This is what gets stored in {@code ~/.dockercfg}.
     */
    @Nonnull
    public abstract DockerHubToken getToken();

    protected DockerHubCredentials(CredentialsScope scope, String id, String description) {
        super(scope, id, description);
    }
}
