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
import javax.annotation.Nonnull;

/**
 * Facet representing the fact that this Docker image was derived from another.
 * This facet would be added to the fingerprint corresponding to an image built using a {@code Dockerfile}.
 * {@link #getAncestorImageIds} indicates the ID of the image specified in the {@code FROM} instruction.
 * @see DockerFingerprints#addFromFacet
 * @see DockerAncestorFingerprintFacet
 */
public class DockerAncestorFingerprintFacet extends DockerRunPtrFingerprintFacet {

    private final Set<String> ancestorImageIds = new TreeSet<String>();

    DockerAncestorFingerprintFacet(Fingerprint fingerprint, long timestamp, String imageId) {
        super(fingerprint, timestamp, imageId);
    }

    synchronized void addAncestorImageId(@Nonnull String id) {
        ancestorImageIds.add(id);
    }

    /**
     * Gets the ancestor image that this image was built from.
     * In principle there could be several, in case distinct {@code Dockerfile}s used distinct {@code FROM} images,
     * yet wound up producing the same result (because some corresponded to intermediate layers which were cached).
     * This is unlikely but possible.
     * The set may be empty in case you built a base image ({@code FROM scratch}), in which case there is no ID for the ancestor.
     * @return a set of 64-digit IDs, typically a singleton
     */
    public synchronized @Nonnull Set<String> getAncestorImageIds() {
        return new TreeSet<String>(ancestorImageIds);
    }

}
