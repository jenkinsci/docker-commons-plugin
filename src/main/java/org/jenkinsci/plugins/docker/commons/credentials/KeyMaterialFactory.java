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
package org.jenkinsci.plugins.docker.commons.credentials;

import hudson.model.AbstractBuild;
import org.jenkinsci.plugins.docker.commons.impl.CompositeKeyMaterialFactory;
import org.jenkinsci.plugins.docker.commons.impl.NullKeyMaterialFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a locally extracted credentials information.
 *
 * <p>
 * Whenever you want to fork off docker directly or indirectly, use this object to set up environment variables
 * so that docker will talk to the right daemon.
 *
 * @author Kohsuke Kawaguchi
 * @see DockerServerEndpoint#newKeyMaterialFactory(AbstractBuild)
 * @see DockerRegistryEndpoint#newKeyMaterialFactory(AbstractBuild)
 */
public abstract class KeyMaterialFactory implements Serializable {

    /**
     * Ensure consistent serialization.
     */
    private static final long serialVersionUID = 1L;
    
    private /* write once */ KeyMaterialContext context;
    
    protected synchronized void checkContextualized() {
        if (context == null) {
            throw new IllegalStateException("KeyMaterialFactories must be contextualized before use");
        }
    }

    /**
     * Sets the {@link KeyMaterialContext} within which this {@link KeyMaterialFactory} can {@link #materialize()}
     * {@link KeyMaterial} instances. Can only be called once. 
     * @param context the {@link KeyMaterialContext}.
     * @return must return {@code this} (which is only returned to simplify use via method chaining)
     */
    public synchronized KeyMaterialFactory contextualize(@Nonnull KeyMaterialContext context) {
        if (this.context != null) {
            throw new IllegalStateException("KeyMaterialFactories cannot be re-contextualized");
        }
        this.context = context;
        return this;
    }

    @Nonnull
    protected synchronized KeyMaterialContext getContext() {
        checkContextualized();
        return context;
    }

    /**
     * Builds the key material environment variables needed to be passed when docker runs, to access
     * {@link DockerServerCredentials} that this object was created from.
     * 
     * <p>
     * When you are done using the credentials, call {@link KeyMaterial#close()} to allow sensitive 
     * information to be removed from the disk.
     */
    public abstract KeyMaterial materialize() throws IOException, InterruptedException;

    /**
     * Merge additional {@link KeyMaterialFactory}s into one.
     */
    public KeyMaterialFactory plus(@Nullable KeyMaterialFactory... factories) {
        if (factories == null || factories.length == 0) {
            return this;
        }
        List<KeyMaterialFactory> tmp = new ArrayList<KeyMaterialFactory>(factories.length + 1);
        tmp.add(this);
        for (KeyMaterialFactory f: factories) {
            if (f != null) tmp.add(f);
        }
        return new CompositeKeyMaterialFactory(tmp.toArray(new KeyMaterialFactory[tmp.size()]));
    }

    public static final KeyMaterialFactory NULL = new NullKeyMaterialFactory();
}
