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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * As a facet on a fingerprint that represents a docker image,
 * indicates that containers are started from the said image.
 * 
 *
 * @author Kohsuke Kawaguchi
 */
public class DockerRunFingerprintFacet extends DockerRunPtrFingerprintFacet {
    public final List<ContainerRecord> records = new CopyOnWriteArrayList<ContainerRecord>();

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
    
    public Object readResolve() {
        for (ContainerRecord record : records) {
            record.setImageId(getImageId());
        }
        return this;
    }
}
