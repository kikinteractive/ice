package com.kik.config.ice.example;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.PrivateModule;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.util.Modules;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.annotations.DefaultValue;
import com.kik.config.ice.annotations.NoDefaultValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ProviderExample implements Provider<ProviderExample.Foo>
{
    private static final Logger log = LoggerFactory.getLogger(ProviderExample.class);

    public interface Config
    {
        @NoDefaultValue
        String keyspaceName();

        @NoDefaultValue
        String connectionString();

        @DefaultValue("true")
        boolean enableAutoNodeDiscovery();
    }

    @Inject
    private Config config;

    @Inject
    public ProviderExample()
    {

    }

    @Override
    public Foo get()
    {
        return new Foo();
    }

    public static class Foo
    {
    }

    public static Module module(final Named name)
    {
        return Modules.combine(
            new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    // ConfigSystem.configModule() creates a module with a lot of bindings that need to be non-private.
                    // make sure you don't specify this within the PrivateModule.
                    install(ConfigSystem.configModule(ProviderExample.Config.class, name));
                }
            },
            new PrivateModule()
            {
                @Override
                protected void configure()
                {
                    // NOTE: MUST explicitly bind and expose this class within the private module for Guice to be able
                    // to use the alias binding below during injection of this class.
                    bind(ProviderExample.class).annotatedWith(name).to(ProviderExample.class);
                    expose(Key.get(ProviderExample.class, name));

                    // alias binding; only applies to other bind() or install() methods in this private module. Do not expose this.
                    // This allows us to @Inject a Config instance into this class w/o knowing the bound name in advance.
                    bind(Config.class).to(Key.get(Config.class, name));

                    // Binding for the class being provided by the provider
                    bind(ProviderExample.Foo.class)
                        .annotatedWith(name)
                        .toProvider(Key.get(ProviderExample.class, name))
                        .in(Scopes.SINGLETON);
                    expose(Key.get(ProviderExample.Foo.class, name));
                }
            });
    }
}
