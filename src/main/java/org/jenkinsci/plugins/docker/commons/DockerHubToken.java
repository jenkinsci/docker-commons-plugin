package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.model.Item;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.docker.commons.impl.PlaintextDockerHubCredentials;

import javax.annotation.CheckForNull;
import java.util.Collections;

/**
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

    /**
     * Plugins that want to refer to a {@link DockerHubCredentials} should do so via ID string,
     * and use this method to resolve it to {@link DockerHubCredentials}.
     *
     * @param context
     *       If you are a build step trying to access DockerHub in the context of a build/job,
     *       specify that job. Otherwise null. If you are scoped to something else, you might
     *       have to interact with {@link CredentialsProvider} directly.
     */
    public static @CheckForNull
    DockerHubToken get(String credentialId, Item context) {
        // as a build step, your access to credentials are constrained by what the build
        // can access, hence Jenkins.getAuthentication()
        DockerHubCredentials v = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        DockerHubCredentials.class, context, Jenkins.getAuthentication(),
                        Collections.<DomainRequirement>emptyList()),
                CredentialsMatchers.withId(credentialId)
        );
        if (v!=null)
            return new DockerHubToken(v.getEmail(), v.getToken());

        // accept the plain username/password token
        UsernamePasswordCredentials v2 = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        UsernamePasswordCredentials.class, context, Jenkins.getAuthentication(),
                        Collections.<DomainRequirement>emptyList()),
                CredentialsMatchers.withId(credentialId)
        );
        if (v2!=null)
            return new PlaintextDockerHubCredentials(null,null,null,v2.getUsername(), v2.getPassword()).getToken();

        return null;
    }
}
