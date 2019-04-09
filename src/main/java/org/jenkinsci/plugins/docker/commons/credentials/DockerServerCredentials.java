/*
 * The MIT License
 *
 * Copyright (c) 2015, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.docker.commons.credentials;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.Util;
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
    private final Secret clientCertificate;
    @CheckForNull 
    private final Secret serverCaCertificate;

    @Deprecated
    public DockerServerCredentials(CredentialsScope scope, String id, String description,
                                   @CheckForNull String clientKey, @CheckForNull String clientCertificate,
                                   @CheckForNull String serverCaCertificate) {
        super(scope, id, description);
        this.clientKey = Util.fixEmptyAndTrim(clientKey) == null ? null : Secret.fromString(clientKey);
        this.clientCertificate = Util.fixEmptyAndTrim(clientCertificate) == null ? null : Secret.fromString(clientCertificate);
        this.serverCaCertificate = Util.fixEmptyAndTrim(serverCaCertificate) == null ? null : Secret.fromString(serverCaCertificate);
    }

    @DataBoundConstructor
    public DockerServerCredentials(CredentialsScope scope, String id, String description,
                                   @CheckForNull Secret clientKey, @CheckForNull Secret clientCertificate,
                                   @CheckForNull Secret serverCaCertificate) {
        super(scope, id, description);
        this.clientKey = clientKey;
        this.clientCertificate = clientCertificate;
        this.serverCaCertificate = serverCaCertificate;
    }

    /**
     * Gets the PEM formatted secret key to identify the client. The {@code --tlskey} option in docker(1)
     *
     * @return null if there's no authentication
     */
    @CheckForNull
    public Secret getClientKey() {
        return clientKey;
    }

    /**
     * Gets the PEM formatted client certificate.
     * The {@code --tlscert} option in docker(1).
     *
     * @return null if there's no authentication
     */
    @CheckForNull 
    public Secret getClientCertificate() {
        return clientCertificate;
    }

    /**
     * Gets the PEM formatted server certificate.
     * The {@code --tlscacert} option in docker(1).
     *
     * @return null if there's no authentication
     */
    @CheckForNull 
    public Secret getServerCaCertificate() {
        return serverCaCertificate;
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {
        @Override
        public String getDisplayName() {
            return "Docker Host Certificate Authentication";
        }

    }

}
