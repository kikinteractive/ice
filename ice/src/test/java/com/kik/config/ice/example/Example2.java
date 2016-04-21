package com.kik.config.ice.example;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.annotations.DefaultValue;
import java.time.Duration;

public class Example2
{
    public interface Config
    {
        @DefaultValue(value = "testFoo")
        String connectionString();

        @DefaultValue("123")
        long connectionTimeout();

        @DefaultValue("P1DT2H32M18S")
        Duration delayTime();
    }

    @Inject
    @VisibleForTesting
    Config config;

    public static Module module()
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                install(ConfigSystem.configModule(Example2.Config.class));
            }
        };
    }
}
