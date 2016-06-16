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

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import com.kik.config.ice.convert.ConfigValueConverter;
import com.kik.config.ice.interceptor.ConfigValueInterceptor;
import com.kik.config.ice.internal.annotations.PropertyIdentifier;
import com.kik.config.ice.source.DynamicConfigSource;
import static java.util.Comparator.comparing;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Supplier;
import static java.util.stream.Collectors.toList;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Subscription;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

@Slf4j
public class PropertyAccessor<T> implements Supplier<T>
{
    private final String propertyName;
    private final T defaultValue;
    private final ConfigValueConverter<T> convertFunc;
    private final List<DynamicConfigSource> dynamicAccessors;
    private final List<ConfigValueInterceptor> configValueInterceptors;
    private final List<Observable<ConfigChangeEvent<String>>> dynamicObservables;
    private final List<Subscription> subscriptions;
    private final AtomicReferenceArray<Optional<T>> overrides;
    private final AtomicReference<T> lastValueEmitted;
    private final Subject<T, T> propertySubject;
    private final Object lock = new Object();
    private final ConfigDescriptor configDescriptor;

    @Inject
    public PropertyAccessor(
        Injector injector,
        PropertyIdentifier propertyIdentifier,
        ConstantValuePropertyAccessor defaultValueAccessor,
        ConfigValueConverter<T> convertFunc,
        Map<Integer, DynamicConfigSource> dynamicAccessors,
        Map<Integer, ConfigValueInterceptor> configValueInterceptorMap)
    {
        this.propertyName = propertyIdentifier.propertyName();
        this.configDescriptor = injector.getInstance(Key.get(ConfigDescriptor.class, Names.named(this.propertyName)));
        this.convertFunc = convertFunc;
        this.dynamicAccessors = dynamicAccessors.entrySet().stream()
            .sorted(comparing(entry -> entry.getKey()))
            .map(entry -> entry.getValue())
            .collect(toList());
        this.configValueInterceptors = configValueInterceptorMap.entrySet().stream()
            .sorted(comparing(entry -> entry.getKey()))
            .map(entry -> entry.getValue())
            .collect(toList());

        Optional<String> rawDefaultValue = applyInterceptors(defaultValueAccessor.getRawValue().map(obj -> (T) obj).map(val -> val.toString()));
        if (rawDefaultValue.isPresent()) {
            this.defaultValue = convertFunc.apply(rawDefaultValue.get());
        }
        else {
            this.defaultValue = convertFunc.apply(applyInterceptors(defaultValueAccessor.getValue()).orElse(null));
        }

        log.trace("Initializing default for {}.  Value is {}", propertyIdentifier.propertyName(), defaultValue);

        this.lastValueEmitted = new AtomicReference<>(null);
        this.overrides = new AtomicReferenceArray<>(this.dynamicAccessors.size());
        for (int idx = 0; idx < this.dynamicAccessors.size(); ++idx) {
            this.overrides.set(idx, Optional.empty());
        }

        this.propertySubject = BehaviorSubject.create(this.defaultValue).toSerialized();

        this.dynamicObservables = this.dynamicAccessors.stream()
            .map(acc -> acc.getObservable(this.propertyName))
            .collect(toList());

        this.subscriptions = Lists.newArrayListWithCapacity(this.dynamicObservables.size());
        for (int idx = 0; idx < this.dynamicObservables.size(); ++idx) {
            final int overrideIndex = idx;
            this.subscriptions.add(this.dynamicObservables.get(idx).subscribe(evt -> this.onConfigEvent(overrideIndex, evt)));
            log.debug("Property {} subscribed to source {}", propertyIdentifier.propertyName(), this.dynamicAccessors.get(idx).getName());
        }
    }

    private Optional<String> applyInterceptors(final Optional<String> inputOpt)
    {
        Optional<String> valueOpt = inputOpt;
        for (ConfigValueInterceptor interceptor : this.configValueInterceptors) {
            if (!interceptor.shouldApply(this.configDescriptor.getMethod(), inputOpt)) {
                continue;
            }
            valueOpt = interceptor.apply(valueOpt);
            if (interceptor.stopChainOnApply()) {
                break;
            }
        }
        return valueOpt;
    }

    private void onConfigEvent(int eventOverrideIdx, ConfigChangeEvent<String> event)
    {
        T prevValue;
        T newValue;

        Optional<T> incomingValue;
        try {
            incomingValue = applyInterceptors(event.getValueOpt()).map(convertFunc);
        }
        catch (Exception ex) {
            log.warn("Failed to convert value for {}.  Value from {} was '{}'",
                propertyName,
                dynamicAccessors.get(eventOverrideIdx).getClass().getName(),
                event.getValueOpt().toString(),
                ex);
            return;
        }

        synchronized (lock) {
            this.overrides.set(eventOverrideIdx, incomingValue);

            log.trace("EVENT for {}, on Idx {} ({}) incomingValue {}",
                propertyName,
                eventOverrideIdx, dynamicAccessors.get(eventOverrideIdx),
                incomingValue.toString());

            newValue = getFirstOverride().orElse(this.defaultValue);
            prevValue = this.lastValueEmitted.getAndSet(newValue);
        }

        if (!Objects.equals(prevValue, newValue)) {
            log.trace("EMIT {}, value {} (was {})",
                propertyName,
                newValue == null ? "NULL" : newValue.toString(),
                prevValue == null ? "NULL" : prevValue.toString());
            this.propertySubject.onNext(newValue);
        }
    }

    private Optional<T> getFirstOverride()
    {
        for (int idx = 0; idx < overrides.length(); ++idx) {
            Optional<T> override = overrides.get(idx);
            if (override.isPresent()) {
                log.trace("Got First override for {} - idx {} ({}), value {}",
                    propertyName, idx, dynamicAccessors.get(idx), override.get());
                return override;
            }
        }
        log.trace("First Override for {} - No available overrides ", propertyName);
        return Optional.empty();
    }

    @Override
    public T get()
    {
        return this.propertySubject.toBlocking().first();
    }

    public Observable<T> getObservable()
    {
        return this.propertySubject;
    }

    public static <C> PrivateModule module(final PropertyIdentifier propertyIdentifier, final ConfigDescriptor desc)
    {
        return new PrivateModule()
        {
            @Override
            protected void configure()
            {
                // Alias the private module scoped bindings for the un-annotated ConstantValuePropertyAccessor and PropertyIdentifier to globally scoped and annotated ones.
                // Intent here is to make the implementation of this nice and guicified but support a number of different instances.
                bind(ConstantValuePropertyAccessor.class).to(Key.get(ConstantValuePropertyAccessor.class, propertyIdentifier));
                bind(PropertyIdentifier.class).to(Key.get(PropertyIdentifier.class, propertyIdentifier));

                TypeLiteral<PropertyAccessor<C>> accessorType = (TypeLiteral<PropertyAccessor<C>>) TypeLiteral.get(Types.newParameterizedType(PropertyAccessor.class, desc.getConfigType()));
                bind(Key.get(accessorType, propertyIdentifier)).to(accessorType).in(Scopes.SINGLETON);
                expose(Key.get(accessorType, propertyIdentifier));
            }
        };
    }
}
