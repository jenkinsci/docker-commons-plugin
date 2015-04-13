package org.jenkinsci.plugins.docker.commons.fingerprint;

import hudson.model.Fingerprint;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * As a facet on a fingerprint that represents a docker image,
 * indicates that containers are started from the said image.
 * 
 *
 * @author Kohsuke Kawaguchi
 */
public class DockerRunFingerprintFacet extends DockerRunPtrFingerprintFacet {
    public final CopyOnWriteArrayList<ContainerRecord> records = new CopyOnWriteArrayList<ContainerRecord>();

    DockerRunFingerprintFacet(Fingerprint fingerprint, long timestamp, String imageId) {
        super(fingerprint, timestamp, imageId);
    }

    public void add(ContainerRecord r) throws IOException {
        for (ContainerRecord e : records) {
            if (e.equals(r))
                return;
        }
        records.add(r);
        getFingerprint().save();
    }
}
