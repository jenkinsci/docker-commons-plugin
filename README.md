# Docker Commons API Plugin for Jenkins

API plugin, which provides the common shared functionality for various Docker-related plugins.

## Summary

* API for managing Docker image and container fingerprints
* Credentials and location of Docker Registry
* Credentials and location of Docker Daemon (aka Docker Remote API)
* <code>ToolInstallation</code> for Docker CLI clients
* <code>DockerImageExtractor</code> extension point to get Docker image relations from jobs
* Simple UI referring related image fingerprints in Docker builds
* etc.

More info is available on the plugin's [Wiki page](https://wiki.jenkins-ci.org/display/JENKINS/Docker+Commons+Plugin)

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
