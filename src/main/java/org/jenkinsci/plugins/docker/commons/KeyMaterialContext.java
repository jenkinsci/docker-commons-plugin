package org.jenkinsci.plugins.docker.commons;

import hudson.FilePath;

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * Represents the context within a {@link KeyMaterialFactory} can {@link KeyMaterialFactory#materialize()}
 * {@link KeyMaterial} instances.
 *
 * @author Stephen Connolly
 */
public class KeyMaterialContext implements Serializable {
    private static final long serialVersionUID = 1L;

    @Nonnull
    private final FilePath baseDir;

    public KeyMaterialContext(@Nonnull FilePath baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Returns the base directory that can be used to {@link KeyMaterialFactory#materialize()}
     * {@link KeyMaterial} instances.
     *
     * @return the base directory.
     */
    @Nonnull
    public FilePath getBaseDir() {
        return baseDir;
    }
}
