package com.kik.config.ice.source;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.annotations.DefaultValue;
import com.kik.config.ice.annotations.NoDefaultValue;
import java.util.List;
import java.util.Optional;

@Singleton
public class ExampleComponent
{
    private static final String subComponentName = "EXAMPLE";

    public interface Config
    {
        @DefaultValue("true")
        boolean enabled();

        @NoDefaultValue
        Integer maxPageSize();

        @NoDefaultValue(innerType = String.class)
        Optional<String> connectionString();

        @DefaultValue(value = "a,b,c", innerType = String.class)
        List<String> hostnames();
    }

    @Inject
    @Named(subComponentName)
    ExampleSubComponent subComp;

    @Inject
    Config config;

    public static Module module()
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bind(ExampleComponent.class);
                install(ConfigSystem.configModule(Config.class));

                install(ExampleSubComponent.module(Names.named(subComponentName)));
                install(ConfigSystem.configModule(ExampleSubComponent.Config.class, Names.named(subComponentName)));
            }
        };
    }
}
