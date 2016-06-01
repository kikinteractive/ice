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
import com.kik.config.ice.source.FileDynamicConfigSource;
import java.time.Duration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class Example1Test
{
    @Inject
    Example1 example;

    @Test(timeout = 5000)
    @SuppressWarnings("UnnecessaryUnboxing")
    public void testExample1() throws Exception
    {
        Injector injector = Guice.createInjector(Modules.override(
            ConfigConfigurator.standardModules(),
            Example1.module())
            .with(new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    bind(String.class).annotatedWith(named(FileDynamicConfigSource.FILENAME_NAME)).toInstance(getClass().getResource("Example1.config").getFile());
                }
            })
        );

        injector.injectMembers(this);

        assertNotNull(example);
        example.doStuff();

        assertEquals("testFoo", example.config.connectionString());
        assertEquals(123L, example.config.connectionTimeout().longValue());
        assertEquals(Duration.parse("P1DT2H32M18S"), example.config.delayTime());
    }

}
