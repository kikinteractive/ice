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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kik.config.ice.ConfigConfigurator;
import com.kik.config.ice.ConfigSystem;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class NoConfigDescriptorTest
{
    @Inject
    private ConfigSystem configSystem;
    @Inject
    private JmxDynamicConfigSource source;

    @Test(timeout = 5000)
    public void testNoConfigDescriptors()
    {
        Injector createInjector = Guice.createInjector(
            ConfigConfigurator.testModules(),
            JmxDynamicConfigSource.module(),
            new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());

                    // Ensure the test works with explicit bindings required
                    binder().requireExplicitBindings();
                }
            });

        createInjector.injectMembers(this);

        assertNotNull(configSystem);
        assertNotNull(source);

        configSystem.validateStaticConfiguration();
    }
}
