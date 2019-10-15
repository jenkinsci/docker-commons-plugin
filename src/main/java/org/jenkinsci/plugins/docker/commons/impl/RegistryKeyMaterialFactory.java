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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

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
    private static final String BLACKLISTED_PROPERTY_CREDS_STORE = "credsStore";
    private static final String BLACKLISTED_PROPERTY_AUTHS = "auths";
    private static final String BLACKLISTED_PROPERTY_PROXIES = "proxies";
    private static final String[] BLACKLISTED_PROPERTIES = { BLACKLISTED_PROPERTY_AUTHS, BLACKLISTED_PROPERTY_CREDS_STORE };
    private static final String[] BLACKLISTED_NESTED_PROPERTIES = { BLACKLISTED_PROPERTY_CREDS_STORE, BLACKLISTED_PROPERTY_PROXIES };

    private final @Nonnull String username;
    private final @Nonnull String password;
    private final @Nonnull URL endpoint;
    private final @Nonnull Launcher launcher;
    private final @Nonnull EnvVars env;
    private final @Nonnull TaskListener listener;
    private final @Nonnull String dockerExecutable;

    public RegistryKeyMaterialFactory(@Nonnull String username, @Nonnull String password, @Nonnull URL endpoint, @Nonnull Launcher launcher, @Nonnull EnvVars env, @Nonnull TaskListener listener, @Nonnull String dockerExecutable) {
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
        dockerConfig = UpdateDockerConfigFromSource(dockerConfig, configJsonPath, BLACKLISTED_PROPERTIES);

        // Read the existing docker config file from a nested config block, will probably hold some previous credentials
        String existingDockerSecretConfigPath = this.env.get("DOCKER_CONFIG");
        if (StringUtils.isNotBlank(existingDockerSecretConfigPath)) {
            // Can't use FilePath(File) yet as not supported till later versions of jenkins..
            //FilePath existingDockerConfig = FilePath(new File(existingDockerSecretConfigPath, DOCKER_CONFIG_FILENAME));
            FilePath baseDir = getContext().getBaseDir();
            // Need to get tmp dir - get base dir length and increase by 1 to include the path separator
            String existingTmpConfigDir = existingDockerSecretConfigPath.substring(baseDir.getRemote().length() + 1);
            FilePath existingDockerConfigPath = baseDir.child(existingTmpConfigDir).child(DOCKER_CONFIG_FILENAME);
            dockerConfig = updateDockerConfigFromSource(dockerConfig, existingDockerConfigPath, BLACKLISTED_NESTED_PROPERTIES);
        }

        try {
            // TODO on Docker 17.07+ use --password-stdin
            EnvVars envWithConfig = new EnvVars(env);
            envWithConfig.put("DOCKER_CONFIG", dockerConfig.getRemote());
            if (launcher.launch().cmds(new ArgumentListBuilder(dockerExecutable, "login", "-u", username, "-p").add(password, true).add(endpoint)).envs(envWithConfig).stdout(listener).join() != 0) {
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

    /**
     * Copy docker config source data to another docker config
     * @param dockerConfig
     * @param dockerConfigSourcePath
     * @param blacklistedProperties
     * @return FilePath dockerConfig
     */
    private FilePath updateDockerConfigFromSource(@Nonnull FilePath dockerConfig, @Nonnull FilePath dockerConfigSourcePath, @Nonnull String[] blacklistedProperties) throws IOException, InterruptedException {
        // Make sure config exists
        if (dockerConfigSourcePath.exists()) {
            String configJson = dockerConfigSourcePath.readToString();
            if (StringUtils.isNotBlank(configJson)) {
                this.launcher.getListener().getLogger().print("Using the existing docker config file.");

                JSONObject json = JSONObject.fromObject(configJson);
                for (String property : blacklistedProperties) {
                    Object value = json.remove(property);
                    if (value != null) {
                        this.launcher.getListener().getLogger().print("Removing blacklisted property: " + property);
                    }
                }

                dockerConfig.child(DOCKER_CONFIG_FILENAME).write(json.toString(), StandardCharsets.UTF_8.name());
            }
        }
        return dockerConfig;
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
