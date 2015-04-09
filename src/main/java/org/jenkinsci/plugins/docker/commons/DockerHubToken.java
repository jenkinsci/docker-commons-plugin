package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.model.Item;
import jenkins.model.Jenkins;
import org.apache.commons.codec.binary.Base64;

import javax.annotation.CheckForNull;
import java.nio.charset.Charset;
import java.util.Collections;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.*;

/**
 * Represents an authentication token that docker(1) understands when pushing/pulling
 * from a docker registry.
 *
 * @author Kohsuke Kawaguchi
 */
public final class DockerHubToken {
    private final String email;
    private final String token;

    public DockerHubToken(String email, String token) {
        this.email = email;
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }
}
