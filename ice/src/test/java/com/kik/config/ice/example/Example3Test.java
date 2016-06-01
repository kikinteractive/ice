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
package com.kik.config.ice.example;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import static com.google.inject.name.Names.named;
import com.google.inject.util.Modules;
import com.kik.config.ice.ConfigConfigurator;
import com.kik.config.ice.internal.ConfigBuilder;
import com.kik.config.ice.internal.ConstantValuePropertyAccessor;
import com.kik.config.ice.internal.OverrideModule;
import com.kik.config.ice.internal.PropertyAccessor;
import com.kik.config.ice.source.FileDynamicConfigSource;
import com.kik.config.ice.source.DebugDynamicConfigSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class Example3Test
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

    @Inject
    private Example3A example;

    @Test(timeout = 3000)
    @SuppressWarnings("UnnecessaryUnboxing")
    public void testNamedSubComponentOverrides() throws Exception
    {
        Injector injector = Guice.createInjector(Modules.override(
            ConfigConfigurator.standardModules(),
            Example3A.module(),
            DebugDynamicConfigSource.module())
            .with(new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    bind(String.class).annotatedWith(named(FileDynamicConfigSource.FILENAME_NAME)).toInstance(getClass().getResource("Example3.config").getFile());
                }
            })
        );
        injector.injectMembers(this);

        assertNotNull(example);
        assertNotNull(example.config);
        assertNotNull(example.otherComponent);
        assertNotNull(example.otherComponent.config);

        // Guice should have different objects for each of these configurations
        assertNotEquals(example.config, example.otherComponent.config);

        assertEquals(5000, example.config.cacheMaxSize().intValue());

        assertEquals(false, example.otherComponent.config.enabled().booleanValue());
        assertEquals(100, example.otherComponent.config.maxQueueSize().intValue());
        assertEquals(12, example.otherComponent.config.threadCount().intValue());
    }
}
