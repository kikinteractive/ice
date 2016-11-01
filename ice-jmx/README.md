# ice-jmx

Provides a `JmxDynamicConfigSource` for managing configuration overrides via JMX MBeans.

## Overview
* Upon application startup, this config source will register an MBean for each configuration interface which has been installed in Guice.
* Values viewed in JMX are the values the config system is currently providing.  *There is an important distinction here!*
  * An override from a higher-priority DynamicConfigSource will be displayed over an override set by this JMX source.  
  * if no override is set via JMX or any higher-priority config source, the value of lower-priority config sources (or the static default) is provided.
* Values set via JMX are string versions of values that provide overrides at the priority that the JmxDynamicConfigSource is installed with.
  * Setting an empty string removes any JMX override.
  * Example: if you wanted to configure a `Duration` value, you would provide something like `"PT1M"`.

### Project Setup

You can use ice-jmx with maven by adding the following to your pom.xml:
```xml
<dependencies>
  <dependency>
    <groupId>com.kik.config</groupId>
    <artifactId>ice-jmx</artifactId>
    <version>1.0.4</version>
  </dependency>
</dependencies>
```

### JmxDynamicConfigSource Setup
No configuration for the JmxDynamicConfigSource itself is necessary.  

It does however depend on having an `MBeanServer` instance available to be injected.  This will require a binding for it, something along the lines of the following:

```java
bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());
```

### Bean Notes

Beans will be registered on application startup with a bean name based on the host class' package and Class Name.

For example, the following class:
```java
package com.foo.app;

public class MyComponent {
  public interface Config {
    @DefaultValue("true")
    boolean enabled();

    @DefaultValue("1000")
    int timeout();
  }
  // ... other component code ...
}
```

Would use the following bean name:
```
com.foo.app:name=MyComponentIceMBean
```

If it was configured with a scope name, it would have an additional `,scope={scopeName}` included.

This bean will have Attributes defined for each for the configuration values in Config interface defined within.

# Author
Kik Interactive Inc.

# License
Use of the "ice" configuration system is subject to the Terms & Conditions and the Acceptable Use Policy.

The source for the "ice" configuration system is available under the Apache 2.0 license. See the LICENSE.txt file for details.
