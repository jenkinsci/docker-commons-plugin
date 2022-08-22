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
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.slaves.WorkspaceList;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import jenkins.security.MasterToSlaveCallable;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.docker.commons.impl.RegistryKeyMaterialFactory;
import org.jenkinsci.plugins.docker.commons.tools.DockerTool;

/**
 * Represents an authentication token that docker(1) understands when pushing/pulling
 * from a docker registry. Obtained from various {@link Credentials} implementations via {@link AuthenticationTokens}.
 *
 * @author Kohsuke Kawaguchi
 * @see AuthenticationTokens
 */
public final class DockerRegistryToken implements Serializable {
    private static final long serialVersionUID = 1L;
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
     * @deprecated use {@link #newKeyMaterialFactory(URL, FilePath, Launcher, EnvVars, TaskListener, String)}
     */
    @Deprecated
    public KeyMaterialFactory newKeyMaterialFactory(final URL endpoint, @Nonnull VirtualChannel target) throws InterruptedException, IOException {
        return newKeyMaterialFactory(endpoint, target, null, TaskListener.NULL);
    }

    /**
     * @deprecated use {@link #newKeyMaterialFactory(URL, FilePath, Launcher, EnvVars, TaskListener, String)}
     */
    @Deprecated
    public KeyMaterialFactory newKeyMaterialFactory(@Nonnull URL endpoint, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener, @Nonnull String dockerExecutable) throws InterruptedException, IOException {
        return newKeyMaterialFactory(endpoint, workspace, launcher, new EnvVars(), listener, dockerExecutable);
    }

    /** Kill switch in case {@code docker login} via {@link RegistryKeyMaterialFactory} does not work. */
    @SuppressWarnings("FieldMayBeFinal")
    private static /* not final */ boolean USE_CUSTOM_LOGIN = Boolean.getBoolean(DockerRegistryToken.class.getName() + ".USE_CUSTOM_LOGIN");

    /**
     * Sets up an environment logged in to the specified Docker registry.
     * @param dockerExecutable as in {@link DockerTool#getExecutable}, with a 1.8+ client
     */
    public KeyMaterialFactory newKeyMaterialFactory(@Nonnull URL endpoint, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull EnvVars env, @Nonnull TaskListener listener, @Nonnull String dockerExecutable) throws InterruptedException, IOException {
        if (!USE_CUSTOM_LOGIN) {
            try {
                // see UsernamePasswordDockerRegistryTokenSource for example
                String usernameColonPassword = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
                int colon = usernameColonPassword.indexOf(':');
                if (colon > 0) {
                    FilePath tempDir = WorkspaceList.tempDir(workspace);
                    if (tempDir != null) {
                        return new RegistryKeyMaterialFactory(usernameColonPassword.substring(0, colon), usernameColonPassword.substring(colon + 1), endpoint, launcher, env, listener, dockerExecutable).
                                contextualize(new KeyMaterialContext(tempDir));
                    } else {
                        listener.getLogger().println("Failed to create temporary directory for docker login");
                    }
                }
            } catch (IllegalArgumentException x) {
                // not Base64-encoded
            }
            listener.getLogger().println("Warning: authentication token does not look like a username:password; falling back to direct manipulation of Docker configuration files");
        }
        return newKeyMaterialFactory(endpoint, workspace.getChannel(), launcher, listener);
    }

    /**
     * Makes the credentials available locally and returns {@link KeyMaterialFactory} that gives you the parameters
     * needed to access it.
     *
     * This is done by inserting the token into {@code ~/.dockercfg}
     * @deprecated use {@link #newKeyMaterialFactory(URL, FilePath, Launcher, EnvVars, TaskListener, String)}
     */
    @Deprecated
    public KeyMaterialFactory newKeyMaterialFactory(final @Nonnull URL endpoint, @Nonnull VirtualChannel target, @CheckForNull Launcher launcher, final @Nonnull TaskListener listener) throws InterruptedException, IOException {
        target.call(new MasterToSlaveCallable<Void, IOException>() {
            /**
             * Insert the token into {@code ~/.dockercfg}
             */
            @Override
            public Void call() throws IOException {
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
                    listener.getLogger().println("Wrote authentication to " + config);
                }
                return null;
            }
        });

        // risky to clean up ~/.dockercfg as multiple builds might try to use the same credentials
        return KeyMaterialFactory.NULL;
    }

}
