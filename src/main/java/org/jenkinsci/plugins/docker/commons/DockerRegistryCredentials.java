package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.common.StandardCredentials;

import javax.annotation.Nonnull;

/**
 * Factory of {@link DockerRegistryToken}, which is normally a secret itself (such as OAuth refresh token.)
 *
 * @author Kohsuke Kawaguchi
 * @see DockerRegistryEndpoint
 */
public interface DockerRegistryCredentials extends StandardCredentials {

    /**
     * Gets the token value to be used for authenticating access to DockerHub.
     * This is what gets stored in {@code ~/.dockercfg}.
     */
    @Nonnull
    DockerRegistryToken getToken();
}
