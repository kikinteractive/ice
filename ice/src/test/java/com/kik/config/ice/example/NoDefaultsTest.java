package com.kik.config.ice.example;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kik.config.ice.ConfigConfigurator;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class NoDefaultsTest
{
    @Inject
    NoDefaultsExample example;

    @Test(timeout = 5000)
    public void testExample1() throws Exception
    {
        Injector injector = Guice.createInjector(
            ConfigConfigurator.testModules(),
            NoDefaultsExample.module());
        injector.injectMembers(this);

        assertNotNull(example);
        assertNotNull(example.config);

        assertNull(example.config.timeout());

        assertNotNull(example.config.delayTime());
        assertFalse(example.config.delayTime().isPresent());

        assertNotNull(example.config.enableMaybe());
        assertTrue(example.config.enableMaybe().isPresent());
        assertTrue(example.config.enableMaybe().get());
    }
}
