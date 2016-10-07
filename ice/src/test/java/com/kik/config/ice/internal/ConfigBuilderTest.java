package com.kik.config.ice.internal;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.kik.config.ice.ConfigConfigurator;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.annotations.DefaultValue;
import java.lang.ref.WeakReference;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ConfigBuilderTest
{
    private static final Logger log = LoggerFactory.getLogger(ConfigBuilderTest.class);

    public interface Config
    {
        @DefaultValue("123")
        int myValue();
    }

    /**
     * Check to ensure that references to the injector do not make it into the class loader which results in a memory
     * leak.
     */
    @Test(timeout = 10_000)
    public void testNoStaticRefsToInjector() throws Exception
    {
        Injector injector = Guice.createInjector(
            ConfigConfigurator.testModules(),
            ConfigSystem.configModule(Config.class));

        assertEquals(123, injector.getInstance(Config.class).myValue());

        WeakReference<Injector> injectorRef = new WeakReference<>(injector);
        injector = null; // Just to be sure

        // Calling System.gc() does not guarantee it gets run - try a bunch of times
        for (int i = 0; i < 10 && injectorRef.get() != null; i++) {
            System.gc();
            Thread.sleep(10);
            System.runFinalization();
            System.gc();
        }

        assertNull(injectorRef.get());
    }

    @Test(timeout = 10_000)
    public void testRefToInjectorWithClass() throws Exception
    {
        Injector injector = Guice.createInjector(
            ConfigConfigurator.testModules(),
            ConfigSystem.configModule(Config.class));

        Config config = injector.getInstance(Config.class);
        assertEquals(123, config.myValue());

        WeakReference<Injector> injectorRef = new WeakReference<>(injector);
        injector = null; // Just to be sure

        System.gc();
        Thread.sleep(50);
        System.runFinalization();
        System.gc();

        assertNotNull(injectorRef.get());

        // Keep the config ref around
        assertEquals(123, config.myValue());

        WeakReference<Config> configRef = new WeakReference<>(config);
        config = null;

        for (int i = 0; i < 10 && (injectorRef.get() != null || configRef.get() != null); i++) {

            System.gc();
            Thread.sleep(50);
            System.runFinalization();
            System.gc();
        }

        assertNull(injectorRef.get());
        assertNull(configRef.get());
    }
}
