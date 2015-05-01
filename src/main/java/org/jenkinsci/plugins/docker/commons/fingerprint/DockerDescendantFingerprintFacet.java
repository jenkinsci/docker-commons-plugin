package org.jenkinsci.plugins.docker.commons.fingerprint;

import hudson.model.Fingerprint;

/**
 * Facet representing the fact that this Docker image was derived from another.
 * This facet would be added to the fingerprint corresponding to an image built using a {@code Dockerfile}.
 * {@link #getAncestorImageId} indicates the ID of the image specified in the {@code FROM} instruction.
 * @see DockerFingerprints#addFromFacet
 * @see DockerAncestorFingerprintFacet
 */
public class DockerDescendantFingerprintFacet extends DockerRunPtrFingerprintFacet {

    private final String ancestorImageId;

    DockerDescendantFingerprintFacet(Fingerprint fingerprint, long timestamp, String imageId, String ancestorImageId) {
        super(fingerprint, timestamp, imageId);
        this.ancestorImageId = ancestorImageId;
    }

    public String getAncestorImageId() {
        return ancestorImageId;
    }

}
