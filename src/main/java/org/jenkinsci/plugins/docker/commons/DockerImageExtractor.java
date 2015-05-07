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
package org.jenkinsci.plugins.docker.commons;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Job;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * Identifies the Docker images used by a Jenkins job. Docker-related plugins can use this
 * extension point to integrate with for example Docker HUB web hook support.
 */
public abstract class DockerImageExtractor implements ExtensionPoint {

    /**
     * Provides a collection of repository names {@code namespace/name} that the job uses as seen by the implementing class.
     * Return an empty collection if none is found.
     *
     * @param job the job being queried.
     * @return a collection of names, or an empty collection.
     */
    @Nonnull
    public abstract Collection<String> getDockerImagesUsedByJob(@Nonnull Job<?,?> job);

    /**
     * Provides a set of repository names {@code namespace/name} that the job uses as seen by all the declared {@link DockerImageExtractor}s.
     * Returns an empty set if none is found.
     *
     * @param job the job being queried.
     * @return a set of names, or an empty set.
     */
    @Nonnull
    public static Set<String> getDockerImagesUsedByJobFromAll(@Nonnull Job<?,?> job) {
        Jenkins j = Jenkins.getInstance();
        Set<String> names = new TreeSet<String>();
        if (j != null) {
            //TODO use ExtensionList.lookup(DockerImageExtractor.class); when core req is past 1.572 to not have to check for Jenkins instance
            for (DockerImageExtractor extractor : j.getExtensionList(DockerImageExtractor.class)) {
                names.addAll(extractor.getDockerImagesUsedByJob(job));
            }
        }
        return names;
    }
}
