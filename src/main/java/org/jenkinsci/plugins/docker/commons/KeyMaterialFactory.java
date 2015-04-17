package org.jenkinsci.plugins.docker.commons;

import hudson.model.AbstractBuild;
import org.jenkinsci.plugins.docker.commons.impl.CompositeKeyMaterialFactory;
import org.jenkinsci.plugins.docker.commons.impl.NullKeyMaterialFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;

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
        KeyMaterialFactory[] tmp = new KeyMaterialFactory[factories.length + 1];
        tmp[0] = this;
        System.arraycopy(factories, 0, tmp, 1, factories.length);
        return new CompositeKeyMaterialFactory(tmp);
    }

    public static final KeyMaterialFactory NULL = new NullKeyMaterialFactory();
}
