package org.jenkinsci.plugins.docker.commons.fingerprint;

import hudson.model.Fingerprint;
import hudson.model.Run;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.Collection;
import jenkins.model.FingerprintFacet;

/**
 * @author Kohsuke Kawaguchi
 */
public class DockerFingerprints {
    private DockerFingerprints() {} // no instantiation

    /**
     * Gets {@link Fingerprint} for a given docker image.
     */
    public static Fingerprint of(String imageId) throws IOException {
        if (imageId.length()!=64)
            throw new IllegalArgumentException("Expecting 64char full image ID, but got "+imageId);

        return Jenkins.getInstance().getFingerprintMap().get(imageId.substring(0, 32));
    }

    public static void addRunFacet(String imageId, ContainerRecord record, Run<?,?> run) throws IOException {
        Fingerprint f = of(imageId);
        Collection<FingerprintFacet> facets = f.getFacets();
        DockerRunFingerprintFacet runFacet = null;
        for (FingerprintFacet facet : facets) {
            if (facet instanceof DockerRunFingerprintFacet) {
                runFacet = (DockerRunFingerprintFacet) facet;
                break;
            }
        }
        if (runFacet == null) {
            runFacet = new DockerRunFingerprintFacet(f, System.currentTimeMillis(), imageId);
            facets.add(runFacet);
        }
        runFacet.add(record);
        runFacet.addFor(run);
    }

    public static void addFromFacet(String imageId, Run<?,?> run) throws IOException {
        Fingerprint f = of(imageId);
        Collection<FingerprintFacet> facets = f.getFacets();
        DockerFromFingerprintFacet fromFacet = null;
        for (FingerprintFacet facet : facets) {
            if (facet instanceof DockerFromFingerprintFacet) {
                fromFacet = (DockerFromFingerprintFacet) facet;
                break;
            }
        }
        if (fromFacet == null) {
            fromFacet = new DockerFromFingerprintFacet(f, System.currentTimeMillis(), imageId);
            facets.add(fromFacet);
        }
        fromFacet.addFor(run);
    }

}
