/*
 * The MIT License
 *
 * Copyright (c) 2015 CloudBees, Inc.
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

import hudson.Plugin;
import org.jenkins.ui.icon.Icon;
import org.jenkins.ui.icon.IconSet;
import org.jenkins.ui.icon.IconType;

/**
 * Plugin class for Docker commons plugin.
 * @author Oleg Nenashev
 */
public class DockerCommonsPlugin extends Plugin {
    
    @Override
    public void start() throws Exception {
        super.start();
        registerIcons();
    }
    
    private void registerIcons() {
        IconSet.icons.addIcon(new Icon("icon-docker-logo icon-sm", "docker-commons/images/16x16/docker.png", Icon.ICON_SMALL_STYLE, IconType.PLUGIN));
        IconSet.icons.addIcon(new Icon("icon-docker-logo icon-md", "docker-commons/images/24x24/docker.png", Icon.ICON_MEDIUM_STYLE, IconType.PLUGIN));
        IconSet.icons.addIcon(new Icon("icon-docker-logo icon-lg", "docker-commons/images/32x32/docker.png", Icon.ICON_LARGE_STYLE, IconType.PLUGIN));
        IconSet.icons.addIcon(new Icon("icon-docker-logo icon-xlg", "docker-commons/images/48x48/docker.png", Icon.ICON_XLARGE_STYLE, IconType.PLUGIN));
    }
}
