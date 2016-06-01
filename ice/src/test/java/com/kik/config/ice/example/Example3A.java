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

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.annotations.DefaultValue;

@Singleton
public class Example3A
{
    public static final String OTHER_NAME = "ex3a_specific";

    public interface Config
    {
        @DefaultValue("5000")
        Integer cacheMaxSize();
    }

    @Inject
    @VisibleForTesting
    Config config;

    @Inject
    @VisibleForTesting
    @Named(OTHER_NAME)
    Example3B otherComponent;

    public static Module module()
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                // setup config impl for this class
                install(ConfigSystem.configModule(Example3A.Config.class));

                install(Example3B.module(Names.named(OTHER_NAME)));

                // setup named config impl for other component
                install(ConfigSystem.configModuleWithOverrides(Example3B.Config.class, Names.named(OTHER_NAME), om -> {
                    om.override(om.id().enabled()).withValue(false);
                    om.override(om.id().maxQueueSize()).withValue(100);
                    om.override(om.id().threadCount()).withValue(12);
                }));
            }
        };
    }
}
