# Docker Commons API Plugin for Jenkins

[![Join the chat at https://gitter.im/jenkinsci/docker](https://badges.gitter.im/jenkinsci/docker.svg)](https://gitter.im/jenkinsci/docker?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/docker-commons.svg)](https://plugins.jenkins.io/docker-commons)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/docker-commons-plugin.svg?label=changelog)](https://github.com/jenkinsci/docker-commons-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/role-strategy.svg?color=blue)](https://plugins.jenkins.io/docker-commons)

API plugin, which provides the common shared functionality for various Docker-related plugins.

## Summary

* API for managing Docker image and container fingerprints
* Credentials and location of Docker Registry
* Credentials and location of Docker Daemon (aka Docker Remote API)
* <code>ToolInstallation</code> for Docker CLI clients
* <code>DockerImageExtractor</code> extension point to get Docker image relations from jobs
* Simple UI referring related image fingerprints in Docker builds
* etc.

## Use-cases

### Credentials and locations

This allows users to configure one set of endpoint/credentials and use it across all the Docker related plugins, 
thereby keeping configuration more [DRY](http://en.wikipedia.org/wiki/Don%27t_repeat_yourself).

See [Docker Pipeline Plugin](https://plugins.jenkins.io/docker-workflow) for the typical usage.

## Declarative pipeline example

An example on how to bind Docker host/daemon credentials in a declarative pipeline: 

```groovy
pipeline {
  agent any
  tools {
    // a bit ugly because there is no `@Symbol` annotation for the DockerTool
    // see the discussion about this in PR 77 and PR 52: 
    // https://github.com/jenkinsci/docker-commons-plugin/pull/77#discussion_r280910822
    // https://github.com/jenkinsci/docker-commons-plugin/pull/52
    'org.jenkinsci.plugins.docker.commons.tools.DockerTool' '18.09'
  }
  environment {
    DOCKER_CERT_PATH = credentials('id-for-a-docker-cred')
  }
  stages {
    stage('foo') {
      steps {
        sh "docker version" // DOCKER_CERT_PATH is automatically picked up by the Docker client
      }
    }
  }
}
```

## License

[MIT License](http://opensource.org/licenses/MIT)

## Changelog

* See [GitHub Releases](https://github.com/jenkinsci/docker-commons-plugin/releases/latest) for the recent versions
* See [the release notes archive](./CHANGELOG.md) for version `1.15` and older
