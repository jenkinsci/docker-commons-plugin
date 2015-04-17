package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;

/**
 * Represents the connection details to talk to a docker host, which involves
 * endpoint URI, optional client secret key &amp; certificate, and optional CA certificate
 * to verify the server.
 *
 * @author Kohsuke Kawaguchi
 * @see DockerServerEndpoint
 */
// TODO migrate to a standard type once we have a certificate type that we can migrate to
public class DockerServerCredentials extends BaseStandardCredentials {

    @CheckForNull 
    private final Secret clientKey;
    @CheckForNull 
    private final String clientCertificate;
    @CheckForNull 
    private final String serverCaCertificate;

    @DataBoundConstructor
    public DockerServerCredentials(CredentialsScope scope, String id, String description,
                                   @CheckForNull String clientKey, @CheckForNull String clientCertificate,
                                   @CheckForNull String serverCaCertificate) {
        super(scope, id, description);
        this.clientKey = clientKey == null ? null : Secret.fromString(clientKey);
        this.clientCertificate = clientCertificate;
        this.serverCaCertificate = serverCaCertificate;
    }

    /**
     * Gets the PEM formatted secret key to identify the client. The {@code --tlskey} option in docker(1)
     *
     * @return null if there's no authentication
     */
    @CheckForNull
    public String getClientKey() {
        return clientKey == null ? null : clientKey.getPlainText();
    }

    /**
     * Gets the PEM formatted client certificate.
     * The {@code --tlscert} option in docker(1).
     *
     * @return null if there's no authentication
     */
    @CheckForNull 
    public String getClientCertificate() {
        return clientCertificate;
    }

    /**
     * Gets the PEM formatted server certificate.
     * The {@code --tlscacert} option in docker(1).
     *
     * @return null if there's no authentication
     */
    @CheckForNull 
    public String getServerCaCertificate() {
        return serverCaCertificate;
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {
        @Override
        public String getDisplayName() {
            return "Docker Server Certificate Authentication";
        }

    }

}
