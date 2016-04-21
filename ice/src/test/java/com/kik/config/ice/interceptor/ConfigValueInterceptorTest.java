package com.kik.config.ice.interceptor;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.kik.config.ice.ConfigConfigurator;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.annotations.DefaultValue;
import com.kik.config.ice.source.DebugDynamicConfigSource;
import java.util.Optional;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ConfigValueInterceptorTest
{
    private static final String cfgName1 = "com.kik.config.ice.interceptor.ConfigValueInterceptorTest$Example$Config.myValue";

    //<editor-fold defaultstate="collapsed" desc="example class">
    @Singleton
    private static class Example
    {
        public interface Config
        {
            @DefaultValue("asdf")
            String myValue();
        }

        @VisibleForTesting
        @Inject
        public Config config;

        public static Module module()
        {
            return new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    bind(Example.class);
                    install(ConfigSystem.configModule(Config.class));
                }
            };
        }
    }
    //</editor-fold>

    @Inject
    DebugDynamicConfigSource debugSource;

    @Inject
    Example example;

    @Test(timeout = 5000)
    public void testInterceptedValue() throws Exception
    {
        Injector injector = Guice.createInjector(
            ConfigConfigurator.testModules(),
            Example.module(),
            ExampleInterceptor.module(5),
            UnwantedInterceptor.module(10));

        injector.injectMembers(this);

        assertEquals("asdf", example.config.myValue());

        debugSource.fireEvent(cfgName1, Optional.of("abcd"));
        assertEquals("abcdefgh", example.config.myValue());
    }
}
