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
package com.kik.config.ice.naming;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Singleton;
import java.lang.reflect.Method;
import java.util.Optional;

@Singleton
public class SimpleConfigNamingStrategy implements ConfigNamingStrategy
{
    @Override
    public String methodToFlatName(Method method, Optional<String> scopeOpt)
    {
        final String className = method.getDeclaringClass().getName();
        final String methodName = method.getName();
        return scopeOpt.isPresent()
            ? String.format("%s.%s:%s", className, methodName, scopeOpt.get())
            : String.format("%s.%s", className, methodName);
    }

    public static Module module()
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bind(ConfigNamingStrategy.class).to(SimpleConfigNamingStrategy.class);
            }
        };
    }
}
