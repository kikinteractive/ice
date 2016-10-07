package com.kik.config.ice.internal;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.kik.config.ice.ConfigConfigurator;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.annotations.DefaultValue;
import java.lang.ref.WeakReference;
import static org.junit.Assert.assertEquals;
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
    public void testNoRefsToInjector() throws Exception
    {
        Injector injector = Guice.createInjector(
            ConfigConfigurator.testModules(),
            ConfigSystem.configModule(Config.class));

        assertEquals(123, injector.getInstance(Config.class).myValue());

        WeakReference<Injector> injectorRef = new WeakReference<>(injector);
        injector = null; // Just to be specific

        for (int i = 0; i < 50 && injectorRef.get() != null; i++) {

            System.gc();
            Thread.sleep(50);
            System.runFinalization();
            System.gc();
        }

        assertNull(injectorRef.get());
    }
}
