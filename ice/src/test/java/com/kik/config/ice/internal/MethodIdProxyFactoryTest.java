package com.kik.config.ice.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.kik.config.ice.ConfigConfigurator;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.ExplicitBindingModule;
import com.kik.config.ice.annotations.DefaultValue;
import com.kik.config.ice.exception.ConfigException;
import com.kik.config.ice.internal.MethodIdProxyFactory.MethodAndScope;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

public class MethodIdProxyFactoryTest
{
    private static final String NAME_A = "name_a";
    private static final String NAME_B = "name_b";

    public interface Config1
    {
        @DefaultValue("123")
        int maxSize();

        @DefaultValue("false")
        boolean enabled();
    }

    public interface Config2
    {
        @DefaultValue("PT1D")
        Duration expiry();
    }

    public interface BadConfig1
    {
        int notAnnotated();
    }

    public interface NoMethodBadConfig2
    {
    }

    @Before
    public void setup()
    {
        Injector injector = Guice.createInjector(new AbstractModule()
        {
            @Override
            protected void configure()
            {
                install(new ExplicitBindingModule());
                install(ConfigConfigurator.testModules());
                install(ConfigSystem.configModule(Config1.class));
                install(ConfigSystem.configModule(Config2.class, Names.named(NAME_A)));
                install(ConfigSystem.configModule(Config2.class, Names.named(NAME_B)));
            }
        });

        injector.injectMembers(this);
    }

    @Test(timeout = 5_000)
    public void testProxyBasics() throws Exception
    {
        // get each proxy
        Config1 proxy1 = MethodIdProxyFactory.getProxy(Config1.class);
        Config2 proxy2a = MethodIdProxyFactory.getProxy(Config2.class, Optional.of(NAME_A));
        Config2 proxy2b = MethodIdProxyFactory.getProxy(Config2.class, Optional.of(NAME_B));

        assertNotNull(proxy1);
        assertNotNull(proxy2a);
        assertNotNull(proxy2b);
        // Intentionally use address comparison
        assertFalse(proxy2a == proxy2b);

        // get each proxy a second time, and ensure they're the same instance
        assertEquals(proxy1, MethodIdProxyFactory.getProxy(Config1.class));
        assertEquals(proxy2a, MethodIdProxyFactory.getProxy(Config2.class, Optional.of(NAME_A)));
        assertEquals(proxy2b, MethodIdProxyFactory.getProxy(Config2.class, Optional.of(NAME_B)));

        // Ensure initial identified MethodAndScope (MAS) is null
        assertNull(MethodIdProxyFactory.getLastIdentifiedMethodAndScope());

        // Check that non-named proxy call is found
        proxy1.enabled();
        MethodAndScope mas = MethodIdProxyFactory.getLastIdentifiedMethodAndScope();
        assertNotNull(mas);
        assertEquals(getProxyMethod(Config1.class, "enabled"), mas.getMethod());
        assertEquals(Optional.empty(), mas.getScopeOpt());

        // Check that second call to getLastIdentifiedMethodAndScope returns null
        assertNull(MethodIdProxyFactory.getLastIdentifiedMethodAndScope());

        // Check Named proxy calls work
        proxy2a.expiry();
        mas = MethodIdProxyFactory.getLastIdentifiedMethodAndScope();
        assertNotNull(mas);
        assertNull(MethodIdProxyFactory.getLastIdentifiedMethodAndScope());
        assertEquals(getProxyMethod(Config2.class, "expiry"), mas.getMethod());
        assertEquals(Optional.of(NAME_A), mas.getScopeOpt());

        proxy2b.expiry();
        mas = MethodIdProxyFactory.getLastIdentifiedMethodAndScope();
        assertNotNull(mas);
        assertNull(MethodIdProxyFactory.getLastIdentifiedMethodAndScope());
        assertEquals(getProxyMethod(Config2.class, "expiry"), mas.getMethod());
        assertEquals(Optional.of(NAME_B), mas.getScopeOpt());
    }

    @Test(timeout = 5_000, expected = ConfigException.class)
    public void testBadConfig1()
    {
        MethodIdProxyFactory.getProxy(BadConfig1.class);
    }

    @Test(timeout = 5_000, expected = ConfigException.class)
    public void testBadConfig2()
    {
        MethodIdProxyFactory.getProxy(NoMethodBadConfig2.class);
    }

    private Method getProxyMethod(final Class<?> proxyClass, final String methodName) throws NoSuchMethodException
    {
        return proxyClass.getMethod(methodName);
    }
}
