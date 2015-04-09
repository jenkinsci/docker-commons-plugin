package org.jenkinsci.plugins.docker.commons.impl;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import hudson.util.Secret;
import org.apache.commons.codec.binary.Base64;
import org.jenkinsci.plugins.docker.commons.DockerHubCredentials;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;

/**
 * (Probably most typical) {@link DockerHubCredentials} implementation that retains
 * username and password.
 *
 * @author Kohsuke Kawaguchi
 */
public class PlaintextDockerHubCredentials extends DockerHubCredentials implements UsernamePasswordCredentials {
    private final String username;
    private final Secret password;

    public PlaintextDockerHubCredentials(CredentialsScope scope, String id, String description, String username, Secret password) {
        super(scope, id, description);
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public Secret getPassword() {
        return password;
    }

    /**
     * In current docker, token is username:password in base64 encoded form.
     */
    @Nonnull @Override
    public String getToken() {
        return Base64.encodeBase64String((username+":"+password.getPlainText()).getBytes(UTF8));
    }

    private static final Charset UTF8 = Charset.forName("UTF-8");
}
