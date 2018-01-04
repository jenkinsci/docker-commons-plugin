/*
 * The MIT License
 *
 * Copyright 2015 Jesse Glick, CloudBees Inc.
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

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.ProxyConfiguration;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import hudson.util.VersionNumber;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import jenkins.security.MasterToSlaveCallable;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Download and install Docker CLI binary, see https://docs.docker.com/engine/installation/binaries/
 */
public class DockerToolInstaller extends ToolInstaller {

    private final String version;

    @DataBoundConstructor
    public DockerToolInstaller(String label, String version) {
        super(label);
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public FilePath performInstallation(ToolInstallation toolInstallation, @Nonnull Node node, TaskListener listener) throws IOException, InterruptedException {

        VirtualChannel nodeChannel = node.getChannel();
        if (nodeChannel == null) {
            throw new IllegalStateException("Node is offline");
        }
        String os = nodeChannel.call(new FindArch());

        final URL url = getDockerImageUrl(os, version);
        FilePath install = preferredLocation(tool, node);

        // (simplified) copy/paste from FilePath as hudson.FilePath.installIfNecessaryFrom do assume the URL points to be a zip/tar archive

        FilePath timestamp = install.child(".timestamp");
        URLConnection con = null;
        long sourceTimestamp;
        try {
            con = ProxyConfiguration.open(url);
            if (timestamp.exists()) {
                con.setIfModifiedSince(timestamp.lastModified());
            }
            con.connect();
            if (con instanceof HttpURLConnection
                    && ((HttpURLConnection)con).getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                return install;
            }
            sourceTimestamp = con.getLastModified();

        } catch (IOException x) {
            if (install.exists()) {
                // Cannot connect now, so assume whatever was last unpacked is still OK.
                if (listener != null) {
                    listener.getLogger().println("Skipping installation: " + x);
                }
                return install;
            } else {
                throw x;
            }
        } finally {
            if (con instanceof HttpURLConnection) ((HttpURLConnection) con).disconnect();
        }

        if (install.exists()) {
            if (timestamp.exists() && sourceTimestamp == timestamp.lastModified())
                return install;   // already up to date
            install.deleteContents();
        }

        listener.getLogger().println(Messages.DockerToolInstaller_downloading_docker_client_(version));
        FilePath bin = install.child("bin");
        FilePath docker = bin.child("docker");

        if (install.isRemote()) {
            // First try to download raw Docker binary from the agent machine, as this is fastest, but only available up to 1.10.x.
            bin.mkdirs();
            try {
                docker.copyFrom(url);
            } catch (IOException x) {
                listener.error("Failed to download pre-1.11.x URL " + url + " from agent: " + x);
            }
        }
        if (!docker.exists()) {
            // That did not work, or we did not even try.
            // Fall back to downloading the tarball, which is available for all versions.
            URL tgz = new URL(url + ".tgz");
            // The core utility automatically tries a direct download first, followed by a download via master.
            install.installIfNecessaryFrom(tgz, listener, "Unpacking " + tgz + " to " + install + " on " + node.getDisplayName());
            // But it is in the wrong directory structure, and contains various server binaries we do not need.
            bin.mkdirs(); // installIfNecessaryFrom will wipe it out
            install.child("docker/docker").renameTo(docker);
            if (!docker.exists()) { // TODO FilePath.renameTo does not check its return value
                throw new AbortException(tgz + " did not contain a docker/docker entry as expected");
            }
            install.child("docker").deleteRecursive();
        }

        docker.chmod(0777);

        timestamp.touch(sourceTimestamp);
        return install;
    }

    static URL getDockerImageUrl(String os, String version) throws MalformedURLException {
        if (parseVersion(version).isNewerThan(parseVersion("17.05.0-ce")))
            return new URL("https://download.docker.com/" + os + "/docker-" + version);

        String osName="";
        if (os.startsWith("linux")) osName = "Linux";
        if (os.startsWith("win")) osName = "Windows";
        if (os.startsWith("mac")) osName = "Darwin";
        return new URL("https://get.docker.com/builds/" + osName + os.substring(os.lastIndexOf("/")) + "/docker-" + version);
    }

    private static VersionNumber parseVersion(String version) {
        // any version that sorts before 17.05.0-ce
        if (version.equals("latest")) return new VersionNumber("0");

        final Matcher matcher = Pattern.compile("(\\d+\\.\\d+\\.\\d+).*").matcher(version);
        if (matcher.matches()) return new VersionNumber(matcher.group(1));

        throw new IllegalArgumentException("Failed to parse version " + version);
    }

    @Extension
    public static class DescriptorImpl extends ToolInstallerDescriptor<DockerToolInstaller> {

        @Override
        public String getDisplayName() {
            return "Install latest from docker.io";
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == DockerTool.class;
        }
    }

    private static class FindArch extends MasterToSlaveCallable<String,IOException> {

        @Override
        public String call() throws IOException {
            String os = System.getProperty("os.name").toLowerCase();
            String arch = System.getProperty("os.arch").contains("64") ? "x86_64" : "i386";
            if (os.contains("linux")) return "linux/static/stable/" + arch;
            if (os.contains("windows")) return "win/static/stable/" + arch;
            if (os.contains("mac")) return "mac/static/stable/" + arch;
            throw new IOException("Failed to determine OS architecture " + os + ":" + arch);
        }

    }

}
