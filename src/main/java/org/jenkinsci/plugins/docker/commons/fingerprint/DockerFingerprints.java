package org.jenkinsci.plugins.docker.commons.fingerprint;

import hudson.BulkChange;
import hudson.model.Fingerprint;
import hudson.model.Run;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.FingerprintFacet;

/**
 * Entry point into fingerprint related functionalities in Docker.
 */
public class DockerFingerprints {
    private DockerFingerprints() {} // no instantiation
 
    /**
     * Gets a fingerprint hash for Docker image ID.
     * This method calculates image hash without retrieving a fingerprint by 
     * {@link DockerFingerprints#of(java.lang.String)}, which may be a high-cost call.
     * 
     * @param imageId Docker image ID.
     *      Only 64-char full image IDs are supported.
     * @return 32-char fingerprint hash
     * @throws IllegalArgumentException Invalid image ID
     */
    public static @Nonnull String getImageFingerprintHash(@Nonnull String imageId) {
        if (imageId.length() != 64) {
            throw new IllegalArgumentException("Expecting 64-char full image ID, but got " + imageId);
        }
        return imageId.substring(0, 32);
    }

    /**
     * Gets {@link Fingerprint} for a given docker image.
     */
    public static @CheckForNull Fingerprint of(@Nonnull String imageId) throws IOException {
        return Jenkins.getInstance().getFingerprintMap().get(getImageFingerprintHash(imageId));
    }

    private static @Nonnull Fingerprint make(@Nonnull Run<?,?> run, @Nonnull String imageId) throws IOException {
        return Jenkins.getInstance().getFingerprintMap().getOrCreate(run, "<docker-image>", getImageFingerprintHash(imageId));
    }

    /**
     * Adds a new {@link ContainerRecord} for the specified image, creating necessary intermediate objects as it goes.
     */
    public static void addRunFacet(@Nonnull ContainerRecord record, @Nonnull Run<?,?> run) throws IOException {
        String imageId = record.getImageId();
        Fingerprint f = make(run, imageId);
        Collection<FingerprintFacet> facets = f.getFacets();
        DockerRunFingerprintFacet runFacet = null;
        for (FingerprintFacet facet : facets) {
            if (facet instanceof DockerRunFingerprintFacet) {
                runFacet = (DockerRunFingerprintFacet) facet;
                break;
            }
        }
        BulkChange bc = new BulkChange(f);
        try {
            if (runFacet == null) {
                runFacet = new DockerRunFingerprintFacet(f, System.currentTimeMillis(), imageId);
                facets.add(runFacet);
            }
            runFacet.add(record);
            runFacet.addFor(run);
            DockerFingerprintAction.addToRun(f, imageId, run);
            bc.commit();
        } finally {
            bc.abort();
        }
    }

    /**
     * Creates a new {@link DockerAncestorFingerprintFacet} and {@link DockerDescendantFingerprintFacet} and adds a run.
     * Or adds to existing facets.
     * @param ancestorImageId the ID of the image specified in a {@code FROM} instruction, or null in case of {@code scratch} (i.e., the descendant is a base image)
     * @param descendantImageId the ID of the image which was built
     * @param run the build in which the image building occurred
     */
    public static void addFromFacet(@CheckForNull String ancestorImageId, @Nonnull String descendantImageId, @Nonnull Run<?,?> run) throws IOException {
        long timestamp = System.currentTimeMillis();
        if (ancestorImageId != null) {
            Fingerprint f = make(run, ancestorImageId);
            Collection<FingerprintFacet> facets = f.getFacets();
            DockerDescendantFingerprintFacet descendantFacet = null;
            for (FingerprintFacet facet : facets) {
                if (facet instanceof DockerDescendantFingerprintFacet) {
                    descendantFacet = (DockerDescendantFingerprintFacet) facet;
                    break;
                }
            }
            BulkChange bc = new BulkChange(f);
            try {
                if (descendantFacet == null) {
                    descendantFacet = new DockerDescendantFingerprintFacet(f, timestamp, ancestorImageId);
                    facets.add(descendantFacet);
                }
                descendantFacet.addDescendantImageId(descendantImageId);
                descendantFacet.addFor(run);
                DockerFingerprintAction.addToRun(f, ancestorImageId, run);
                bc.commit();
            } finally {
                bc.abort();
            }
        }
        Fingerprint f = make(run, descendantImageId);
        Collection<FingerprintFacet> facets = f.getFacets();
        DockerAncestorFingerprintFacet ancestorFacet = null;
        for (FingerprintFacet facet : facets) {
            if (facet instanceof DockerAncestorFingerprintFacet) {
                ancestorFacet = (DockerAncestorFingerprintFacet) facet;
                break;
            }
        }
        BulkChange bc = new BulkChange(f);
        try {
            if (ancestorFacet == null) {
                ancestorFacet = new DockerAncestorFingerprintFacet(f, timestamp, descendantImageId);
                facets.add(ancestorFacet);
            }
            if (ancestorImageId != null) {
                ancestorFacet.addAncestorImageId(ancestorImageId);
            }
            ancestorFacet.addFor(run);
            DockerFingerprintAction.addToRun(f, descendantImageId, run);
            bc.commit();
        } finally {
            bc.abort();
        }
    }

}
