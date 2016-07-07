package com.kik.config.ice.source;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kik.config.ice.ConfigConfigurator;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class FileDynamicConfigSourceNoConfigDescriptorsTest
{
    @Inject
    private FileDynamicConfigSource source;

    @Test(timeout = 5000)
    public void testNoConfigDescriptors()
    {
        Injector createInjector = Guice.createInjector(ConfigConfigurator.standardModules(), FileDynamicConfigSource.module());
        createInjector.injectMembers(this);

        assertNotNull(source);
    }
}
