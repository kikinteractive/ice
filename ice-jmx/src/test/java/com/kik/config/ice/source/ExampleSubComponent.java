package com.kik.config.ice.source;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.PrivateModule;
import com.google.inject.name.Named;
import com.kik.config.ice.annotations.DefaultValue;
import java.time.Duration;

public class ExampleSubComponent
{
    public interface Config
    {
        @DefaultValue("5")
        int maxRetries();

        @DefaultValue("10000")
        long defaultTimeout();

        @DefaultValue("PT5M30S")
        Duration expiry();
    }

    @Inject
    Config config;

    public static Module module(final Named name)
    {
        return new PrivateModule()
        {
            @Override
            protected void configure()
            {
                // Alias unannotated config to annotated one
                bind(Config.class).to(Key.get(Config.class, name));

                bind(ExampleSubComponent.class).annotatedWith(name).to(ExampleSubComponent.class);
                expose(Key.get(ExampleSubComponent.class, name));
            }
        };
    }
}
