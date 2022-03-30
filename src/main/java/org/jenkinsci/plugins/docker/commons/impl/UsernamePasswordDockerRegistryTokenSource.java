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
package org.jenkinsci.plugins.docker.commons.impl;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.authentication.tokens.api.AuthenticationTokenException;
import jenkins.authentication.tokens.api.AuthenticationTokenSource;
import org.apache.commons.codec.binary.Base64;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryToken;

import java.nio.charset.StandardCharsets;

@Extension
public class UsernamePasswordDockerRegistryTokenSource extends
        AuthenticationTokenSource<DockerRegistryToken, UsernamePasswordCredentials> {
    public UsernamePasswordDockerRegistryTokenSource() {
        super(DockerRegistryToken.class, UsernamePasswordCredentials.class);
    }

    @NonNull
    @Override
    public DockerRegistryToken convert(UsernamePasswordCredentials c) throws AuthenticationTokenException {
        return new DockerRegistryToken(c.getUsername(),
                Base64.encodeBase64String((c.getUsername() + ":" + c.getPassword().getPlainText())
                        .getBytes(StandardCharsets.UTF_8)));
    }
}
