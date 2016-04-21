package com.kik.config.ice.example;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.PrivateModule;
import com.google.inject.name.Named;
import com.kik.config.ice.annotations.DefaultValue;

public class Example3B
{
    public interface Config
    {
        @DefaultValue("true")
        Boolean enabled();

        @DefaultValue("10")
        Integer maxQueueSize();

        @DefaultValue("5")
        Integer threadCount();
    }

    @Inject
    @VisibleForTesting
    Config config;

    public static Module module(final Named name)
    {
        return new PrivateModule()
        {
            @Override
            protected void configure()
            {
                // Alias named Example3B to unnamed
                bind(Config.class).to(Key.get(Config.class, name));

                bind(Example3B.class).annotatedWith(name).to(Example3B.class);
                expose(Key.get(Example3B.class, name));
            }
        };
    }
}
