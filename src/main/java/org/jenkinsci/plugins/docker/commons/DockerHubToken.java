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

        // look for subtypes that know how to create a token, such as Google Container Registry
        DockerHubCredentials v = firstOrNull(CredentialsProvider.lookupCredentials(
                DockerHubCredentials.class, context, Jenkins.getAuthentication(),
                Collections.<DomainRequirement>emptyList()),
            withId(credentialId));
        if (v!=null)
            return v.getToken();

        // allow the plain username/password token and treat it like how DockerHub turns it into a token,
        UsernamePasswordCredentials w = firstOrNull(CredentialsProvider.lookupCredentials(
                UsernamePasswordCredentials.class, context, Jenkins.getAuthentication(),
                Collections.<DomainRequirement>emptyList()),
            withId(credentialId));
        if (w!=null)
            return new DockerHubToken(w.getUsername(),
                    Base64.encodeBase64String((w.getUsername() + ":" + w.getPassword().getPlainText()).getBytes(UTF8)));

        return null;
    }

    private static final Charset UTF8 = Charset.forName("UTF-8");
}
