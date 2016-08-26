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

import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.kik.config.ice.exception.ConfigException;
import com.kik.config.ice.internal.ConfigChangeEvent;
import com.kik.config.ice.internal.ConfigDescriptor;
import com.kik.config.ice.internal.ConfigDescriptorHolder;
import com.kik.config.ice.internal.MethodIdProxyFactory;
import com.kik.config.ice.sink.ConfigEventSink;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.subjects.BehaviorSubject;

@Slf4j
@Singleton
public class DebugDynamicConfigSource extends AbstractDynamicConfigSource implements ConfigEventSink<String>
{
    private static final int CONFIG_SOURCE_PRIORITY_DEFAULT = 0;

    private final Set<ConfigDescriptor> validDescriptors;

    @Inject
    protected DebugDynamicConfigSource(ConfigDescriptorHolder configDescriptorHolder)
    {
        super(Collections.emptySet());
        validDescriptors = configDescriptorHolder.configDescriptors;
    }

    @Override
    public Observable<ConfigChangeEvent<String>> getObservable(String configName)
    {
        initializeConfigValue(configName);
        return subjectMap.get(configName);
    }

    public <C> C id(final Class<C> configInterface)
    {
        return MethodIdProxyFactory.getProxy(configInterface);
    }

    public <C> C id(final Class<C> configInterface, final Optional<String> scopeNameOpt)
    {
        return MethodIdProxyFactory.getProxy(configInterface, scopeNameOpt);
    }

    public <V> DebugValueSetter<V> set(final V ignoredValueFromProxy)
    {
        final MethodIdProxyFactory.MethodAndScope lastProxyMethodAndScope = MethodIdProxyFactory.getLastIdentifiedMethodAndScope();
        if (lastProxyMethodAndScope == null) {
            throw new ConfigException("Failed to identify config method previous to calling overrideDefault");
        }

        final Optional<ConfigDescriptor> configDescOpt = validDescriptors.stream()
            .filter(desc -> desc.getMethod().equals(lastProxyMethodAndScope.getMethod()) && desc.getScope().equals(lastProxyMethodAndScope.getScopeOpt()))
            .findAny();
        final String configKey = configDescOpt.map(desc -> desc.getConfigName()).orElseThrow(
            () -> new ConfigException("Config method identified is not correctly registered in the config system"));

        return new DebugValueSetter<V>()
        {
            @Override
            public void toValue(final V value)
            {
                final String stringValue = value == null ? "" : String.valueOf(value);
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
    public void fireEvent(String configName, Optional<String> valueOpt)
    {
        initializeConfigValue(configName);
        emitEvent(configName, valueOpt);
    }

    private void initializeConfigValue(String name)
    {
        if (Strings.isNullOrEmpty(name) || subjectMap.containsKey(name)) {
            return;
        }
        BehaviorSubject<ConfigChangeEvent<String>> behaviorSubject = BehaviorSubject.create();
        behaviorSubject.onNext(new ConfigChangeEvent<>(name, Optional.empty()));
        subjectMap.put(name, behaviorSubject.toSerialized());
        lastEmittedValues.put(name, Optional.empty());
        log.trace("Initialized Config Value '{}'", name);
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
            }
        };
    }
}
