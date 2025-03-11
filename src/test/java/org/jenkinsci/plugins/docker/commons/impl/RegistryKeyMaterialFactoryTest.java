/*
 * The MIT License
 *
 * Copyright 2018 Krassimir Valev.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.emptyArray;
import static org.junit.jupiter.api.Assertions.*;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.LocalChannel;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.jenkinsci.plugins.docker.commons.credentials.KeyMaterial;
import org.jenkinsci.plugins.docker.commons.credentials.KeyMaterialContext;
import org.jenkinsci.plugins.docker.commons.credentials.KeyMaterialFactory;
import org.jenkinsci.plugins.docker.commons.tools.DockerTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.FakeLauncher;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.PretendSlave;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class RegistryKeyMaterialFactoryTest {

    @TempDir
    private File tempFolder;

    private KeyMaterialFactory factory;

    @BeforeEach
    void setup(JenkinsRule j) throws Exception {
        // fake launcher for the docker login invocation
        FakeLauncher faker = p -> new FakeLauncher.FinishedProc(0);

        PretendSlave slave = j.createPretendSlave(faker);
        // VirtualChannel channel = slave.getChannel();
        // FreeStyleProject project = j.createFreeStyleProject();

        TaskListener listener = TaskListener.NULL;
        Launcher launcher = slave.createLauncher(listener);
        launcher = new Launcher.DecoratedLauncher(launcher) {
            @Override
            public VirtualChannel getChannel() {
                return new LocalChannel(null) {
                    @Override
                    public <V, T extends Throwable> V call(final Callable<V, T> callable) throws T {
                        // ugly as hell, but we need a way to mock fetching the home directory
                        return (V) new FilePath(tempFolder);
                    }
                };
            }
        };

        URL endpoint = new DockerRegistryEndpoint(null, null).getEffectiveUrl();
        EnvVars env = new EnvVars();
        String dockerExecutable = DockerTool.getExecutable(null, null, listener, env);

        factory = new RegistryKeyMaterialFactory(
                        "username", "password", endpoint, launcher, env, listener, dockerExecutable)
                .contextualize(new KeyMaterialContext(new FilePath(newFolder(tempFolder, "junit"))));
    }

    @Test
    void materialize_userConfigFileNotPresent_notCreated() throws Exception {
        // act
        KeyMaterial material = factory.materialize();

        // assert
        String dockerCfgFolderPath = material.env().get("DOCKER_CONFIG", null);
        assertNotNull(dockerCfgFolderPath);

        File dockerCfgFolder = new File(dockerCfgFolderPath);
        assertTrue(dockerCfgFolder.exists());
        assertThat(dockerCfgFolder.list(), emptyArray());
    }

    @Test
    void materialize_userConfigFileBlank_notCreated() throws Exception {
        // arrange
        File cfgFile = new File(new File(tempFolder, ".docker"), "config.json");
        FileUtils.write(cfgFile, "    ", Charset.defaultCharset());

        // act
        KeyMaterial material = factory.materialize();

        // assert
        String dockerCfgFolderPath = material.env().get("DOCKER_CONFIG", null);
        assertNotNull(dockerCfgFolderPath);

        File dockerCfgFolder = new File(dockerCfgFolderPath);
        assertTrue(dockerCfgFolder.exists());
        assertThat(dockerCfgFolder.list(), emptyArray());
    }

    @Test
    void materialize_userConfigFileWithBrackets_createdEmpty() throws Exception {
        // arrange
        File cfgFile = new File(new File(tempFolder, ".docker"), "config.json");
        FileUtils.write(cfgFile, "{}", Charset.defaultCharset());

        // act
        KeyMaterial material = factory.materialize();

        // assert
        String dockerCfgFolderPath = material.env().get("DOCKER_CONFIG", null);
        assertNotNull(dockerCfgFolderPath);

        File dockerCfgFolder = new File(dockerCfgFolderPath);
        assertTrue(dockerCfgFolder.exists());

        String[] files = dockerCfgFolder.list();
        assertThat(files, arrayContaining("config.json"));

        File jsonFile = new File(dockerCfgFolder, "config.json");
        assertEquals("{}", FileUtils.readFileToString(jsonFile, Charset.defaultCharset()));
    }

    @Test
    void materialize_userConfigFileWithAuths_createdEmpty() throws Exception {
        // arrange
        File cfgFile = new File(new File(tempFolder, ".docker"), "config.json");
        FileUtils.write(
                cfgFile,
                "{\"auths\": { \"localhost:5001\": { \"auth\": \"whatever\", \"email\": \"\"} }}",
                Charset.defaultCharset());

        // act
        KeyMaterial material = factory.materialize();

        // assert
        String dockerCfgFolderPath = material.env().get("DOCKER_CONFIG", null);
        assertNotNull(dockerCfgFolderPath);

        File dockerCfgFolder = new File(dockerCfgFolderPath);
        assertTrue(dockerCfgFolder.exists());

        String[] files = dockerCfgFolder.list();
        assertThat(files, arrayContaining("config.json"));

        File jsonFile = new File(dockerCfgFolder, "config.json");
        assertEquals("{}", FileUtils.readFileToString(jsonFile, Charset.defaultCharset()));
    }

    @Test
    void materialize_userConfigFileWithAuthsAndProxies_createdWithProxies() throws Exception {
        // arrange
        File cfgFile = new File(new File(tempFolder, ".docker"), "config.json");
        FileUtils.write(
                cfgFile,
                "{" + "\"auths\": { \"localhost:5001\": { \"auth\": \"whatever\", \"email\": \"\"} },"
                        + "\"proxies\": { \"default\": { \"httpProxy\": \"proxy\", \"noProxy\": \"something\" } }"
                        + "}",
                Charset.defaultCharset());

        // act
        KeyMaterial material = factory.materialize();

        // assert
        String dockerCfgFolderPath = material.env().get("DOCKER_CONFIG", null);
        assertNotNull(dockerCfgFolderPath);

        File dockerCfgFolder = new File(dockerCfgFolderPath);
        assertTrue(dockerCfgFolder.exists());

        String[] files = dockerCfgFolder.list();
        assertThat(files, arrayContaining("config.json"));

        File jsonFile = new File(dockerCfgFolder, "config.json");
        assertEquals(
                "{\"proxies\":{\"default\":{\"httpProxy\":\"proxy\",\"noProxy\":\"something\"}}}",
                FileUtils.readFileToString(jsonFile, Charset.defaultCharset()));
    }

    @Test
    void materialize_userConfigFileWithCredStoreAndHttpHeaders_createdWithHeaders() throws Exception {
        // arrange
        File cfgFile = new File(new File(tempFolder, ".docker"), "config.json");
        FileUtils.write(
                cfgFile,
                "{" + "\"credsStore\" : \"osxkeychain\", " + "\"HttpHeaders\" : {\"User-Agent\" : \"Docker-Client\"}"
                        + "}",
                Charset.defaultCharset());

        // act
        KeyMaterial material = factory.materialize();

        // assert
        String dockerCfgFolderPath = material.env().get("DOCKER_CONFIG", null);
        assertNotNull(dockerCfgFolderPath);

        File dockerCfgFolder = new File(dockerCfgFolderPath);
        assertTrue(dockerCfgFolder.exists());

        String[] files = dockerCfgFolder.list();
        assertThat(files, arrayContaining("config.json"));

        File jsonFile = new File(dockerCfgFolder, "config.json");
        assertEquals(
                "{\"HttpHeaders\":{\"User-Agent\":\"Docker-Client\"}}",
                FileUtils.readFileToString(jsonFile, Charset.defaultCharset()));
    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }
}
