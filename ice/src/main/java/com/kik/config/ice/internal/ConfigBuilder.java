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

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.annotations.DefaultValue;
import com.kik.config.ice.annotations.NoDefaultValue;
import com.kik.config.ice.convert.ConfigValueConverter;
import com.kik.config.ice.exception.ConfigException;
import com.kik.config.ice.internal.annotations.PropertyIdentifier;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * Static builder class for configuration interfaces. {@link #configModule(java.lang.Class, java.util.Optional)}
 * dynamically constructs a class to implement an application configuration interface.
 * <br>
 * The configuration interface is expected to have at least one valid method with the following features:
 * <ul>
 * <li>Not a default method</li>
 * <li>No arguments</li>
 * <li>A return type that has a corresponding {@link ConfigValueConverter} in Guice. This includes most basic object
 * types
 * such as String, Integer, Long, Boolean, and Duration</li>
 * <li>Annotated with either {@link DefaultValue} or {@link NoDefaultValue}. If there are no defaults for all config
 * methods in the class, {@link NoDefaultValue} can be applied at the interface level instead.
 * </ul>
 * <br>
 * The resulting module will bind the following with Guice:
 * <ul>
 * <li>A set of {@link ConfigDescriptor} generated from the given config interface</li>
 * <li>A {@link PropertyIdentifier} based on ConfigDescriptors
 * <li>A {@link ConstantValuePropertyAccessor} which provides the default value</li>
 * <li>A {@link PropertyAccessor} used by the dynamic implementation class</li>
 * <li>The dynamic implementation class is bound to the given configuration interface</li>
 * </ul>
 * <br>
 * Overrides can be accomplished via the {@link OverrideModule}, which has a Mockito-style API.
 */
@Slf4j
public class ConfigBuilder
{
    /**
     * Intended for use by {@link ConfigSystem} only.
     *
     * @param <C>             the config interface class being configured
     * @param configInterface a reference to the interface class being configured
     * @param nameOpt         an optional {@link Named} used to provide a scope for the generated config module
     * @return a Guice Module which provides all the needed bindings to support an injection of the config interface
     *         provided
     */
    public static <C> Module configModule(final Class<C> configInterface, final Optional<Named> nameOpt)
    {
        checkNotNull(configInterface);
        checkNotNull(nameOpt);
        final Optional<String> nameStrOpt = nameOpt.map(Named::value);

        // Build config descriptors for the given config interface
        final List<ConfigDescriptor> configDescList = ConfigSystem.descriptorFactory.buildDescriptors(configInterface, nameStrOpt);

        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                // Create Implementation Builder
                DynamicType.Builder<C> typeBuilder = new ByteBuddy().subclass(configInterface);

                Multibinder<ConfigDescriptor> multiBinder = Multibinder.newSetBinder(binder(), ConfigDescriptor.class);

                // Define a field to keep a local reference to the list of propertyAccessorProviders on the dynamic
                // instance so the provideres dont get GCed. The providers are supplied to the InvocationHandlerImpl
                // using a WeakReference so the generated (static) code will not have a strong reference to the injector
                // which will cause a memory leak.
                String propertyAccessorProvidersFieldName = "propertyAccessorProviders$" + ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
                typeBuilder = typeBuilder.defineField(propertyAccessorProvidersFieldName, Collection.class, Visibility.PRIVATE);
                ImmutableList.Builder<Provider<PropertyAccessor<?>>> propertyAccessorProviders = ImmutableList.builder();

                for (ConfigDescriptor desc : configDescList) {
                    // Bind the propertyIdentifier
                    final PropertyIdentifier propertyId = ConfigSystem.getIdentifier(desc);
                    bind(PropertyIdentifier.class).annotatedWith(propertyId).toInstance(propertyId);

                    Provider<PropertyAccessor<?>> accessorProvider;
                    if (desc.isObservable()) {
                        // find associated method descriptor for observable
                        ConfigDescriptor otherDesc = findAssociatedDescForObservable(configDescList, desc);
                        final PropertyIdentifier otherPropertyId = ConfigSystem.getIdentifier(otherDesc);

                        // Get accessorProvider of the associated method for use in the configuration method implementation
                        accessorProvider = getAccessorProvider(otherDesc, otherPropertyId);
                    }
                    else {
                        // Bind the named ConfigDescriptor
                        bind(ConfigDescriptor.class).annotatedWith(Names.named(desc.getConfigName())).toInstance(desc);

                        // MultiBind ConfigDescriptor so we can later inject Set<ConfigDescriptor>
                        multiBinder.addBinding().toInstance(desc);

                        // Bind the static default value and propertyIdentifier
                        bind(ConstantValuePropertyAccessor.class).annotatedWith(propertyId).toInstance(ConstantValuePropertyAccessor.fromStringOpt(desc.getDefaultValue()));

                        // Bind the PropertyAccessor
                        install(PropertyAccessor.module(propertyId, desc));

                        // Get accessorProvider for use in the configuration method implementation
                        accessorProvider = getAccessorProvider(desc, propertyId);
                    }
                    propertyAccessorProviders.add(accessorProvider);

                    // Register method implementation in the class builder
                    // Wrap the accessorProvider in a WeakReference before giving it to ByteBuddy to avoid a reference
                    // to the injector making its way into the class loader which results in a memory leak.
                    typeBuilder = typeBuilder.method(ElementMatchers.is(desc.getMethod())).intercept(InvocationHandlerAdapter.of(new InvocationHandlerImpl(desc, new WeakReference<>(accessorProvider))));
                }

                Class<? extends C> configImpl = typeBuilder.make()
                    .load(configInterface.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded();

                // Bind Config Interface to an instance of the newly created impl class
                try {
                    C instance = (C) configImpl.newInstance();

                    Field propertyAccessorProvidersField = instance.getClass().getDeclaredField(propertyAccessorProvidersFieldName);
                    if (!propertyAccessorProvidersField.isAccessible()) {
                        propertyAccessorProvidersField.setAccessible(true);
                    }
                    propertyAccessorProvidersField.set(instance, propertyAccessorProviders.build());

                    if (nameOpt.isPresent()) {
                        bind(configInterface).annotatedWith(nameOpt.get()).toInstance(instance);
                    }
                    else {
                        bind(configInterface).toInstance(instance);
                    }
                }
                catch (InstantiationException | IllegalAccessException | NoSuchFieldException | SecurityException ex) {
                    throw new ConfigException("Failed to instantiate implementation of Config {}",
                        configInterface.getName(), ex);
                }
            }

            private Provider<PropertyAccessor<?>> getAccessorProvider(ConfigDescriptor desc, PropertyIdentifier propertyId)
            {
                TypeLiteral<PropertyAccessor<?>> accessorKey =
                    (TypeLiteral<PropertyAccessor<?>>) TypeLiteral.get(
                        Types.newParameterizedType(PropertyAccessor.class, desc.getConfigType()));
                return getProvider(Key.get(accessorKey, propertyId));
            }
        };
    }

    private static class InvocationHandlerImpl implements InvocationHandler
    {
        private final ConfigDescriptor desc;
        /**
         * It is incredibly important that we do not hold a strong reference to the Provider otherwise a reference to
         * the underlying injector gets into the classloader and results in a memory leak.
         */
        private final WeakReference<Provider<PropertyAccessor<?>>> accessorProviderRef;

        public InvocationHandlerImpl(ConfigDescriptor desc, WeakReference<Provider<PropertyAccessor<?>>> accessorProviderRef)
        {
            this.desc = desc;
            this.accessorProviderRef = accessorProviderRef;
        }

        @Override
        public Object invoke(Object proxy, Method method1, Object[] args)
        {
            log.debug("InvocationHandler invoking for method {} proxy {}, argCount {}", method1.getName(), proxy.toString(), args.length);
            Object value;

            if (desc.isObservable()) {
                value = accessorProviderRef.get().get().getObservable();
                log.debug("Invoked method {} returning Observable", method1.getName());
            }
            else {
                value = accessorProviderRef.get().get().get();
                log.debug("Invoked method {} returning value {}", method1.getName(), value == null ? "null" : value.toString());
            }
            return value;
        }
    }

    private static ConfigDescriptor findAssociatedDescForObservable(List<ConfigDescriptor> descList, ConfigDescriptor obsDesc)
    {
        final String obsDescName = obsDesc.getMethod().getName();
        final String nameToFind = obsDescName.substring(0, obsDescName.length() - StaticConfigHelper.OBSERVABLE_METHOD_SUFFIX.length());
        log.debug("Looking for associated Descriptor named {}", nameToFind);
        return descList.stream()
            .filter(d -> d.getMethod().getName().equals(nameToFind))
            .findFirst()
            .get(); // validation allows us to assume this is present.
    }
}
