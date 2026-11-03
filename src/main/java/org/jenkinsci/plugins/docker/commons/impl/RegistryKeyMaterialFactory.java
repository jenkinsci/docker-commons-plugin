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

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jenkinsci.plugins.docker.commons.credentials.KeyMaterial;
import org.jenkinsci.plugins.docker.commons.credentials.KeyMaterialFactory;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.docker.commons.credentials.KeyMaterial2;

/**
 * Logs you in to a Docker registry.
 */
@Restricted(NoExternalUse.class)
public class RegistryKeyMaterialFactory extends KeyMaterialFactory {

    private static final String DOCKER_CONFIG_FILENAME = "config.json";
    private static final String[] BLACKLISTED_PROPERTIES = { "auths", "credsStore" };
    private static final String PASSWORD_FILENAME = "password";
    private static final String LOGIN_SCRIPT_FILENAME = "login.sh";
    // A file, not "sh -c": the Kubernetes container step passes each argument through a shell, which expands "$".
    private static final String LOGIN_SCRIPT = "f=\"$1\"\nshift\nexec \"$@\" < \"$f\"\n";

    protected static final String DOCKER_REGISTRY_HOST_ONLY = "DOCKER_REGISTRY_HOST_ONLY";

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
    public KeyMaterial2 materialize2() throws IOException, InterruptedException {
        FilePath dockerConfig = createSecretsDirectory();

        // read the existing docker config file, which might hold some important settings (e.b. proxies)
        FilePath configJsonPath = FilePath.getHomeDirectory(this.launcher.getChannel()).child(".docker").child(DOCKER_CONFIG_FILENAME);
        if (configJsonPath.exists()) {
            String configJson = configJsonPath.readToString();
            if (!configJson.isBlank()) {
                launcher.getListener().getLogger().println("Using the existing docker config file.");

                JSONObject json = JSONObject.fromObject(configJson);
                for (String property : BLACKLISTED_PROPERTIES) {
                    Object value = json.remove(property);
                    if (value != null) {
                        launcher.getListener().getLogger().println("Removing blacklisted property: " + property);
                    }
                }

                dockerConfig.child(DOCKER_CONFIG_FILENAME).write(json.toString(), StandardCharsets.UTF_8.name());
            }
        }

        try {
            EnvVars envWithConfig = new EnvVars(env);
            envWithConfig.put("DOCKER_CONFIG", dockerConfig.getRemote());
            if (login(dockerConfig, envWithConfig) != 0) {
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
        return new RegistryKeyMaterial2(dockerConfig, new EnvVars("DOCKER_CONFIG", dockerConfig.getRemote()));
    }

    // Not ProcStarter.stdin: withDockerContainer and the Kubernetes container step do not forward it (JENKINS-69436, JENKINS-69432).
    private int login(FilePath dockerConfig, EnvVars envWithConfig) throws IOException, InterruptedException {
        if (!launcher.isUnix()) {
            // TODO use --password-stdin on Windows too
            return launcher.launch().cmds(new ArgumentListBuilder(dockerExecutable, "login", "-u", username, "-p").add(password, true).add(registry())).envs(envWithConfig).stdout(listener).join();
        }
        FilePath script = dockerConfig.child(LOGIN_SCRIPT_FILENAME);
        FilePath passwordFile = dockerConfig.child(PASSWORD_FILENAME);
        try {
            script.write(LOGIN_SCRIPT, StandardCharsets.UTF_8.name());
            passwordFile.write(password, StandardCharsets.UTF_8.name());
            passwordFile.chmod(0600);
            return launcher.launch().cmds(new ArgumentListBuilder("sh", script.getRemote(), passwordFile.getRemote(), dockerExecutable, "login", "-u", username, "--password-stdin", registry())).envs(envWithConfig).stdout(listener).join();
        } finally {
            passwordFile.delete();
            script.delete();
        }
    }

    protected String registry() {
    	if (dockerExecutable.endsWith("podman") || Boolean.parseBoolean(env.get(DOCKER_REGISTRY_HOST_ONLY, "false"))) {
    		return endpoint.getAuthority();
    	}

    	return endpoint.toString();
    }

    private static class RegistryKeyMaterial2 extends KeyMaterial2 {

        private final String dockerConfig;

        RegistryKeyMaterial2(FilePath dockerConfig, EnvVars envVars) {
            super(envVars);
            this.dockerConfig = dockerConfig.getRemote();
        }

        @Override
        public void close(VirtualChannel channel) throws IOException, InterruptedException {
            new FilePath(channel, dockerConfig).deleteRecursive();
        }

    }

    @SuppressFBWarnings(value = {"NP_UNWRITTEN_FIELD", "UWF_NULL_FIELD"})
    @Deprecated
    private static class RegistryKeyMaterial extends KeyMaterial {

        private final FilePath dockerConfig = null;

        private RegistryKeyMaterial() {
            super(null);
            assert false : "only deserialized";
        }

        @Override
        public void close() throws IOException {
            try {
                dockerConfig.deleteRecursive();
            } catch (InterruptedException x) {
                throw new IOException(x);
            }
        }

    }

}
