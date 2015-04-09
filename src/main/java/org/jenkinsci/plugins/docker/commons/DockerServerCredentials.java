package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Item;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.bouncycastle.openssl.PEMReader;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;

/**
 * Represents the connection details to talk to a docker host, which involves
 * endpoint URI, optional client secret key &amp; certificate, and optional CA certificate
 * to verify the server.
 *  
 * TODO: implement {@link StandardCertificateCredentials} but not immediately necessary.
 *  
 * @author Kohsuke Kawaguchi
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

    /**
     * Plugins that want to refer to a {@link DockerServerCredentials} should do so via ID string,
     * and use this method to resolve it to {@link DockerServerCredentials}.
     *
     * @param context
     *       If you are a build step trying to access DockerHub in the context of a build/job,
     *       specify that job. Otherwise null. If you are scoped to something else, you might
     *       have to interact with {@link CredentialsProvider} directly.
     */
    public static @CheckForNull
    DockerServerCredentials get(String id, Item context) {
        // as a build step, your access to credentials are constrained by what the build
        // can access, hence Jenkins.getAuthentication()
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        DockerServerCredentials.class, context, Jenkins.getAuthentication(),
                        Collections.<DomainRequirement>emptyList()),
                CredentialsMatchers.withId(id)
        );
    }
}
