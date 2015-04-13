package org.jenkinsci.plugins.docker.commons.fingerprint;

import hudson.model.Fingerprint;

/**
 * As a facet on a fingerprint that represents a docker image,
 * indicates that another image was produced by deriving from it. See "FROM" in Dockerfile.
 *  
 * @author Kohsuke Kawaguchi
 */
public class DockerFromFingerprintFacet extends DockerRunPtrFingerprintFacet {
    DockerFromFingerprintFacet(Fingerprint fingerprint, long timestamp, String imageId) {
        super(fingerprint, timestamp, imageId);
    }
}
