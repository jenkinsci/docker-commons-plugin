/*
 * The MIT License
 *
 * Copyright (c) 2015, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.docker.commons.fingerprint;

import hudson.model.Fingerprint;
import hudson.model.Run;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import jenkins.model.FingerprintFacet;
import jenkins.model.RunAction2;
import org.jenkinsci.plugins.docker.commons.Messages;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.Facet;

/**
 * Adds a link, which provides a list of fingerprints with
 * {@link DockerRunPtrFingerprintFacet}s.
 *
 * @author Oleg Nenashev
 */
public class DockerFingerprintAction implements RunAction2 {

    private final Set<String> imageIDs;
    transient Run<?, ?> run;

    public DockerFingerprintAction() {
        this.imageIDs = new HashSet<String>();
    }

    public Run<?, ?> getRun() {
        return run;
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        run = r;
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        run = r;
    }

    @Override
    public String getIconFileName() {
        return "fingerprint.png";
    }

    @Override
    public String getDisplayName() {
        return Messages.DockerFingerprintAction_DisplayName();
    }

    @Override
    public String getUrlName() {
        return "docker-fingerprints";
    }

    /**
     * Gets a list of fingerprints related to the action
     *
     * @return Unmodifiable set of items.
     */
    public Set<String> getImageIDs() {
        return Collections.unmodifiableSet(imageIDs);
    }

    @Restricted(NoExternalUse.class)
    public @CheckForNull String getFingerprintHash(@CheckForNull String imageId) {
        return (imageId != null) ? DockerFingerprints.getImageFingerprintHash(imageId) : null;
    }
    
    @Restricted(NoExternalUse.class)
    public @CheckForNull Fingerprint getFingerprint(@CheckForNull String imageId) {
        if (imageId == null) {
            return null;
        }
        
        try {
            return DockerFingerprints.of(imageId);
        } catch (IOException ex) {
            return null; // nothing to do in web UI - return null as well
        }
    }
    
    public List<DockerFingerprintFacet> getDockerFacets(String imageId) {
        List<DockerFingerprintFacet> res = new LinkedList<DockerFingerprintFacet>();
        final Fingerprint fp = getFingerprint(imageId);
        if (fp != null) {
            for (final FingerprintFacet f : fp.getFacets()) {
                if (f instanceof DockerFingerprintFacet) {
                    res.add((DockerFingerprintFacet) f);
                }
            }
        }
        return res;
    }

    /**
     * Adds an action with a reference to fingerprint if required. It's
     * recommended to call the method from {
     *
     * @BulkChange} transaction to avoid saving the {@link Run} multiple times.
     * @param fp Fingerprint
     * @param imageId ID of the docker image
     * @param run Run to be updated
     * @throws IOException Cannot save the action
     */
    static void addToRun(Fingerprint fp, String imageId, Run run) throws IOException {
        synchronized (run) {
            DockerFingerprintAction action = run.getAction(DockerFingerprintAction.class);
            if (action == null) {
                action = new DockerFingerprintAction();
                run.addAction(action);
            }
            
            if (action.imageIDs.add(imageId)) {
                run.save();
            } // else no need to save updates
        }
    }
}
