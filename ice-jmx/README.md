# ice-jmx

Provides a `JmxDynamicConfigSource`.

This config source exposes configuration values via JMX.  Values viewed in JMX are the values the config system is currently providing.  Values set via JMX are stringified values that provide overrides at the priority that the JmxDynamicConfigSource is installed with.  Note that setting an empty string is the equivalent of removing the JMX override.

You can use ice-jmx with maven by adding the following to your pom.xml:
```xml
<dependencies>
  <dependency>
    <groupId>com.kik.config</groupId>
    <artifactId>ice-jmx</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

# Author
Kik Interactive Inc.

# License
Use of the "ice" configuration system is subject to the Terms & Conditions and the Acceptable Use Policy.

The source for the "ice" configuration system is available under the Apache 2.0 license. See the LICENSE.txt file for details.
