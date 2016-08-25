package com.kik.config.ice.source;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kik.config.ice.exception.ConfigException;
import com.kik.config.ice.internal.ConfigDescriptor;
import com.kik.config.ice.naming.ConfigNamingStrategy;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import static java.util.stream.Collectors.toSet;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

@Slf4j
@Singleton
public class DebugController
{
    @Inject
    private DebugDynamicConfigSource debugConfigSource;

    @Inject
    private ConfigNamingStrategy namingStrategy;

    @Inject(optional = true)
    public Set<ConfigDescriptor> configDescriptors;

    private final ThreadLocal<Method> lastIdentifiedMethod = new ThreadLocal();

    private final ConcurrentMap<Class<?>, Object> proxyMap = Maps.newConcurrentMap();

    /**
     * Protected controller to ensure it is only created via Guice
     */
    @Inject
    protected DebugController()
    {

    }

    public <C> C id(final Class<C> configInterface)
    {
        return id(configInterface, Optional.empty());
    }

    public <C> C id(final Class<C> configInterface, final Optional<String> scopeNameOpt)
    {
        final C methodIdProxy;
        if (proxyMap.containsKey(configInterface)) {
            methodIdProxy = (C) proxyMap.get(configInterface);
        }
        else {
            methodIdProxy = createMethodIdProxy(configInterface, scopeNameOpt);
            proxyMap.put(configInterface, methodIdProxy);
        }
        return methodIdProxy;
    }

    private <C> C createMethodIdProxy(Class<C> interfaceToProxy, Optional<String> scopeNameOpt)
    {
        Set<ConfigDescriptor> descriptorSet = configDescriptors.stream()
            .filter(cd -> interfaceToProxy.equals(cd.getMethod().getDeclaringClass()))
            .filter(cd -> scopeNameOpt.equals(cd.getScope()))
            .collect(toSet());

        DynamicType.Builder<C> typeBuilder = new ByteBuddy().subclass(interfaceToProxy);
        for (ConfigDescriptor desc : descriptorSet) {
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

    public <V> DebugValueSetter<V> set(final V ignoredValueFromProxy)
    {
        final Method lastProxyMethodCalled = lastIdentifiedMethod.get();
        if (lastProxyMethodCalled == null) {
            throw new ConfigException("Failed to identify config method previous to calling overrideDefault");
        }
        // Clear lastIdentifiedMethod after each use
        lastIdentifiedMethod.set(null);

        final Optional<String> scopeNameOpt = Optional.empty();
        final String configKey = namingStrategy.methodToFlatName(lastProxyMethodCalled, scopeNameOpt);

        return new DebugValueSetter<V>()
        {
            @Override
            public void toValue(final V value)
            {
                final String stringValue = value == null ? "" : String.valueOf(value);
                debugConfigSource.fireEvent(configKey, Optional.ofNullable(stringValue));
            }

            @Override
            public void toEmpty()
            {
                debugConfigSource.fireEvent(configKey, Optional.empty());
            }
        };
    }

    public interface DebugValueSetter<V>
    {
        void toValue(V value);

        void toEmpty();
    }
}
