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
package com.kik.config.ice;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.util.Modules;
import com.google.inject.util.Types;
import com.kik.config.ice.exception.ConfigException;
import com.kik.config.ice.internal.ConfigBuilder;
import com.kik.config.ice.internal.ConfigDescriptor;
import com.kik.config.ice.internal.ConfigDescriptorFactory;
import com.kik.config.ice.internal.OverrideModule;
import com.kik.config.ice.internal.PropertyAccessor;
import com.kik.config.ice.internal.annotations.PropertyIdentifier;
import com.kik.config.ice.internal.annotations.PropertyIdentifierImpl;
import com.kik.config.ice.naming.ConfigNamingStrategy;
import com.kik.config.ice.naming.SimpleConfigNamingStrategy;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Central location for tools and static configurations for the ConfigSystem.
 * See {@link ConfigBuilder} for more details on config interfaces and implementations thereof.
 */
@Slf4j
@Singleton
public class ConfigSystem
{
    public static final ConfigNamingStrategy namingStrategy = new SimpleConfigNamingStrategy();
    public static final ConfigDescriptorFactory descriptorFactory = new ConfigDescriptorFactory(namingStrategy);

    @Inject(optional = true)
    private Set<ConfigDescriptor> allConfigDescriptors;

    @Inject
    private Injector injector;

    /**
     * Generates a Guice Module for use with Injector creation. The generated Guice Module binds a number of support
     * classes to service a dynamically generated implementation of the provided configuration interface. See
     * {@link ConfigBuilder} for more information.
     *
     * @param <C>             The configuration interface type to be implemented
     * @param configInterface The configuration interface
     * @return a module to install in your Guice Injector
     */
    public static <C> Module configModule(final Class<C> configInterface)
    {
        checkNotNull(configInterface);
        return ConfigBuilder.configModule(configInterface, Optional.empty());
    }

    /**
     * Generates a Guice Module for use with Injector creation. The generated Guice Module binds a number of support
     * classes to service a dynamically generated implementation of the provided configuration interface. See
     * {@link ConfigBuilder} for more information.
     *
     * @param <C>             The configuration interface type to be implemented
     * @param configInterface The configuration interface
     * @param name            Named annotation to provide an arbitrary scope to the configuration interface. Used when
     *                        there are multiple implementations of the config interface.
     * @return a module to install in your Guice Injector
     */
    public static <C> Module configModule(final Class<C> configInterface, Named name)
    {
        checkNotNull(configInterface);
        checkNotNull(name);
        return ConfigBuilder.configModule(configInterface, Optional.of(name));
    }

    /**
     * Generates a Guice Module for use with Injector creation. THe generate Guice module binds a number of support
     * classes to service a dynamically generate implementation of the provided configuration interface.
     * This method further overrides the annotated defaults on the configuration class as per the code in
     * the given overrideConsumer
     *
     * @param <C>              The configuration interface type to be implemented
     * @param configInterface  The configuration interface
     * @param overrideConsumer a lambda which is given an instance of {@link OverrideModule} which can be used to
     *                         build type-safe overrides for the default configuration of the config.
     * @return a module to install in your Guice Injector
     */
    public static <C> Module configModuleWithOverrides(final Class<C> configInterface, OverrideConsumer<C> overrideConsumer)
    {
        checkNotNull(configInterface);
        checkNotNull(overrideConsumer);
        return configModuleWithOverrides(configInterface, Optional.empty(), Optional.ofNullable(overrideConsumer));
    }

    /**
     * Generates a Guice Module for use with Injector creation. The generate Guice module binds a number of support
     * classes to service a dynamically generate implementation of the provided configuration interface.
     * This method further overrides the annotated defaults on the configuration class as per the code in
     * the given overrideConsumer
     *
     * @param <C>              The configuration interface type to be implemented
     * @param configInterface  The configuration interface
     * @param name             Named annotation to provide an arbitrary scope to the configuration interface. Used when
     *                         there are multiple implementations of the config interface.
     * @param overrideConsumer a lambda which is given an instance of {@link OverrideModule} which can be used to
     *                         build type-safe overrides for the default configuration of the config.
     * @return a module to install in your Guice Injector
     */
    public static <C> Module configModuleWithOverrides(final Class<C> configInterface, Named name, OverrideConsumer<C> overrideConsumer)
    {
        checkNotNull(configInterface);
        checkNotNull(name);
        checkNotNull(overrideConsumer);
        return configModuleWithOverrides(configInterface, Optional.ofNullable(name), Optional.ofNullable(overrideConsumer));
    }

    private static <C> Module configModuleWithOverrides(final Class<C> configInterface, final Optional<Named> nameOpt, final Optional<OverrideConsumer<C>> overrideOpt)
    {
        checkNotNull(configInterface);
        checkNotNull(nameOpt);
        checkNotNull(overrideOpt);

        if (overrideOpt.isPresent()) {
            return Modules.override(ConfigBuilder.configModule(configInterface, nameOpt))
                .with(new OverrideModule<C>(configInterface, nameOpt.map(n -> n.value()))
                {
                    @Override
                    protected void configure()
                    {
                        overrideOpt.get().accept(this);
                    }
                });
        }
        else {
            return ConfigBuilder.configModule(configInterface, nameOpt);
        }
    }

    /**
     * Generates a Guice Module for use with Injector creation. The module created is intended to be used in a
     * <code>Modules.override</code> manner, to produce overrides for static configuration.
     *
     * @param configInterface  The configuration interface type for which values are to be overridden
     * @param overrideConsumer a lambda which is given an instance of {@link OverrideModule} which can be used to build
     *                         type-safe overrides
     * @param <C>              The configuration interface type
     * @return a module to install in your Guice Injector positioned to override an earlier module created via
     *         {@link #configModule(Class)}
     */
    public static <C> Module overrideModule(
        final Class<C> configInterface,
        final OverrideConsumer<C> overrideConsumer)
    {
        return overrideModule(configInterface, Optional.empty(), overrideConsumer);
    }

    /**
     * Generates a Guice Module for use with Injector creation. The module created is intended to be used in a
     * <code>Modules.override</code> manner, to produce overrides for static configuration.
     *
     * @param configInterface  The configuration interface type for which values are to be overridden
     * @param name             Named annotation to provide an arbitrary scope for the configuration interface.
     *                         Needs to match the corresponding name used in the ConfigModule being overridden
     * @param overrideConsumer a lambda which is given an instance of {@link OverrideModule} which can be used to build
     *                         type-safe overrides
     * @param <C>              The configuration interface type
     * @return a module to install in your Guice Injector positioned to override an earlier module created via
     *         {@link #configModule(Class, Named)}
     */
    public static <C> Module overrideModule(
        final Class<C> configInterface,
        final Named name,
        final OverrideConsumer<C> overrideConsumer)
    {
        return overrideModule(configInterface, Optional.of(name), overrideConsumer);
    }

    private static <C> Module overrideModule(
        final Class<C> configInterface,
        final Optional<Named> nameOpt,
        final OverrideConsumer<C> overrideConsumer)
    {
        checkNotNull(configInterface);
        checkNotNull(nameOpt);
        checkNotNull(overrideConsumer);

        return new OverrideModule<C>(configInterface, nameOpt.map(n -> n.value()))
        {
            @Override
            protected void configure()
            {
                overrideConsumer.accept(this);
            }
        };
    }

    /**
     * Validates all static configurations (i.e. strings in your @DefaultValue annotations) which are found in the
     * injector in which ConfigSystem was retrieved from.
     * Intended to be used both in unit tests as well as in the initialization logic of your application immediately
     * after the Guice Injector has been created. It is essentially a sanity check of all static configurations.
     *
     * @throws ConfigException if Guice had issues provisioning property accessors
     */
    public void validateStaticConfiguration()
    {
        int failedConfigCount = 0;

        if (allConfigDescriptors == null) {
            log.warn("No config descriptors found. If you don't have any configurations installed, this warning can be ignored");
            return;
        }

        for (ConfigDescriptor desc : allConfigDescriptors) {
            try {
                log.trace("Checking static config for property {}, with default value of {}",
                    desc.getConfigName(), desc.getDefaultValue());
                PropertyIdentifier propertyId = getIdentifier(desc);
                TypeLiteral<PropertyAccessor<?>> accessorKey =
                    (TypeLiteral<PropertyAccessor<?>>) TypeLiteral.get(
                        Types.newParameterizedType(PropertyAccessor.class, desc.getConfigType()));
                Provider<PropertyAccessor<?>> propertyAccessor = injector.getProvider(Key.get(accessorKey, propertyId));

                propertyAccessor.get();
            }
            catch (ProvisionException ex) {
                ++failedConfigCount;
                log.warn("Failed static config check for property {}", desc.getConfigName(), ex);
            }
        }

        if (failedConfigCount > 0) {
            throw new ConfigException("{} of {} configuration values failed static config checks",
                failedConfigCount,
                allConfigDescriptors.size());
        }
    }

    /**
     * Generate a {@link PropertyIdentifier} annotation for use with Guice configuration-related bindings
     *
     * @param descriptor a {@link ConfigDescriptor} generated via {@link ConfigDescriptorFactory}
     * @return a PropertyIdentifier for use with Guice bindings
     */
    public static PropertyIdentifier getIdentifier(final ConfigDescriptor descriptor)
    {
        checkNotNull(descriptor);
        return new PropertyIdentifierImpl(descriptor.getConfigName(), descriptor.getMethod().getDeclaringClass());
    }

    /**
     * Interface used to provide lambdas to the config override mechanism.
     *
     * @param <C> The config interface type whose default values are being overridden
     */
    @FunctionalInterface
    public interface OverrideConsumer<C>
    {
        void accept(OverrideModule<C> overrideModule);
    }

    public static Module module()
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bind(ConfigSystem.class);
            }
        };
    }
}
