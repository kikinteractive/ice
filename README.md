# ICE (Interface Config Elements)

A configuration system written in Java 8 that leverages Dependency Injection via [Google Guice](https://github.com/google/guice).  It allows you to define your configuration with an annotated interface containing no-arg methods.

## Features
* Allows easy per-component configuration, with explicit default values and type conversion.
* Designed to allow easy extensions. Most common extensions would be:
  * `ConfigValueConverter` to handle converting configuration string values to the type you want returned in your config interface (see [here](https://github.com/kikinteractive/ice/tree/master/ice/src/main/java/com/kik/config/ice/convert) for existing converters)
  * `DynamicConfigSource` to provide additional sources of config overrides. See [here](https://github.com/kikinteractive/ice/tree/master/ice/src/main/java/com/kik/config/ice/source) for basic config sources)
* The config system uses rx.Observable internally, so there is no polling within the system (other than when watching for config file changes in `FileDynamicConfigSource`)
* You can optionally have your interface method provide an `Observable<X>`, which will return an observable that will never complete.  Your app can then subscribe to it for notification of changes to that config value.
* No confusing use of separate "bootstrap" guice injectors; All ice system classes are just included in your app's main injector.

## Usage
**Note:** *Maven artifacts are not yet published.  This notice will be removed when we get them published to Maven Central.*
### Project Setup
You can use ice with maven by adding the following to your pom.xml:
```xml
<dependencies>
  <dependency>
    <groupId>com.kik.config</groupId>
    <artifactId>ice</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

### Defining and Using Config
In your application class(es) you can define an interface for your config.  This interface has a few rules:

1. The interface currently needs to be `public`.
2. Methods need to be annotated with one of `@DefaultValue("some_value_here")` or `@NoDefaultValue`.
   1. `@DefaultValue("my_value_as_string")` specifies your config entry will have the given value as its default if no overrides are specified in any DynamicConfigSource.  The value must be specified in string string form.
   2. `@NoDefaultValue` specifies that your config entry will have the system default value if it is not overridden in any DynamicConfigSource.  i.e.: Objects will default to `null`. Primitives will default to `false`, `0`, or equivalent.  Optional<T> values will default to `Optional.empty()`.
3. Methods return whatever type is required in your app.
4. Methods cannot have any arguments.
5. If your method returns a generic type, you need to include the inner type in the annotation.  Eg: returning `Optional<Integer>` would require something like `@NoDefaultValue(innerType=Integer.class)`
6. If you want an Observable of a config value, there's a few extra things:
   1. There must be a non-observable method, with the same name and annotated with `@DefaultValue` or `@NoDefaultValue`
   2. The observable-returning method name needs to have the form `[otherMethodName]Observable`.  eg: for an int config entry named "foo", you would define `@DefaultValue("123") Integer foo();` and `Observable<Integer> fooObservable();`
   3. The system currently doesn't support Observables of other generic types.  I.e. no support for `Observable<List<String>>`
7. Methods marked `default` can be defined. They are ignored by the config system.


### Examples
#### Basic Example
An example component class in your application:
```java
public class ExampleComponent
{
    // Define your configuration
    public interface Config
    {
        @DefaultValue("3")
        int retryCount();

        @DefaultValue("PT0.5S")
        Duration timeout();
    }

    // Inject your configuration via Guice
    @Inject
    Config config;

    // Use the config elsewhere ...
    void foo() {
        int retries = config.retryCount();
        // ...
    }
}
```

Your Application's Guice bootstrap will need to include Guice bindings:
```java
public class MyApplicationModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        // Bindings for the ice system components
        install(ConfigConfigurator.standardModules());

        // ConfigConfigurator.standardModules() installs
        // FileDynamicConfigSource which can be configured with guice bindings
        // here:

        // FileDynamicConfigSource: config file path
        bind(String.class).annotatedWith(Names.named(FileDynamicConfigSource.FILENAME_NAME))
            .toInstance("/path/to/config/app.config");

        // FileDynamicConfigSource: config reload interval
        bind(Duration.class).annotatedWith(Names.named(FileDynamicConfigSource.POLL_INTERVAL_NAME))
            .toInstance(Duration.ofMinutes(30));

        // Binding for your component
        bind(ExampleComponent.class);

        // Install the generated Module for your component's configuration
        install(ConfigSystem.configModule(ExampleComponent.Config.class));

        // ... other bindings for your application ...
    }
}
```

The config file named in the example above (`/path/to/config/app.config`) might look like the following:
```
com.foo.app.ExampleComponent$Config.retryCount=5
com.foo.app.ExampleComponent$Config.timeout=PT0.65S
```

#### Example with Observable<Integer>
An example component defining an Observable:
```java
public class ExampleWithObservableComponent
{
    private static final Logger log = LoggerFactory.getLogger(Example1.class);

    public interface Config
    {
        @DefaultValue("true")
        Boolean enabled();

        @DefaultValue("100")
        Integer maxSize();

        Observable<Boolean> enabledObservable();

        Observable<Integer> maxSizeObservable();
    }

    @Inject
    Config config;

    public void foo()
    {
        rx.Subscription maxSizeSubscription = config.maxSizeObservable()
            .subscribe(maxSize ->
                log.info("Max Size changed to: {}", maxSize));
    }
}
```

Bindings for the above component class would be the same as the previous example.

#### Further Examples
Further examples of usage can be found in [com.kik.config.ice.example](https://github.com/kikinteractive/ice/tree/master/ice/src/test/java/com/kik/config/ice/example)


### Tips

* Since all default values in @DefaultValue annotations are untyped strings, you will want a means to verify they convert correctly to the appropriate type.  `ConfigSystem` class can be injected and used to validate that all static configuration values are correct.  See [ConfigSystem.java](https://github.com/kikinteractive/ice/blob/master/ice/src/main/java/com/kik/config/ice/ConfigSystem.java), specifically the `validateStaticConfiguration()` method.
* The various DynamicConfigSource implementations have default priorities.  The config value returned will be from the highest priority config source that has a value for your config value, or the default if none have an override value.  For all sources that are configured, the priority by default is:
  1. DebugDynamicConfigSource (priority value '0' -- highest priority)
  2. JmxDynamicConfigSource (priority value '25')
  3. ZooKeeperDynamicConfigSource (priority value '50')
  4. FileDynamicConfigSource (priority value '100')
  5. Static defaults configured by code (eg: with `@DefaultValue("foo")`)
* Type-safe configuration overrides can be done in your application's Guice bootstrap to account for such things as environment-specific configuration.  This is demonstrated partially in [ProviderExampleTest.java](https://github.com/kikinteractive/ice/blob/master/ice/src/test/java/com/kik/config/ice/example/ProviderExampleTest.java).  More documentation for this is forthcoming.

## Motivations
ICE was developed with a few ideas in mind, some of which were not readily available in other pre-existing configuration libraries.  These ideas were:

* **Simple setup with a single Guice injector.**  We wanted a simple setup of a single Guice injector for your app, which could be given a Module for the config system, and then modules for each config interface to be used.
* **Condensed config definition** Defining the config value's name, type, and default value is done all in one place, with inherent context to where it was defined.  Using interfaces also forces keeping configuration definitions together -- all your configuration for a class can be identified easily.
* **Reactive elements** ICE uses the excellent [RxJava](https://github.com/ReactiveX/RxJava) library internally, and makes your config values available via Observables.  This allows you to subscribe to config updates.  An example where this might be useful is for re-initializing a components on-the-fly without the need to poll for changes.
* **Flexible and Extendable** If you need several sources of configuration, you can use multiple DynamicConfigSource implementations concurrently, where sources are set up to override each other in a priority list.  If you need configuration from a custom system, you can implement a new DynamicConfigSource for that system.  Custom types in your config interfaces can be supported by providing guice bindings for more ConfigValueConverter implementations.  (See [ConfigValueConverters.java](https://github.com/kikinteractive/ice/blob/master/ice/src/main/java/com/kik/config/ice/convert/ConfigValueConverters.java)

# Author
Kik Interactive Inc.

# License
Use of the "ice" configuration system is subject to the Terms & Conditions and the Acceptable Use Policy.

The source for the "ice" configuration system is available under the Apache 2.0 license. See the LICENSE.txt file for details.
