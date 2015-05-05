/*
 * Copyright 2015 Carlos Sanchez <carlos@apache.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.docker.commons;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

/**
 * @author Carlos Sanchez <carlos@apache.org>
 */
public class DockerRegistryEndpointTest {

    @Test
    public void testParse() throws Exception {
        assertRegistry("https://index.docker.io/v1/", "acme/test");
        assertRegistry("https://index.docker.io/v1/", "busybox");
        assertRegistry("https://docker.acme.com:8080", "docker.acme.com:8080/acme/test");
        assertRegistry("https://docker.acme.com", "docker.acme.com/acme/test");
        assertRegistry("https://docker.acme.com", "docker.acme.com/busybox");
    }

    @Test
    public void testParseWithTags() throws Exception {
        assertRegistry("https://index.docker.io/v1/", "acme/test:tag");
        assertRegistry("https://index.docker.io/v1/", "busybox:tag");
        assertRegistry("https://docker.acme.com:8080", "docker.acme.com:8080/acme/test:tag");
        assertRegistry("https://docker.acme.com", "docker.acme.com/acme/test:tag");
        assertRegistry("https://docker.acme.com", "docker.acme.com/busybox:tag");
    }

    private void assertRegistry(String url, String repo) throws IOException {
        assertEquals(url, DockerRegistryEndpoint.fromImageName(repo, null).getEffectiveUrl().toString());
    }

}
