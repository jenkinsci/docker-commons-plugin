package org.jenkinsci.plugins.docker.commons.impl;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.authentication.tokens.api.AuthenticationTokenException;
import jenkins.authentication.tokens.api.AuthenticationTokenSource;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.Charsets;
import org.jenkinsci.plugins.docker.commons.DockerRegistryToken;

@Extension
public class UsernamePasswordDockerRegistryTokenSource extends
        AuthenticationTokenSource<DockerRegistryToken, UsernamePasswordCredentials> {
    public UsernamePasswordDockerRegistryTokenSource() {
        super(DockerRegistryToken.class, UsernamePasswordCredentials.class);
    }

    @NonNull
    @Override
    public DockerRegistryToken convert(UsernamePasswordCredentials c) throws AuthenticationTokenException {
        return new DockerRegistryToken(c.getUsername(),
                Base64.encodeBase64String((c.getUsername() + ":" + c.getPassword().getPlainText())
                        .getBytes(Charsets.UTF_8)));
    }
}
