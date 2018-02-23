Aired
===============
Toy project to try out SMACK stack.

Prerequisites
-------------
- Java 8
- sbt
- Docker engine
- Docker compose

Build
-----
Build process is a bit complicated due to https://github.com/sbt/sbt/issues/1448. Shared module has to be built and
published separately.
```bash
$ sbt +shared/publishLocal
```
To build docker images type:
```bash
$ sbt Docker/publishLocal
```

Run
---
Docker compose will take care of running all the containers.
```bash
$ docker-compose up
```


