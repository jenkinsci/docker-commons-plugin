# Version history (archive)

[![GitHub release](https://img.shields.io/github/release/jenkinsci/docker-commons-plugin.svg?label=changelog)](https://github.com/jenkinsci/docker-commons-plugin/releases/latest)

### Newer versions

See [GitHub Releases](https://github.com/jenkinsci/docker-commons-plugin/releases/latest).

### 1.15

Release date: (May 13, 2019)

-   [JENKINS-57439](https://issues.jenkins-ci.org/browse/JENKINS-57439) - 
    Add support of Docker credentials specification in Declarative Pipeline
    - Documentation: <https://github.com/jenkinsci/docker-commons-plugin#declarative-pipeline-example>
-   [JENKINS-52724](https://issues.jenkins-ci.org/browse/JENKINS-52724),  
    [JENKINS-52737](https://issues.jenkins-ci.org/browse/JENKINS-52737)
      - Inherit properties from the
    local Docker config.json if it exists
-   [JENKINS-57420](https://issues.jenkins-ci.org/browse/JENKINS-57420)
      - Use a new way to specify multi-line secrets in the plugin

### 1.14

Release date: (April 2, 2019)

-   Improve help text formatting ([PR 73](https://github.com/jenkinsci/docker-commons-plugin/pull/73))
-   Developer: Add new API for getting Docker-related credentials for [JENKINS-48437](https://issues.jenkins-ci.org/browse/JENKINS-48437)
    

### 1.13

Release date: (May 18, 2018)

-   API for [JENKINS-51397](https://issues.jenkins-ci.org/browse/JENKINS-51397) -
 `docker login` was being called without server-related environment variables in the Docker Pipeline Plugin
    

### 1.12

Release date: (May 11, 2018)

-   [JENKINS-38018](https://issues.jenkins-ci.org/browse/JENKINS-38018) - `withDockerRegistry` was failing to authenticate with DockerHub
-   [JENKINS-41880](https://issues.jenkins-ci.org/browse/JENKINS-41880) - Fix the `ConcurrentModificationException` while creating docker image fingerprints
-   [JENKINS-49075](https://issues.jenkins-ci.org/browse/JENKINS-49075) -
    Docker Commons Plugin was not working with PCT
    (implies a newer Jenkins core dependency)

### 1.11

Release date: (Jan 04, 2018)

-   [JENKINS-48674](https://issues.jenkins-ci.org/browse/JENKINS-48674) -
    Failure to download newer Docker releases from the automatic tool installer.

### 1.10

Release date: (Dec 11, 2017)

-   [JENKINS-48453](https://issues.jenkins-ci.org/browse/JENKINS-48453)
      Regression in Jenkins 2.93+ when using server credentials.

### 1.9

Release date: (Oct 10, 2017)

-   Removing `icon-shim` plugin dependency.

### 1.8

Release date: (Jul 10, 2017)

-   [Fix security issue](https://jenkins.io/security/advisory/2017-07-10/)

### 1.7

Release date: (Jun 16, 2017)

-   Always include port in image name.
-   Add credentials binding implementation for Docker server credentials.

### 1.6

Release date: (Jan 11, 2017)

-   ![(error)](docs/images/error.svg)
    [JENKINS-39181](https://issues.jenkins-ci.org/browse/JENKINS-39181)
    Fix Invalid fully qualified image name when registry URL is specified

### 1.5

Release date: (Oct 05, 2016)

-   ![(plus)](docs/images/add.svg)
    [JENKINS-38018](https://issues.jenkins-ci.org/browse/JENKINS-38018)
    API for more informative logging about use of registry credentials.

### 1.4.1

Release date: (Sep 08, 2016)

-   ![(error)](docs/images/error.svg)
    [JENKINS-36082](https://issues.jenkins-ci.org/browse/JENKINS-36082)
    [JENKINS-32790](https://issues.jenkins-ci.org/browse/JENKINS-32790)
    The Docker tool installer (used for example by `withTool` in the
    [Docker Pipeline Plugin](https://wiki.jenkins.io/display/JENKINS/Docker+Pipeline+Plugin))
    was broken.

### 1.4.0

Release date: (Jun 17, 2016)

-   ![(info)](docs/images/information.svg) Migration to the new parent POM
    ([JENKINS-35018](https://issues.jenkins-ci.org/browse/JENKINS-35018))
-   ![(info)](docs/images/information.svg) Jenkins core dependency has been updated to 1.580.x

### 1.3.1

Release date: (Feb 19, 2016)

-   ![(error)](docs/images/error.svg) Polishing the fix of
    [JENKINS-28776](https://issues.jenkins-ci.org/browse/JENKINS-28776)

### 1.3

Release date: (Feb 08, 2016)

-   ![(plus)](docs/images/add.svg) Support the "sha256:" prefix in image ID, required
    for Docker 1.10
    ([JENKINS-32792](https://issues.jenkins-ci.org/browse/JENKINS-32792))
-   ![(plus)](docs/images/add.svg) Replace build action icon stubs by Docker icons
    ([JENKINS-28776](https://issues.jenkins-ci.org/browse/JENKINS-28776))

### 1.2

Release date: (Jul 29 2015)

-   ![(error)](docs/images/error.svg) NPE when using credentials together with docker 1.7
    ([JENKINS-29627](https://issues.jenkins-ci.org/browse/JENKINS-29627))
-   ![(plus)](docs/images/add.svg) Support creation of named fingerprints in the API
    ([JENKINS-29098](https://issues.jenkins-ci.org/browse/JENKINS-29098))

### 1.1

Release date: (Jul 07 2015)

-   ![(error)](docs/images/error.svg) Cleanup of FindBugs issues, update of the CI system
-   ![(error)](docs/images/error.svg) Fix the issue with the Docker Tool selector ([PR
    \#40](https://github.com/jenkinsci/docker-commons-plugin/pull/40))
-   ![(plus)](docs/images/add.svg) Support Docker 1.7+ .docker/config.json ([PR
    \#38](https://github.com/jenkinsci/docker-commons-plugin/pull/38))

### 1.0

Release date: (May 22 2015)

-   First stable release
-   There were alpha releases before this release, see the commit history in GitHub
