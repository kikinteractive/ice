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

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.kik.config.ice.exception.ConfigException;
import com.kik.config.ice.internal.ConfigDescriptor;
import com.kik.config.ice.internal.ConfigDescriptorHolder;
import com.kik.config.ice.internal.MethodIdProxyFactory;
import com.kik.config.ice.sink.ConfigEventSink;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * An {@link AbstractDynamicConfigSource} which allows direct manipulation of configuration values.
 * Inject this config source and use {@link #set(Object)} and {@link #id(Class)} to identify and change configuration
 * values.
 * Example:
 * <pre><code>
 * {@literal @}Inject
 * DebugDynamicConfigSource debugSource;
 * // later in class, setting value of foo()
 * debugSource.set(debugSource.id().foo()).toValue("abc");
 * // Also, the value may be cleared:
 * debugSource.set(debugSource.id().foo()).toEmpty();
 * </code></pre>
 */
@Slf4j
@Singleton
public class DebugDynamicConfigSource extends AbstractDynamicConfigSource implements ConfigEventSink<String>
{
    private static final int CONFIG_SOURCE_PRIORITY_DEFAULT = 0;

    @Inject
    protected DebugDynamicConfigSource(ConfigDescriptorHolder configDescriptorHolder)
    {
        super(configDescriptorHolder.configDescriptors);
    }

    /**
     * Returns an method-identifying proxy of the given config interface, used within a call to {@link #set(Object)}
     * to identify the method for which its value is to be set or cleared.
     *
     * @param configInterface the config interface to be proxied
     * @param <C>             the config interface class
     * @return the method-identifying proxy
     */
    public <C> C id(final Class<C> configInterface)
    {
        return MethodIdProxyFactory.getProxy(configInterface);
    }

    /**
     * Returns an method-identifying proxy of the given config interface, used within a call to {@link #set(Object)}
     * to identify the method for which its value is to be set or cleared.
     *
     * @param configInterface the config interface to be proxied
     * @param scopeNameOpt    the optional scope to identify the particular config value to modify
     * @param <C>             the config interface class
     * @return the method-identifying proxy
     */
    public <C> C id(final Class<C> configInterface, final Optional<String> scopeNameOpt)
    {
        return MethodIdProxyFactory.getProxy(configInterface, scopeNameOpt);
    }

    /**
     * Provides the start of a call chain which identifies and sets a configuration value. Note that this needs to be
     * used in conjunction with a method call to a method-identifying proxy, which can be retrieved via
     * {@link #id(Class)}
     *
     * @param ignoredValueFromProxy The value returned by the method call against a method-identifying proxy. The actual
     *                              value here is irrelevant and is ignored.
     * @param <V>                   The value type of the configuration entry to be changed
     * @return a {@link DebugValueSetter} instance that will change the identified configuration value.
     */
    public <V> DebugValueSetter<V> set(final V ignoredValueFromProxy)
    {
        final MethodIdProxyFactory.MethodAndScope lastProxyMethodAndScope = MethodIdProxyFactory.getLastIdentifiedMethodAndScope();
        if (lastProxyMethodAndScope == null) {
            throw new ConfigException("Failed to identify config method previous to calling overrideDefault");
        }

        final Optional<ConfigDescriptor> configDescOpt = configDescriptors.stream()
            .filter(desc -> desc.getMethod().equals(lastProxyMethodAndScope.getMethod()) && desc.getScope().equals(lastProxyMethodAndScope.getScopeOpt()))
            .findAny();
        final String configKey = configDescOpt.map(desc -> desc.getConfigName()).orElseThrow(
            () -> new ConfigException("Config method identified is not correctly registered in the config system"));
        final Class<?> configClass = getClass(configDescOpt.get().getConfigType());

        return new DebugValueSetter<V>()
        {
            @Override
            public void toValue(final V value)
            {
                final String stringValue;

                if (configClass != null && Collection.class.isAssignableFrom(configClass)) {
                    final Collection collection = (Collection) value;
                    stringValue = collection == null ? "" : (String) collection.stream().collect(Collectors.joining(","));
                }
                else {
                    stringValue = value == null ? "" : String.valueOf(value);
                }
                fireEvent(configKey, Optional.ofNullable(stringValue));
            }

            @Override
            public void toEmpty()
            {
                fireEvent(configKey, Optional.empty());
            }
        };
    }

    public interface DebugValueSetter<V>
    {
        void toValue(V value);

        void toEmpty();
    }

    /**
     * A raw handle to cause this ConfigSource to be updated to the new value, emitting an event if it is different
     * than the previous value.
     * It is recommended to instead use {@link #set(Object)} to provide a type-safe value rather than using this method
     * directly in tests.
     *
     * @param configName the string name of the configuration value to be fired
     * @param valueOpt   an Optional String value of the config value to provide to the system. Optional.empty()
     *                   implies that the value has been "unset" for the purposes of this config source.
     */
    @Override
    public void fireEvent(String configName, Optional<String> valueOpt) throws ConfigException
    {
        if (!subjectMap.containsKey(configName)) {
            throw new ConfigException("Unknown configName {}", configName);
        }
        emitEvent(configName, valueOpt);
    }

    private static Class<?> getClass(Type type)
    {
        if (type instanceof Class) {
            return (Class) type;
        }
        else if (type instanceof ParameterizedType) {
            return getClass(((ParameterizedType) type).getRawType());
        }
        else {
            return null;
        }
    }

    public static Module module()
    {
        return module(CONFIG_SOURCE_PRIORITY_DEFAULT);
    }

    public static Module module(final int configSourcePriority)
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                MapBinder<Integer, DynamicConfigSource> mapBinder = MapBinder.newMapBinder(binder(), Integer.class, DynamicConfigSource.class);
                mapBinder.addBinding(configSourcePriority).to(DebugDynamicConfigSource.class);
                bind(DebugDynamicConfigSource.class);
            }
        };
    }
}
