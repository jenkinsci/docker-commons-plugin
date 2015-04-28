The common API for various docker-related plugins to share their configurations. It covers:

* Credentials and location of Docker Registry
* Credentials and location of Docker Daemon (aka Docker Remote API)

This allows users to configure one set of endpoint/credentials and use it across all the Docker
related plugins, thereby keeping configuration more [DRY](http://en.wikipedia.org/wiki/Don%27t_repeat_yourself).

See [here](src/test/java/org/jenkinsci/plugins/docker/commons/SampleDockerBuilder.java)
and [here](src/test/java/org/jenkinsci/plugins/docker/commons/SampleDockerRegistryBuilder.java)
and their corresponding `config.jelly` files for an example of how to use them.
