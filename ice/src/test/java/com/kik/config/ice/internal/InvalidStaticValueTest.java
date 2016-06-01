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
package com.kik.config.ice.internal;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import com.kik.config.ice.ConfigConfigurator;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.annotations.DefaultValue;
import org.junit.Test;

public class InvalidStaticValueTest
{
    //<editor-fold defaultstate="collapsed" desc="example class">
    @Singleton
    private static class InvalidValueExample
    {
        public interface Config
        {
            @DefaultValue("asdf") // intentionally bad
            Integer myValue();
        }

        @VisibleForTesting
        @Inject
        public Config config;

        public static Module module()
        {
            return new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    bind(InvalidValueExample.class);
                    install(ConfigSystem.configModule(Config.class));
                }
            };
        }
    }
    //</editor-fold>

    @Inject
    InvalidValueExample example;

    @Test(timeout = 5000, expected = ProvisionException.class)
    @SuppressWarnings("UnnecessaryUnboxing")
    public void testInvalidStaticValue()
    {
        Injector injector = Guice.createInjector(
            ConfigConfigurator.testModules(),
            InvalidValueExample.module());

        injector.injectMembers(this);

        example.config.myValue();
    }
}
