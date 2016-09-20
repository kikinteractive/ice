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
import com.kik.config.ice.exception.ConfigException;
import com.kik.config.ice.internal.annotations.PropertyIdentifier;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

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
    }

    /**
     * Setup an override binding builder for a configuration interface.
     *
     * @param <T>               The type returned by the method being overridden.
     * @param proxiedMethodCall technically the null or default value returned by the proxy used to identify the method
     *                          call made. this parameter value is ignored.
     * @return an anonymous implementation of {@link DefaultOverride} which will create the Guice bind when its
     * appropriate method is called.
     */
    public <T> DefaultOverride<T> override(final T proxiedMethodCall)
    {
        final MethodIdProxyFactory.MethodAndScope lastProxyMethodAndScope = MethodIdProxyFactory.getLastIdentifiedMethodAndScope();
        if (lastProxyMethodAndScope == null) {
            throw new ConfigException("Failed to identify config method previous to calling overrideDefault");
        }
        if (!lastProxyMethodAndScope.getScopeOpt().equals(scopeNameOpt)) {
            throw new ConfigException("Identified config method is not using proxy for the same scope name. Scope expected: {}, from last proxy call: {}",
                scopeNameOpt, lastProxyMethodAndScope.getScopeOpt());
        }

        final ConfigDescriptor desc = ConfigSystem.descriptorFactory.buildDescriptor(lastProxyMethodAndScope.getMethod(), scopeNameOpt);
        final PropertyIdentifier propertyId = ConfigSystem.getIdentifier(desc);

        return value -> {
            // Override Binding of ConstantValuePropertyAccessor
            ConstantValuePropertyAccessor cvProp = ConstantValuePropertyAccessor.fromRawValue(value);
            OverrideModule.this.bind(ConstantValuePropertyAccessor.class).annotatedWith(propertyId).toInstance(cvProp);

            log.trace("Bound default override on method {}.{} scope {} to {}",
                lastProxyMethodAndScope.getMethod().getDeclaringClass().getName(),
                lastProxyMethodAndScope.getMethod().getName(),
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
        return MethodIdProxyFactory.getProxy(configInterface, scopeNameOpt);
    }
}
