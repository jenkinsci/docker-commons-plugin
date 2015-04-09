package org.jenkinsci.plugins.docker.commons;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Kohsuke Kawaguchi
 */
public class /*Mock*/ DockerBuilder {
    /**
     * config.jelly should inline this.
     * Not meant to be instantiated and referenced externally.
     */
    private final DockerEndpoint endpoint;
    /**
     * This should be a part of job configuration
     */
    private final String credentialId;

    @DataBoundConstructor
    public DockerBuilder(DockerEndpoint endpoint, String credentialId) {
        this.endpoint = endpoint;
        this.credentialId = credentialId;
    }
}
