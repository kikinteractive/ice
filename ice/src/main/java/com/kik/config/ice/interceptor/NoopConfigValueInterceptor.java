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
package com.kik.config.ice.interceptor;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.multibindings.MapBinder;
import java.lang.reflect.Method;
import java.util.Optional;

public class NoopConfigValueInterceptor implements ConfigValueInterceptor
{
    public static final int DEFAULT_FILTER_PRIORITY = 0;

    @Override
    public boolean shouldApply(Method configMethod, Optional<String> input)
    {
        return false;
    }

    @Override
    public Optional<String> apply(Optional<String> input)
    {
        throw new UnsupportedOperationException("NoopConfigValueInterceptor should never have apply called");
    }

    @Override
    public boolean stopChainOnApply()
    {
        throw new UnsupportedOperationException("NoopConfigValueInterceptor should never have stopChainOnApply called");
    }

    public static Module module()
    {
        return module(DEFAULT_FILTER_PRIORITY);
    }

    public static Module module(final int priority)
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                MapBinder<Integer, ConfigValueInterceptor> mapBinder = MapBinder.newMapBinder(binder(), Integer.class, ConfigValueInterceptor.class);
                mapBinder.addBinding(priority).to(NoopConfigValueInterceptor.class);
            }
        };
    }
}
