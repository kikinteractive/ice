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

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.kik.config.ice.convert.ConfigValueConverters;
import com.kik.config.ice.interceptor.NoopConfigValueInterceptor;
import com.kik.config.ice.internal.ConfigDescriptorHolder;
import com.kik.config.ice.naming.SimpleConfigNamingStrategy;
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
                bind(ConfigDescriptorHolder.class);
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
                bind(ConfigDescriptorHolder.class);
            }
        };
    }
}
