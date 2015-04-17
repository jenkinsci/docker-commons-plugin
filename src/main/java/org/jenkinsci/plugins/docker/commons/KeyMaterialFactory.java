package org.jenkinsci.plugins.docker.commons;

import hudson.model.AbstractBuild;
import org.jenkinsci.plugins.docker.commons.impl.CompositeKeyMaterialFactory;
import org.jenkinsci.plugins.docker.commons.impl.NullKeyMaterialFactory;

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

    private static final long serialVersionUID = 1L;

    public static final KeyMaterialFactory NULL = new NullKeyMaterialFactory();
}
