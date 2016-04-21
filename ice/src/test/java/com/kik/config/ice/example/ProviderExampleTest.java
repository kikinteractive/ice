package com.kik.config.ice.example;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.ConfigConfigurator;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

public class ProviderExampleTest
{
    @Inject
    Injector injector;

    @Before
    public void setup()
    {
        final Named name = Names.named("provider-test");

        Module module = Modules.override(
            ConfigConfigurator.testModules(),
            ProviderExample.module(name)
        ).with(new AbstractModule()
        {
            @Override
            protected void configure()
            {
                install(ConfigSystem.overrideModule(ProviderExample.Config.class, name, om -> {
                    om.override(om.id().connectionString()).withValue("foo bar");
                    om.override(om.id().keyspaceName()).withValue("keyspace baz");
                }));
            }
        });

        Guice.createInjector(module).injectMembers(this);
    }

    @Test(timeout = 5000L)
    public void testProviderConfig()
    {
        ProviderExample.Foo foo = injector.getInstance(ProviderExample.Foo.class);
        assertNotNull(foo);
    }
}
