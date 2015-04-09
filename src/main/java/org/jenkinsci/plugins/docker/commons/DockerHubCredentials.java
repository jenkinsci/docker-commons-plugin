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

    protected DockerHubCredentials(CredentialsScope scope, String id, String description) {
        super(scope, id, description);
    }
}
