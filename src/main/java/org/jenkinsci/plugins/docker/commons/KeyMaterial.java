package org.jenkinsci.plugins.docker.commons;

import hudson.EnvVars;
import hudson.FilePath;

import java.io.Closeable;
import java.io.IOException;

/**
 * Represents a locally extracted copies of key/certificates that can be used to invoke docker.
 *  
 * <p>
 * Whenever you want to fork off docker directly or indirectly, use this object to set up environment variables
 * so that docker will talk to the right daemon.
 *
 * @author Kohsuke Kawaguchi
 * @see DockerServerCredentials#materialize(FilePath)
 */
public interface KeyMaterial extends Closeable {
    /**
     * Builds the environment variables needed to be passed when docker runs, to access
     * {@link DockerServerCredentials} that this object was created from.
     */
    EnvVars buildEnvironments();

    /**
     * Deletes the key materials from the file system. As key materials are copied into files
     * every time {@link KeyMaterial} is created, it must be also cleaned up each time. 
     */
    void close() throws IOException;
}
