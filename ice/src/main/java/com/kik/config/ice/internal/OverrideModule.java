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
import com.google.inject.AbstractModule;
import com.google.inject.name.Named;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.internal.annotations.PropertyIdentifier;
import com.kik.config.ice.exception.ConfigException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * Creates a Module which allows overriding specific config interface method defaults. This Module must be set up in
 * the Guice setup to override the module created via {@link ConfigBuilder}, and must be given the same scope name.
 * <br>
 * Overrides are defined by creating an anonymous subclass of this module, and using the protected methods herein.
 * <br>
 * Example usage within the anonymous subclass' configure method:<br>
 * <pre>
 *     override(id().myConfigMethod()).withValue(100);
 * </pre><ul>
 * <li>{@link #override(java.lang.Object)} starts up the override binding builder</li>
 * <li>{@link #id()} provides a dynamic proxy of the config interface to identify the method to be overridden. A method
 * from this proxy must be executed immediately before the override method is executed.</li>
 * <li>{@link DefaultOverride#withValue(java.lang.Object)} provides the override value and creates the guice
 * binding.</li>
 * </ul>
 *
 * @param <C> the configuration interface for which defaults are being overridden
 */
@Slf4j
public abstract class OverrideModule<C> extends AbstractModule
{
    private final Class<C> configInterface;
    private final Optional<String> scopeNameOpt;
    private final ThreadLocal<Method> lastIdentifiedMethod;
    private C methodIdProxy = null;

    //<editor-fold defaultstate="collapsed" desc="Interfaces used for chained override binding">
    public interface DefaultOverride<T>
    {
        void withValue(T value);
    }
    //</editor-fold>

    public OverrideModule(Class<C> configInterface)
    {
        this(configInterface, Optional.empty());
    }

    public OverrideModule(Class<C> configInterface, Named name)
    {
        this(configInterface, Optional.ofNullable(name).map(Named::value));
    }

    public OverrideModule(Class<C> configInterface, Optional<String> scopeOpt)
    {
        this.configInterface = configInterface;
        this.scopeNameOpt = checkNotNull(scopeOpt);
        this.lastIdentifiedMethod = new ThreadLocal<>();
    }

    /**
     * Setup an override binding builder for a configuration interface.
     *
     * @param <T>               The type returned by the method being overridden.
     * @param proxiedMethodCall technically the null or default value returned by the proxy used to identify the method
     *                          call made. this parameter value is ignored.
     * @return an anonymous implementation of {@link DefaultOverride} which will create the Guice bind when its
     *         appropriate method is called.
     */
    public <T> DefaultOverride<T> override(final T proxiedMethodCall)
    {
        final Method lastProxyMethodCalled = lastIdentifiedMethod.get();
        if (lastProxyMethodCalled == null) {
            throw new ConfigException("Failed to identify config method previous to calling overrideDefault");
        }

        final ConfigDescriptor desc = ConfigSystem.descriptorFactory.buildDescriptor(lastProxyMethodCalled, scopeNameOpt);
        final PropertyIdentifier propertyId = ConfigSystem.getIdentifier(desc);

        return value -> {
            // Override Binding of ConstantValuePropertyAccessor
            ConstantValuePropertyAccessor cvProp = ConstantValuePropertyAccessor.fromRawValue(value);
            OverrideModule.this.bind(ConstantValuePropertyAccessor.class).annotatedWith(propertyId).toInstance(cvProp);

            log.trace("Bound default override on method {}.{} scope {} to {}",
                lastProxyMethodCalled.getDeclaringClass().getName(),
                lastProxyMethodCalled.getName(),
                scopeNameOpt,
                value.toString());
        };
    }

    /**
     * Provides a proxy implementation of the config interface. Method calls on this object always return
     * null (or default value if primitive), and mark the last method called to allow
     * {@link #override(java.lang.Object)}
     * to identify which method is to be overridden.
     *
     * @return an identifying proxy implementation to be used with {@link #override(java.lang.Object)}.
     */
    public C id()
    {
        if (methodIdProxy == null) {
            methodIdProxy = createMethodIdProxy(configInterface, scopeNameOpt);
        }
        return methodIdProxy;
    }

    private C createMethodIdProxy(Class<C> interfaceToProxy, Optional<String> scopeNameOpt)
    {
        final List<ConfigDescriptor> configDescList = ConfigSystem.descriptorFactory.buildDescriptors(interfaceToProxy, scopeNameOpt);
        DynamicType.Builder<C> typeBuilder = new ByteBuddy().subclass(interfaceToProxy);
        for (ConfigDescriptor desc : configDescList) {
            typeBuilder = typeBuilder.method(ElementMatchers.is(desc.getMethod())).intercept(InvocationHandlerAdapter.of((Object proxy, Method method1, Object[] args) -> {
                log.trace("BB InvocationHandler identifying method {} proxy {}, argCount {}", method1.getName(), proxy.toString(), args.length);
                lastIdentifiedMethod.set(method1);
                return defaultForType(desc.getMethod().getReturnType());
            }));
        }

        Class<? extends C> configImpl = typeBuilder.make()
            .load(interfaceToProxy.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
            .getLoaded();
        try {
            return configImpl.newInstance();
        }
        catch (InstantiationException | IllegalAccessException ex) {
            throw new ConfigException("Failed to instantiate identification implementation of Config {} scope {}",
                interfaceToProxy.getName(), scopeNameOpt.orElse("<empty>"), ex);
        }
    }

    /**
     * Provide the default value for any given type.
     *
     * @param type Type to get the default for
     * @return default value - primitive value or null depending on the type.
     */
    private static Object defaultForType(Class<?> type)
    {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type.equals(boolean.class)) {
            return false;
        }
        if (type.equals(long.class)) {
            return (long) 0L;
        }
        if (type.equals(int.class)) {
            return (int) 0;
        }
        if (type.equals(short.class)) {
            return (short) 0;
        }
        if (type.equals(byte.class)) {
            return (byte) 0;
        }
        if (type.equals(char.class)) {
            return (char) '\u0000';
        }
        if (type.equals(float.class)) {
            return (float) 0.0f;
        }
        if (type.equals(double.class)) {
            return (double) 0.0d;
        }
        // should never happen
        throw new IllegalStateException("Unknown primitive type: " + type.getName());
    }
}
