package org.jenkinsci.plugins.docker.commons.impl;

import com.cloudbees.plugins.credentials.Credentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.authentication.tokens.api.AuthenticationTokenException;
import jenkins.authentication.tokens.api.AuthenticationTokenSource;
import org.jenkinsci.plugins.docker.commons.DockerServerCredentials;
import org.jenkinsci.plugins.docker.commons.KeyMaterialFactory;

/**
 * @author Stephen Connolly
 */
@Extension
public class ServerKeyMaterialFactoryFromDockerCredentials extends AuthenticationTokenSource<KeyMaterialFactory, DockerServerCredentials> {
    
    public ServerKeyMaterialFactoryFromDockerCredentials() {
        super(KeyMaterialFactory.class, DockerServerCredentials.class);
    }

    @NonNull
    @Override
    public KeyMaterialFactory convert(@NonNull DockerServerCredentials credential) throws AuthenticationTokenException {
        return new ServerKeyMaterialFactory(credential);
    }
}
