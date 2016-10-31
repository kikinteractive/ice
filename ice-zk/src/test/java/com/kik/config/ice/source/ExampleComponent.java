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

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.annotations.DefaultValue;
import com.kik.config.ice.annotations.NoDefaultValue;
import java.util.List;
import java.util.Optional;

@Singleton
public class ExampleComponent
{
    private static final String subComponentName = "EXAMPLE";

    //<editor-fold defaultstate="collapsed" desc="Config">
    public interface Config
    {
        @DefaultValue("true")
        boolean enabled();

        @NoDefaultValue
        Integer maxPageSize();

        @NoDefaultValue(innerType = String.class)
        Optional<String> connectionString();

        @DefaultValue(value = "a,b,c", innerType = String.class)
        List<String> hostnames();

        // Values beyond this point are intended to check the startup time of th DynamicConfigSource component

        @DefaultValue("1")
        int extraValue1();

        @DefaultValue("1")
        int extraValue2();

        @DefaultValue("1")
        int extraValue3();

        @DefaultValue("1")
        int extraValue4();

        @DefaultValue("1")
        int extraValue5();

        @DefaultValue("1")
        int extraValue6();

        @DefaultValue("1")
        int extraValue7();

        @DefaultValue("1")
        int extraValue8();

        @DefaultValue("1")
        int extraValue9();

        @DefaultValue("1")
        int extraValue10();

        @DefaultValue("1")
        int extraValue11();

        @DefaultValue("1")
        int extraValue12();

        @DefaultValue("1")
        int extraValue13();

        @DefaultValue("1")
        int extraValue14();

        @DefaultValue("1")
        int extraValue15();

        @DefaultValue("1")
        int extraValue16();

        @DefaultValue("1")
        int extraValue17();

        @DefaultValue("1")
        int extraValue18();

        @DefaultValue("1")
        int extraValue19();

        @DefaultValue("1")
        int extraValue20();

        @DefaultValue("1")
        int extraValue21();

        @DefaultValue("1")
        int extraValue22();

        @DefaultValue("1")
        int extraValue23();

        @DefaultValue("1")
        int extraValue24();

        @DefaultValue("1")
        int extraValue25();

        @DefaultValue("1")
        int extraValue26();

        @DefaultValue("1")
        int extraValue27();

        @DefaultValue("1")
        int extraValue28();
    }
    //</editor-fold>

    @VisibleForTesting
    @Inject
    @Named(subComponentName)
    ExampleSubComponent subComp;

    @VisibleForTesting
    @Inject
    Config config;

    public static Module module()
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bind(ExampleComponent.class);
                install(ConfigSystem.configModule(Config.class));

                install(ExampleSubComponent.module(Names.named(subComponentName)));
                install(ConfigSystem.configModule(ExampleSubComponent.Config.class, Names.named(subComponentName)));
            }
        };
    }
}
