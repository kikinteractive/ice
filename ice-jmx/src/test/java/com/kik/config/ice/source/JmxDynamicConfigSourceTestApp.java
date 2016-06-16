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
package com.kik.config.ice.source;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.kik.config.ice.ConfigConfigurator;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.internal.ConfigBuilder;
import com.kik.config.ice.internal.ConstantValuePropertyAccessor;
import com.kik.config.ice.internal.OverrideModule;
import com.kik.config.ice.internal.PropertyAccessor;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import org.slf4j.LoggerFactory;

/**
 * Useful application to test JMX functionality with
 * <br>
 * Run via maven with:
 * <pre>
 * mvn exec:java -Dexec.mainClass="JmxDynamicConfigSourceTestApp" -Dexec.classpathScope="test"
 * </pre>
 */
@Singleton
public class JmxDynamicConfigSourceTestApp
{
    public static void setLogLevel(Class<?> loggingClass, Level logLevel)
    {
        Logger logger = (Logger) LoggerFactory.getLogger(loggingClass);
        logger.setLevel(logLevel);
    }

    public static void main(String[] args)
    {
        // Logging setup
        setLogLevel(JmxDynamicConfigSource.class, Level.TRACE);
        setLogLevel(ConfigDynamicMBean.class, Level.TRACE);
        setLogLevel(ConfigBuilder.class, Level.TRACE);
        setLogLevel(OverrideModule.class, Level.TRACE);
        setLogLevel(PropertyAccessor.class, Level.TRACE);
        setLogLevel(ConstantValuePropertyAccessor.class, Level.TRACE);

        // Create Guice Injector
        Injector injector = Guice.createInjector(
            ConfigConfigurator.testModules(),
            JmxDynamicConfigSource.module(),
            ExampleComponent.module(),
            new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());
                }
            });

        // "run" app
        JmxDynamicConfigSourceTestApp app = injector.getInstance(JmxDynamicConfigSourceTestApp.class);
        app.run();
    }

    /**
     * Injection needed for Guice to create the component. Config for this component is then manipulated by the tester
     * via a JMX client application.
     */
    @Inject
    private ExampleComponent example;

    @Inject
    private ConfigSystem configSystem;

    public void run()
    {
        configSystem.validateStaticConfiguration();

        System.out.println("JMX beans should be initialized...");
        System.out.println("PRESS ANY KEY TO EXIT");
        try {
            System.in.read();
        }
        catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }
}
