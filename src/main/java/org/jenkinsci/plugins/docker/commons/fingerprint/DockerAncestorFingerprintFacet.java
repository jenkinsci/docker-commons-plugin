package org.jenkinsci.plugins.docker.commons.fingerprint;

import hudson.model.Fingerprint;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nonnull;

/**
 * Facet representing the fact that some Docker image was derived from this one.
 * This facet would be added to the fingerprint corresponding to the {@code FROM} instruction in a {@code Dockerfile}.
 * {@link #getDescendantImageId} indicates the image that was built from that file.
 * @see DockerFingerprints#addFromFacet
 * @see DockerDescendantFingerprintFacet
 */
public class DockerAncestorFingerprintFacet extends DockerRunPtrFingerprintFacet {

    private final Set<String> descendantImageIds = new TreeSet<String>();

    DockerAncestorFingerprintFacet(Fingerprint fingerprint, long timestamp, String imageId) {
        super(fingerprint, timestamp, imageId);
    }

    synchronized void addDescendantImageId(String id) {
        descendantImageIds.add(id);
    }

    /**
     * Gets the descendant images built from this image.
     * @return a set of 64-digit IDs, never empty
     */
    public synchronized @Nonnull Set<String> getDescendantImageIds() {
        return new TreeSet<String>(descendantImageIds);
    }

}
