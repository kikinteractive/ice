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
package com.kik.config.ice.convert;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kik.config.ice.ConfigConfigurator;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.ExplicitBindingModule;
import com.kik.config.ice.annotations.DefaultValue;
import com.kik.config.ice.source.DebugDynamicConfigSource;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class EndToEndValueConvertersTest
{
    public interface Config
    {
        @DefaultValue("123")
        String stringVal();

        @DefaultValue(value = "a,b,c", innerType = String.class)
        List<String> listOfStrings();

        @DefaultValue(value = "abc,\"foo,bar\"", innerType = String.class)
        Set<String> setOfStrings();
    }

    @Inject
    ConfigSystem configSystem;

    @Inject
    DebugDynamicConfigSource configSource;
    @Inject
    Config config;

    @Test(timeout = 5000)
    public void simpleHapyPath()
    {
        Injector injector = Guice.createInjector(
            new ExplicitBindingModule(),
            ConfigConfigurator.testModules(),
            ConfigSystem.configModule(Config.class));

        injector.injectMembers(this);

        configSystem.validateStaticConfiguration();

        assertEquals("123", config.stringVal());
        assertEquals(Lists.newArrayList("a", "b", "c"), config.listOfStrings());
        assertEquals(Sets.newHashSet("foo,bar", "abc"), config.setOfStrings());

        configSource.fireEvent("com.kik.config.ice.convert.EndToEndValueConvertersTest$Config.setOfStrings", Optional.of("1,2,3"));
        assertEquals(Sets.newHashSet("3", "2", "1"), config.setOfStrings());
    }

}
