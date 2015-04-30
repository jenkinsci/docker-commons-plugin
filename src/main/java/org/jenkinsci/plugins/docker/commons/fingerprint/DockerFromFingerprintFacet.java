package org.jenkinsci.plugins.docker.commons.fingerprint;

import hudson.model.Fingerprint;

/**
 * Facet representing the fact that one Docker image was derived from another.
 * This corresponds to the {@code FROM} directive in a {@code Dockerfile}.
 * As a facet on a fingerprint corresponding to the base image, refers to the derived image.
 * As a facet on a fingerprint corresponding to the derived image, refers to the base image.
 * Both fingerprints should be associated with the same build.
 * @see DockerFingerprints#addFromFacet
 */
public class DockerFromFingerprintFacet extends DockerRunPtrFingerprintFacet {

    private final String otherImageId;
    private final boolean inverse;

    DockerFromFingerprintFacet(Fingerprint fingerprint, long timestamp, String imageId, String otherImageId, boolean inverse) {
        super(fingerprint, timestamp, imageId);
        this.otherImageId = otherImageId;
        this.inverse = inverse;
    }

    public String getBaseImageId() {
        return inverse ? otherImageId : getImageId();
    }

    public String getDerivedImageId() {
        return inverse ? getImageId() : otherImageId;
    }

}
