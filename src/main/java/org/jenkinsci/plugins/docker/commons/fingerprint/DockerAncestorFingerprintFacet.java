package org.jenkinsci.plugins.docker.commons.fingerprint;

import hudson.model.Fingerprint;

/**
 * Facet representing the fact that some Docker image was derived from this one.
 * This facet would be added to the fingerprint corresponding to the {@code FROM} instruction in a {@code Dockerfile}.
 * {@link #getDescendantImageId} indicates the image that was built from that file.
 * @see DockerFingerprints#addFromFacet
 * @see DockerDescendantFingerprintFacet
 */
public class DockerAncestorFingerprintFacet extends DockerRunPtrFingerprintFacet {

    private final String descendantImageId;

    DockerAncestorFingerprintFacet(Fingerprint fingerprint, long timestamp, String imageId, String descendantImageId) {
        super(fingerprint, timestamp, imageId);
        this.descendantImageId = descendantImageId;
    }

    public String getDescendantImageId() {
        return descendantImageId;
    }

}
