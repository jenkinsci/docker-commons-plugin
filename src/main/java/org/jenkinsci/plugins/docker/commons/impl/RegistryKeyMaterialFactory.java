/*
 * The MIT License
 *
 * Copyright 2018 CloudBees, Inc.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.docker.commons.credentials.KeyMaterial;
import org.jenkinsci.plugins.docker.commons.credentials.KeyMaterialFactory;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import net.sf.json.JSONObject;

/**
 * Logs you in to a Docker registry.
 */
@Restricted(NoExternalUse.class)
public class RegistryKeyMaterialFactory extends KeyMaterialFactory {

    private static final String DOCKER_CONFIG_FILENAME = "config.json";
    private static final String[] BLACKLISTED_PROPERTIES = { "auths", "credsStore" };

    private final @NonNull String username;
    private final @NonNull String password;
    private final @NonNull URL endpoint;
    private final @NonNull Launcher launcher;
    private final @NonNull EnvVars env;
    private final @NonNull TaskListener listener;
    private final @NonNull String dockerExecutable;

    public RegistryKeyMaterialFactory(@NonNull String username, @NonNull String password, @NonNull URL endpoint, @NonNull Launcher launcher, @NonNull EnvVars env, @NonNull TaskListener listener, @NonNull String dockerExecutable) {
        this.username = username;
        this.password = password;
        this.endpoint = endpoint;
        this.launcher = launcher;
        this.env = env;
        this.listener = listener;
        this.dockerExecutable = dockerExecutable;
    }

    @Override
    public KeyMaterial materialize() throws IOException, InterruptedException {
        FilePath dockerConfig = createSecretsDirectory();

        // read the existing docker config file, which might hold some important settings (e.b. proxies)
        FilePath configJsonPath = FilePath.getHomeDirectory(this.launcher.getChannel()).child(".docker").child(DOCKER_CONFIG_FILENAME);
        if (configJsonPath.exists()) {
            String configJson = configJsonPath.readToString();
            if (StringUtils.isNotBlank(configJson)) {
                launcher.getListener().getLogger().print("Using the existing docker config file.");

                JSONObject json = JSONObject.fromObject(configJson);
                for (String property : BLACKLISTED_PROPERTIES) {
                    Object value = json.remove(property);
                    if (value != null) {
                        launcher.getListener().getLogger().print("Removing blacklisted property: " + property);
                    }
                }

                dockerConfig.child(DOCKER_CONFIG_FILENAME).write(json.toString(), StandardCharsets.UTF_8.name());
            }
        }

        try {
            EnvVars envWithConfig = new EnvVars(env);
            envWithConfig.put("DOCKER_CONFIG", dockerConfig.getRemote());
            if (launcher.launch().cmds(new ArgumentListBuilder(dockerExecutable, "login", "-u", username, "--password-stdin").add(endpoint)).envs(envWithConfig).stdin(new ByteArrayInputStream(password.getBytes("UTF-8"))).stdout(listener).join() != 0) {
                throw new AbortException("docker login failed");
            }
        } catch (IOException | InterruptedException x) {
            try {
                dockerConfig.deleteRecursive();
            } catch (Exception x2) {
                x.addSuppressed(x2);
            }
            throw x;
        }
        return new RegistryKeyMaterial(dockerConfig, new EnvVars("DOCKER_CONFIG", dockerConfig.getRemote()));
    }

    private static class RegistryKeyMaterial extends KeyMaterial {

        private final FilePath dockerConfig;

        RegistryKeyMaterial(FilePath dockerConfig, EnvVars envVars) {
            super(envVars);
            this.dockerConfig = dockerConfig;
        }

        @Override
        public void close() throws IOException {
            try {
                dockerConfig.deleteRecursive();
            } catch (InterruptedException x) {
                // TODO would better have been thrown from KeyMaterial.close to begin with
                throw new IOException(x);
            }
        }

    }

}
