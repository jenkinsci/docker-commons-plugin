package org.jenkinsci.plugins.docker.commons.impl;

import org.jenkinsci.plugins.docker.commons.KeyMaterial;
import org.jenkinsci.plugins.docker.commons.KeyMaterialFactory;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.IOException;

/**
 * {@link org.jenkinsci.plugins.docker.commons.KeyMaterialFactory} that does nothing.
 *
 * @see org.jenkinsci.plugins.docker.commons.KeyMaterial#NULL
 * @author Kohsuke Kawaguchi
 */
@Restricted(NoExternalUse.class)
public final class NullKeyMaterialFactory extends KeyMaterialFactory {

    private static final long serialVersionUID = 1L;

    @Override
    public KeyMaterial materialize() throws IOException, InterruptedException {
        return KeyMaterial.NULL;
    }
}
