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

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kik.config.ice.ConfigConfigurator;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class NoDefaultsTest
{
    @Inject
    NoDefaultsExample example;

    @Test(timeout = 5000)
    public void testExample1() throws Exception
    {
        Injector injector = Guice.createInjector(
            ConfigConfigurator.testModules(),
            NoDefaultsExample.module());
        injector.injectMembers(this);

        assertNotNull(example);
        assertNotNull(example.config);

        assertNull(example.config.timeout());

        assertNotNull(example.config.delayTime());
        assertFalse(example.config.delayTime().isPresent());

        assertNotNull(example.config.enableMaybe());
        assertTrue(example.config.enableMaybe().isPresent());
        assertTrue(example.config.enableMaybe().get());
    }
}
