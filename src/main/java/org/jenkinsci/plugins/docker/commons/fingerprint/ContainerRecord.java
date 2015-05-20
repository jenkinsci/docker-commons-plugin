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

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Records a container started from a known image.
 * 
 * Key properties out of "docker inspect" limited to a subset because fingerprints are performance sensitive.
 * We may opt to store the whole JSON but that's probably need a better data store.
 *  
 * @author Kohsuke Kawaguchi
 * @see DockerRunFingerprintFacet 
 */
public class ContainerRecord implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String host;
    private final String containerId;
    private transient String imageId;
    private final String containerName;

    private final long created;
    private final Map<String,String> tags;

    public ContainerRecord(@Nonnull String host, @Nonnull String containerId, @Nonnull String imageId, @Nonnull String containerName, long created, @Nonnull Map<String,String> tags) {
        this.host = host;
        this.containerId = containerId;
        this.imageId = imageId;
        this.containerName = containerName;
        this.created = created;
        this.tags = new HashMap<String,String>(tags);
    }

    /**
     * The host name on which the container is run. 
     */
    public String getHost() {
        return host;
    }

    /**
     * 64byte sha1 container ID. 
     */
    public String getContainerId() {
        return containerId;
    }

    /**
     * Get the ID of the image from which this container was started.
     * @return The ID of the image from which this container was started.
     */
    public String getImageId() {
        return imageId;
    }

    /**
     * Set the image ID of the image from which this container was started.
     * @param imageId The image ID of the image from which this container was started.
     */
    public void setImageId(@Nonnull String imageId) {
        this.imageId = imageId;
    }

    /**
     * Human readable container name. 
     */
    public String getContainerName() {
        return containerName;
    }

    /**
     * When was this container created? 
     */
    public long getCreated() {
        return created;
    }

    /**
     * Additional user-specified context information submitted from clients.
     */
    public Map<String, String> getTags() {
        return Collections.unmodifiableMap(tags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContainerRecord that = (ContainerRecord) o;
        return containerId.equals(that.containerId);

    }

    @Override
    public int hashCode() {
        return containerId.hashCode();
    }
}
