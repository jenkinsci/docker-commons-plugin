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
 * Entry point into fignerprint related functionalities in Docker.
 */
public class DockerFingerprints {
    private DockerFingerprints() {} // no instantiation

    private static String trim(String imageId) {
        if (imageId.length() != 64) {
            throw new IllegalArgumentException("Expecting 64char full image ID, but got " + imageId);
        }
        return imageId.substring(0, 32);
    }

    /**
     * Gets {@link Fingerprint} for a given docker image.
     */
    public static @CheckForNull Fingerprint of(@Nonnull String imageId) throws IOException {
        return Jenkins.getInstance().getFingerprintMap().get(trim(imageId));
    }

    private static @Nonnull Fingerprint make(@Nonnull Run<?,?> run, @Nonnull String imageId) throws IOException {
        return Jenkins.getInstance().getFingerprintMap().getOrCreate(run, "<docker-image>", trim(imageId));
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
            DockerAncestorFingerprintFacet fromFacet = null;
            for (FingerprintFacet facet : facets) {
                if (facet instanceof DockerAncestorFingerprintFacet) {
                    fromFacet = (DockerAncestorFingerprintFacet) facet;
                    break;
                }
            }
            BulkChange bc = new BulkChange(f);
            try {
                if (fromFacet == null) {
                    fromFacet = new DockerAncestorFingerprintFacet(f, timestamp, ancestorImageId, descendantImageId);
                    facets.add(fromFacet);
                }
                fromFacet.addFor(run);
                bc.commit();
            } finally {
                bc.abort();
            }
        }
        Fingerprint f = make(run, descendantImageId);
        Collection<FingerprintFacet> facets = f.getFacets();
        DockerDescendantFingerprintFacet fromFacet = null;
        for (FingerprintFacet facet : facets) {
            if (facet instanceof DockerDescendantFingerprintFacet) {
                fromFacet = (DockerDescendantFingerprintFacet) facet;
                break;
            }
        }
        BulkChange bc = new BulkChange(f);
        try {
            if (fromFacet == null) {
                fromFacet = new DockerDescendantFingerprintFacet(f, timestamp, descendantImageId, ancestorImageId);
                facets.add(fromFacet);
            }
            fromFacet.addFor(run);
            bc.commit();
        } finally {
            bc.abort();
        }
    }

}
