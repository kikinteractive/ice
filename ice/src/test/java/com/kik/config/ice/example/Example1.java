package com.kik.config.ice.example;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.annotations.DefaultValue;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Example1
{
    private static final Logger log = LoggerFactory.getLogger(Example1.class);

    public interface Config
    {
        @DefaultValue("testFoo")
        String connectionString();

        @DefaultValue("123")
        Long connectionTimeout();

        @DefaultValue("P1DT2H32M18S")
        Duration delayTime();
    }

    @Inject
    @VisibleForTesting
    Config config;

    public void doStuff()
    {
        log.info("Connection string was: {} with timeout {}",
            config.connectionString(),
            config.connectionTimeout());
        log.info("DelayTime was: {}", config.delayTime());
    }

    public static Module module()
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                install(ConfigSystem.configModule(Config.class));
            }
        };
    }
}
