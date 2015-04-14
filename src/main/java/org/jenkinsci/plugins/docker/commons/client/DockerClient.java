/*
 * The MIT License
 *
 * Copyright (c) 2013-2015, CloudBees, Inc.
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
package org.jenkinsci.plugins.docker.commons.client;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.util.ArgumentListBuilder;
import org.jenkinsci.plugins.docker.commons.KeyMaterial;
import org.jenkinsci.plugins.docker.commons.fingerprint.ContainerRecord;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class DockerClient {

    private static final Logger LOGGER = Logger.getLogger(DockerClient.class.getName());

    // e.g. 2015-04-09T13:40:21.981801679Z
    public static final String DOCKER_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    // TODO: Is there some way to setup the path used by the Launcher.
    // So I can point it to the install location for docker.
    private static final String DOCKER_COMMAND = System.getProperty("DOCKER_COMMAND", "docker");

    private Launcher launcher;
    private KeyMaterial keyMaterial;

    public DockerClient(@Nonnull Launcher launcher) {
        this.launcher = launcher;
    }

    public DockerClient setKeyMaterial(@Nonnull KeyMaterial keyMaterial) {
        this.keyMaterial = keyMaterial;
        return this;
    }

    // TODO return a fingerprint
    public String build(@Nonnull FilePath pwd, @CheckForNull String tag) throws IOException, InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add("build");
        if (tag != null) {
            args.add("-t", tag);
        }
        args.add(".");
        LaunchResult result = launch(pwd, args);
        if (result.getStatus() != 0) {
            throw new IOException(String.format("Failed to build Dockerfile in '%s'.", pwd));
        }

        // Extract the short imageId from the end of the docker output
        String stdout = result.getOut();
        int i = stdout.length() - 1;
        StringBuffer shortImageId = new StringBuffer();
        while (i > 0) {
            char c = stdout.charAt(i);
            if (Character.isWhitespace(c)) {
                break;
            }
            shortImageId.insert(0, c);
            i--;
        }

        // inspect that image and get the full 64 char image Id
        String expandedImageId = inspect(shortImageId.toString(), ".Id");

        return expandedImageId;
    }

    public ContainerRecord run(@Nonnull String image, @CheckForNull String workdir, @Nonnull Map<String, String> volumes, @Nonnull String user, @CheckForNull String ... command) throws IOException, InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder();

        args.add("run", "-t", "-d", "-u", user);
        if (workdir != null) {
            args.add("-w", workdir);
        }
        for (Map.Entry<String, String> volume : volumes.entrySet()) {
            args.add("-v", volume.getKey() + ":" + volume.getValue() + ":rw");
        }
        if (command != null) {
            args.add(image).add(command);
        }

        // TODO: change to use BourneShellScript to make it durable
        LaunchResult result = launch(null, args);
        if (result.getStatus() == 0) {
            String containerId = result.getOut();
            return getContainerRecord(containerId);
        } else {
            throw new IOException(String.format("Failed to run image '%s'. Error: %s", image, result.getErr()));
        }
    }

    public void kill(@Nonnull String containerId) throws IOException, InterruptedException {
        LaunchResult result = launch("kill", containerId);
        if (result.getStatus() != 0) {
            throw new IOException(String.format("Failed to kill container '%s'.", containerId));
        }
        rm(containerId);
    }

    public void rm(@Nonnull String containerId) throws IOException, InterruptedException {
        LaunchResult result;
        result = launch("rm", containerId);
        if (result.getStatus() != 0) {
            throw new IOException(String.format("Failed to rm container '%s'.", containerId));
        }
    }

    String inspect(@Nonnull String objectId, @Nonnull String fieldPath) throws IOException, InterruptedException {
        LaunchResult result = launch("inspect", "-f", String.format("{{%s}}", fieldPath), objectId);
        if (result.getStatus() == 0) {
            return result.getOut();
        } else {
            return null;
        }
    }

    private Date getCreatedDate(@Nonnull String objectId) throws IOException, InterruptedException {
        String createdString = inspect(objectId, ".Created");
        try {
            // TODO Currently truncating. Find out how to specify last part for parsing (TZ etc)
            return new SimpleDateFormat(DOCKER_DATE_TIME_FORMAT).parse(createdString.substring(0, DOCKER_DATE_TIME_FORMAT.length() - 2));
        } catch (ParseException e) {
            throw new IOException(String.format("Error parsing created date '%s' for object '%s'.", createdString, objectId), e);
        }
    }

    private LaunchResult launch(@Nonnull String... args) throws IOException, InterruptedException {
        return launch(null, args);
    }
    private LaunchResult launch(FilePath pwd, @Nonnull String... args) throws IOException, InterruptedException {
        return launch(pwd, new ArgumentListBuilder(args));
    }
    private LaunchResult launch(FilePath pwd, @Nonnull ArgumentListBuilder args) throws IOException, InterruptedException {
        EnvVars envVars = new EnvVars();

        // Prepend the docker command
        args.prepend(DOCKER_COMMAND);

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Executing docker command {0}", args.toString());
        }

        if (keyMaterial != null) {
            envVars.putAll(keyMaterial.env());
        }

        Launcher.ProcStarter procStarter = launcher.launch();

        if (pwd != null) {
            procStarter.pwd(pwd);
        }

        LaunchResult result = new LaunchResult();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        try {
            result.setStatus(procStarter.cmds(args).envs(envVars).stdout(out).stderr(err).join());
            return result;
        } finally {
            try {
                result.setOut(out.toString());
                out.close();
            } finally {
                result.setErr(err.toString());
                err.close();
            }
        }
    }

    /**
     * Who is executing this {@link DockerClient} instance.
     *
     * @return a {@link String} containing the <strong>uid:gid</strong>.
     */
    public String whoAmI() throws IOException, InterruptedException {
        ByteArrayOutputStream userId = new ByteArrayOutputStream();
        launcher.launch().cmds("id", "-u").stdout(userId).quiet(true).join();

        ByteArrayOutputStream groupId = new ByteArrayOutputStream();
        launcher.launch().cmds("id", "-g").stdout(groupId).quiet(true).join();

        return String.format("%s:%s", userId.toString().trim(), groupId.toString().trim());

    }

    private ContainerRecord getContainerRecord(String containerId) throws IOException, InterruptedException {
        String host = inspect(containerId, ".Config.Hostname");
        String containerName = inspect(containerId, ".Name");
        Date created = getCreatedDate(containerId);

        // TODO get tags and add for ContainerRecord
        return new ContainerRecord(host, containerId, containerName, created.getTime(), Collections.<String,String>emptyMap());
    }
}
