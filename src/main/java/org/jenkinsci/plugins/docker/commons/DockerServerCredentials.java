package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.util.Secret;
import org.bouncycastle.openssl.PEMReader;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

/**
 * Represents the connection details to talk to a docker host, which involves
 * endpoint URI, optional client secret key &amp; certificate, and optional CA certificate
 * to verify the server.
 *  
 * @author Kohsuke Kawaguchi
 * @see DockerServerEndpoint
 */
public abstract class DockerServerCredentials extends BaseStandardCredentials implements StandardCertificateCredentials {
    protected DockerServerCredentials(CredentialsScope scope, String id, String description) {
        super(scope, id, description);
    }

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

    @NonNull
    @Override
    public KeyStore getKeyStore() {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.setKeyEntry("default", getClientSecretKey(), null,
                    new Certificate[] {getClientCertificate()});
            return ks;
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Unable to load key into keystore",e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to load key into keystore",e);
        }
    }

    @NonNull
    @Override
    public Secret getPassword() {
        return Secret.fromString("");    // fixed since docker can't handle it
    }
}
