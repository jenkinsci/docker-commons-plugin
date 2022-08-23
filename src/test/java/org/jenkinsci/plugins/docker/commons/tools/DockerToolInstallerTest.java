/*
 * The MIT License
 *
 * Copyright 2016 CloudBees, Inc.
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
package org.jenkinsci.plugins.docker.commons.tools;

import hudson.FilePath;
import hudson.Functions;
import hudson.console.PlainTextConsoleOutputStream;
import hudson.model.TaskListener;
import hudson.slaves.DumbSlave;
import hudson.tools.InstallSourceProperty;
import hudson.util.StreamTaskListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.output.TeeOutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.Assume;
import org.junit.Rule;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

public class DockerToolInstallerTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Issue({"JENKINS-48674"})
    @WithoutJenkins
    @Test
    public void testImageUrl() throws MalformedURLException {
        assertEquals(new URL("https://get.docker.com/builds/Linux/x86_64/docker-1.10.0"), DockerToolInstaller.getDockerImageUrl("linux/x86_64", "1.10.0"));
        assertEquals(new URL("https://get.docker.com/builds/Windows/x86_64/docker-1.10.0"), DockerToolInstaller.getDockerImageUrl("win/x86_64","1.10.0"));
        assertEquals(new URL("https://get.docker.com/builds/Darwin/x86_64/docker-1.10.0"), DockerToolInstaller.getDockerImageUrl("mac/x86_64","1.10.0"));
        assertEquals(new URL("https://get.docker.com/builds/Linux/x86_64/docker-17.05.0-ce"), DockerToolInstaller.getDockerImageUrl("linux/x86_64", "17.05.0-ce"));
        assertEquals(new URL("https://get.docker.com/builds/Windows/x86_64/docker-17.05.0-ce"), DockerToolInstaller.getDockerImageUrl("win/x86_64","17.05.0-ce"));
        assertEquals(new URL("https://get.docker.com/builds/Darwin/x86_64/docker-17.05.0-ce"), DockerToolInstaller.getDockerImageUrl("mac/x86_64","17.05.0-ce"));
        assertEquals(new URL("https://get.docker.com/builds/Linux/x86_64/docker-latest"), DockerToolInstaller.getDockerImageUrl("linux/x86_64", "latest"));
        assertEquals(new URL("https://get.docker.com/builds/Windows/x86_64/docker-latest"), DockerToolInstaller.getDockerImageUrl("win/x86_64","latest"));
        assertEquals(new URL("https://get.docker.com/builds/Darwin/x86_64/docker-latest"), DockerToolInstaller.getDockerImageUrl("mac/x86_64","latest"));
        assertEquals(new URL("https://download.docker.com/linux/static/stable/x86_64/docker-17.09.0-ce"), DockerToolInstaller.getDockerImageUrl("linux/x86_64", "17.09.0-ce"));
        assertEquals(new URL("https://download.docker.com/win/static/stable/x86_64/docker-17.09.0-ce"), DockerToolInstaller.getDockerImageUrl("win/x86_64","17.09.0-ce"));
        assertEquals(new URL("https://download.docker.com/mac/static/stable/x86_64/docker-17.09.0-ce"), DockerToolInstaller.getDockerImageUrl("mac/x86_64","17.09.0-ce"));
        assertEquals(new URL("https://download.docker.com/linux/static/stable/x86_64/docker-20.10.6"), DockerToolInstaller.getDockerImageUrl("linux/x86_64", "20.10.6"));
        assertEquals(new URL("https://download.docker.com/win/static/stable/x86_64/docker-20.10.6"), DockerToolInstaller.getDockerImageUrl("win/x86_64","20.10.6"));
        assertEquals(new URL("https://download.docker.com/mac/static/stable/x86_64/docker-20.10.6"), DockerToolInstaller.getDockerImageUrl("mac/x86_64","20.10.6"));
    }

    @Issue({"JENKINS-36082", "JENKINS-32790", "JENKINS-48674"})
    @Test
    public void smokes() throws Exception {
        Assume.assumeFalse(Functions.isWindows());
        try {
            new URL("https://get.docker.com/").openStream().close();
            new URL("https://download.docker.com/").openStream().close();
        } catch (IOException x) {
            Assume.assumeNoException("Cannot contact download sites, perhaps test machine is not online", x);
        }

        String[] testedVersions = {"20.10.6", "19.03.9"};
        DockerTool[] installations = new DockerTool[testedVersions.length];
        for (int i = 0; i < testedVersions.length; i++) {
            String v = testedVersions[i];
            installations[i] = new DockerTool(v, "", Collections.singletonList(new InstallSourceProperty(Collections.singletonList(new DockerToolInstaller("", v)))));
        }
        r.jenkins.getDescriptorByType(DockerTool.DescriptorImpl.class).setInstallations(installations);
        DumbSlave slave = r.createOnlineSlave();
        FilePath toolDir = slave.getRootPath().child("tools/org.jenkinsci.plugins.docker.commons.tools.DockerTool");

        List<FilePath> files = new ArrayList<>();
        for (String v : testedVersions) {
            FilePath exe = downloadDocker(slave, toolDir, v);
            files.add(exe);
            files.add(toolDir.child(v + "/.timestamp"));
        }
        assertThat("we do not have any extra files in here", toolDir.list("**"), arrayContainingInAnyOrder(files.toArray(new FilePath[files.size()])));
    }

    private FilePath downloadDocker(DumbSlave slave, FilePath toolDir, String version) throws IOException, InterruptedException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TeeOutputStream tee = new TeeOutputStream(baos, new PlainTextConsoleOutputStream(System.err));
        TaskListener l = new StreamTaskListener(tee);

        FilePath exe = toolDir.child(version+"/bin/docker");
        // Download for first time:
        assertEquals(exe.getRemote(), DockerTool.getExecutable(version, slave, l, null));
        assertTrue(exe.exists());
        assertThat(baos.toString(), containsString(Messages.DockerToolInstaller_downloading_docker_client_(version)));
        // Next time we do not need to download:
        baos.reset();
        assertEquals(exe.getRemote(), DockerTool.getExecutable(version, slave, l, null));
        assertTrue(exe.exists());
        assertThat(baos.toString(), not(containsString(Messages.DockerToolInstaller_downloading_docker_client_(version))));
        // Version check:
        baos.reset();
        assertEquals(0, slave.createLauncher(l).launch().cmds(exe.getRemote(), "version", "--format", "{{.Client.Version}}").quiet(true).stdout(tee).stderr(System.err).join());
        if (!version.equals("latest")) {
            assertEquals(version, baos.toString().trim());
        }
        return exe;
    }

}
