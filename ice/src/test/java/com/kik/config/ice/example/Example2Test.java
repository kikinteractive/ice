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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import static com.google.inject.name.Names.named;
import com.google.inject.util.Modules;
import com.kik.config.ice.ConfigConfigurator;
import com.kik.config.ice.source.DebugDynamicConfigSource;
import com.kik.config.ice.source.FileDynamicConfigSource;
import java.util.Optional;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class Example2Test
{
    private static final String cfgName1 = "com.kik.config.ice.example.Example2$Config.connectionTimeout";

    @Inject
    Example2 example;

    @Inject
    DebugDynamicConfigSource debugSource;

    @Test(timeout = 5000)
    @SuppressWarnings("UnnecessaryUnboxing")
    public void testFileOverridesDefaults() throws Exception
    {
        Injector injector = Guice.createInjector(Modules.override(
            ConfigConfigurator.standardModules(),
            Example2.module(),
            DebugDynamicConfigSource.module())
            .with(new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    bind(String.class).annotatedWith(named(FileDynamicConfigSource.FILENAME_NAME)).toInstance(getClass().getResource("Example2.config").getFile());
                }
            })
        );
        injector.injectMembers(this);

        assertNotNull(example);

        // Initial value from file source
        assertEquals(333L, example.config.connectionTimeout());

        // set debug source override, which has a higher priority than file source
        debugSource.fireEvent(cfgName1, Optional.of("500"));
        assertEquals(500L, example.config.connectionTimeout());

        // unset debug source override, should revert to previous override.
        debugSource.fireEvent(cfgName1, Optional.empty());
        assertEquals(333L, example.config.connectionTimeout());

        // set debug source override to an invalid value - no value change expected.
        debugSource.fireEvent(cfgName1, Optional.of("abcd"));
        assertEquals(333L, example.config.connectionTimeout());
    }
}
