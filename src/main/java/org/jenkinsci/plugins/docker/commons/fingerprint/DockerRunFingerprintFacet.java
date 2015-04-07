package org.jenkinsci.plugins.docker.commons.fingerprint;

import hudson.model.Fingerprint;
import jenkins.model.FingerprintFacet;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * As a facet on a fingerprint that represents a docker image,
 * indicates that containers are started from the said image.
 * 
 *
 * @author Kohsuke Kawaguchi
 */
public class DockerRunFingerprintFacet extends FingerprintFacet {
    public final CopyOnWriteArrayList<ContainerRecord> records = new CopyOnWriteArrayList<ContainerRecord>();

    public DockerRunFingerprintFacet(Fingerprint fingerprint, long timestamp) {
        super(fingerprint, timestamp);
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
