/*
 * Copyright 2016 Kik Interactive, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kik.config.ice;

import com.kik.config.ice.internal.ConfigBuilder;
import com.kik.config.ice.internal.OverrideModule;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.kik.config.ice.annotations.DefaultValue;
import com.kik.config.ice.exception.ConfigException;
import com.kik.config.ice.internal.ConstantValuePropertyAccessor;
import com.kik.config.ice.internal.PropertyAccessor;
import com.kik.config.ice.source.DebugDynamicConfigSource;
import com.kik.config.ice.source.FileDynamicConfigSource;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class ConfigSystemTest
{
    public static void setLogLevel(Class<?> loggingClass, Level logLevel)
    {
        Logger logger = (Logger) LoggerFactory.getLogger(loggingClass);
        logger.setLevel(logLevel);
    }

    static {
        setLogLevel(DebugDynamicConfigSource.class, Level.TRACE);
        setLogLevel(FileDynamicConfigSource.class, Level.TRACE);
        setLogLevel(ConfigBuilder.class, Level.TRACE);
        setLogLevel(OverrideModule.class, Level.TRACE);
        setLogLevel(PropertyAccessor.class, Level.TRACE);
        setLogLevel(ConstantValuePropertyAccessor.class, Level.TRACE);
    }

    //<editor-fold defaultstate="collapsed" desc="example classes">
    @Singleton
    private static class InvalidValueExample
    {
        public interface Config
        {
            @DefaultValue("asdf") // intentionally bad
            Integer myValue();
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
                    bind(InvalidValueExample.class);
                    install(ConfigSystem.configModule(Config.class));
                }
            };
        }
    }

    @Singleton
    private static class ValidValueExample
    {
        public interface Config
        {
            @DefaultValue("123")
            Integer myValue();
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
                    bind(ValidValueExample.class);
                    install(ConfigSystem.configModule(Config.class));
                }
            };
        }
    }
    //</editor-fold>

    @Inject
    ConfigSystem configSystem;

    @Test(timeout = 5000, expected = ConfigException.class)
    public void testValidateStaticConfigurationWithBad()
    {
        Injector injector = Guice.createInjector(
            ConfigConfigurator.testModules(),
            InvalidValueExample.module());

        injector.injectMembers(this);

        configSystem.validateStaticConfiguration();
    }

    @Test(timeout = 5000)
    public void testValidateStaticConfigurationWithGood()
    {
        Injector injector = Guice.createInjector(
            ConfigConfigurator.testModules(),
            ValidValueExample.module());

        injector.injectMembers(this);

        configSystem.validateStaticConfiguration();
    }
}
