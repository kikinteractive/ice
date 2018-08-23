package com.kik.config.ice.source;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kik.config.ice.ConfigConfigurator;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.annotations.DefaultValue;
import com.kik.config.ice.exception.ConfigException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class DebugDynamicConfigSourceTest
{

    public interface Config1
    {
        @DefaultValue("false")
        boolean enabled();

        @DefaultValue("123")
        long timeout();

        @DefaultValue("a test string")
        String connectionString();
    }

    public interface Config2
    {
        @DefaultValue("PT1H")
        Duration expiry();

        @DefaultValue(value = "alice,bob", innerType = String.class)
        Set<String> stringSet();

        @DefaultValue(value = "x,y", innerType = String.class)
        List<String> stringList();

        // TODO: Fix Generics
        // @DefaultValue("abc");
        // Optional<String> foo();
    }

    /**
     * This interface is intentionally NOT configured in Guice
     */
    public interface InvalidConfig
    {
        @DefaultValue("true")
        boolean enabled();
    }

    @Before
    public void setup()
    {
        Injector injector = Guice.createInjector(new AbstractModule()
        {
            @Override
            protected void configure()
            {
                install(ConfigConfigurator.testModules());
                install(ConfigSystem.configModule(Config1.class));
                install(ConfigSystem.configModule(Config2.class));
            }
        });

        injector.injectMembers(this);
    }

    @Inject
    private DebugDynamicConfigSource dcs;

    @Inject
    private Config1 c1;

    @Inject
    private Config2 c2;

    @Test(timeout = 5_000)
    public void testDebugController()
    {
        // Check basic Config1 changes
        assertEquals(false, c1.enabled());
        assertEquals(123L, c1.timeout(), 123L);
        assertEquals("a test string", c1.connectionString());

        // NOTE: the identifying proxy is kept in a variable and re-used here, but keeping this in-line is a reasonable
        // and expected use case.
        Config1 c1Proxy = dcs.id(Config1.class);
        dcs.set(c1Proxy.enabled()).toValue(true);
        dcs.set(c1Proxy.timeout()).toValue(10_000L);

        assertEquals(true, c1.enabled());
        assertEquals(10_000L, c1.timeout());

        // Check the new method id proxy is the same instance
        // direct reference comparison is intended here
        assertTrue(c1Proxy == dcs.id(Config1.class));

        // Check clearing of Config1 values
        dcs.set(dcs.id(Config1.class).enabled()).toEmpty();
        assertEquals(false, c1.enabled());

        dcs.set(dcs.id(Config1.class).timeout()).toEmpty();
        assertEquals(123L, c1.timeout());

        dcs.set(dcs.id(Config1.class).connectionString()).toEmpty();
        assertEquals("a test string", c1.connectionString());

        dcs.fireEvent("com.kik.config.ice.source.DebugDynamicConfigSourceTest$Config1.connectionString", Optional.of("Foo"));
        assertEquals("Foo", c1.connectionString());

        // Check basic Config2 changes
        assertEquals(Duration.ofHours(1), c2.expiry());
        assertTrue(c2.stringSet().containsAll(Sets.newHashSet("alice", "bob")));
        assertTrue(c2.stringList().containsAll(Lists.newArrayList("x", "y")));

        Config2 c2Proxy = dcs.id(Config2.class);
        dcs.set(c2Proxy.expiry()).toValue(Duration.ofSeconds(222));

        assertEquals(Duration.ofSeconds(222), c2.expiry());

        dcs.set(dcs.id(Config2.class).stringSet()).toValue(Sets.newHashSet("jim", ",judy,jasper", "jacob"));
        assertEquals(3, c2.stringSet().size());
        assertTrue(c2.stringSet().containsAll(Sets.newHashSet("jim", ",judy,jasper", "jacob")));

        dcs.set(dcs.id(Config2.class).stringList()).toValue(Lists.newArrayList("a", "\"b1,b2\",b3,", "c"));
        assertEquals(3, c2.stringList().size());
        assertTrue(c2.stringList().containsAll(Lists.newArrayList("a", "\"b1,b2\",b3,", "c")));

        // Clearing Config2 values
        dcs.set(c2Proxy.expiry()).toEmpty();
        assertEquals(Duration.ofHours(1), c2.expiry());

        dcs.set(dcs.id(Config2.class).stringSet()).toEmpty();
        assertTrue(c2.stringSet().containsAll(Sets.newHashSet("alice", "bob")));

        dcs.set(dcs.id(Config2.class).stringList()).toEmpty();
        assertTrue(c2.stringList().containsAll(Lists.newArrayList("x", "y")));
    }

    @Test(timeout = 5_000, expected = ConfigException.class)
    public void testNoMethodId()
    {
        // No call on a method ID proxy prior to calling .set()
        dcs.set(0L).toValue(123L);
    }

    @Test(timeout = 5_000, expected = ConfigException.class)
    public void testNoMethodIdFromFireEvent()
    {
        dcs.fireEvent("com.kik.config.ice.source.DebugDynamicConfigSourceTest$Config1.connectionStringXXX", Optional.of("wont work"));
    }

    @Test(timeout = 5_000, expected = ConfigException.class)
    public void testInvalidIdentification()
    {
        // InvalidConfig.class is not configured in guice, so you can't specify it as a configuration to change
        dcs.set(dcs.id(InvalidConfig.class)).toEmpty();
    }
}
