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
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.PrivateModule;
import com.google.inject.name.Named;
import com.kik.config.ice.annotations.DefaultValue;

public class Example3B
{
    public interface Config
    {
        @DefaultValue("true")
        Boolean enabled();

        @DefaultValue("10")
        Integer maxQueueSize();

        @DefaultValue("5")
        Integer threadCount();
    }

    @Inject
    @VisibleForTesting
    Config config;

    public static Module module(final Named name)
    {
        return new PrivateModule()
        {
            @Override
            protected void configure()
            {
                // Alias named Example3B to unnamed
                bind(Config.class).to(Key.get(Config.class, name));

                bind(Example3B.class).annotatedWith(name).to(Example3B.class);
                expose(Key.get(Example3B.class, name));
            }
        };
    }
}
