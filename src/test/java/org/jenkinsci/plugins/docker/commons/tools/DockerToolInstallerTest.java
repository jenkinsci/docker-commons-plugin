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
import java.util.Collections;
import org.apache.commons.io.output.TeeOutputStream;
import static org.hamcrest.Matchers.*;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Assume;
import org.junit.Rule;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

public class DockerToolInstallerTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Issue({"JENKINS-48674"})
    @Test
    public void testImageUrl() throws MalformedURLException {
        assertEquals(new URL("https://get.docker.com/builds/Linux/x86_64/docker-1.10.0"), DockerToolInstaller.getDockerImageUrl("linux/static/stable/x86_64", "1.10.0"));
        assertEquals(new URL("https://get.docker.com/builds/Windows/x86_64/docker-1.10.0"), DockerToolInstaller.getDockerImageUrl("win/static/stable/x86_64","1.10.0"));
        assertEquals(new URL("https://get.docker.com/builds/Darwin/x86_64/docker-1.10.0"), DockerToolInstaller.getDockerImageUrl("mac/static/stable/x86_64","1.10.0"));
        assertEquals(new URL("https://get.docker.com/builds/Linux/x86_64/docker-17.05.0-ce"), DockerToolInstaller.getDockerImageUrl("linux/static/stable/x86_64", "17.05.0-ce"));
        assertEquals(new URL("https://get.docker.com/builds/Windows/x86_64/docker-17.05.0-ce"), DockerToolInstaller.getDockerImageUrl("win/static/stable/x86_64","17.05.0-ce"));
        assertEquals(new URL("https://get.docker.com/builds/Darwin/x86_64/docker-17.05.0-ce"), DockerToolInstaller.getDockerImageUrl("mac/static/stable/x86_64","17.05.0-ce"));
        assertEquals(new URL("https://get.docker.com/builds/Linux/x86_64/docker-latest"), DockerToolInstaller.getDockerImageUrl("linux/static/stable/x86_64", "latest"));
        assertEquals(new URL("https://get.docker.com/builds/Windows/x86_64/docker-latest"), DockerToolInstaller.getDockerImageUrl("win/static/stable/x86_64","latest"));
        assertEquals(new URL("https://get.docker.com/builds/Darwin/x86_64/docker-latest"), DockerToolInstaller.getDockerImageUrl("mac/static/stable/x86_64","latest"));
        assertEquals(new URL("https://download.docker.com/linux/static/stable/x86_64/docker-17.09.0-ce"), DockerToolInstaller.getDockerImageUrl("linux/static/stable/x86_64", "17.09.0-ce"));
        assertEquals(new URL("https://download.docker.com/win/static/stable/x86_64/docker-17.09.0-ce"), DockerToolInstaller.getDockerImageUrl("win/static/stable/x86_64","17.09.0-ce"));
        assertEquals(new URL("https://download.docker.com/mac/static/stable/x86_64/docker-17.09.0-ce"), DockerToolInstaller.getDockerImageUrl("mac/static/stable/x86_64","17.09.0-ce"));
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
        r.jenkins.getDescriptorByType(DockerTool.DescriptorImpl.class).setInstallations(
            new DockerTool("latest", "", Collections.singletonList(new InstallSourceProperty(Collections.singletonList(new DockerToolInstaller("", "latest"))))),
            new DockerTool("1.10.0", "", Collections.singletonList(new InstallSourceProperty(Collections.singletonList(new DockerToolInstaller("", "1.10.0"))))),
            new DockerTool("17.09.1-ce", "", Collections.singletonList(new InstallSourceProperty(Collections.singletonList(new DockerToolInstaller("", "17.09.1-ce"))))));
        DumbSlave slave = r.createOnlineSlave();
        FilePath toolDir = slave.getRootPath().child("tools/org.jenkinsci.plugins.docker.commons.tools.DockerTool");

        FilePath exe10 = downloadDocker(slave, toolDir, "1.10.0");
        FilePath exeLatest = downloadDocker(slave, toolDir, "latest");
        FilePath exe17 = downloadDocker(slave, toolDir, "17.09.1-ce");

        assertThat("we do not have any extra files in here", toolDir.list("**"), arrayContainingInAnyOrder(exe10, toolDir.child("1.10.0/.timestamp"), exeLatest, toolDir.child("latest/.timestamp"), exe17, toolDir.child("17.09.1-ce/.timestamp")));
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
