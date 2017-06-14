package org.jenkinsci.plugins.docker.commons.credentials;

import java.io.IOException;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.credentialsbinding.BindingDescriptor;
import org.jenkinsci.plugins.credentialsbinding.impl.AbstractOnDiskBinding;
import org.jenkinsci.plugins.docker.commons.Messages;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.FilePath;

public class DockerServerCredentialsBinding extends AbstractOnDiskBinding<DockerServerCredentials> {

    @DataBoundConstructor
    public DockerServerCredentialsBinding(String variable, String credentialsId) {
        super(variable, credentialsId);
    }

    @Override
    protected Class<DockerServerCredentials> type() {
        return DockerServerCredentials.class;
    }

    @Override
    protected FilePath write(DockerServerCredentials credentials, FilePath dir) throws IOException, InterruptedException {
        FilePath clientKey = dir.child("key.pem");
        clientKey.write(credentials.getClientKey(), null);
        clientKey.chmod(0600);

        FilePath clientCert = dir.child("cert.pem");
        clientCert.write(credentials.getClientCertificate(), null);
        clientCert.chmod(0600);

        FilePath serverCACert = dir.child("ca.pem");
        serverCACert.write(credentials.getServerCaCertificate(), null);
        serverCACert.chmod(0600);

        return dir;
    }

    @Extension
    @Symbol("dockerCert")
    public static class DescriptorImpl extends BindingDescriptor<DockerServerCredentials> {

        @Override
        protected Class<DockerServerCredentials> type() {
            return DockerServerCredentials.class;
        }

        @Override
        public String getDisplayName() {
            return Messages.DockerServerCredentialsBinding_DisplayName();
        }

    }

}
