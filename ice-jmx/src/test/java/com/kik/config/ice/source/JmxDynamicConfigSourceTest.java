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
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kik.config.ice.ConfigConfigurator;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.internal.ConfigBuilder;
import com.kik.config.ice.internal.ConstantValuePropertyAccessor;
import com.kik.config.ice.internal.OverrideModule;
import com.kik.config.ice.internal.PropertyAccessor;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Optional;
import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

@Slf4j
public class JmxDynamicConfigSourceTest
{
    static {
        setLogLevel(Level.TRACE,
            JmxDynamicConfigSource.class,
            ConfigDynamicMBean.class,
            ConfigBuilder.class,
            OverrideModule.class,
            PropertyAccessor.class,
            ConstantValuePropertyAccessor.class);
    }

    @Inject
    private ConfigSystem configSystem;

    @Inject
    private ExampleComponent component;

    // NOTE: do not setup as a JUnit @ClassRule
    @Inject
    private JmxRemoteTestRule testRule;

    private ObjectName objName1;
    private ObjectName objName2;

    @Before
    public void setup() throws Exception
    {
        objName1 = new ObjectName("com.kik.config.ice.source:name=ExampleComponentIceMBean");
        objName2 = new ObjectName("com.kik.config.ice.source:name=ExampleSubComponentIceMBean,scope=EXAMPLE");

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

        injector.injectMembers(this);

        // NOTE: need to run this validation to force eager construction by Guice
        configSystem.validateStaticConfiguration();

        testRule.before();
    }

    @After
    public void teardown() throws Exception
    {
        testRule.after();
    }

    @Test(timeout = 5000)
    public void testGettingAttributes() throws Exception
    {
        testRule.remoteTest(mbsc -> {
            assertEquals(true, mbsc.getAttribute(objName1, "enabled"));
            assertNull(mbsc.getAttribute(objName1, "maxPageSize"));

            // Note: NOT Optional.empty() due to needing to be able to update it via standard JMX tools.
            assertNull(mbsc.getAttribute(objName1, "connectionString"));
            assertEquals("a,b,c", mbsc.getAttribute(objName1, "hostnames"));

            assertEquals(5, mbsc.getAttribute(objName2, "maxRetries"));
            assertEquals(10000L, mbsc.getAttribute(objName2, "defaultTimeout"));
            assertEquals(Duration.parse("PT5M30S"), mbsc.getAttribute(objName2, "expiry"));

            // Setting
            mbsc.setAttribute(objName1, new Attribute("enabled", false));
            assertFalse(component.config.enabled());
            assertEquals(false, mbsc.getAttribute(objName1, "enabled"));

            mbsc.setAttribute(objName1, new Attribute("connectionString", "asdf"));
            assertEquals(Optional.of("asdf"), component.config.connectionString());
            assertEquals("asdf", mbsc.getAttribute(objName1, "connectionString"));

            Duration dur = Duration.parse("PT6M15S");
            mbsc.setAttribute(objName2, new Attribute("expiry", dur));
            assertEquals(dur, component.subComp.config.expiry());
            assertEquals(dur, mbsc.getAttribute(objName2, "expiry"));
        });
    }

    public static void setLogLevel(Level logLevel, Class<?>... loggingClasses)
    {
        for (Class<?> loggingClass : loggingClasses) {
            final Logger logger = (Logger) LoggerFactory.getLogger(loggingClass);
            logger.setLevel(logLevel);
        }
    }
}
