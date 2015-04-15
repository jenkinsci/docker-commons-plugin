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
    public static @CheckForNull Fingerprint of(String imageId) throws IOException {
        return Jenkins.getInstance().getFingerprintMap().get(trim(imageId));
    }

    private static @Nonnull Fingerprint make(Run<?,?> run, String imageId) throws IOException {
        return Jenkins.getInstance().getFingerprintMap().getOrCreate(run, "<docker-image>", trim(imageId));
    }

    /**
     * Adds a new {@link ContainerRecord} for the specified image, creating necessary intermediate objects as it goes.
     */
    public static void addRunFacet(String imageId, ContainerRecord record, Run<?,?> run) throws IOException {
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
     * Creates a new {@link DockerFromFingerprintFacet} and adds a run. Or adds to an existing one.
     */
    public static void addFromFacet(String imageId, Run<?,?> run) throws IOException {
        Fingerprint f = make(run, imageId);
        Collection<FingerprintFacet> facets = f.getFacets();
        DockerFromFingerprintFacet fromFacet = null;
        for (FingerprintFacet facet : facets) {
            if (facet instanceof DockerFromFingerprintFacet) {
                fromFacet = (DockerFromFingerprintFacet) facet;
                break;
            }
        }
        BulkChange bc = new BulkChange(f);
        try {
            if (fromFacet == null) {
                fromFacet = new DockerFromFingerprintFacet(f, System.currentTimeMillis(), imageId);
                facets.add(fromFacet);
            }
            fromFacet.addFor(run);
            bc.commit();
        } finally {
            bc.abort();
        }
    }

}
