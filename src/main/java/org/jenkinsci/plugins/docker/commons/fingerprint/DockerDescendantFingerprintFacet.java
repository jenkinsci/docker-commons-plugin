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
import java.util.Set;
import java.util.TreeSet;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Facet representing the fact that some Docker image was derived from this one.
 * This facet would be added to the fingerprint corresponding to the {@code FROM} instruction in a {@code Dockerfile}.
 * {@link #getDescendantImageIds} indicates the image that was built from that file.
 * @see DockerFingerprints#addFromFacet
 * @see DockerDescendantFingerprintFacet
 */
public class DockerDescendantFingerprintFacet extends DockerRunPtrFingerprintFacet {

    private final Set<String> descendantImageIds = new TreeSet<String>();

    DockerDescendantFingerprintFacet(Fingerprint fingerprint, long timestamp, String imageId) {
        super(fingerprint, timestamp, imageId);
    }

    synchronized void addDescendantImageId(@NonNull String id) {
        descendantImageIds.add(id);
    }

    /**
     * Gets the descendant images built from this image.
     * @return a set of 64-digit IDs, never empty
     */
    public synchronized @NonNull Set<String> getDescendantImageIds() {
        return new TreeSet<String>(descendantImageIds);
    }

}
