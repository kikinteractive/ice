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
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.kik.config.ice.ConfigConfigurator;
import com.kik.zookeeper.ZooKeeperServerRule;
import org.junit.ClassRule;
import org.junit.Test;

public class NoConfigDescriptorTest
{
    @ClassRule
    public static final ZooKeeperServerRule serverRule = new ZooKeeperServerRule();

    @Test(timeout = 5000)
    public void testNoConfigDescriptors()
    {
        Injector injector = Guice.createInjector(ConfigConfigurator.testModules(), ZooKeeperDynamicConfigSource.module(), new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bind(String.class).annotatedWith(Names.named(ZooKeeperDynamicConfigSource.CONFIG_CONNECTION_STRING)).toInstance(serverRule.getConnectionString());
            }
        });

        injector.injectMembers(this);
    }
}
