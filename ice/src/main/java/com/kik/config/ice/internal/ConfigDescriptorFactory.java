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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.util.Types;
import com.kik.config.ice.annotations.NoDefaultValue;
import com.kik.config.ice.naming.ConfigNamingStrategy;
import com.kik.config.ice.annotations.DefaultValue;
import com.kik.config.ice.exception.ConfigException;
import com.kik.config.ice.internal.annotations.None;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;

/**
 * Constructs a {@link ConfigDescriptor} based on the given config interface and injected state.
 */
@Slf4j
@Singleton
public class ConfigDescriptorFactory
{
    private static final ImmutableMap<Class<?>, Class<?>> primitiveToBoxed;

    static {
        ImmutableMap.Builder<Class<?>, Class<?>> builder = ImmutableMap.builder();
        builder.put(byte.class, Byte.class);
        builder.put(char.class, Character.class);
        builder.put(short.class, Short.class);
        builder.put(int.class, Integer.class);
        builder.put(long.class, Long.class);
        builder.put(float.class, Float.class);
        builder.put(double.class, Double.class);
        builder.put(boolean.class, Boolean.class);
        builder.put(void.class, Void.class);

        primitiveToBoxed = builder.build();
    }

    private final ConfigNamingStrategy namingStrategy;

    @Inject
    public ConfigDescriptorFactory(ConfigNamingStrategy namingStrategy)
    {
        this.namingStrategy = namingStrategy;
    }

    /**
     * Build a {@link ConfigDescriptor} given a configuration interface reference.
     *
     * @param configClass config interface to build descriptors for
     * @param scopeOpt    optional scope name to include in config descriptors.
     * @return A list of {@link ConfigDescriptor} instances describing the given config interface and scope name.
     */
    public List<ConfigDescriptor> buildDescriptors(Class<?> configClass, Optional<String> scopeOpt)
    {
        if (!StaticConfigHelper.isValidConfigInterface(configClass)) {
            // condition is already logged.
            throw new ConfigException("Invalid Configuration class.");
        }

        List<ConfigDescriptor> descriptors = Lists.newArrayList();

        for (Method method : configClass.getMethods()) {
            StaticConfigHelper.MethodValidationState validationState = StaticConfigHelper.isValidConfigInterfaceMethod(method);
            switch (validationState) {
                case OK:
                    descriptors.add(internalBuildDescriptor(method, scopeOpt, getMethodDefaultValue(method)));
                case IS_DEFAULT:
                    // Skip default interface methods
                    break;
                default:
                    log.debug("Configuration class {} was found to be invalid: {}",
                        configClass.getName(), validationState.name());
                    throw new ConfigException("Invalid Configuration class: {}", validationState.name());
            }
        }

        return descriptors;
    }

    /**
     * Build a {@link ConfigDescriptor} for a specific Method, and given optional scope.
     *
     * @param method   method to include in config descriptor
     * @param scopeOpt optional scope for the config descriptor
     * @return a {@link ConfigDescriptor} for the given method, to be used internally in the config system.
     */
    public ConfigDescriptor buildDescriptor(Method method, Optional<String> scopeOpt)
    {
        return buildDescriptor(method, scopeOpt, Optional.empty());
    }

    /**
     * Build a {@link ConfigDescriptor} for a specific Method, given optional scope, and given override value.
     *
     * @param method        method to include in config descriptor
     * @param scopeOpt      optional scope for the config descriptor
     * @param overrideValue optional override value used to override the static value in the config descriptor
     * @return a {@link ConfigDescriptor} for the given method, to be used internally in the config system.
     */
    public ConfigDescriptor buildDescriptor(Method method, Optional<String> scopeOpt, Optional<String> overrideValue)
    {
        checkNotNull(method);
        checkNotNull(scopeOpt);
        checkNotNull(overrideValue);
        StaticConfigHelper.MethodValidationState validationState = StaticConfigHelper.isValidConfigInterfaceMethod(method);
        if (validationState != StaticConfigHelper.MethodValidationState.OK) {
            log.debug("Configuration class {} was found to be invalid: {}",
                method.getDeclaringClass().getName(), validationState.name());
            throw new ConfigException("Invalid Configuration class: {}", validationState.name());
        }

        Optional<String> value = getMethodDefaultValue(method);
        if (overrideValue.isPresent()) {
            value = overrideValue;
        }

        return internalBuildDescriptor(method, scopeOpt, value);
    }

    private ConfigDescriptor internalBuildDescriptor(Method method, Optional<String> scopeOpt, Optional<String> defaultValue)
    {
        String configName = namingStrategy.methodToFlatName(method, scopeOpt);

        DefaultValue dv = method.getAnnotation(DefaultValue.class);
        NoDefaultValue ndv = method.getAnnotation(NoDefaultValue.class);
        Class<?> innerClass = null;
        if (dv != null && dv.innerType() != None.class) {
            innerClass = dv.innerType();
        }
        else if (ndv != null && ndv.innerType() != None.class) {
            innerClass = ndv.innerType();
        }

        // Only using the base return type to determine isObservable. Validation is ensuring the other requirements.
        boolean isObservable = Observable.class.isAssignableFrom(method.getReturnType());

        Type configType = innerClass == null
            ? ensureBoxedType(method.getReturnType())
            : Types.newParameterizedType(ensureBoxedType(method.getReturnType()), innerClass);

        return new ConfigDescriptor(method, configName, configType, isObservable, scopeOpt, defaultValue);
    }

    private Class<?> ensureBoxedType(Class<?> input)
    {
        if (!input.isPrimitive()) {
            return input;
        }
        return primitiveToBoxed.get(input);
    }

    private Optional<String> getMethodDefaultValue(Method method)
    {
        DefaultValue defaultValue = method.getAnnotation(DefaultValue.class);
        return defaultValue == null
            ? Optional.empty()
            : Optional.ofNullable(Strings.emptyToNull(defaultValue.value()));
    }
}
