# ice-zk

Provides a `ZooKeeperDynamicConfigSource` for managing configuration overrides via ZooKeeper nodes.

## Overview

* On system startup, ZooKeeperDynamicConfigSource will create a node in ZooKeeper for each config value.
  * Nodes are created with the following scheme (`${}` indicates the appropriate value would be substituted):
```
/${appDefinedRoot}/config/overrides/${fullConfigPkgAndClassName}.${methodName}[:${optionalContext}]
```
* For each running application instance, an ephemeral node is created named with the hostname and PID of the application.  This allows you to easily determine which applications and instances thereof that are actively using which config values.

## Usage
### Project Imports

You can use ice-zk with maven by adding the following to your pom.xml:
```xml
<dependencies>
  <dependency>
    <groupId>com.kik.config</groupId>
    <artifactId>ice-zk</artifactId>
    <version>1.0.4</version>
  </dependency>
</dependencies>
```

### ZooKeeperDynamicConfigSource Configuration

Configuration for the ZooKeeperDynamicConfigSource itself is accomplished by binding String values in guice using Guice's `@Named` annotation to distinguish them.  The config source has one *required* configuration, and several optional ones.

Below is an example defining some of these values in a Module to be included in an application's injector creation:

```java
public class ZkConfigModule extends AbstractModule
{
  @Override
  protected void configure()
  {
    // Install the ZooKeeperDynamicConfigSource itself
    install(ZooKeeperDynamicConfigSource.module());

    // Remainder is configuration for the ZooKeeperDynamicConfigSource singleton

    // Connection string is REQUIRED
    bind(String.class).annotatedWith(Names.named(ZooKeeperDynamicConfigSource.CONFIG_CONNECTION_STRING))
      .toInstance("my-zk-server.foo.com:2181");

    // All the rest are optional.
    bind(String.class).annotatedWith(Names.named(ZooKeeperDynamicConfigSource.CONFIG_CURATOR_NAMESPACE))
      .toInstance("myApp");
    bind(Integer.class).annotatedWith(Names.named(ZooKeeperDynamicConfigSource.CONFIG_CURATOR_SESSION_TIMEOUT))
      .toInstance(60_000);
  }
}
```

Further configuration names can be found in [ZooKeeperDynamicConfigSource.java](https://github.com/kikinteractive/ice/blob/master/ice-zk/src/main/java/com/kik/config/ice/source/ZooKeeperDynamicConfigSource.java#L41-L51).  Default values are defined just under the config constants.

# Author
Kik Interactive Inc.

# License
Use of the "ice" configuration system is subject to the Terms & Conditions and the Acceptable Use Policy.

The source for the "ice" configuration system is available under the Apache 2.0 license. See the LICENSE.txt file for details.
