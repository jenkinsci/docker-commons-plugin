package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import org.bouncycastle.openssl.PEMReader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

/**
 * Represents the connection details to talk to a docker host, which involves
 * endpoint URI, optional client secret key &amp; certificate, and optional CA certificate
 * to verify the server.
 *  
 * TODO: implement {@link CertificateCredentials} but not immediately necessary.
 *  
 * @author Kohsuke Kawaguchi
 */
public abstract class DockerHostCredentials extends BaseStandardCredentials {
    protected DockerHostCredentials(CredentialsScope scope, String id, String description) {
        super(scope, id, description);
    }

    /**
     * Endpoint URL, such as {@code unix:///var/run/docker.sock}. The {@code -H} option in docker(1).
     */
    public abstract @Nonnull URI getEndpoint();

    /**
     * Gets the PEM formatted secret key to identify the client. The {@code --tlskey} option in docker(1)
     * 
     * @return null if there's no authentication
     */
    public abstract @Nullable String getClientSecretKeyInPEM();

    public RSAPublicKey getClientSecretKey() throws IOException {
        String v = getClientSecretKeyInPEM();
        if (v==null)    return null;
        Object x = new PEMReader(new StringReader(v)).readObject();
        return (RSAPublicKey)((KeyPair) x).getPublic();
    }
    
    /**
     * Gets the PEM formatted client certificate that matches with {@link #getClientSecretKey()}.
     * The {@code --tlscert} option in docker(1). 
     * 
     * @return null if there's no authentication
     */
    public abstract @Nullable String getClientCertificateInPEM();

    public X509Certificate getClientCertificate() throws IOException {
        String v = getClientCertificateInPEM();
        if (v==null)    return null;
        Object x = new PEMReader(new StringReader(v)).readObject();
        return (X509Certificate)x;
    }

    /**
     * Gets the PEM formatted server certificate that matches with {@link #getClientSecretKey()}.
     * The {@code --tlscacert} option in docker(1).
     *
     * @return null if there's no authentication
     */
    public abstract @Nullable String getServerCaCertificateInPEM();
    
    public X509Certificate getServerCaCertificate() throws IOException {
        String v = getServerCaCertificateInPEM();
        if (v==null)    return null;
        Object x = new PEMReader(new StringReader(v)).readObject();
        return (X509Certificate)x;
    }
}
