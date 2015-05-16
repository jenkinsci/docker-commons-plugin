package org.jenkinsci.plugins.docker.commons.fingerprint;

import hudson.model.Fingerprint;
import hudson.model.Fingerprint.RangeSet;
import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.FingerprintFacet;

import java.io.IOException;
import java.util.Hashtable;

/**
 * {@link FingerprintFacet} for docker that refers to other {@link Run}s.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class DockerRunPtrFingerprintFacet extends DockerFingerprintFacet {

    private final String imageId;
    /**
     * Range of builds that use this as base image keyed by a job full name.
     */
    private final Hashtable<String,RangeSet> usages = new Hashtable<String,RangeSet>();

    DockerRunPtrFingerprintFacet(Fingerprint fingerprint, long timestamp, String imageId) {
        super(fingerprint, timestamp);
        this.imageId = imageId;
    }

    public String getImageId() {
        return imageId;
    }

    public void addFor(Run<?,?> b) throws IOException {
        add(b.getParent().getFullName(), b.getNumber());
    }

    /**
     * Records that a build of a job has used this file.
     */
    public synchronized void add(String jobFullName, int n) throws IOException {
        addWithoutSaving(jobFullName, n);
        getFingerprint().save();
    }

    private synchronized void addWithoutSaving(String jobFullName, int n) {
        RangeSet r = usages.get(jobFullName);
        if(r==null) {
            r = new RangeSet();
            usages.put(jobFullName,r);
        }
        r.add(n);
    }

    public Hashtable<String,RangeSet> getUsages() {
        return usages;
    }

    /**
     * Gets the build range set for the given job name.
     *
     * <p>
     * These builds of this job has used this file.
     */
    public RangeSet getRangeSet(String jobFullName) {
        RangeSet r = usages.get(jobFullName);
        if(r==null) r = new RangeSet();
        return r;
    }

    public RangeSet getRangeSet(Job<?,?> job) {
        return getRangeSet(job.getFullName());
    }
}
