# ICE Releases

### Version 1.0.2 - October 7, 2016 ([Maven](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.kik.config%22%20AND%20v%3A%221.0.2%22))

#### Features

  - `DebugDynamicConfigSource` now allows a type-safe method of setting configuration values, useful for writing tests. A quick example would look like:
   ```java
   @Inject
   DebugDynamicConfigSource dcs;

   // later ...
   dcs.set(dcs.id(Config1.class).enabled()).toValue(true);
   ```

#### Bugfixes

  - Fixed a memory leak where generated implementation classes were indirectly holding references to the Guice Injector in a static member.  This manifested itself in large test runs when many injectors were being created over time.

### Version 1.0.1 - July 8, 2016 ([Maven](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.kik.config%22%20AND%20v%3A%221.0.1%22))

#### Bugfixes

  - Allow system to startup with warning when no configuration entries are defined

### Version 1.0.0 - June 16, 2016 ([Maven](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.kik.config%22%20AND%20v%3A%221.0.0%22))

  - Initial release
