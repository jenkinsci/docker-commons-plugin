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

import com.cloudbees.plugins.credentials.Credentials;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;

/**
 * Represents an authentication token that docker(1) understands when pushing/pulling
 * from a docker registry. Obtained from various {@link Credentials} implementations via {@link AuthenticationTokens}.
 *
 * @author Kohsuke Kawaguchi
 * @see AuthenticationTokens
 */
public final class DockerRegistryToken implements Serializable {
    private final String email;
    private final String token;

    public DockerRegistryToken(String email, String token) {
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
     * Makes the credentials available locally and returns {@link KeyMaterialFactory} that gives you the parameters
     * needed to access it.
     *
     * This is done by inserting the token into {@code ~/.dockercfg}
     */
    public KeyMaterialFactory newKeyMaterialFactory(final URL endpoint, @Nonnull VirtualChannel target) throws InterruptedException, IOException {
        target.call(new Callable<Void, IOException>() {
            /**
             * Insert the token into {@code ~/.dockercfg}
             */
            @Override
            public Void call() throws IOException {
                // TODO: TF: Should this not be done via docker login (possibly preceded by a logout) ?               

                JSONObject json;
                JSONObject auths;

                synchronized (DockerRegistryToken.class) {// feeble attempt at serializing access to ~/.dockercfg

                    File config = new File(System.getProperty("user.home"), ".docker/config.json");
                    if (config.exists()) {
                        json = JSONObject.fromObject(FileUtils.readFileToString(config, "UTF-8"));
                        auths = json.getJSONObject("auths");
                    } else {
                        config = new File(System.getProperty("user.home"), ".dockercfg");
                        if (config.exists()) {
                            auths = json = JSONObject.fromObject(FileUtils.readFileToString(config, "UTF-8"));
                        } else {
                            // Use legacy .dockercfg to ensure this works well with pre-1.7 docker client
                            // client will pick this one if .docker/config.json does not yet exists
                            auths = json = new JSONObject();
                        }
                    }
                    auths.put(endpoint.toString(), new JSONObject()
                            .accumulate("auth", getToken())
                            .accumulate("email", getEmail()));
                    
                    FileUtils.writeStringToFile(config, json.toString(2), "UTF-8");
                }
                return null;
            }
        });

        // risky to clean up ~/.dockercfg as multiple builds might try to use the same credentials
        return KeyMaterialFactory.NULL;
    }

    private static final long serialVersionUID = 1L;
}
