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

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kik.config.ice.ConfigConfigurator;
import com.kik.config.ice.ExplicitBindingModule;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class FileDynamicConfigSourceNoConfigDescriptorsTest
{
    @Inject
    private FileDynamicConfigSource source;

    @Test(timeout = 5000)
    public void testNoConfigDescriptors()
    {
        Injector createInjector = Guice.createInjector(
            new ExplicitBindingModule(),
            ConfigConfigurator.standardModules(),
            FileDynamicConfigSource.module());
        createInjector.injectMembers(this);

        assertNotNull(source);
    }
}
