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

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.kik.config.ice.exception.ConfigException;
import com.kik.config.ice.internal.ConfigChangeEvent;
import com.kik.config.ice.internal.ConfigDescriptor;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collector;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

/**
 * Base class that implements much of the shared plumbing for all DynamicConfigSource implementations
 */
@Slf4j
public abstract class AbstractDynamicConfigSource implements DynamicConfigSource
{
    protected final ImmutableList<ConfigDescriptor> configDescriptors;
    protected final ConcurrentMap<String, Optional<String>> lastEmittedValues;
    protected final ImmutableMap<String, Subject<ConfigChangeEvent<String>, ConfigChangeEvent<String>>> subjectMap;

    protected AbstractDynamicConfigSource(Collection<ConfigDescriptor> configDescriptors)
    {
        if (configDescriptors == null) {
            configDescriptors = Collections.emptySet();
        }

        if (configDescriptors.isEmpty()) {
            log.warn("No config descriptors found. If you don't have any configurations installed, this warning can be ignored.");
        }

        this.configDescriptors = configDescriptors.stream()
            .sorted(Comparator.comparing(desc -> desc.getConfigName()))
            .collect(toImmutableList());

        this.lastEmittedValues = Maps.newConcurrentMap();

        // Initialize lastEmittedValues and subjectMap for each descriptor
        ImmutableMap.Builder mapBuilder = ImmutableMap.builder();
        for (ConfigDescriptor desc : configDescriptors) {
            BehaviorSubject<ConfigChangeEvent<String>> behaviorSubject = BehaviorSubject.create();
            behaviorSubject.onNext(new ConfigChangeEvent<>(desc.getConfigName(), Optional.empty()));
            mapBuilder.put(desc.getConfigName(), behaviorSubject.toSerialized());
            lastEmittedValues.put(desc.getConfigName(), Optional.empty());
        }
        subjectMap = mapBuilder.build();

        log.debug("Finished constructing Rx.Subjects for {} configuration keys", configDescriptors.size());
    }

    @Override
    public Observable<ConfigChangeEvent<String>> getObservable(String configName)
    {
        if (!subjectMap.containsKey(configName)) {
            throw new ConfigException("Unknown configName {}", configName);
        }
        return subjectMap.get(configName);
    }

    protected void emitEvent(String configKey, Optional<String> valueOpt)
    {
        emitEvent(new ConfigChangeEvent<>(configKey, valueOpt));
    }

    protected void emitEvent(ConfigChangeEvent<String> event)
    {
        checkNotNull(event);

        final Optional<String> oldEventValue = this.lastEmittedValues.put(event.getName(), event.getValueOpt());
        if (!event.getValueOpt().equals(oldEventValue)) {
            Subject<ConfigChangeEvent<String>, ConfigChangeEvent<String>> subject = subjectMap.get(event.getName());
            if (subject == null) {
                log.warn("Event Subject was not initialized for key {} !", event.getName());
                return;
            }
            log.trace("EMIT {} - value {}", event.getName(), event.getValueOpt());
            subject.onNext(event);
        }
        else {
            log.trace("NOT EMITTING key {} value {} - no change from previous value.", event.getName(), event.getValueOpt());
        }
    }

    private static <T> Collector<T, ImmutableList.Builder<T>, ImmutableList<T>> toImmutableList()
    {
        return Collector.of(
            ImmutableList::builder,
            (acc, item) -> acc.add(item),
            (acc1, acc2) -> acc1.addAll(acc2.build()),
            acc -> acc.build());
    }
}
