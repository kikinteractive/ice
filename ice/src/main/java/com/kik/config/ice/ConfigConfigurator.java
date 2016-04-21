package com.kik.config.ice;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.kik.config.ice.interceptor.NoopConfigValueInterceptor;
import com.kik.config.ice.naming.SimpleConfigNamingStrategy;
import com.kik.config.ice.convert.ConfigValueConverters;
import com.kik.config.ice.source.DebugDynamicConfigSource;
import com.kik.config.ice.source.FileDynamicConfigSource;

/**
 * Used to setup a basic configuration in Guice
 */
public class ConfigConfigurator
{
    public static Module standardModules()
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                install(ConfigSystem.module());
                install(SimpleConfigNamingStrategy.module());
                install(ConfigValueConverters.module());

                install(FileDynamicConfigSource.module());
                install(NoopConfigValueInterceptor.module());
            }
        };
    }

    public static Module testModules()
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                install(ConfigSystem.module());
                install(SimpleConfigNamingStrategy.module());
                install(ConfigValueConverters.module());

                install(DebugDynamicConfigSource.module());
                install(NoopConfigValueInterceptor.module());
            }
        };
    }
}
