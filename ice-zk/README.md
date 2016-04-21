# ice-zk

Provides a `ZooKeeperDynamicConfigSource`.

This config source will create a node in ZooKeeper for each config value.  For each running application instance, an ephemeral node is created, with the hostname and PID of the application, so you can determine what application(s) are actively using which values.

By default, the nodes are created with the following scheme (`${}` indicates the appropriate value would be substituted):
```
/app/config/overrides/${fullConfigPkgAndClassName}.${methodName}[:${optionalContext}]
```

You can use ice-zk with maven by adding the following to your pom.xml:
```xml
<dependencies>
  <dependency>
    <groupId>com.kik.config</groupId>
    <artifactId>ice-zk</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

# Author
Kik Interactive Inc.

# License
Use of the "ice" configuration system is subject to the Terms & Conditions and the Acceptable Use Policy.

The source for the "ice" configuration system is available under the Apache 2.0 license. See the LICENSE.txt file for details.
