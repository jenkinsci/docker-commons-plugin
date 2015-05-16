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

import hudson.FilePath;

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * Represents the context within a {@link KeyMaterialFactory} can {@link KeyMaterialFactory#materialize()}
 * {@link KeyMaterial} instances.
 *
 * @author Stephen Connolly
 */
public class KeyMaterialContext implements Serializable {
    private static final long serialVersionUID = 1L;

    @Nonnull
    private final FilePath baseDir;

    public KeyMaterialContext(@Nonnull FilePath baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Returns the base directory that can be used to {@link KeyMaterialFactory#materialize()}
     * {@link KeyMaterial} instances.
     *
     * @return the base directory.
     */
    @Nonnull
    public FilePath getBaseDir() {
        return baseDir;
    }
}
