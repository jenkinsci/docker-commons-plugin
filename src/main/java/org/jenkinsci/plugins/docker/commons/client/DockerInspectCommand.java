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


import net.sf.json.JSONArray;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Docker <a href="https://docs.docker.com/reference/commandline/cli/#inspect">inspect</a>.
 * 
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class DockerInspectCommand extends DockerCommand<DockerInspectCommand> {
    
    // e.g. 2015-04-09T13:40:21.981801679Z
    public static final String DOCKER_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    private final String objectId;

    /**
     * Docker {@code inspect} command.
     * @param objectId The image/container ID or Name.
     */
    public DockerInspectCommand(@Nonnull String objectId) {
        addArgs("inspect");
        this.objectId = objectId;
    }

    @Override
    public void preLaunch() {        
        addArgs(objectId);
    }
    
    public JSONArray getResultAsJSON() {
        String out = getOut();        
        if (out == null) {
            return null;
        }
        
        return JSONArray.fromObject(out);
    }

    /**
     * Execute the inspect command with a formatting option.
     * 
     * <p>
     * Handy for narrowing/formatting the inspect result. 
     * 
     * @param goFormattedTemplate The go template. <a href="https://docs.docker.com/reference/commandline/cli/#inspect">See docs</a>.
     * @return {@code this}.
     */
    public DockerInspectCommand withGoFormattedTemplate(String goFormattedTemplate) {
        if (isOptionSet(DockerCommandOption.FORMAT)) {
            throw new UnsupportedOperationException("Format already specified. docker inspect only respects a single formatting option.");
        }
        addArgs(DockerCommandOption.FORMAT, goFormattedTemplate);
        return this;
    }
    
    public static String getObjectName(@Nonnull String objectId, @Nonnull DockerClient dockerClient) throws IOException, InterruptedException {
        return getField(objectId, ".Name", dockerClient);
    }
    
    public static String getHostName(@Nonnull String objectId, @Nonnull DockerClient dockerClient) throws IOException, InterruptedException {
        return getField(objectId, ".Config.Hostname", dockerClient);
    }
    
    public static Date getCreated(@Nonnull String objectId, @Nonnull DockerClient dockerClient) throws IOException, InterruptedException {
        String createdString = getField(objectId, ".Created", dockerClient);
        try {
            // TODO Currently truncating. Find out how to specify last part for parsing (TZ etc)
            return new SimpleDateFormat(DOCKER_DATE_TIME_FORMAT).parse(createdString.substring(0, DOCKER_DATE_TIME_FORMAT.length() - 2));
        } catch (ParseException e) {
            throw new IOException(String.format("Error parsing created date '' for object ''.", createdString, objectId), e);
        }
    }

    public static String getField(@Nonnull String objectId, @Nonnull String fieldPath, @Nonnull DockerClient dockerClient) throws IOException, InterruptedException {
        DockerInspectCommand command = new DockerInspectCommand(objectId).withGoFormattedTemplate(String.format("{{%s}}", fieldPath));
        if (dockerClient.launch(command) == 0) {
            return command.getOut().trim();
        } else {
            // TODO need to do more than this I'm ure
            throw new IOException(command.getErr());
        }
    }

    /**
     * Check for the existence of an image/container.
     * @param objectId The image/container ID.
     * @param dockerClient The {@link DockerClient} instance to use for launching the query.
     * @return {@code true} if the image/container exists, otherwise {@code false}.
     */
    public static boolean exists(@Nonnull String objectId, @Nonnull DockerClient dockerClient) throws IOException, InterruptedException {
        DockerInspectCommand command = new DockerInspectCommand(objectId).withGoFormattedTemplate("{{.Name}}");
        return (dockerClient.launch(command) == 0);
    }
}
