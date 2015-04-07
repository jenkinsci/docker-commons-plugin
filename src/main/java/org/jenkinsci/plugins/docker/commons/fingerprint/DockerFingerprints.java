package org.jenkinsci.plugins.docker.commons.fingerprint;

import hudson.model.Fingerprint;
import jenkins.model.Jenkins;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class DockerFingerprints {
    private DockerFingerprints() {} // no instantiation

    /**
     * Gets {@link Fingerprint} for a given docker image.
     */
    public Fingerprint of(String imageId) throws IOException {
        if (imageId.length()!=64)
            throw new IllegalArgumentException("Expecting 64char full image ID, but go "+imageId);

        return Jenkins.getInstance().getFingerprintMap().get(imageId.substring(0, 32));
    }
}
