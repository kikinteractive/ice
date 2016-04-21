package com.kik.config.ice.example;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.annotations.DefaultValue;
import com.kik.config.ice.annotations.NoDefaultValue;
import java.time.Duration;
import java.util.Optional;

public class NoDefaultsExample
{
    /**
     * NOTE: @NoDefaultValue can also be placed on the class to apply it to all members.
     */
    public interface Config
    {
        @NoDefaultValue
        Long timeout();

        @NoDefaultValue(innerType = Duration.class)
        Optional<Duration> delayTime();

        @DefaultValue(value = "true", innerType = Boolean.class)
        Optional<Boolean> enableMaybe();
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
                install(ConfigSystem.configModule(Config.class));
            }
        };
    }
}
